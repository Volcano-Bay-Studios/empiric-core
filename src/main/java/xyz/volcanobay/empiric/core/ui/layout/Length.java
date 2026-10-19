package xyz.volcanobay.empiric.core.ui.layout;

/**
 * How big a widget wants to be along one axis: a fixed pixel size, its content size, or a weighted share of the free space.
 */
public record Length(Kind kind, float value) {

    public static final Length FIT = new Length(Kind.FIT, 0);
    public static final Length FILL = new Length(Kind.FILL, 1);

    public enum Kind {
        PIXELS,
        FIT,
        FILL
    }

    public static Length px(int pixels) {
        return new Length(Kind.PIXELS, pixels);
    }

    public static Length fill(float weight) {
        return weight == 1 ? FILL : new Length(Kind.FILL, weight);
    }

    public boolean fixed() {
        return kind == Kind.PIXELS;
    }

    public boolean fills() {
        return kind == Kind.FILL;
    }
}
