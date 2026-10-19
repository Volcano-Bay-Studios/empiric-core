package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.Widget;

/**
 * A themed background that holds other widgets and stops pointer events reaching what's behind it.
 */
public class Panel extends Widget {

    private String style = "panel";
    private String border = "panel_border";

    public Panel style(String style, String border) {
        this.style = style;
        this.border = border;
        return this;
    }

    @Override
    protected boolean interactive() {
        return true;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        Draw.box(canvas, theme(), style, border, 0, 0, width(), height());
    }
}
