package be.guldentops.geert.ccperf;

import be.guldentops.geert.ccperf.shapes.union.ShapeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the assumption (relied on throughout the SoA/table-driven code and
 * the shared data factory) that {@link ShapeType} ordinals are
 * SQUARE=0, RECTANGLE=1, TRIANGLE=2, CIRCLE=3.
 */
class ShapeTypeOrdinalTest {

    @Test
    void ordinalsMatchExpectedTableIndices() {
        assertEquals(0, ShapeType.SQUARE.ordinal());
        assertEquals(1, ShapeType.RECTANGLE.ordinal());
        assertEquals(2, ShapeType.TRIANGLE.ordinal());
        assertEquals(3, ShapeType.CIRCLE.ordinal());
        assertEquals(4, ShapeType.values().length);
    }
}
