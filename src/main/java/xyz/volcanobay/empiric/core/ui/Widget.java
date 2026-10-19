package xyz.volcanobay.empiric.core.ui;

import xyz.volcanobay.empiric.core.ui.bind.UiBinder;
import xyz.volcanobay.empiric.core.ui.layout.Anchor;
import xyz.volcanobay.empiric.core.ui.layout.AnchorLayout;
import xyz.volcanobay.empiric.core.ui.layout.Insets;
import xyz.volcanobay.empiric.core.ui.layout.Layout;
import xyz.volcanobay.empiric.core.ui.layout.Length;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A node in a UI tree. Screens, panels, backgrounds and buttons are all widgets, so any widget can be shown on its own or
 * nested inside another. Positions are relative to the parent's content space, which a parent can pan and zoom.
 */
public class Widget {

    private Widget parent;
    private UiRoot root;
    private final List<Widget> children = new ArrayList<>();
    private final List<Widget> childrenView = Collections.unmodifiableList(children);
    private List<Widget> drawOrder;
    private List<Watch<?>> watches;
    private String id = "";
    private int x;
    private int y;
    private int width;
    private int height;
    private Length preferredWidth = Length.FIT;
    private Length preferredHeight = Length.FIT;
    private Insets margin = Insets.NONE;
    private Insets padding = Insets.NONE;
    private Anchor anchor = Anchor.TOP_LEFT;
    private int offsetX;
    private int offsetY;
    private int layer;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean clip;
    private Layout layout = AnchorLayout.INSTANCE;
    private boolean assembled;
    private Supplier<String> hint;
    private List<ContextEntry> contextEntries;
    protected float contentX;
    protected float contentY;
    protected float contentScale = 1;

    public <W extends Widget> W add(W child) {
        Widget node = child;
        if (node.parent != null) {
            node.parent.remove(node);
        }
        node.parent = this;
        children.add(node);
        drawOrder = null;
        if (root != null) {
            node.attach(root);
        }
        requestLayout();
        return child;
    }

    public void remove(Widget child) {
        if (!children.remove(child)) {
            return;
        }
        if (child.root != null) {
            child.detach();
        }
        child.parent = null;
        drawOrder = null;
        requestLayout();
    }

    public void clear() {
        for (Widget child : List.copyOf(children)) {
            remove(child);
        }
    }

    public List<Widget> children() {
        return childrenView;
    }

    public Widget parent() {
        return parent;
    }

    public UiRoot root() {
        return root;
    }

    final void attach(UiRoot root) {
        if (this.root == root) {
            return;
        }
        this.root = root;
        if (!assembled) {
            assembled = true;
            UiBinder.assemble(this, root.source());
        }
        for (int i = 0; i < children.size(); i++) {
            children.get(i).attach(root);
        }
        onAttach();
    }

    final void detach() {
        onDetach();
        for (Widget child : children) {
            child.detach();
        }
        root.forget(this);
        root = null;
    }

    protected void onAttach() {
    }

    protected void onDetach() {
    }

    public <T> void watch(Supplier<T> source, Consumer<T> listener) {
        if (watches == null) {
            watches = new ArrayList<>();
        }
        watches.add(new Watch<>(source, listener, source.get()));
    }

    final void tickTree() {
        if (watches != null) {
            for (Watch<?> watch : watches) {
                watch.poll();
            }
        }
        tick();
        for (int i = 0; i < children.size(); i++) {
            children.get(i).tickTree();
        }
    }

    protected void tick() {
    }

    public void render(UiCanvas canvas, float partialTick) {
        if (!visible) {
            return;
        }
        canvas.nextLayer();
        draw(canvas, partialTick);
        if (!children.isEmpty()) {
            if (clip) {
                canvas.pushClip(0, 0, width, height);
            }
            canvas.push();
            canvas.translate(contentX, contentY);
            if (contentScale != 1) {
                canvas.scale(contentScale, contentScale);
            }
            for (Widget child : drawOrder()) {
                if (child.visible) {
                    canvas.push();
                    canvas.translate(child.x, child.y);
                    child.render(canvas, partialTick);
                    canvas.pop();
                }
            }
            canvas.pop();
            if (clip) {
                canvas.popClip();
            }
            canvas.nextLayer();
        }
        drawOver(canvas, partialTick);
    }

    protected void draw(UiCanvas canvas, float partialTick) {
    }

    protected void drawOver(UiCanvas canvas, float partialTick) {
    }

    private List<Widget> drawOrder() {
        if (drawOrder == null) {
            List<Widget> sorted = new ArrayList<>(children);
            sorted.sort(Comparator.comparingInt(widget -> widget.layer));
            drawOrder = sorted;
        }
        return drawOrder;
    }

    public Widget pick(double x, double y) {
        if (!visible) {
            return null;
        }
        boolean inside = contains(x, y);
        if (clip && !inside) {
            return null;
        }
        if (!children.isEmpty()) {
            double contentPointX = (x - contentX) / contentScale;
            double contentPointY = (y - contentY) / contentScale;
            List<Widget> order = drawOrder();
            for (int i = order.size() - 1; i >= 0; i--) {
                Widget child = order.get(i);
                Widget hit = child.pick(contentPointX - child.x, contentPointY - child.y);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return inside && interactive() ? this : null;
    }

    public boolean contains(double x, double y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /**
     * Whether this widget takes pointer events itself, instead of letting them through to what's behind it.
     */
    protected boolean interactive() {
        return false;
    }

    public UiPoint toLocal(double rootX, double rootY) {
        if (parent == null) {
            return new UiPoint(rootX - x, rootY - y);
        }
        UiPoint point = parent.toLocal(rootX, rootY);
        return new UiPoint((point.x() - parent.contentX) / parent.contentScale - x, (point.y() - parent.contentY) / parent.contentScale - y);
    }

    public UiPoint toRoot(double localX, double localY) {
        double parentX = localX + x;
        double parentY = localY + y;
        if (parent == null) {
            return new UiPoint(parentX, parentY);
        }
        return parent.toRoot(parentX * parent.contentScale + parent.contentX, parentY * parent.contentScale + parent.contentY);
    }

    protected boolean pointerDown(double x, double y, int button) {
        return false;
    }

    protected void pointerUp(double x, double y, int button) {
    }

    protected void pointerDrag(double x, double y, double dx, double dy, int button) {
    }

    protected boolean scroll(double x, double y, double amount) {
        return false;
    }

    protected boolean key(int key, int modifiers) {
        return false;
    }

    protected boolean character(int codepoint, int modifiers) {
        return false;
    }

    protected void focusChanged(boolean focused) {
    }

    protected void hoverChanged(boolean hovered) {
    }

    public boolean focusable() {
        return false;
    }

    /**
     * Whether this widget takes typed keys while focused, so a host in the world stops them moving the player.
     */
    public boolean capturesKeyboard() {
        return false;
    }

    public boolean isFocused() {
        return root != null && root.focused() == this;
    }

    public boolean isHovered() {
        return root != null && root.hovered() == this;
    }

    public boolean isPressed() {
        return root != null && root.captured() == this;
    }

    public int measuredWidth() {
        return preferredWidth.fixed() ? (int) preferredWidth.value() : contentWidth();
    }

    public int measuredHeight() {
        return preferredHeight.fixed() ? (int) preferredHeight.value() : contentHeight();
    }

    protected int contentWidth() {
        return layout.measureWidth(this);
    }

    protected int contentHeight() {
        return layout.measureHeight(this);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        layout.arrange(this);
        onLayout();
    }

    protected void onLayout() {
    }

    public void requestLayout() {
        if (root != null) {
            root.requestLayout();
        }
    }

    public UiTheme theme() {
        return root == null ? UiTheme.FALLBACK : root.theme();
    }

    public int color(String key) {
        return theme().color(key);
    }

    public UiFont font() {
        return root.host().font();
    }

    public void sound(String key) {
        if (root != null) {
            root.sound(key, 1);
        }
    }

    public void sound(String key, float pitch) {
        if (root != null) {
            root.sound(key, pitch);
        }
    }

    public String id() {
        return id;
    }

    /**
     * Text shown near the pointer after it rests on this widget, or null.
     */
    public String hint() {
        return hint == null ? null : hint.get();
    }

    public <W extends Widget> W hint(String hint) {
        return hint(() -> hint);
    }

    public <W extends Widget> W hint(Supplier<String> hint) {
        this.hint = hint;
        return self();
    }

    /**
     * Entries shown in a menu when this widget, or one inside it that doesn't take right clicks, is right clicked.
     */
    public List<ContextEntry> contextEntries() {
        return contextEntries == null ? List.of() : Collections.unmodifiableList(contextEntries);
    }

    public <W extends Widget> W contextEntry(ContextEntry entry) {
        if (contextEntries == null) {
            contextEntries = new ArrayList<>();
        }
        contextEntries.add(entry);
        return self();
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public Length preferredWidth() {
        return preferredWidth;
    }

    public Length preferredHeight() {
        return preferredHeight;
    }

    public Insets margin() {
        return margin;
    }

    public Insets padding() {
        return padding;
    }

    public Anchor anchor() {
        return anchor;
    }

    public int offsetX() {
        return offsetX;
    }

    public int offsetY() {
        return offsetY;
    }

    public int layer() {
        return layer;
    }

    public Layout layout() {
        return layout;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isEnabled() {
        return enabled && (parent == null || parent.isEnabled());
    }

    public boolean isShown() {
        return visible && (parent == null || parent.isShown());
    }

    public boolean clips() {
        return clip;
    }

    @SuppressWarnings("unchecked")
    private <W extends Widget> W self() {
        return (W) this;
    }

    public <W extends Widget> W id(String id) {
        this.id = Objects.requireNonNull(id);
        return self();
    }

    public <W extends Widget> W size(int width, int height) {
        return size(Length.px(width), Length.px(height));
    }

    public <W extends Widget> W size(Length width, Length height) {
        this.preferredWidth = width;
        this.preferredHeight = height;
        requestLayout();
        return self();
    }

    public <W extends Widget> W width(Length width) {
        this.preferredWidth = width;
        requestLayout();
        return self();
    }

    public <W extends Widget> W height(Length height) {
        this.preferredHeight = height;
        requestLayout();
        return self();
    }

    public <W extends Widget> W fill() {
        return size(Length.FILL, Length.FILL);
    }

    public <W extends Widget> W margin(Insets margin) {
        this.margin = margin;
        requestLayout();
        return self();
    }

    public <W extends Widget> W padding(Insets padding) {
        this.padding = padding;
        requestLayout();
        return self();
    }

    public <W extends Widget> W anchor(Anchor anchor) {
        return anchor(anchor, 0, 0);
    }

    public <W extends Widget> W anchor(Anchor anchor, int offsetX, int offsetY) {
        this.anchor = anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        requestLayout();
        return self();
    }

    public <W extends Widget> W layer(int layer) {
        this.layer = layer;
        if (parent != null) {
            parent.drawOrder = null;
        }
        return self();
    }

    public <W extends Widget> W layout(Layout layout) {
        this.layout = layout;
        requestLayout();
        return self();
    }

    public <W extends Widget> W visible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            requestLayout();
        }
        return self();
    }

    public <W extends Widget> W enabled(boolean enabled) {
        this.enabled = enabled;
        return self();
    }

    public <W extends Widget> W clip(boolean clip) {
        this.clip = clip;
        return self();
    }

    private static final class Watch<T> {

        private final Supplier<T> source;
        private final Consumer<T> listener;
        private T last;

        Watch(Supplier<T> source, Consumer<T> listener, T last) {
            this.source = source;
            this.listener = listener;
            this.last = last;
        }

        void poll() {
            T value = source.get();
            if (!Objects.equals(value, last)) {
                last = value;
                listener.accept(value);
            }
        }
    }
}
