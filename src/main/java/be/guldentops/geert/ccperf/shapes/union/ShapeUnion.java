package be.guldentops.geert.ccperf.shapes.union;

/**
 * Direct (array-of-objects) port of the C++ {@code struct shape_union} in
 * Listing 25: a flattened, single "type + width + height" record instead of
 * a class hierarchy. NOTE: in Java, an array of these is still an array of
 * *references* (there is no native contiguous value-type array pre-Valhalla),
 * so this representation does not get the full "no indirection" benefit the
 * article measures in C++. See {@code dev.ccperf.shapes.soa.ShapeSoA} for the
 * struct-of-arrays representation that reproduces that benefit in Java.
 */
public final class ShapeUnion {

    public final ShapeType type;
    public final float width;
    public final float height;

    public ShapeUnion(ShapeType type, float width, float height) {
        this.type = type;
        this.width = width;
        this.height = height;
    }
}
