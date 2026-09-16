package be.guldentops.geert.ccperf.shapes.poly;

import be.guldentops.geert.ccperf.shapes.ShapeConstants;

/** Direct port of the C++ {@code circle} class (Listing 22/32). */
public final class Circle extends Shape {

    private final float radius;

    public Circle(float radius) {
        this.radius = radius;
    }

    @Override
    public float area() {
        return ShapeConstants.PI32 * radius * radius;
    }

    @Override
    public int cornerCount() {
        return 0;
    }
}
