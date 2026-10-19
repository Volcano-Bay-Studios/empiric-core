package xyz.volcanobay.empiric.core.ui.layout;

import xyz.volcanobay.empiric.core.ui.Widget;

/**
 * Stacks every child inside the padded bounds, placed by its anchor and offset. Later layers draw on top.
 */
public final class AnchorLayout implements Layout {

    public static final AnchorLayout INSTANCE = new AnchorLayout();

    private AnchorLayout() {
    }

    @Override
    public int measureWidth(Widget widget) {
        int max = 0;
        for (Widget child : widget.children()) {
            if (child.isVisible()) {
                max = Math.max(max, child.measuredWidth() + child.margin().horizontal() + Math.abs(child.offsetX()));
            }
        }
        return max + widget.padding().horizontal();
    }

    @Override
    public int measureHeight(Widget widget) {
        int max = 0;
        for (Widget child : widget.children()) {
            if (child.isVisible()) {
                max = Math.max(max, child.measuredHeight() + child.margin().vertical() + Math.abs(child.offsetY()));
            }
        }
        return max + widget.padding().vertical();
    }

    @Override
    public void arrange(Widget widget) {
        Insets padding = widget.padding();
        int width = widget.width() - padding.horizontal();
        int height = widget.height() - padding.vertical();
        for (Widget child : widget.children()) {
            if (child.isVisible()) {
                place(child, padding.left(), padding.top(), width, height);
            }
        }
    }

    public static void place(Widget child, int x, int y, int width, int height) {
        Insets margin = child.margin();
        int availableWidth = Math.max(0, width - margin.horizontal());
        int availableHeight = Math.max(0, height - margin.vertical());
        int childWidth = child.preferredWidth().fills() ? availableWidth : child.measuredWidth();
        int childHeight = child.preferredHeight().fills() ? availableHeight : child.measuredHeight();
        int childX = x + margin.left() + Math.round((availableWidth - childWidth) * child.anchor().x()) + child.offsetX();
        int childY = y + margin.top() + Math.round((availableHeight - childHeight) * child.anchor().y()) + child.offsetY();
        child.setBounds(childX, childY, childWidth, childHeight);
    }
}
