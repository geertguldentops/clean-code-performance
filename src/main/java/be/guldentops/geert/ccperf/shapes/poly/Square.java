package be.guldentops.geert.ccperf.shapes.poly;

/** Direct port of the C++ {@code square} class (Listing 22/32). */
public final class Square extends Shape {

    private final float side;

    public Square(float side) {
        this.side = side;
    }

    @Override
    public float area() {
        return side * side;
    }

    @Override
    public int cornerCount() {
        return 4;
    }
}
