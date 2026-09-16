package be.guldentops.geert.ccperf.shapes.soa;

import be.guldentops.geert.ccperf.shapes.ShapeConstants;

/**
 * Per-shape operations over the struct-of-arrays representation.
 * <p>
 * {@code getAreaSwitch}/{@code getCornerCountSwitch} mirror Listings 25/34
 * exactly (just reading fields out of parallel arrays instead of a struct),
 * to test the "flattened switch-based data, but still branching" hypothesis.
 * <p>
 * {@code getAreaTable}/{@code getCornerAreaTable} mirror Listings 27/36: a
 * single table lookup and multiply, no branching at all.
 */
public final class ShapeSoAOps {

    // Must match dev.ccperf.shapes.union.ShapeType ordinal order exactly
    // (asserted by ShapeTypeOrdinalTest); switch case labels require true
    // compile-time constants, so plain literals are used here instead of
    // ShapeType.X.ordinal().
    private static final int SQUARE = 0;
    private static final int RECTANGLE = 1;
    private static final int TRIANGLE = 2;
    private static final int CIRCLE = 3;

    private ShapeSoAOps() {
    }

    public static float getAreaSwitch(int type, float width, float height) {
        float result = 0.0f;
        switch (type) {
            case SQUARE -> result = width * width;
            case RECTANGLE -> result = width * height;
            case TRIANGLE -> result = 0.5f * width * height;
            case CIRCLE -> result = ShapeConstants.PI32 * width * width;
            default -> throw new IllegalArgumentException("Unknown shape type: " + type);
        }
        return result;
    }

    public static int getCornerCountSwitch(int type) {
        int result;
        switch (type) {
            case SQUARE -> result = 4;
            case RECTANGLE -> result = 4;
            case TRIANGLE -> result = 3;
            case CIRCLE -> result = 0;
            default -> throw new IllegalArgumentException("Unknown shape type: " + type);
        }
        return result;
    }

    /** Listing 27: {@code GetAreaUnion}. */
    public static float getAreaTable(int type, float width, float height) {
        return ShapeTables.AREA_COEFFICIENTS[type] * width * height;
    }

    /** Listing 36: {@code GetCornerAreaUnion}. */
    public static float getCornerAreaTable(int type, float width, float height) {
        return ShapeTables.CORNER_AREA_COEFFICIENTS[type] * width * height;
    }
}
