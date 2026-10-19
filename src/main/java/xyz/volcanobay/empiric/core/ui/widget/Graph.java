package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.UiCanvas;

import java.util.function.DoubleFunction;

/**
 * Plots a value over time. Each tick it samples its value, which is usually bound to a module field, into a fixed
 * history. The range is fixed or fits the history.
 */
public class Graph extends ValueWidget<Double> {

    private final double[] samples;
    private int head;
    private int count;
    private Double min;
    private Double max;
    private String line = "accent";
    private DoubleFunction<String> format = value -> String.format("%.2f", value);

    public Graph(int history) {
        super(Double.class, 0.0);
        this.samples = new double[Math.max(2, history)];
    }

    public Graph range(double min, double max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public Graph line(String key) {
        this.line = key;
        return this;
    }

    public Graph format(DoubleFunction<String> format) {
        this.format = format;
        return this;
    }

    public int samples() {
        return count;
    }

    public double sample(int age) {
        return samples[Math.floorMod(head - 1 - age, samples.length)];
    }

    public void record(double value) {
        samples[head] = value;
        head = (head + 1) % samples.length;
        count = Math.min(count + 1, samples.length);
    }

    @Override
    protected void tick() {
        record(value() == null ? 0 : value());
    }

    @Override
    protected int contentWidth() {
        return 100;
    }

    @Override
    protected int contentHeight() {
        return 40;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        int width = width();
        int height = height();
        Draw.box(canvas, theme(), "track", "widget_border", 0, 0, width, height);
        double low = min != null ? min : Double.MAX_VALUE;
        double high = max != null ? max : -Double.MAX_VALUE;
        if (min == null || max == null) {
            for (int i = 0; i < count; i++) {
                double value = sample(i);
                if (min == null) {
                    low = Math.min(low, value);
                }
                if (max == null) {
                    high = Math.max(high, value);
                }
            }
        }
        if (count == 0 || high <= low) {
            high = low + 1;
        }
        int grid = color("widget_border");
        for (int i = 1; i < 4; i++) {
            int y = height * i / 4;
            canvas.fill(1, y, width - 1, y + 1, grid & 0x60FFFFFF);
        }
        int color = color(line);
        float step = (width - 2) / (float) (samples.length - 1);
        for (int age = 0; age + 1 < count; age++) {
            float x0 = width - 1 - age * step;
            float x1 = x0 - step;
            float y0 = plot(sample(age), low, high, height);
            float y1 = plot(sample(age + 1), low, high, height);
            canvas.line(x0, y0, x1, y1, 1.2f, color);
        }
        if (count > 0) {
            canvas.text(format.apply(sample(0)), 3, 3, color("text"), true);
        }
    }

    private static float plot(double value, double low, double high, int height) {
        double t = (value - low) / (high - low);
        return (float) (height - 2 - Draw.clamp(t, 0, 1) * (height - 4));
    }
}
