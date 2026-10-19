package xyz.volcanobay.empiric.core.ui.layout;

import xyz.volcanobay.empiric.core.ui.Widget;

/**
 * Sizes and positions a widget's children inside its bounds.
 */
public interface Layout {

    int measureWidth(Widget widget);

    int measureHeight(Widget widget);

    void arrange(Widget widget);
}
