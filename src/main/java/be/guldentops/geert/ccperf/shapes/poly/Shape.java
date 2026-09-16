package be.guldentops.geert.ccperf.shapes.poly;

/**
 * Polymorphic base class, direct port of the C++ {@code shape_base} in
 * Listing 22/32 of the article: prefers polymorphism over switch/if-else,
 * and hides each shape's internal fields from callers.
 */
public abstract class Shape {

    public abstract float area();

    public abstract int cornerCount();
}
