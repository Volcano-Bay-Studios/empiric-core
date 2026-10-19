package xyz.volcanobay.empiric.core.ui.layout;

public enum Anchor {
    TOP_LEFT(0, 0),
    TOP(0.5f, 0),
    TOP_RIGHT(1, 0),
    LEFT(0, 0.5f),
    CENTER(0.5f, 0.5f),
    RIGHT(1, 0.5f),
    BOTTOM_LEFT(0, 1),
    BOTTOM(0.5f, 1),
    BOTTOM_RIGHT(1, 1);

    private final float x;
    private final float y;

    Anchor(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }
}
