package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.Widget;

import java.util.ArrayList;
import java.util.List;

public class Button extends Widget {

    private String text;
    private final List<Runnable> actions = new ArrayList<>();

    public Button(String text) {
        this.text = text;
    }

    public Button(String text, Runnable action) {
        this(text);
        actions.add(action);
    }

    public Button onPress(Runnable action) {
        actions.add(action);
        return this;
    }

    public Button text(String text) {
        this.text = text;
        requestLayout();
        return this;
    }

    public String text() {
        return text;
    }

    public void press() {
        sound("click");
        for (Runnable action : actions) {
            action.run();
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
        return button == Keys.LEFT_BUTTON;
    }

    @Override
    protected void pointerUp(double x, double y, int button) {
        if (contains(x, y) && isEnabled()) {
            press();
        }
    }

    @Override
    protected boolean key(int key, int modifiers) {
        if (key == Keys.ENTER || key == Keys.KEYPAD_ENTER || key == Keys.SPACE) {
            press();
            return true;
        }
        return false;
    }

    @Override
    protected int contentWidth() {
        return root() == null ? 0 : font().width(text) + 12;
    }

    @Override
    protected int contentHeight() {
        return root() == null ? 0 : font().lineHeight() + 8;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        String fill = isPressed() ? "accent" : isHovered() && isEnabled() ? "widget_hover" : "widget";
        Draw.box(canvas, theme(), fill, isFocused() ? "focus" : "widget_border", 0, 0, width(), height());
        canvas.centeredText(text, width() / 2, (height() - font().lineHeight()) / 2 + 1, color(isEnabled() ? "text" : "text_dim"), false);
    }
}
