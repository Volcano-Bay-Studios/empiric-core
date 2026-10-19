package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.layout.Length;

/**
 * A bar between two panes in a row that drags to resize one of them.
 */
public class Splitter extends Widget {

    private static final int WIDTH = 4;

    private final Widget pane;
    private final boolean before;
    private int min = 40;
    private int max = 2000;

    /**
     * @param pane   the pane whose width changes
     * @param before whether that pane is left of the bar, so dragging right widens it
     */
    public Splitter(Widget pane, boolean before) {
        this.pane = pane;
        this.before = before;
        width(Length.px(WIDTH));
        height(Length.FILL);
        hint("Drag to resize");
    }

    public Splitter limits(int min, int max) {
        this.min = min;
        this.max = max;
        return this;
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
    protected void pointerDrag(double x, double y, double dx, double dy, int button) {
        int current = pane.width();
        int next = (int) Math.round(current + (before ? dx : -dx));
        next = Math.max(min, Math.min(max, next));
        if (next != current) {
            pane.width(Length.px(next));
        }
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        String key = isPressed() ? "accent" : isHovered() ? "widget_hover" : "widget_border";
        canvas.fill(WIDTH / 2 - 1 + (isPressed() || isHovered() ? 0 : 1), 0, WIDTH / 2 + 1, height(), color(key));
    }
}
