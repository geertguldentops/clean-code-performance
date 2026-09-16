package be.guldentops.geert.ccperf.benchmarks;

import be.guldentops.geert.ccperf.data.ShapeDataFactory;
import be.guldentops.geert.ccperf.shapes.poly.Shape;
import be.guldentops.geert.ccperf.shapes.soa.ShapeSoA;
import be.guldentops.geert.ccperf.shapes.soa.ShapeSoAOps;
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
 * Best-effort analog of the article's "cold" measurement (run once, with L2/L1
 * flushed and an untrained branch predictor). Uses JMH's
 * {@link Mode#SingleShotTime} with zero warmup and a fresh JVM fork per
 * measured invocation, so each measurement starts from an un-JIT-compiled,
 * interpreter-mode, cold-code-cache state.
 * <p>
 * <b>Important caveat:</b> this is NOT equivalent to the article's native
 * cold-cache test. In the JVM:
 * <ul>
 *   <li>A "cold" JVM fork is cold with respect to JIT compilation and class
 *       loading, not necessarily with respect to CPU L1/L2/L3 caches or the
 *       branch predictor (the OS and CPU may still have residual state from
 *       other recent activity on the machine).</li>
 *   <li>The first invocation runs in the interpreter (or minimally C1-compiled),
 *       which has completely different performance characteristics than the
 *       fully JIT-optimized code measured by {@link AreaBenchmark} /
 *       {@link CornerAreaBenchmark}. So these numbers are dominated by
 *       interpreter/compilation overhead, not by the polymorphism-vs-switch-
 *       vs-table difference alone.</li>
 * </ul>
 * Treat these results as a rough, directional sanity check only - the
 * steady-state benchmarks in {@link AreaBenchmark} are the primary evidence
 * for the article's hypotheses. See README.md for further discussion.
 * <p>
 * Run with: {@code java -jar target/benchmarks.jar ColdStartBenchmark}
 */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 0)
@Measurement(iterations = 1, batchSize = 1)
@Fork(value = 30, warmups = 0)
public class ColdStartBenchmark {

    /**
     * Kept modest on purpose: single-shot timing includes the cost of the
     * work itself, and we want a fresh-fork measurement dominated by
     * "first execution" behavior rather than by sheer data volume.
     */
    @Param({"100000"})
    public int shapeCount;

    private Shape[] polymorphic;
    private ShapeSoA soa;

    @Setup(Level.Trial)
    public void setup() {
        ShapeDataFactory.Dataset dataset = ShapeDataFactory.generate(shapeCount, ShapeDataFactory.DEFAULT_SEED);
        polymorphic = dataset.polymorphic();
        soa = dataset.soa();
    }

    @Benchmark
    public float totalArea_polymorphism_singleShot() {
        float accum = 0.0f;
        for (Shape shape : polymorphic) {
            accum += shape.area();
        }
        return accum;
    }

    @Benchmark
    public float totalArea_switch_soa_singleShot() {
        int[] types = soa.types();
        float[] widths = soa.widths();
        float[] heights = soa.heights();
        float accum = 0.0f;
        for (int i = 0; i < types.length; i++) {
            accum += ShapeSoAOps.getAreaSwitch(types[i], widths[i], heights[i]);
        }
        return accum;
    }

    @Benchmark
    public float totalArea_tableDriven_soa_singleShot() {
        int[] types = soa.types();
        float[] widths = soa.widths();
        float[] heights = soa.heights();
        float accum = 0.0f;
        for (int i = 0; i < types.length; i++) {
            accum += ShapeSoAOps.getAreaTable(types[i], widths[i], heights[i]);
        }
        return accum;
    }
}
