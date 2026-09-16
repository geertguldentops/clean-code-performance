package be.guldentops.geert.ccperf.shapes.union;

/**
 * Direct port of the C++ {@code enum shape_type} in Listing 25. Ordinal
 * order matters: it must match the coefficient tables used by the
 * struct-of-arrays / table-driven representation.
 */
public enum ShapeType {
    SQUARE,
    RECTANGLE,
    TRIANGLE,
    CIRCLE
}
