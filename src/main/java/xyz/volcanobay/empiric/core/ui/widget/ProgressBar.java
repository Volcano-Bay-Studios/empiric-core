package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.layout.Direction;

import java.util.function.DoubleFunction;

/**
 * Shows a value as a filled bar, left to right in a row or bottom to top in a column. It never edits its value.
 */
public class ProgressBar extends ValueWidget<Double> {

    private final double min;
    private final double max;
    private Direction direction = Direction.ROW;
    private String fill = "accent";
    private DoubleFunction<String> format;

    public ProgressBar(double min, double max) {
        super(Double.class, min);
        this.min = min;
        this.max = max;
    }

    public ProgressBar direction(Direction direction) {
        this.direction = direction;
        requestLayout();
        return this;
    }

    public ProgressBar fill(String key) {
        this.fill = key;
        return this;
    }

    public ProgressBar format(DoubleFunction<String> format) {
        this.format = format;
        requestLayout();
        return this;
    }

    public double fraction() {
        double value = value() == null ? min : value();
        return max == min ? 0 : Draw.clamp((value - min) / (max - min), 0, 1);
    }

    @Override
    protected int contentWidth() {
        return direction == Direction.ROW ? 100 : 8;
    }

    @Override
    protected int contentHeight() {
        if (direction == Direction.COLUMN) {
            return 40;
        }
        return format != null && root() != null ? font().lineHeight() + 4 : 8;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        int width = width();
        int height = height();
        Draw.box(canvas, theme(), "track", "widget_border", 0, 0, width, height);
        double fraction = fraction();
        if (direction == Direction.ROW) {
            canvas.fill(1, 1, 1 + (int) Math.round(fraction * (width - 2)), height - 1, color(fill));
        } else {
            canvas.fill(1, height - 1 - (int) Math.round(fraction * (height - 2)), width - 1, height - 1, color(fill));
        }
        if (format != null) {
            canvas.centeredText(format.apply(value() == null ? min : value()), width / 2, (height - font().lineHeight()) / 2 + 1, color("text"), true);
        }
    }
}
