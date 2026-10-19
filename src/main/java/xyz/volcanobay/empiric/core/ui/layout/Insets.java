package xyz.volcanobay.empiric.core.ui.layout;

public record Insets(int left, int top, int right, int bottom) {

    public static final Insets NONE = new Insets(0, 0, 0, 0);

    public static Insets of(int all) {
        return all == 0 ? NONE : new Insets(all, all, all, all);
    }

    public static Insets of(int horizontal, int vertical) {
        return new Insets(horizontal, vertical, horizontal, vertical);
    }

    public int horizontal() {
        return left + right;
    }

    public int vertical() {
        return top + bottom;
    }
}
