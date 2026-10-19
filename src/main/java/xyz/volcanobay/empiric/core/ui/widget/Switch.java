package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;

public class Switch extends ValueWidget<Boolean> {

    private float progress;
    private float previous;

    public Switch() {
        this(false);
    }

    public Switch(boolean on) {
        super(Boolean.class, on);
        progress = previous = on ? 1 : 0;
    }

    public boolean on() {
        return Boolean.TRUE.equals(value());
    }

    public void toggle() {
        if (editable()) {
            setValue(!on());
            sound("toggle", on() ? 1.2f : 0.9f);
        }
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
        toggle();
        return true;
    }

    @Override
    protected boolean key(int key, int modifiers) {
        if (key == Keys.ENTER || key == Keys.KEYPAD_ENTER || key == Keys.SPACE) {
            toggle();
            return true;
        }
        return false;
    }

    @Override
    protected void tick() {
        previous = progress;
        float target = on() ? 1 : 0;
        progress += (target - progress) * 0.6f;
        if (Math.abs(target - progress) < 0.01f) {
            progress = target;
        }
    }

    @Override
    protected int contentWidth() {
        return 22;
    }

    @Override
    protected int contentHeight() {
        return 12;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        float t = previous + (progress - previous) * partialTick;
        int width = width();
        int height = height();
        canvas.fill(0, 0, width, height, Draw.lerp(color("track"), color("accent"), t));
        canvas.outline(0, 0, width, height, color(isFocused() ? "focus" : "widget_border"));
        int knob = height - 2;
        int x = 1 + Math.round(t * (width - 2 - knob));
        canvas.fill(x, 1, x + knob, 1 + knob, color(editable() ? "knob" : "text_dim"));
    }
}
