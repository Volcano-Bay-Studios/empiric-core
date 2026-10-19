package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;

/**
 * A rotary knob. Dragging around it turns the needle with the pointer across a 270 degree sweep, and each step it
 * crosses clicks.
 */
public class Dial extends ValueWidget<Double> {

    private static final float START = (float) (Math.PI * 0.75);
    private static final float SWEEP = (float) (Math.PI * 1.5);

    private final double min;
    private final double max;
    private final double step;
    private double raw;
    private double lastAngle = Double.NaN;

    public Dial(double min, double max, double step) {
        super(Double.class, min);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double amount() {
        return value() == null ? min : value();
    }

    public double fraction() {
        return max == min ? 0 : (amount() - min) / (max - min);
    }

    private void turnTo(double value) {
        double snapped = Draw.snap(value, min, max, step);
        if (snapped != amount()) {
            setValue(snapped);
            sound("dial", 0.8f + 0.6f * (float) fraction());
        }
    }

    private void nudge(double direction) {
        if (editable()) {
            turnTo(amount() + direction * (step > 0 ? step : (max - min) / 100));
        }
    }

    private double angle(double x, double y) {
        double dx = x - width() / 2.0;
        double dy = y - height() / 2.0;
        return dx * dx + dy * dy < 4 ? Double.NaN : Math.atan2(dy, dx);
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
        raw = amount();
        lastAngle = angle(x, y);
        return true;
    }

    @Override
    protected void pointerDrag(double x, double y, double dx, double dy, int button) {
        if (!isEditing()) {
            return;
        }
        double angle = angle(x, y);
        if (Double.isNaN(angle)) {
            return;
        }
        if (!Double.isNaN(lastAngle)) {
            double delta = angle - lastAngle;
            if (delta > Math.PI) {
                delta -= Math.PI * 2;
            } else if (delta < -Math.PI) {
                delta += Math.PI * 2;
            }
            raw = Draw.clamp(raw + delta / SWEEP * (max - min), min, max);
            turnTo(raw);
        }
        lastAngle = angle;
    }

    @Override
    protected void pointerUp(double x, double y, int button) {
        endEdit();
        lastAngle = Double.NaN;
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
        return 24;
    }

    @Override
    protected int contentHeight() {
        return 24;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        float radius = Math.min(width(), height()) / 2f;
        float x = width() / 2f;
        float y = height() / 2f;
        float value = START + SWEEP * (float) fraction();
        canvas.arc(x, y, radius * 0.72f, radius, START, START + SWEEP, color("track"));
        canvas.arc(x, y, radius * 0.72f, radius, START, value, color(editable() ? "accent" : "text_dim"));
        String knob = isPressed() ? "widget_hover" : isHovered() && editable() ? "widget_hover" : "widget";
        canvas.circle(x, y, radius * 0.62f, color(isFocused() ? "focus" : "widget_border"));
        canvas.circle(x, y, radius * 0.55f, color(knob));
        canvas.line(x, y, x + (float) Math.cos(value) * radius * 0.5f, y + (float) Math.sin(value) * radius * 0.5f, Math.max(1.5f, radius / 8), color("knob"));
    }
}
