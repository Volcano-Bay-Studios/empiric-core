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

/**
 * One node of a {@link NodeGraphView}. It reads its node from the graph by id each time, so a sync that
 * replaces the graph doesn't leave it holding stale data.
 */
public class NodeWidget extends Widget {

    static final int TITLE = 12;
    static final int ROW = 11;
    static final int PAD = 4;
    private static final float PORT = 3;
    private static final float PORT_REACH = 5;

    public record PortHit(GraphPort port, boolean input) {
    }

    private final NodeGraphView view;
    private final GraphNodeType type;
    private final int id;
    private final Widget body;
    private boolean moving;
    private boolean linking;
    private float dragX;
    private float dragY;
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
        layout(new NodeLayout());
        contextEntry(ContextEntry.of("Duplicate", this::duplicate));
        contextEntry(ContextEntry.of("Disconnect", () -> {
            if (view.canEdit()) {
                view.graph().links.removeIf(link -> link.from == id || link.to == id);
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

    void place() {
        GraphNode node = node();
        float x = moving ? dragX : node == null ? placedX : node.x;
        float y = moving ? dragY : node == null ? placedY : node.y;
        if (!moving) {
            placedX = x;
            placedY = y;
        }
        setBounds(Math.round(x), Math.round(y), measuredWidth(), measuredHeight());
    }

    private int rows() {
        return Math.max(type.inputs().size(), type.outputs().size());
    }

    private int bodyTop() {
        return TITLE + rows() * ROW + PAD / 2;
    }

    @Override
    protected int contentWidth() {
        return Math.max(type.width(), body == null ? 0 : body.measuredWidth() + PAD * 2);
    }

    @Override
    protected int contentHeight() {
        return bodyTop() + (body == null ? 0 : body.measuredHeight() + PAD) + PAD / 2;
    }

    /**
     * Where a port's link attaches, in graph space.
     */
    public UiPoint portPoint(GraphPort port, boolean input) {
        List<GraphPort> ports = input ? type.inputs() : type.outputs();
        int index = Math.max(0, ports.indexOf(port));
        return new UiPoint(x() + (input ? 0 : width()), y() + TITLE + index * ROW + ROW / 2.0 + 1);
    }

    /**
     * The port near a point in this widget's coordinates, or null.
     */
    public PortHit portAt(double x, double y) {
        PortHit hit = portAt(type.inputs(), true, 0, x, y);
        return hit != null ? hit : portAt(type.outputs(), false, width(), x, y);
    }

    private static PortHit portAt(List<GraphPort> ports, boolean input, double portX, double x, double y) {
        for (int i = 0; i < ports.size(); i++) {
            double portY = TITLE + i * ROW + ROW / 2.0 + 1;
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
        PortHit hit = portAt(x, y);
        if (hit != null) {
            linking = true;
            view.beginLink(this, hit.port(), hit.input(), (float) (x() + x), (float) (y() + y));
            return true;
        }
        if (y < TITLE) {
            moving = true;
            dragX = x();
            dragY = y();
        }
        return true;
    }

    @Override
    protected void pointerDrag(double x, double y, double dx, double dy, int button) {
        if (moving) {
            dragX += (float) (dx / view.zoom());
            dragY += (float) (dy / view.zoom());
            view.requestLayout();
        } else if (linking) {
            view.dragLink((float) (x() + x), (float) (y() + y));
        }
    }

    @Override
    protected void pointerUp(double x, double y, int button) {
        if (moving) {
            moving = false;
            view.moveNode(id, dragX, dragY);
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
        Draw.box(canvas, theme(), "panel", view.isSelected(id) ? "focus" : "panel_border", 0, 0, width(), height());
        canvas.fill(1, 1, width() - 1, TITLE, color(type.color()));
        canvas.text(font().trim(type.title(), width() - PAD * 2), PAD, 2, 0xFFFFFFFF, true);
        drawPorts(canvas, type.inputs(), true);
        drawPorts(canvas, type.outputs(), false);
    }

    private void drawPorts(UiCanvas canvas, List<GraphPort> ports, boolean input) {
        int text = color("text_dim");
        for (int i = 0; i < ports.size(); i++) {
            GraphPort port = ports.get(i);
            float portY = TITLE + i * ROW + ROW / 2f + 1;
            float portX = input ? 0 : width();
            canvas.circle(portX, portY, PORT + 1, 0xFF000000);
            canvas.circle(portX, portY, PORT, LinkWidget.kindColor(port.kind()));
            int labelY = Math.round(portY - font().lineHeight() / 2f);
            if (input) {
                canvas.text(port.label(), PAD + 2, labelY, text, false);
            } else {
                canvas.text(port.label(), width() - PAD - 2 - font().width(port.label()), labelY, text, false);
            }
        }
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
        }
    }
}
