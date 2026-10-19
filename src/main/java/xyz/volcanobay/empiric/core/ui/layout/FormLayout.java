package xyz.volcanobay.empiric.core.ui.layout;

import xyz.volcanobay.empiric.core.ui.Widget;

import java.util.ArrayList;
import java.util.List;

public record FormLayout(int columnGap, int rowGap) implements Layout {

    @Override
    public int measureWidth(Widget widget) {
        List<Widget> children = visible(widget);
        if (children.isEmpty()) {
            return widget.padding().horizontal();
        }
        return columnWidth(children, 0) + columnGap + columnWidth(children, 1) + widget.padding().horizontal();
    }

    @Override
    public int measureHeight(Widget widget) {
        List<Widget> children = visible(widget);
        int rows = (children.size() + 1) / 2;
        int height = 0;
        for (int row = 0; row < rows; row++) {
            height += rowHeight(children, row);
        }
        return height + Math.max(0, rows - 1) * rowGap + widget.padding().vertical();
    }

    @Override
    public void arrange(Widget widget) {
        List<Widget> children = visible(widget);
        Insets padding = widget.padding();
        int labelWidth = columnWidth(children, 0);
        int controlX = padding.left() + labelWidth + columnGap;
        int controlWidth = Math.max(columnWidth(children, 1), widget.width() - padding.right() - controlX);
        int y = padding.top();
        int rows = (children.size() + 1) / 2;
        for (int row = 0; row < rows; row++) {
            int height = rowHeight(children, row);
            Widget label = children.get(row * 2);
            label.setBounds(padding.left(), y + (height - label.measuredHeight()) / 2, label.measuredWidth(), label.measuredHeight());
            if (row * 2 + 1 < children.size()) {
                Widget control = children.get(row * 2 + 1);
                control.setBounds(controlX, y + (height - control.measuredHeight()) / 2, controlWidth, control.measuredHeight());
            }
            y += height + rowGap;
        }
    }

    private static List<Widget> visible(Widget widget) {
        List<Widget> children = new ArrayList<>();
        for (Widget child : widget.children()) {
            if (child.isVisible()) {
                children.add(child);
            }
        }
        return children;
    }

    private static int columnWidth(List<Widget> children, int column) {
        int max = 0;
        for (int i = column; i < children.size(); i += 2) {
            max = Math.max(max, children.get(i).measuredWidth());
        }
        return max;
    }

    private static int rowHeight(List<Widget> children, int row) {
        int height = children.get(row * 2).measuredHeight();
        if (row * 2 + 1 < children.size()) {
            height = Math.max(height, children.get(row * 2 + 1).measuredHeight());
        }
        return height;
    }
}
