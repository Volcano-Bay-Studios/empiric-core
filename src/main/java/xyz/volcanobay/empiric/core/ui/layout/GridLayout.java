package xyz.volcanobay.empiric.core.ui.layout;

import xyz.volcanobay.empiric.core.ui.Widget;

/**
 * Places visible children row by row in equal cells, each as big as the largest child.
 */
public record GridLayout(int columns, int gap) implements Layout {

    @Override
    public int measureWidth(Widget widget) {
        int count = visible(widget);
        int used = Math.min(columns, count);
        return used == 0 ? widget.padding().horizontal() : used * cellWidth(widget) + (used - 1) * gap + widget.padding().horizontal();
    }

    @Override
    public int measureHeight(Widget widget) {
        int rows = (visible(widget) + columns - 1) / columns;
        return rows == 0 ? widget.padding().vertical() : rows * cellHeight(widget) + (rows - 1) * gap + widget.padding().vertical();
    }

    @Override
    public void arrange(Widget widget) {
        int cellWidth = cellWidth(widget);
        int cellHeight = cellHeight(widget);
        Insets padding = widget.padding();
        int index = 0;
        for (Widget child : widget.children()) {
            if (!child.isVisible()) {
                continue;
            }
            int column = index % columns;
            int row = index / columns;
            child.setBounds(padding.left() + column * (cellWidth + gap), padding.top() + row * (cellHeight + gap), cellWidth, cellHeight);
            index++;
        }
    }

    private static int visible(Widget widget) {
        int count = 0;
        for (Widget child : widget.children()) {
            if (child.isVisible()) {
                count++;
            }
        }
        return count;
    }

    private static int cellWidth(Widget widget) {
        int max = 0;
        for (Widget child : widget.children()) {
            if (child.isVisible()) {
                max = Math.max(max, child.measuredWidth());
            }
        }
        return max;
    }

    private static int cellHeight(Widget widget) {
        int max = 0;
        for (Widget child : widget.children()) {
            if (child.isVisible()) {
                max = Math.max(max, child.measuredHeight());
            }
        }
        return max;
    }
}
