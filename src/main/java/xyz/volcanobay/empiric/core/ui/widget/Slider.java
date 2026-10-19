package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;

import java.util.function.DoubleFunction;

public class Slider extends ValueWidget<Double> {

    private static final int KNOB = 6;

    private final double min;
    private final double max;
    private final double step;
    private DoubleFunction<String> format;

    public Slider(double min, double max, double step) {
        super(Double.class, min);
        this.min = min;
        this.max = max;
        this.step = step;
        this.format = value -> Draw.format(value, step);
    }

    public Slider format(DoubleFunction<String> format) {
        this.format = format;
        return this;
    }

    public double amount() {
        return value() == null ? min : value();
    }

    public double fraction() {
        return max == min ? 0 : (amount() - min) / (max - min);
    }

    private void nudge(double direction) {
        if (editable()) {
            setValue(Draw.snap(amount() + direction * (step > 0 ? step : (max - min) / 100), min, max, step));
        }
    }

    private void setFromX(double x) {
        double t = Draw.clamp((x - KNOB / 2.0) / (width() - KNOB), 0, 1);
        setValue(Draw.snap(min + t * (max - min), min, max, step));
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    protected boolean interactive() {
        return true;
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        if (button != Keys.LEFT_BUTTON || !editable()) {
            return false;
        }
        beginEdit();
        setFromX(x);
        return true;
    }

    @Override
    protected void pointerDrag(double x, double y, double dx, double dy, int button) {
        if (isEditing()) {
            setFromX(x);
        }
    }

    @Override
    protected void pointerUp(double x, double y, int button) {
        endEdit();
        sound("click", 1.4f);
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        if (!editable()) {
            return false;
        }
        nudge(Math.signum(amount));
        return true;
    }

    @Override
    protected boolean key(int key, int modifiers) {
        if (key == Keys.RIGHT || key == Keys.UP) {
            nudge(1);
            return true;
        }
        if (key == Keys.LEFT || key == Keys.DOWN) {
            nudge(-1);
            return true;
        }
        return false;
    }

    @Override
    protected int contentWidth() {
        return 100;
    }

    @Override
    protected int contentHeight() {
        return root() == null ? 12 : font().lineHeight() + 6;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        int width = width();
        int height = height();
        Draw.box(canvas, theme(), "track", isFocused() ? "focus" : "widget_border", 0, 0, width, height);
        int knob = (int) Math.round(fraction() * (width - KNOB));
        canvas.fill(1, 1, knob + KNOB / 2, height - 1, Draw.lerp(color("track"), color("accent"), 0.6f));
        String fill = isPressed() ? "accent" : isHovered() && editable() ? "knob_hover" : "knob";
        canvas.fill(knob, 0, knob + KNOB, height, color(editable() ? fill : "text_dim"));
        canvas.centeredText(format.apply(amount()), width / 2, (height - font().lineHeight()) / 2 + 1, color("text"), true);
    }
}
