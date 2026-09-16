package be.guldentops.geert.ccperf;

import be.guldentops.geert.ccperf.data.ShapeDataFactory;
import be.guldentops.geert.ccperf.shapes.poly.Shape;
import be.guldentops.geert.ccperf.shapes.soa.ShapeSoA;
import be.guldentops.geert.ccperf.shapes.soa.ShapeSoAOps;
import be.guldentops.geert.ccperf.shapes.soa.ShapeTables;
import be.guldentops.geert.ccperf.shapes.union.ShapeUnion;
import be.guldentops.geert.ccperf.shapes.union.ShapeUnionOps;
import be.guldentops.geert.ccperf.shapes.vector.VectorAreaOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Correctness guard: every representation (polymorphism, AoS switch, SoA
 * switch, SoA table-driven, Vector API) must compute the same total area /
 * corner-weighted area for the same seeded dataset. This exists so the
 * benchmark numbers can be trusted to be measuring equivalent work, not a
 * bug (or a JIT dead-code-eliminated no-op) in one of the variants.
 */
class ShapeRepresentationsAgreeTest {

    /**
     * Relative tolerance: floating-point sums over tens of millions of
     * elements legitimately differ in their low bits depending on
     * accumulation order (vectorized/unrolled vs sequential), so we compare
     * with a relative epsilon rather than a fixed absolute one.
     */
    private static final float RELATIVE_TOLERANCE = 1e-4f;

    private static void assertEquals(float expected, float actual, String message) {
        float allowed = Math.max(1e-3f, Math.abs(expected) * RELATIVE_TOLERANCE);
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, allowed, message);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 7, 100, 10_000})
    void allRepresentationsAgreeOnTotalArea(int count) {
        ShapeDataFactory.Dataset dataset = ShapeDataFactory.generate(count, ShapeDataFactory.DEFAULT_SEED);

        float polyTotal = totalAreaPolymorphic(dataset.polymorphic());
        float switchAoSTotal = totalAreaSwitchAoS(dataset.unionAoS());
        float switchSoATotal = totalAreaSwitchSoA(dataset.soa());
        float tableSoATotal = totalAreaTableSoA(dataset.soa());
        float vectorTotal = VectorAreaOps.totalArea(dataset.soa(), ShapeTables.AREA_COEFFICIENTS);

        assertEquals(polyTotal, switchAoSTotal, "switch-AoS should match polymorphism");
        assertEquals(polyTotal, switchSoATotal, "switch-SoA should match polymorphism");
        assertEquals(polyTotal, tableSoATotal, "table-driven should match polymorphism");
        assertEquals(polyTotal, vectorTotal, "vector API should match polymorphism");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 7, 100, 10_000})
    void allRepresentationsAgreeOnCornerWeightedArea(int count) {
        ShapeDataFactory.Dataset dataset = ShapeDataFactory.generate(count, ShapeDataFactory.DEFAULT_SEED);

        float polyTotal = cornerAreaPolymorphic(dataset.polymorphic());
        float switchAoSTotal = cornerAreaSwitchAoS(dataset.unionAoS());
        float switchSoATotal = cornerAreaSwitchSoA(dataset.soa());
        float tableSoATotal = cornerAreaTableSoA(dataset.soa());
        float vectorTotal = VectorAreaOps.totalArea(dataset.soa(), ShapeTables.CORNER_AREA_COEFFICIENTS);

        assertEquals(polyTotal, switchAoSTotal, "switch-AoS corner-area should match polymorphism");
        assertEquals(polyTotal, switchSoATotal, "switch-SoA corner-area should match polymorphism");
        assertEquals(polyTotal, tableSoATotal, "table-driven corner-area should match polymorphism");
        assertEquals(polyTotal, vectorTotal, "vector API corner-area should match polymorphism");
    }

    @Test
    void datasetGenerationIsReproducibleForSameSeed() {
        ShapeDataFactory.Dataset a = ShapeDataFactory.generate(5_000, ShapeDataFactory.DEFAULT_SEED);
        ShapeDataFactory.Dataset b = ShapeDataFactory.generate(5_000, ShapeDataFactory.DEFAULT_SEED);

        assertEquals(totalAreaPolymorphic(a.polymorphic()), totalAreaPolymorphic(b.polymorphic()), "same seed should reproduce same dataset");
    }

    private static float totalAreaPolymorphic(Shape[] shapes) {
        float accum = 0.0f;
        for (Shape shape : shapes) {
            accum += shape.area();
        }
        return accum;
    }

    private static float cornerAreaPolymorphic(Shape[] shapes) {
        float accum = 0.0f;
        for (Shape shape : shapes) {
            accum += (1.0f / (1.0f + shape.cornerCount())) * shape.area();
        }
        return accum;
    }

    private static float totalAreaSwitchAoS(ShapeUnion[] shapes) {
        float accum = 0.0f;
        for (ShapeUnion shape : shapes) {
            accum += ShapeUnionOps.getAreaSwitch(shape);
        }
        return accum;
    }

    private static float cornerAreaSwitchAoS(ShapeUnion[] shapes) {
        float accum = 0.0f;
        for (ShapeUnion shape : shapes) {
            accum += (1.0f / (1.0f + ShapeUnionOps.getCornerCountSwitch(shape.type))) * ShapeUnionOps.getAreaSwitch(shape);
        }
        return accum;
    }

    private static float totalAreaSwitchSoA(ShapeSoA shapes) {
        float accum = 0.0f;
        int[] types = shapes.types();
        float[] widths = shapes.widths();
        float[] heights = shapes.heights();
        for (int i = 0; i < shapes.size(); i++) {
            accum += ShapeSoAOps.getAreaSwitch(types[i], widths[i], heights[i]);
        }
        return accum;
    }

    private static float cornerAreaSwitchSoA(ShapeSoA shapes) {
        float accum = 0.0f;
        int[] types = shapes.types();
        float[] widths = shapes.widths();
        float[] heights = shapes.heights();
        for (int i = 0; i < shapes.size(); i++) {
            float weight = 1.0f / (1.0f + ShapeSoAOps.getCornerCountSwitch(types[i]));
            accum += weight * ShapeSoAOps.getAreaSwitch(types[i], widths[i], heights[i]);
        }
        return accum;
    }

    private static float totalAreaTableSoA(ShapeSoA shapes) {
        float accum = 0.0f;
        int[] types = shapes.types();
        float[] widths = shapes.widths();
        float[] heights = shapes.heights();
        for (int i = 0; i < shapes.size(); i++) {
            accum += ShapeSoAOps.getAreaTable(types[i], widths[i], heights[i]);
        }
        return accum;
    }

    private static float cornerAreaTableSoA(ShapeSoA shapes) {
        float accum = 0.0f;
        int[] types = shapes.types();
        float[] widths = shapes.widths();
        float[] heights = shapes.heights();
        for (int i = 0; i < shapes.size(); i++) {
            accum += ShapeSoAOps.getCornerAreaTable(types[i], widths[i], heights[i]);
        }
        return accum;
    }
}
