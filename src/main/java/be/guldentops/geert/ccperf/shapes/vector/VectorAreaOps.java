package be.guldentops.geert.ccperf.shapes.vector;

import be.guldentops.geert.ccperf.shapes.soa.ShapeSoA;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD (Java Vector API, {@code jdk.incubator.vector}) analog of the
 * article's hand-optimized AVX version. This is the natural conclusion of
 * the table-driven approach in Listings 27/36: once area/corner-area is
 * reduced to {@code coefficient * width * height}, the loop is a trivial,
 * branch-free gather + FMA + reduction that vectorizes directly - something
 * that is far harder (often practically impossible without a rewrite) to do
 * starting from a class hierarchy or a per-shape switch statement.
 * <p>
 * The same {@code totalArea} routine computes both the plain area sum and
 * the corner-weighted area sum, depending only on which coefficient table is
 * passed in - exactly mirroring the article's observation that adding the
 * "corner count" property required no code changes, only new table values.
 * <p>
 * NOTE: {@code jdk.incubator.vector} is still an incubating module as of
 * JDK 25; running/compiling code that uses it requires
 * {@code --add-modules jdk.incubator.vector} (already wired into this
 * project's pom.xml and JMH fork arguments).
 */
public final class VectorAreaOps {

    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

    private VectorAreaOps() {
    }

    public static float totalArea(ShapeSoA shapes, float[] coefficientTable) {
        final int n = shapes.size();
        final float[] widths = shapes.widths();
        final float[] heights = shapes.heights();
        final int[] types = shapes.types();

        final int upperBound = SPECIES.loopBound(n);
        FloatVector acc = FloatVector.zero(SPECIES);
        int i = 0;
        for (; i < upperBound; i += SPECIES.length()) {
            FloatVector w = FloatVector.fromArray(SPECIES, widths, i);
            FloatVector h = FloatVector.fromArray(SPECIES, heights, i);
            // Gather coefficientTable[types[i .. i+lanes)] directly into a vector.
            FloatVector c = FloatVector.fromArray(SPECIES, coefficientTable, 0, types, i);
            acc = c.mul(w).fma(h, acc);
        }

        float result = acc.reduceLanes(VectorOperators.ADD);
        for (; i < n; i++) {
            result += coefficientTable[types[i]] * widths[i] * heights[i];
        }
        return result;
    }
}
