package be.guldentops.geert.ccperf.shapes.poly;

/** Direct port of the C++ {@code triangle} class (Listing 22/32). */
public final class Triangle extends Shape {

    private final float base;
    private final float height;

    public Triangle(float base, float height) {
        this.base = base;
        this.height = height;
    }

    @Override
    public float area() {
        return 0.5f * base * height;
    }

    @Override
    public int cornerCount() {
        return 3;
    }
}
