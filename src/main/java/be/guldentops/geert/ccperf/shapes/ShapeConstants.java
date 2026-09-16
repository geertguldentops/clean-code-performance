package be.guldentops.geert.ccperf.shapes;

/**
 * Shared constants used across all shape representations (polymorphic,
 * union/switch, and struct-of-arrays), so every implementation computes
 * area with exactly the same value of pi.
 */
public final class ShapeConstants {

    /** f32 Pi32 in the original C++ listings. */
    public static final float PI32 = (float) Math.PI;

    private ShapeConstants() {
    }
}
