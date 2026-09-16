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
 * Reproduces the article's "total area of a series of shapes" comparison
 * (Listings 22-27): polymorphism vs switch-statement vs table-driven code,
 * including 4-way manually unrolled accumulators (to remove the
 * loop-carried-dependency confound, exactly as the article does) and a
 * Vector API (SIMD) variant standing in for the article's hand-optimized
 * AVX version.
 * <p>
 * Run with: {@code java -jar target/benchmarks.jar AreaBenchmark}
 * <p>
 * Report results as ns/op; divide by {@code shapeCount} to get an
 * approximate "time per shape" figure comparable to the article's
 * cycles-per-shape numbers (see README for the ns-to-cycles caveat).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = "--add-modules=jdk.incubator.vector")
public class AreaBenchmark {

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

    /** Listing 23: polymorphic virtual-call accumulation. */
    @Benchmark
    public float totalArea_polymorphism() {
        float accum = 0.0f;
        for (Shape shape : polymorphic) {
            accum += shape.area();
        }
        return accum;
    }

    /** Listing 24: same, but 4-way unrolled to remove the loop-carried dependency. */
    @Benchmark
    public float totalArea_polymorphism_x4() {
        Shape[] shapes = polymorphic;
        float accum0 = 0.0f;
        float accum1 = 0.0f;
        float accum2 = 0.0f;
        float accum3 = 0.0f;

        int count = shapes.length / 4;
        int index = 0;
        while (count-- > 0) {
            accum0 += shapes[index].area();
            accum1 += shapes[index + 1].area();
            accum2 += shapes[index + 2].area();
            accum3 += shapes[index + 3].area();
            index += 4;
        }
        return accum0 + accum1 + accum2 + accum3;
    }

    /** Listing 26: switch-statement accumulation, literal array-of-objects port. */
    @Benchmark
    public float totalArea_switch_aos() {
        float accum = 0.0f;
        for (ShapeUnion shape : unionAoS) {
            accum += ShapeUnionOps.getAreaSwitch(shape);
        }
        return accum;
    }

    /**
     * Switch-statement accumulation over the struct-of-arrays representation:
     * same branching logic as Listing 26, but over truly flattened,
     * non-reference-indirected memory (Java's analog of the C++ array of
     * value-type structs).
     */
    @Benchmark
    public float totalArea_switch_soa() {
        int[] types = soa.types();
        float[] widths = soa.widths();
        float[] heights = soa.heights();
        float accum = 0.0f;
        for (int i = 0; i < types.length; i++) {
            accum += ShapeSoAOps.getAreaSwitch(types[i], widths[i], heights[i]);
        }
        return accum;
    }

    /** Listing 27: table-driven, branch-free computation over the SoA data. */
    @Benchmark
    public float totalArea_tableDriven_soa() {
        int[] types = soa.types();
        float[] widths = soa.widths();
        float[] heights = soa.heights();
        float accum = 0.0f;
        for (int i = 0; i < types.length; i++) {
            accum += ShapeSoAOps.getAreaTable(types[i], widths[i], heights[i]);
        }
        return accum;
    }

    /** Table-driven computation, 4-way unrolled (mirrors Listing 24's unroll, applied to Listing 27). */
    @Benchmark
    public float totalArea_tableDriven_soa_x4() {
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
            accum0 += ShapeSoAOps.getAreaTable(types[index], widths[index], heights[index]);
            accum1 += ShapeSoAOps.getAreaTable(types[index + 1], widths[index + 1], heights[index + 1]);
            accum2 += ShapeSoAOps.getAreaTable(types[index + 2], widths[index + 2], heights[index + 2]);
            accum3 += ShapeSoAOps.getAreaTable(types[index + 3], widths[index + 3], heights[index + 3]);
            index += 4;
        }
        return accum0 + accum1 + accum2 + accum3;
    }

    /** SIMD / AVX analog: Java Vector API gather + FMA + reduction over the SoA data. */
    @Benchmark
    public float totalArea_vectorApi_soa() {
        return VectorAreaOps.totalArea(soa, ShapeTables.AREA_COEFFICIENTS);
    }
}
