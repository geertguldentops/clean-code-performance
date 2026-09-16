package be.guldentops.geert.ccperf.data;

import be.guldentops.geert.ccperf.shapes.poly.Circle;
import be.guldentops.geert.ccperf.shapes.poly.Rectangle;
import be.guldentops.geert.ccperf.shapes.poly.Shape;
import be.guldentops.geert.ccperf.shapes.poly.Square;
import be.guldentops.geert.ccperf.shapes.poly.Triangle;
import be.guldentops.geert.ccperf.shapes.soa.ShapeSoA;
import be.guldentops.geert.ccperf.shapes.union.ShapeType;
import be.guldentops.geert.ccperf.shapes.union.ShapeUnion;

import java.util.Random;

/**
 * Generates one logical random dataset of shapes and materializes it into
 * every representation used by the benchmarks (polymorphic hierarchy, AoS
 * union/switch, and SoA table-driven), so that every benchmark variant
 * operates on exactly the same shapes/dimensions. Shape types are assigned
 * uniformly at random per element (not grouped) so branch prediction cannot
 * "learn" a repeating pattern, as discussed in the article's own harness
 * design.
 * <p>
 * Following the article's Listing 27 guidance, single-parameter shapes
 * (square, circle) have their one dimension duplicated into both the
 * "width" and "height" slots, so the same {@code coefficient * width *
 * height} formula works uniformly for every shape type.
 */
public final class ShapeDataFactory {

    /** Default seed used by all benchmarks/tests for reproducible datasets. */
    public static final long DEFAULT_SEED = 42L;

    private static final float MIN_DIMENSION = 1.0f;
    private static final float MAX_DIMENSION = 100.0f;

    private ShapeDataFactory() {
    }

    public record Dataset(Shape[] polymorphic, ShapeUnion[] unionAoS, ShapeSoA soa) {
    }

    public static Dataset generate(int count, long seed) {
        Random random = new Random(seed);
        ShapeType[] allTypes = ShapeType.values();

        Shape[] polymorphic = new Shape[count];
        ShapeUnion[] unionAoS = new ShapeUnion[count];
        int[] types = new int[count];
        float[] widths = new float[count];
        float[] heights = new float[count];

        for (int i = 0; i < count; i++) {
            ShapeType type = allTypes[random.nextInt(allTypes.length)];
            float param1 = randomDimension(random);
            float param2 = switch (type) {
                case SQUARE, CIRCLE -> param1; // duplicate single dimension, per Listing 27
                case RECTANGLE, TRIANGLE -> randomDimension(random);
            };

            polymorphic[i] = switch (type) {
                case SQUARE -> new Square(param1);
                case RECTANGLE -> new Rectangle(param1, param2);
                case TRIANGLE -> new Triangle(param1, param2);
                case CIRCLE -> new Circle(param1);
            };
            unionAoS[i] = new ShapeUnion(type, param1, param2);
            types[i] = type.ordinal();
            widths[i] = param1;
            heights[i] = param2;
        }

        return new Dataset(polymorphic, unionAoS, new ShapeSoA(types, widths, heights));
    }

    private static float randomDimension(Random random) {
        return MIN_DIMENSION + random.nextFloat() * (MAX_DIMENSION - MIN_DIMENSION);
    }
}
