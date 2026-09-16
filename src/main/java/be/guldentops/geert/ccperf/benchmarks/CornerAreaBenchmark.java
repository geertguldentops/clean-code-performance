package be.guldentops.geert.ccperf.benchmarks;

import be.guldentops.geert.ccperf.data.ShapeDataFactory;
import be.guldentops.geert.ccperf.shapes.poly.Shape;
import be.guldentops.geert.ccperf.shapes.soa.ShapeSoA;
import be.guldentops.geert.ccperf.shapes.soa.ShapeSoAOps;
import be.guldentops.geert.ccperf.shapes.soa.ShapeTables;
import be.guldentops.geert.ccperf.shapes.union.ShapeUnion;
import be.guldentops.geert.ccperf.shapes.union.ShapeUnionOps;
import be.guldentops.geert.ccperf.shapes.vector.VectorAreaOps;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Reproduces the article's second, harder comparison (Listings 32-36): adds
 * a "corner count" property to each shape and computes
 * {@code sum(1/(1+cornerCount) * area)} instead of a plain area sum. This
 * tests the hypothesis that the performance gap between "clean" and
 * table/data-driven code *grows* as more properties are added to the data
 * model - for the table-driven version, adding this property requires no
 * code changes at all, only new coefficient values (see
 * {@link ShapeTables#CORNER_AREA_COEFFICIENTS} and how it's the exact same
 * {@link VectorAreaOps#totalArea} routine used by {@link AreaBenchmark}).
 * <p>
 * Run with: {@code java -jar target/benchmarks.jar CornerAreaBenchmark}
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = "--add-modules=jdk.incubator.vector")
public class CornerAreaBenchmark {

    @Param({"1000", "100000", "10000000"})
    public int shapeCount;

    private Shape[] polymorphic;
    private ShapeUnion[] unionAoS;
    private ShapeSoA soa;

    @Setup(Level.Trial)
    public void setup() {
        ShapeDataFactory.Dataset dataset = ShapeDataFactory.generate(shapeCount, ShapeDataFactory.DEFAULT_SEED);
        polymorphic = dataset.polymorphic();
        unionAoS = dataset.unionAoS();
        soa = dataset.soa();
    }

    /** Corner-weighted equivalent of Listing 23, using the extended hierarchy of Listing 32. */
    @Benchmark
    public float cornerArea_polymorphism() {
        float accum = 0.0f;
        for (Shape shape : polymorphic) {
            accum += (1.0f / (1.0f + (float) shape.cornerCount())) * shape.area();
        }
        return accum;
    }

    /** Corner-weighted equivalent of Listing 24 (4-way unrolled). */
    @Benchmark
    public float cornerArea_polymorphism_x4() {
        Shape[] shapes = polymorphic;
        float accum0 = 0.0f;
        float accum1 = 0.0f;
        float accum2 = 0.0f;
        float accum3 = 0.0f;

        int count = shapes.length / 4;
        int index = 0;
        while (count-- > 0) {
            accum0 += (1.0f / (1.0f + (float) shapes[index].cornerCount())) * shapes[index].area();
            accum1 += (1.0f / (1.0f + (float) shapes[index + 1].cornerCount())) * shapes[index + 1].area();
            accum2 += (1.0f / (1.0f + (float) shapes[index + 2].cornerCount())) * shapes[index + 2].area();
            accum3 += (1.0f / (1.0f + (float) shapes[index + 3].cornerCount())) * shapes[index + 3].area();
            index += 4;
        }
        return accum0 + accum1 + accum2 + accum3;
    }

    /** Listings 34/35: switch-based corner count + area, literal array-of-objects port. */
    @Benchmark
    public float cornerArea_switch_aos() {
        float accum = 0.0f;
        for (ShapeUnion shape : unionAoS) {
            float weight = 1.0f / (1.0f + (float) ShapeUnionOps.getCornerCountSwitch(shape.type));
            accum += weight * ShapeUnionOps.getAreaSwitch(shape);
        }
        return accum;
    }

    /** Same switch-based logic, over the struct-of-arrays (non-indirected) representation. */
    @Benchmark
    public float cornerArea_switch_soa() {
        int[] types = soa.types();
        float[] widths = soa.widths();
        float[] heights = soa.heights();
        float accum = 0.0f;
        for (int i = 0; i < types.length; i++) {
            float weight = 1.0f / (1.0f + (float) ShapeSoAOps.getCornerCountSwitch(types[i]));
            accum += weight * ShapeSoAOps.getAreaSwitch(types[i], widths[i], heights[i]);
        }
        return accum;
    }

    /**
     * Listing 36: table-driven corner-weighted area. Note this is *exactly*
     * the same code shape as {@link AreaBenchmark#totalArea_tableDriven_soa()} -
     * only the coefficient table differs.
     */
    @Benchmark
    public float cornerArea_tableDriven_soa() {
        int[] types = soa.types();
        float[] widths = soa.widths();
        float[] heights = soa.heights();
        float accum = 0.0f;
        for (int i = 0; i < types.length; i++) {
            accum += ShapeSoAOps.getCornerAreaTable(types[i], widths[i], heights[i]);
        }
        return accum;
    }

    /** Table-driven corner-weighted area, 4-way unrolled. */
    @Benchmark
    public float cornerArea_tableDriven_soa_x4() {
        int[] types = soa.types();
        float[] widths = soa.widths();
        float[] heights = soa.heights();

        float accum0 = 0.0f;
        float accum1 = 0.0f;
        float accum2 = 0.0f;
        float accum3 = 0.0f;

        int count = types.length / 4;
        int index = 0;
        while (count-- > 0) {
            accum0 += ShapeSoAOps.getCornerAreaTable(types[index], widths[index], heights[index]);
            accum1 += ShapeSoAOps.getCornerAreaTable(types[index + 1], widths[index + 1], heights[index + 1]);
            accum2 += ShapeSoAOps.getCornerAreaTable(types[index + 2], widths[index + 2], heights[index + 2]);
            accum3 += ShapeSoAOps.getCornerAreaTable(types[index + 3], widths[index + 3], heights[index + 3]);
            index += 4;
        }
        return accum0 + accum1 + accum2 + accum3;
    }

    /**
     * SIMD / AVX analog for the corner-weighted case: the exact same
     * {@link VectorAreaOps#totalArea} routine as {@link AreaBenchmark}, just
     * given the corner-weighted coefficient table.
     */
    @Benchmark
    public float cornerArea_vectorApi_soa() {
        return VectorAreaOps.totalArea(soa, ShapeTables.CORNER_AREA_COEFFICIENTS);
    }
}
