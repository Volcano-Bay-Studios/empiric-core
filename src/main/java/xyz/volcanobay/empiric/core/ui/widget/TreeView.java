package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.ContextEntry;
import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiPoint;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.layout.Align;
import xyz.volcanobay.empiric.core.ui.layout.FlexLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A scrolling, folding tree over any objects, read through functions so the tree itself stays the source of truth. The
 * rows are rebuilt whenever what's visible changes, which is checked every tick. Items are compared by identity.
 */
public class TreeView<T> extends ScrollPanel {

    private static final int INDENT = 10;
    private static final int DRAG_START = 3;

    private final Supplier<T> rootItem;
    private final Function<T, List<T>> children;
    private Function<T, String> label = String::valueOf;
    private Function<T, String> labelColor = item -> "text";
    private Function<T, String> hints = item -> null;
    private Function<T, Icon> icons;
    private Function<T, List<ContextEntry>> menu = item -> List.of();
    private Predicate<T> draggable = item -> false;
    private BiPredicate<T, T> canDrop = (moved, onto) -> true;
    private BiConsumer<T, T> dropped;
    private final List<Consumer<T>> selectListeners = new ArrayList<>();
    private final Set<T> folded = Collections.newSetFromMap(new IdentityHashMap<>());
    private T selected;
    private T revealed;
    private List<Entry<T>> shown = List.of();
    private Row dragging;
    private boolean dragActive;

    /**
     * A small picture drawn in front of a row's label, inside a square of {@code size} pixels at {@code x}, {@code y}.
     */
    public interface Icon {

        void draw(UiCanvas canvas, int x, int y, int size, int color);
    }

    private record Entry<T>(T item, int depth, String label, boolean parent, boolean folded) {

        boolean same(Entry<T> other) {
            return item == other.item && depth == other.depth && parent == other.parent && folded == other.folded && label.equals(other.label);
        }
    }

    public TreeView(Supplier<T> rootItem, Function<T, List<T>> children) {
        this.rootItem = rootItem;
        this.children = children;
        // Rows take focus when clicked, so the arrow keys reach the tree through them.
        content().layout(FlexLayout.column(0).align(Align.STRETCH));
    }

    public TreeView<T> label(Function<T, String> label) {
        this.label = label;
        return this;
    }

    /**
     * The theme colour key a row's text is drawn in, such as {@code "error"} for items that need attention.
     */
    public TreeView<T> labelColor(Function<T, String> labelColor) {
        this.labelColor = labelColor;
        return this;
    }

    public TreeView<T> icons(Function<T, Icon> icons) {
        this.icons = icons;
        return this;
    }

    public TreeView<T> hints(Function<T, String> hints) {
        this.hints = hints;
        return this;
    }

    public TreeView<T> menu(Function<T, List<ContextEntry>> menu) {
        this.menu = menu;
        return this;
    }

    /**
     * Lets rows be dragged onto each other. {@code canDrop} says where a dragged item may land, and {@code dropped}
     * gets the dragged item and the one it landed on.
     */
    public TreeView<T> onDrop(Predicate<T> draggable, BiPredicate<T, T> canDrop, BiConsumer<T, T> dropped) {
        this.draggable = draggable;
        this.canDrop = canDrop;
        this.dropped = dropped;
        return this;
    }

    public TreeView<T> onSelect(Consumer<T> listener) {
        selectListeners.add(listener);
        return this;
    }

    public T selected() {
        return selected;
    }

    /**
     * Selects an item without telling the select listeners, for when the selection changed elsewhere. Its parents
     * unfold so it shows, and the list scrolls to it.
     */
    public void select(T item) {
        selected = item;
        if (item != null) {
            unfoldTo(rootItem.get(), item);
        }
        refresh();
    }

    private void choose(T item) {
        if (selected == item) {
            return;
        }
        selected = item;
        refresh();
        for (Consumer<T> listener : selectListeners) {
            listener.accept(item);
        }
    }

    public boolean isFolded(T item) {
        return folded.contains(item);
    }

    public void fold(T item, boolean fold) {
        if (fold) {
            folded.add(item);
        } else {
            folded.remove(item);
        }
        refresh();
    }

    private boolean unfoldTo(T from, T item) {
        if (from == item) {
            return true;
        }
        for (T child : children.apply(from)) {
            if (unfoldTo(child, item)) {
                folded.remove(from);
                return true;
            }
        }
        return false;
    }

    /**
     * The items currently shown, top to bottom.
     */
    public List<T> visibleItems() {
        List<T> items = new ArrayList<>();
        for (Entry<T> entry : shown) {
            items.add(entry.item);
        }
        return items;
    }

    /**
     * Rebuilds the rows if what they show changed.
     */
    public void refresh() {
        List<Entry<T>> next = new ArrayList<>();
        T top = rootItem.get();
        if (top != null) {
            collect(top, 0, next);
        }
        boolean same = next.size() == shown.size();
        for (int i = 0; same && i < next.size(); i++) {
            same = next.get(i).same(shown.get(i));
        }
        if (!same) {
            shown = next;
            Widget focusedRow = root() == null ? null : root().focused();
            T focusedItem = focusedRow instanceof TreeView<?>.Row row && row.tree() == this ? cast(row.entry.item) : null;
            content().clear();
            dragging = null;
            dragActive = false;
            for (Entry<T> entry : next) {
                Row row = content().add(new Row(entry));
                if (entry.item == focusedItem && root() != null) {
                    root().focus(row);
                }
            }
        }
        if (selected != revealed && root() != null) {
            revealed = selected;
            reveal();
        }
    }

    @SuppressWarnings("unchecked")
    private T cast(Object item) {
        return (T) item;
    }

    private void collect(T item, int depth, List<Entry<T>> into) {
        List<T> kids = children.apply(item);
        boolean isFolded = !kids.isEmpty() && folded.contains(item);
        into.add(new Entry<>(item, depth, Objects.toString(label.apply(item)), !kids.isEmpty(), isFolded));
        if (!isFolded) {
            for (T child : kids) {
                collect(child, depth + 1, into);
            }
        }
    }

    private void reveal() {
        for (Widget child : content().children()) {
            if (child instanceof TreeView<?>.Row row && row.entry.item == selected) {
                int top = row.y();
                int bottom = top + row.measuredHeight();
                int view = height() - padding().vertical();
                if (top < scrollOffset()) {
                    scrollTo(top);
                } else if (bottom > scrollOffset() + view) {
                    scrollTo(bottom - view);
                }
                return;
            }
        }
    }

    private void step(T from, int direction) {
        List<T> items = visibleItems();
        int index = items.indexOf(from);
        int next = Math.max(0, Math.min(items.size() - 1, index + direction));
        if (!items.isEmpty()) {
            choose(items.get(next));
            focusRow(items.get(next));
        }
    }

    private void focusRow(T item) {
        for (Widget child : content().children()) {
            if (child instanceof TreeView<?>.Row row && row.entry.item == item && root() != null) {
                root().focus(row);
            }
        }
    }

    private T parentOf(T from, T item) {
        for (T child : children.apply(from)) {
            if (child == item) {
                return from;
            }
            T found = parentOf(child, item);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Override
    protected void tick() {
        refresh();
    }

    @Override
    protected void onLayout() {
        super.onLayout();
        if (revealed == selected) {
            reveal();
            revealed = selected;
        }
    }

    @Override
    protected void drawOver(UiCanvas canvas, float partialTick) {
        super.drawOver(canvas, partialTick);
        if (dragging == null || !dragActive || root() == null) {
            return;
        }
        String text = dragging.entry.label;
        UiPoint pointer = toLocal(root().pointerX(), root().pointerY());
        int x = (int) pointer.x() + 8;
        int y = (int) pointer.y() + 2;
        canvas.overlay();
        Draw.box(canvas, theme(), "panel", "widget_border", x, y, font().width(text) + 6, font().lineHeight() + 4);
        canvas.text(text, x + 3, y + 3, color("text"), false);
    }

    private final class Row extends Widget {

        private final Entry<T> entry;
        private double pressX;
        private double pressY;

        Row(Entry<T> entry) {
            this.entry = entry;
            hint(() -> hints.apply(entry.item));
        }

        TreeView<T> tree() {
            return TreeView.this;
        }

        private int iconWidth() {
            return icons == null ? 0 : font().lineHeight() + 2;
        }

        private int arrowX() {
            return 2 + entry.depth * INDENT;
        }

        private T dropTarget() {
            if (root() == null || !(root().hovered() instanceof TreeView<?>.Row row) || row.tree() != TreeView.this) {
                return null;
            }
            T onto = cast(row.entry.item);
            return onto != entry.item && canDrop.test(entry.item, onto) ? onto : null;
        }

        @Override
        public List<ContextEntry> contextEntries() {
            return menu.apply(entry.item);
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
            if (button == Keys.RIGHT_BUTTON) {
                choose(entry.item);
                return false;
            }
            if (button != Keys.LEFT_BUTTON) {
                return false;
            }
            if (entry.parent && x >= arrowX() - 2 && x < arrowX() + 8) {
                fold(entry.item, !entry.folded);
                return true;
            }
            choose(entry.item);
            pressX = x;
            pressY = y;
            if (dropped != null && draggable.test(entry.item)) {
                dragging = this;
                dragActive = false;
            }
            return true;
        }

        @Override
        protected void pointerDrag(double x, double y, double dx, double dy, int button) {
            if (dragging == this && !dragActive && Math.abs(x - pressX) + Math.abs(y - pressY) > DRAG_START) {
                dragActive = true;
            }
        }

        @Override
        protected void pointerUp(double x, double y, int button) {
            if (dragging != this) {
                return;
            }
            T onto = dragActive ? dropTarget() : null;
            dragging = null;
            dragActive = false;
            if (onto != null) {
                dropped.accept(entry.item, onto);
                refresh();
            }
        }

        @Override
        protected boolean key(int key, int modifiers) {
            switch (key) {
                case Keys.UP -> step(entry.item, -1);
                case Keys.DOWN -> step(entry.item, 1);
                case Keys.LEFT -> {
                    if (entry.parent && !entry.folded) {
                        fold(entry.item, true);
                    } else {
                        T parent = parentOf(rootItem.get(), entry.item);
                        if (parent != null) {
                            choose(parent);
                            focusRow(parent);
                        }
                    }
                }
                case Keys.RIGHT -> {
                    if (entry.folded) {
                        fold(entry.item, false);
                    }
                }
                default -> {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected int contentWidth() {
            return root() == null ? 0 : arrowX() + 10 + iconWidth() + font().width(entry.label) + 4;
        }

        @Override
        protected int contentHeight() {
            return root() == null ? 0 : font().lineHeight() + 4;
        }

        @Override
        protected void draw(UiCanvas canvas, float partialTick) {
            if (entry.item == selected) {
                canvas.fill(0, 0, width(), height(), color("selection"));
            } else if (isHovered()) {
                canvas.fill(0, 0, width(), height(), color("widget_hover"));
            }
            if (dragActive && dragging != null && dragging != this && isHovered()) {
                boolean allowed = dragging.dropTarget() == entry.item;
                canvas.outline(0, 0, width(), height(), color(allowed ? "focus" : "error"));
            }
            if (isFocused()) {
                canvas.outline(0, 0, width(), height(), color("widget_border"));
            }
            float cx = arrowX() + 3;
            float cy = height() / 2f;
            int arrow = color("text_dim");
            if (entry.parent && entry.folded) {
                canvas.quad(cx - 1.5f, cy - 2.5f, cx - 1.5f, cy + 2.5f, cx + 1.5f, cy, cx + 1.5f, cy, arrow);
            } else if (entry.parent) {
                canvas.quad(cx - 2.5f, cy - 1.5f, cx, cy + 1.5f, cx, cy + 1.5f, cx + 2.5f, cy - 1.5f, arrow);
            }
            int textColor = color(labelColor.apply(entry.item));
            Icon icon = icons == null ? null : icons.apply(entry.item);
            if (icon != null) {
                int size = font().lineHeight();
                icon.draw(canvas, arrowX() + 10, (height() - size) / 2, size, textColor);
            }
            canvas.text(entry.label, arrowX() + 10 + iconWidth(), (height() - font().lineHeight()) / 2 + 1, textColor, false);
        }
    }
}
