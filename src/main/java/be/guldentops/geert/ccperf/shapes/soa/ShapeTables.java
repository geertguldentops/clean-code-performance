package be.guldentops.geert.ccperf.shapes.soa;

import be.guldentops.geert.ccperf.shapes.ShapeConstants;
import be.guldentops.geert.ccperf.shapes.union.ShapeType;

/**
 * Table-driven coefficients, direct ports of Listing 27 ({@code CTable}) and
 * Listing 36 ({@code CTable} for the corner-weighted area). Index order must
 * match {@link ShapeType} ordinals: SQUARE, RECTANGLE,
 * TRIANGLE, CIRCLE.
 * <p>
 * The key point from the article is reproduced here: adding the "corner
 * count" property does not change a single line of the computation code
 * (see {@link ShapeSoAOps#getAreaTable} / vector code) - only the table
 * values change.
 */
public final class ShapeTables {

    /** Listing 27: {@code CTable[Shape_Count] = {1.0f, 1.0f, 0.5f, Pi32}}. */
    public static final float[] AREA_COEFFICIENTS = {
            1.0f,               // SQUARE
            1.0f,               // RECTANGLE
            0.5f,               // TRIANGLE
            ShapeConstants.PI32 // CIRCLE
    };

    /**
     * Listing 36: {@code CTable[Shape_Count] = {1/(1+4), 1/(1+4), 0.5/(1+3), Pi32}}.
     * Corner counts: square=4, rectangle=4, triangle=3, circle=0 (so its
     * weight 1/(1+0) == 1, folded directly into the Pi32 coefficient).
     */
    public static final float[] CORNER_AREA_COEFFICIENTS = {
            1.0f / (1.0f + 4.0f),
            1.0f / (1.0f + 4.0f),
            0.5f / (1.0f + 3.0f),
            ShapeConstants.PI32
    };

    private ShapeTables() {
    }
}
