package be.guldentops.geert.ccperf.shapes.soa;

import be.guldentops.geert.ccperf.shapes.union.ShapeType;

/**
 * Struct-of-arrays representation: parallel primitive arrays instead of an
 * array of objects. This is the idiomatic Java way to get a truly flattened,
 * contiguous, non-reference-indirected memory layout (Java object arrays are
 * always arrays of references, so an array-of-{@code ShapeUnion} objects does
 * not by itself reproduce the "no indirection" benefit the article measures
 * in C++ for its flattened struct). {@code types[i]} holds the ordinal of
 * {@link ShapeType} for shape {@code i}.
 */
public final class ShapeSoA {

    private final int[] types;
    private final float[] widths;
    private final float[] heights;

    public ShapeSoA(int[] types, float[] widths, float[] heights) {
        if (types.length != widths.length || types.length != heights.length) {
            throw new IllegalArgumentException("types/widths/heights must have the same length");
        }
        this.types = types;
        this.widths = widths;
        this.heights = heights;
    }

    public int size() {
        return types.length;
    }

    public int[] types() {
        return types;
    }

    public float[] widths() {
        return widths;
    }

    public float[] heights() {
        return heights;
    }
}
