package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;

public class Checkbox extends ValueWidget<Boolean> {

    private static final int BOX = 10;

    private String text;

    public Checkbox(String text) {
        this(text, false);
    }

    public Checkbox(String text, boolean checked) {
        super(Boolean.class, checked);
        this.text = text;
    }

    public boolean checked() {
        return Boolean.TRUE.equals(value());
    }

    public Checkbox text(String text) {
        this.text = text;
        requestLayout();
        return this;
    }

    public void toggle() {
        if (editable()) {
            setValue(!checked());
            sound("toggle");
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
    protected int contentWidth() {
        return root() == null || text.isEmpty() ? BOX : BOX + 4 + font().width(text);
    }

    @Override
    protected int contentHeight() {
        return root() == null ? BOX : Math.max(BOX, font().lineHeight());
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        int top = (height() - BOX) / 2;
        String fill = isHovered() && editable() ? "widget_hover" : "field";
        Draw.box(canvas, theme(), fill, isFocused() ? "focus" : "widget_border", 0, top, BOX, BOX);
        if (checked()) {
            canvas.fill(2, top + 2, BOX - 2, top + BOX - 2, color(editable() ? "accent" : "text_dim"));
        }
        if (!text.isEmpty()) {
            canvas.text(text, BOX + 4, (height() - font().lineHeight()) / 2 + 1, color(isEnabled() ? "text" : "text_dim"), false);
        }
    }
}
