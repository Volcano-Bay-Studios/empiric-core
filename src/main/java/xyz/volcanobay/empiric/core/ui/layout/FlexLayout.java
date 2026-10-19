package xyz.volcanobay.empiric.core.ui.layout;

import xyz.volcanobay.empiric.core.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * Lines children up along one axis. Fixed and fitted children keep their size, and filling children split what's left by weight.
 */
public record FlexLayout(Direction direction, int gap, Align align, Justify justify) implements Layout {

    public static FlexLayout row(int gap) {
        return new FlexLayout(Direction.ROW, gap, Align.START, Justify.START);
    }

    public static FlexLayout column(int gap) {
        return new FlexLayout(Direction.COLUMN, gap, Align.START, Justify.START);
    }

    public FlexLayout align(Align align) {
        return new FlexLayout(direction, gap, align, justify);
    }

    public FlexLayout justify(Justify justify) {
        return new FlexLayout(direction, gap, align, justify);
    }

    @Override
    public int measureWidth(Widget widget) {
        return measure(widget, direction == Direction.ROW, true) + widget.padding().horizontal();
    }

    @Override
    public int measureHeight(Widget widget) {
        return measure(widget, direction == Direction.COLUMN, false) + widget.padding().vertical();
    }

    private int measure(Widget widget, boolean main, boolean horizontal) {
        int total = 0;
        int count = 0;
        for (Widget child : widget.children()) {
            if (!child.isVisible()) {
                continue;
            }
            int size = horizontal ? child.measuredWidth() + child.margin().horizontal() : child.measuredHeight() + child.margin().vertical();
            total = main ? total + size : Math.max(total, size);
            count++;
        }
        return main && count > 1 ? total + gap * (count - 1) : total;
    }

    @Override
    public void arrange(Widget widget) {
        List<Widget> items = new ArrayList<>();
        for (Widget child : widget.children()) {
            if (child.isVisible()) {
                items.add(child);
            }
        }
        if (items.isEmpty()) {
            return;
        }
        boolean row = direction == Direction.ROW;
        Insets padding = widget.padding();
        int mainSize = row ? widget.width() - padding.horizontal() : widget.height() - padding.vertical();
        int crossSize = row ? widget.height() - padding.vertical() : widget.width() - padding.horizontal();
        int count = items.size();
        int[] sizes = new int[count];
        int used = gap * (count - 1);
        float weights = 0;
        for (int i = 0; i < count; i++) {
            Widget child = items.get(i);
            Length length = row ? child.preferredWidth() : child.preferredHeight();
            used += row ? child.margin().horizontal() : child.margin().vertical();
            if (length.fills()) {
                weights += length.value();
            } else {
                sizes[i] = row ? child.measuredWidth() : child.measuredHeight();
                used += sizes[i];
            }
        }
        int free = Math.max(0, mainSize - used);
        if (weights > 0) {
            int remaining = free;
            int last = -1;
            for (int i = 0; i < count; i++) {
                Length length = row ? items.get(i).preferredWidth() : items.get(i).preferredHeight();
                if (length.fills()) {
                    sizes[i] = Math.round(free * length.value() / weights);
                    remaining -= sizes[i];
                    last = i;
                }
            }
            sizes[last] += remaining;
            free = 0;
        }
        float position = 0;
        float between = gap;
        switch (justify) {
            case CENTER -> position = free / 2f;
            case END -> position = free;
            case SPACE_BETWEEN -> {
                if (count > 1) {
                    between += free / (float) (count - 1);
                } else {
                    position = free / 2f;
                }
            }
            case SPACE_AROUND -> {
                float share = free / (float) count;
                position = share / 2;
                between += share;
            }
            default -> {
            }
        }
        for (int i = 0; i < count; i++) {
            Widget child = items.get(i);
            Insets margin = child.margin();
            Length crossLength = row ? child.preferredHeight() : child.preferredWidth();
            int available = crossSize - (row ? margin.vertical() : margin.horizontal());
            int cross = align == Align.STRETCH || crossLength.fills() ? available : (row ? child.measuredHeight() : child.measuredWidth());
            int crossOffset = switch (align) {
                case CENTER -> (available - cross) / 2;
                case END -> available - cross;
                default -> 0;
            };
            int main = Math.round(position);
            if (row) {
                child.setBounds(padding.left() + main + margin.left(), padding.top() + margin.top() + crossOffset, sizes[i], cross);
                position += margin.horizontal() + sizes[i] + between;
            } else {
                child.setBounds(padding.left() + margin.left() + crossOffset, padding.top() + main + margin.top(), cross, sizes[i]);
                position += margin.vertical() + sizes[i] + between;
            }
        }
    }
}
