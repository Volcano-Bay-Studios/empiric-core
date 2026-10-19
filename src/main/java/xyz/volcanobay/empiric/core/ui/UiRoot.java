package xyz.volcanobay.empiric.core.ui;

import xyz.volcanobay.empiric.core.module.Module;
import xyz.volcanobay.empiric.core.ui.bind.UiBinder;
import xyz.volcanobay.empiric.core.ui.layout.AnchorLayout;
import xyz.volcanobay.empiric.core.ui.widget.ContextMenu;
import xyz.volcanobay.empiric.core.ui.widget.Draw;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The top of a UI tree. It owns focus, hover and pointer capture, routes input to widgets, and binds the tree to its
 * source module. Input coordinates are in root space, which the host maps from the screen or a world surface.
 */
public final class UiRoot {

    private static final int HINT_DELAY = 10;
    private static final int HINT_WIDTH = 160;

    private final Widget content;
    private final Module<?> source;
    private final Map<Class<?>, Object> services;
    private UiHost host;
    private UiTheme theme;
    private int width;
    private int height;
    private boolean layoutDirty = true;
    private final List<Widget> popups = new ArrayList<>();
    private Widget focused;
    private Widget hovered;
    private Widget captured;
    private int capturedButton = -1;
    private long ticks;
    private int hoverTicks;
    private double pointerX;
    private double pointerY;
    private boolean disposed;

    public UiRoot(Widget content, Module<?> source, UiHost host) {
        this(content, source, host, Map.of());
    }

    /**
     * @param services objects widgets can look up with {@link #service}, such as the player's inventory in a menu
     */
    public UiRoot(Widget content, Module<?> source, UiHost host, Map<Class<?>, Object> services) {
        this.content = content;
        this.source = source;
        this.services = Map.copyOf(services);
        setHost(host);
        content.attach(this);
    }

    /**
     * Moves the tree to another host, such as a screen taking over a tree a menu built.
     */
    public void setHost(UiHost host) {
        this.host = host;
        UiTheme resolved = host.theme(UiBinder.theme(content));
        this.theme = resolved == null ? UiTheme.FALLBACK : resolved;
        layoutDirty = true;
    }

    public <T> T service(Class<T> type) {
        return type.cast(services.get(type));
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        layoutDirty = true;
    }

    public void layout() {
        layoutDirty = false;
        AnchorLayout.place(content, 0, 0, width, height);
        for (Widget popup : popups) {
            popup.setBounds(popup.x(), popup.y(), popup.measuredWidth(), popup.measuredHeight());
        }
    }

    public void ensureLayout() {
        if (layoutDirty) {
            layout();
        }
    }

    public void requestLayout() {
        layoutDirty = true;
    }

    public void tick() {
        ticks++;
        hoverTicks++;
        content.tickTree();
        for (int i = 0; i < popups.size(); i++) {
            popups.get(i).tickTree();
        }
        if (layoutDirty) {
            layout();
        }
    }

    public void render(UiCanvas canvas, float partialTick) {
        renderContent(canvas, partialTick);
        renderPopups(canvas, partialTick);
    }

    /**
     * Draws everything except popups, for hosts that draw something of their own between the two.
     */
    public void renderContent(UiCanvas canvas, float partialTick) {
        if (layoutDirty) {
            layout();
        }
        canvas.push();
        canvas.translate(content.x(), content.y());
        content.render(canvas, partialTick);
        canvas.pop();
    }

    public void renderPopups(UiCanvas canvas, float partialTick) {
        for (Widget popup : popups) {
            canvas.overlay();
            canvas.push();
            canvas.translate(popup.x(), popup.y());
            popup.render(canvas, partialTick);
            canvas.pop();
        }
        renderHint(canvas);
    }

    private void renderHint(UiCanvas canvas) {
        if (hoverTicks < HINT_DELAY || captured != null) {
            return;
        }
        String hint = null;
        for (Widget widget = hovered; widget != null && hint == null; widget = widget.parent()) {
            hint = widget.hint();
        }
        if (hint == null || hint.isEmpty()) {
            return;
        }
        UiFont font = host.font();
        List<String> lines = font.wrap(hint, HINT_WIDTH);
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, font.width(line));
        }
        width += 8;
        int height = lines.size() * font.lineHeight() + 6;
        int x = (int) Math.min(pointerX + 10, this.width - width);
        int y = (int) Math.min(pointerY + 10, this.height - height);
        if (y < pointerY + 10 && x < pointerX + 10) {
            y = (int) Math.max(0, pointerY - 4 - height);
        }
        canvas.overlay();
        Draw.box(canvas, theme, "panel", "widget_border", x, y, width, height);
        for (int i = 0; i < lines.size(); i++) {
            canvas.text(lines.get(i), x + 4, y + 4 + i * font.lineHeight(), theme.color("text"), false);
        }
    }

    public Widget pick(double x, double y) {
        if (layoutDirty) {
            layout();
        }
        for (int i = popups.size() - 1; i >= 0; i--) {
            Widget popup = popups.get(i);
            Widget hit = popup.pick(x - popup.x(), y - popup.y());
            if (hit != null) {
                return hit;
            }
        }
        return content.pick(x - content.x(), y - content.y());
    }

    /**
     * Shows a widget above everything else with its top left at {@code x, y} in root space, sized to its content and kept
     * on screen. A click outside every popup closes them all; a right click then carries on as a normal click.
     */
    public void openPopup(Widget popup, double x, double y) {
        popups.add(popup);
        popup.attach(this);
        int width = popup.measuredWidth();
        int height = popup.measuredHeight();
        int left = (int) Math.max(0, Math.min(x, this.width - width));
        int top = (int) Math.max(0, Math.min(y, this.height - height));
        popup.setBounds(left, top, width, height);
    }

    public void closePopup(Widget popup) {
        if (popups.remove(popup)) {
            popup.detach();
        }
    }

    public void closePopups() {
        for (Widget popup : List.copyOf(popups)) {
            closePopup(popup);
        }
    }

    public List<Widget> popups() {
        return List.copyOf(popups);
    }

    private boolean inPopup(Widget widget) {
        for (Widget current = widget; current != null; current = current.parent()) {
            if (popups.contains(current)) {
                return true;
            }
        }
        return false;
    }

    private void pointer(double x, double y) {
        pointerX = x;
        pointerY = y;
    }

    public double pointerX() {
        return pointerX;
    }

    public double pointerY() {
        return pointerY;
    }

    public boolean pointerMove(double x, double y) {
        pointer(x, y);
        Widget target = pick(x, y);
        setHovered(target);
        return target != null;
    }

    public boolean pointerDown(double x, double y, int button) {
        pointer(x, y);
        hoverTicks = 0;
        Widget target = pick(x, y);
        setHovered(target);
        if (!popups.isEmpty() && !inPopup(target)) {
            closePopups();
            focus(null);
            // A right click elsewhere carries on, so a picker or context menu reopens at the new spot in one click.
            if (button != Keys.RIGHT_BUTTON) {
                return true;
            }
            target = pick(x, y);
            setHovered(target);
        }
        for (Widget widget = target; widget != null; widget = widget.parent()) {
            if (!widget.isEnabled()) {
                continue;
            }
            UiPoint local = widget.toLocal(x, y);
            Widget focusedBefore = focused;
            if (widget.pointerDown(local.x(), local.y(), button)) {
                if (widget.root() == this) {
                    captured = widget;
                    capturedButton = button;
                }
                // A handler that moved focus itself, such as one opening a popup with a search field, keeps it.
                if (focused == focusedBefore) {
                    focus(widget.focusable() && widget.root() == this ? widget : null);
                }
                return true;
            }
        }
        focus(null);
        if (button == Keys.RIGHT_BUTTON) {
            for (Widget widget = target; widget != null; widget = widget.parent()) {
                if (!widget.contextEntries().isEmpty()) {
                    openPopup(new ContextMenu(widget.contextEntries()), x, y);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean pointerUp(double x, double y, int button) {
        if (captured == null || button != capturedButton) {
            return false;
        }
        Widget widget = captured;
        captured = null;
        capturedButton = -1;
        UiPoint local = widget.toLocal(x, y);
        widget.pointerUp(local.x(), local.y(), button);
        return true;
    }

    public boolean pointerDrag(double x, double y, double dx, double dy, int button) {
        pointer(x, y);
        setHovered(pick(x, y));
        if (captured == null || button != capturedButton) {
            return false;
        }
        UiPoint local = captured.toLocal(x, y);
        UiPoint before = captured.toLocal(x - dx, y - dy);
        captured.pointerDrag(local.x(), local.y(), local.x() - before.x(), local.y() - before.y(), button);
        return true;
    }

    public boolean scroll(double x, double y, double amount) {
        for (Widget widget = pick(x, y); widget != null; widget = widget.parent()) {
            if (!widget.isEnabled()) {
                continue;
            }
            UiPoint local = widget.toLocal(x, y);
            if (widget.scroll(local.x(), local.y(), amount)) {
                return true;
            }
        }
        return false;
    }

    public boolean key(int key, int modifiers) {
        // With nothing focused, keys go to the content, where they'd end up after bubbling anyway.
        for (Widget widget = focused == null ? content : focused; widget != null; widget = widget.parent()) {
            if (widget.isEnabled() && widget.key(key, modifiers)) {
                return true;
            }
        }
        if (key == Keys.TAB) {
            focusNext(Keys.shift(modifiers));
            return true;
        }
        if (key == Keys.ESCAPE && !popups.isEmpty()) {
            closePopup(popups.getLast());
            return true;
        }
        if (key == Keys.ESCAPE && focused != null) {
            focus(null);
            return true;
        }
        return false;
    }

    public boolean character(int codepoint, int modifiers) {
        return focused != null && focused.isEnabled() && focused.character(codepoint, modifiers);
    }

    public void focus(Widget widget) {
        if (focused == widget) {
            return;
        }
        Widget old = focused;
        focused = widget;
        if (old != null) {
            old.focusChanged(false);
        }
        if (widget != null) {
            widget.focusChanged(true);
        }
    }

    public void focusNext(boolean backwards) {
        List<Widget> order = new ArrayList<>();
        collectFocusable(content, order);
        for (Widget popup : popups) {
            collectFocusable(popup, order);
        }
        if (order.isEmpty()) {
            return;
        }
        int index = order.indexOf(focused);
        int size = order.size();
        int next = index < 0 ? (backwards ? size - 1 : 0) : Math.floorMod(index + (backwards ? -1 : 1), size);
        focus(order.get(next));
    }

    private static void collectFocusable(Widget widget, List<Widget> order) {
        if (!widget.isVisible()) {
            return;
        }
        if (widget.focusable() && widget.isEnabled()) {
            order.add(widget);
        }
        for (Widget child : widget.children()) {
            collectFocusable(child, order);
        }
    }

    private void setHovered(Widget widget) {
        if (hovered == widget) {
            return;
        }
        Widget old = hovered;
        hovered = widget;
        hoverTicks = 0;
        if (old != null) {
            old.hoverChanged(false);
        }
        if (widget != null) {
            widget.hoverChanged(true);
        }
    }

    void forget(Widget widget) {
        if (focused == widget) {
            focused = null;
        }
        if (hovered == widget) {
            hovered = null;
        }
        if (captured == widget) {
            captured = null;
            capturedButton = -1;
        }
    }

    public void sound(String key, float pitch) {
        String id = theme.sound(key);
        if (id != null) {
            host.playSound(id, pitch);
        }
    }

    public void close() {
        host.close();
    }

    /**
     * Detaches the tree so widgets can release what they hold. The root can't be used afterwards.
     */
    public void dispose() {
        if (!disposed) {
            disposed = true;
            focus(null);
            closePopups();
            content.detach();
        }
    }

    public Widget content() {
        return content;
    }

    public Module<?> source() {
        return source;
    }

    public UiHost host() {
        return host;
    }

    public UiTheme theme() {
        return theme;
    }

    public void setTheme(UiTheme theme) {
        this.theme = theme == null ? UiTheme.FALLBACK : theme;
    }

    public UiFont font() {
        return host.font();
    }

    public long ticks() {
        return ticks;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public Widget focused() {
        return focused;
    }

    public Widget hovered() {
        return hovered;
    }

    public Widget captured() {
        return captured;
    }
}
