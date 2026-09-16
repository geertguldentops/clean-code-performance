package be.guldentops.geert.ccperf.shapes.union;

import be.guldentops.geert.ccperf.shapes.ShapeConstants;

/**
 * Switch-statement operations over {@link ShapeUnion}, direct ports of
 * Listing 25 ({@code GetAreaSwitch}) and Listing 34 ({@code GetCornerCountSwitch}).
 */
public final class ShapeUnionOps {

    private ShapeUnionOps() {
    }

    public static float getAreaSwitch(ShapeUnion shape) {
        float result = 0.0f;
        switch (shape.type) {
            case SQUARE -> result = shape.width * shape.width;
            case RECTANGLE -> result = shape.width * shape.height;
            case TRIANGLE -> result = 0.5f * shape.width * shape.height;
            case CIRCLE -> result = ShapeConstants.PI32 * shape.width * shape.width;
        }
        return result;
    }

    public static int getCornerCountSwitch(ShapeType type) {
        int result = 0;
        switch (type) {
            case SQUARE -> result = 4;
            case RECTANGLE -> result = 4;
            case TRIANGLE -> result = 3;
            case CIRCLE -> result = 0;
        }
        return result;
    }
}
