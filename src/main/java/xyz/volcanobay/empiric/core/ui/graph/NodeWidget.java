package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.ui.ContextEntry;
import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiPoint;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.layout.Layout;
import xyz.volcanobay.empiric.core.ui.widget.Draw;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One node of a {@link NodeGraphView}. It reads its node from the graph by id each time, so a sync that
 * replaces the graph doesn't leave it holding stale data.
 */
public class NodeWidget extends Widget {

    private static final int TITLE = 12;
    private static final int ROW = 11;
    public static final int PAD = 4;
    private static final float PORT = 3;
    private static final float PORT_REACH = 5;

    public record PortHit(GraphPort port, boolean input) {
    }

    private final NodeGraphView view;
    private final GraphNodeType type;
    private final int id;
    private final Widget body;
    private final Map<String, Widget> portWidgets;
    private int shownRows = -1;
    private boolean moving;
    private boolean linking;
    private float dragX;
    private float dragY;
    private float validX;
    private float validY;
    private float placedX = Float.NaN;
    private float placedY = Float.NaN;

    NodeWidget(NodeGraphView view, GraphNodeType type, int id) {
        this.view = view;
        this.type = type;
        this.id = id;
        this.body = type.createBody(new NodeContext(view, id));
        if (body != null) {
            add(body);
        }
        this.portWidgets = type.createPortWidgets(new NodeContext(view, id));
        portWidgets.values().forEach(this::add);
        layout(new NodeLayout());
        contextEntry(ContextEntry.of("Duplicate", this::duplicate));
        contextEntry(ContextEntry.of("Disconnect", () -> {
            if (view.canEdit()) {
                view.graph().links.removeIf(link -> link.from == id || link.to == id);
                view.pruneAnchors();
                view.commit();
            }
        }));
        contextEntry(ContextEntry.of("Delete", () -> {
            if (view.canEdit()) {
                view.removeNode(id);
            }
        }));
    }

    public int nodeId() {
        return id;
    }

    public GraphNodeType type() {
        return type;
    }

    public GraphNode node() {
        return view.graph().node(id);
    }

    boolean movedFrom(GraphNode node) {
        return !moving && (node.x != placedX || node.y != placedY);
    }

    private void settle() {
        if (type.isAnchor()) {
            validX = view.snap(dragX);
            validY = view.snap(dragY);
            return;
        }
        float[] spot = view.freeSpot(this, dragX, dragY, measuredWidth(), measuredHeight());
        validX = spot[0];
        validY = spot[1];
    }

    void place() {
        GraphNode node = node();
        if (moving) {
            settle();
        } else if (node != null && Float.isNaN(placedX) && !type.isAnchor() && measuredWidth() > 0 && view.overlaps(this, node.x, node.y, measuredWidth(), measuredHeight())) {
            float[] spot = view.freeSpot(this, node.x, node.y, measuredWidth(), measuredHeight());
            node.x = spot[0];
            node.y = spot[1];
            placedX = spot[0];
            placedY = spot[1];
            if (view.canEdit()) {
                view.commit();
            }
        }
        float x = moving ? validX : node == null ? placedX : node.x;
        float y = moving ? validY : node == null ? placedY : node.y;
        if (!moving) {
            placedX = x;
            placedY = y;
        }
        if (type.isAnchor()) {
            int size = view.grid();
            setBounds(Math.round(x) - size / 2, Math.round(y) - size / 2, size, size);
            return;
        }
        setBounds(Math.round(x), Math.round(y), measuredWidth(), measuredHeight());
    }

    private int up(int value) {
        int grid = view.grid();
        return (value + grid - 1) / grid * grid;
    }

    public NodeGraphView view() {
        return view;
    }

    public int titleHeight() {
        return up(TITLE);
    }

    public int rowHeight() {
        return up(ROW);
    }

    /**
     * Where the port in a row sits, measured from the node's top, on a grid line.
     */
    public int portY(int index) {
        int grid = view.grid();
        return titleHeight() + index * rowHeight() + Math.max(grid, rowHeight() / 2 / grid * grid);
    }

    private static int indexOf(List<GraphPort> ports, String id) {
        for (int i = 0; i < ports.size(); i++) {
            if (ports.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Whether a port sits on the body row that shares its id instead of taking a row of its own.
     */
    public boolean inline(GraphPort port) {
        return port.toggle() != null && body != null && find(body, port.id()) != null;
    }

    private static Widget find(Widget from, String id) {
        if (id.equals(from.id())) {
            return from;
        }
        for (Widget child : from.children()) {
            Widget found = find(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Where a port sits from the node's top. Ports with a matching body row sit on that row, the rest stack in order.
     */
    public int portY(List<GraphPort> ports, int index) {
        GraphPort port = ports.get(index);
        if (inline(port)) {
            Widget row = find(body, port.id());
            int top = 0;
            for (Widget at = row; at != null && at != this; at = at.parent()) {
                top += at.y();
            }
            return top + row.height() / 2;
        }
        int slot = 0;
        for (int i = 0; i < index; i++) {
            if (!inline(ports.get(i))) {
                slot++;
            }
        }
        return portY(slot);
    }

    public boolean selected() {
        return view.isSelected(id);
    }

    public String icon() {
        return type.icon().isEmpty() ? view.palette().categoryIcon(type.category()) : type.icon();
    }

    public List<GraphPort> inputs() {
        return type.inputs(node());
    }

    boolean shows(GraphPort port) {
        return port.shownOn(node());
    }

    @Override
    protected void tick() {
        int now = rows();
        if (now != shownRows) {
            shownRows = now;
            requestLayout();
        }
    }

    private int rows() {
        int inputs = 0;
        for (GraphPort port : inputs()) {
            if (!inline(port)) {
                inputs++;
            }
        }
        return Math.max(inputs, type.outputs().size());
    }

    private int bodyTop() {
        return up(titleHeight() + rows() * rowHeight() + PAD / 2);
    }

    @Override
    protected int contentWidth() {
        if (type.isAnchor()) {
            return view.grid();
        }
        int widest = Math.max(type.width(), body == null ? 0 : body.measuredWidth() + PAD * 2);
        for (Widget extra : portWidgets.values()) {
            widest = Math.max(widest, extra.measuredWidth() + PAD * 2 + 30);
        }
        return up(widest);
    }

    @Override
    protected int contentHeight() {
        if (type.isAnchor()) {
            return view.grid();
        }
        return up(bodyTop() + (body == null ? 0 : body.measuredHeight() + PAD) + PAD / 2);
    }

    /**
     * Where a port's link attaches, in graph space.
     */
    public UiPoint portPoint(GraphPort port, boolean input) {
        if (type.isAnchor()) {
            return new UiPoint(x() + width() / 2.0, y() + height() / 2.0);
        }
        List<GraphPort> ports = input ? inputs() : type.outputs();
        int index = Math.max(0, ports.indexOf(port));
        return new UiPoint(x() + (input ? 0 : width()), y() + portY(ports, index));
    }

    /**
     * The port near a point in this widget's coordinates, or null.
     */
    public PortHit portAt(double x, double y) {
        if (type.isAnchor()) {
            return anchorHit(false, x, y);
        }
        PortHit hit = portAt(inputs(), true, 0, x, y);
        return hit != null ? hit : portAt(type.outputs(), false, width(), x, y);
    }

    PortHit anchorHit(boolean input, double x, double y) {
        double reach = Math.max(PORT_REACH, view.grid() / 2.0 + 1);
        if (Math.abs(x - width() / 2.0) > reach || Math.abs(y - height() / 2.0) > reach) {
            return null;
        }
        return new PortHit(input ? type.inputs().getFirst() : type.outputs().getFirst(), input);
    }

    private PortHit portAt(List<GraphPort> ports, boolean input, double portX, double x, double y) {
        for (int i = 0; i < ports.size(); i++) {
            double portY = portY(ports, i);
            if (Math.abs(x - portX) <= PORT_REACH + 2 && Math.abs(y - portY) <= PORT_REACH) {
                return new PortHit(ports.get(i), input);
            }
        }
        return null;
    }

    private void duplicate() {
        GraphNode node = node();
        if (node == null || !view.canEdit()) {
            return;
        }
        GraphNode copy = view.graph().add(node.type, node.x + 16, node.y + 16);
        copy.values = new HashMap<>(node.values);
        view.commit();
    }

    @Override
    protected boolean interactive() {
        return true;
    }

    /**
     * Ports sit on the edges, half outside, so the hit area reaches out far enough to grab them.
     */
    @Override
    public boolean contains(double x, double y) {
        return x >= -PORT_REACH && y >= 0 && x < width() + PORT_REACH && y < height();
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        if (button != Keys.LEFT_BUTTON) {
            return false;
        }
        if (!view.canEdit()) {
            return true;
        }
        if (type.isAnchor() && !root().host().shiftDown()) {
            moving = true;
            view.dragging(id);
            dragX = x() + width() / 2f;
            dragY = y() + height() / 2f;
            validX = view.snap(dragX);
            validY = view.snap(dragY);
            return true;
        }
        PortHit hit = type.isAnchor() ? anchorHit(false, x, y) : portAt(x, y);
        if (hit != null) {
            linking = true;
            view.beginLink(this, hit.port(), hit.input(), (float) (x() + x), (float) (y() + y));
            return true;
        }
        if (y < titleHeight()) {
            moving = true;
            view.dragging(id);
            dragX = x();
            dragY = y();
            validX = x();
            validY = y();
        }
        return true;
    }

    @Override
    protected void pointerDrag(double x, double y, double dx, double dy, int button) {
        if (moving) {
            dragX += (float) dx;
            dragY += (float) dy;
            view.requestLayout();
        } else if (linking) {
            view.dragLink((float) (x() + x), (float) (y() + y));
        }
    }

    @Override
    protected void pointerUp(double x, double y, int button) {
        if (moving) {
            settle();
            moving = false;
            view.dragging(-1);
            view.moveNode(id, validX, validY);
            if (type.isAnchor()) {
                view.connectAnchor(id);
            }
            view.requestLayout();
        } else if (linking) {
            linking = false;
            view.finishLink((float) (x() + x), (float) (y() + y));
        }
    }

    @Override
    protected void focusChanged(boolean focused) {
        if (focused) {
            view.select(id);
        }
    }

    @Override
    protected boolean key(int key, int modifiers) {
        if ((key == Keys.DELETE || key == Keys.BACKSPACE) && view.canEdit()) {
            view.removeNode(id);
            return true;
        }
        return false;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        if (type.isAnchor()) {
            view.nodeStyle().drawAnchor(canvas, this);
            return;
        }
        view.nodeStyle().drawNode(canvas, this);
    }

    private final class NodeLayout implements Layout {

        @Override
        public int measureWidth(Widget widget) {
            return 0;
        }

        @Override
        public int measureHeight(Widget widget) {
            return 0;
        }

        @Override
        public void arrange(Widget widget) {
            if (body != null) {
                body.setBounds(PAD, bodyTop(), width() - PAD * 2, body.measuredHeight());
            }
            portWidgets.forEach((port, extra) -> {
                List<GraphPort> inputs = inputs();
                int index = indexOf(inputs, port);
                boolean input = index >= 0;
                List<GraphPort> ports = input ? inputs : type.outputs();
                if (!input) {
                    index = indexOf(ports, port);
                }
                if (index < 0) {
                    return;
                }
                int w = extra.measuredWidth();
                int h = extra.measuredHeight();
                extra.setBounds(input ? width() - PAD - w : PAD, portY(ports, index) - h / 2, w, h);
            });
        }
    }
}
