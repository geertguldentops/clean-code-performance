package be.guldentops.geert.ccperf.shapes.poly;

/** Direct port of the C++ {@code rectangle} class (Listing 22/32). */
public final class Rectangle extends Shape {

    private final float width;
    private final float height;

    public Rectangle(float width, float height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public float area() {
        return width * height;
    }

    @Override
    public int cornerCount() {
        return 4;
    }
}
