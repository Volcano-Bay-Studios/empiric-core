package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiPoint;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.layout.Layout;
import xyz.volcanobay.empiric.core.ui.widget.ValueWidget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Edits a {@link NodeGraph} held in a synced field. Its widgets are rebuilt by diffing the graph every tick: a
 * {@link NodeWidget} per node and a {@link LinkWidget} per link, so a sync from another viewer shows up like a local edit.
 */
public class NodeGraphView extends ValueWidget<NodeGraph> {

    static final float MIN_ZOOM = 0.25f;
    static final float MAX_ZOOM = 3f;
    private static final int GRID = 16;
    private static final int SNAP = 4;

    private final GraphPalette palette;
    private final Map<Integer, NodeWidget> nodeWidgets = new HashMap<>();
    private final Map<String, LinkWidget> linkWidgets = new HashMap<>();
    private boolean panning;
    private int selected = -1;
    private final List<Consumer<GraphNode>> selectListeners = new ArrayList<>();
    private NodeWidget linkFrom;
    private GraphPort linkPort;
    private boolean linkFromInput;
    private float linkX;
    private float linkY;

    public NodeGraphView(GraphPalette palette) {
        super(NodeGraph.class, new NodeGraph());
        this.palette = palette;
        layout(new GraphLayout());
        clip(true);
        fill();
    }

    public GraphPalette palette() {
        return palette;
    }

    /**
     * The graph being edited, never null.
     */
    public NodeGraph graph() {
        NodeGraph graph = property().get();
        return graph == null ? new NodeGraph() : graph;
    }

    public boolean canEdit() {
        return isEnabled() && !readOnly();
    }

    /**
     * Writes the graph back to the field after an edit, which syncs it.
     */
    public void commit() {
        NodeGraph graph = graph();
        if (property().writable()) {
            property().set(graph);
        }
        property().markChanged();
        syncWidgets();
    }

    /**
     * The node last clicked, or null. Selection stays while focus moves to other widgets, such as an inspector's
     * buttons, and clears on a click on empty space or when the node goes.
     */
    public GraphNode selectedNode() {
        return selected < 0 ? null : graph().node(selected);
    }

    /**
     * Runs when the selected node changes, with the new node or null.
     */
    public NodeGraphView onSelect(Consumer<GraphNode> listener) {
        selectListeners.add(listener);
        return this;
    }

    public void select(int id) {
        if (id != selected) {
            selected = id;
            GraphNode node = selectedNode();
            selectListeners.forEach(listener -> listener.accept(node));
        }
    }

    public boolean isSelected(int id) {
        return selected == id && selected >= 0;
    }

    /**
     * The graph point at the middle of the view, where palette buttons drop new nodes.
     */
    public UiPoint centre() {
        return toGraph(width() / 2.0, height() / 2.0);
    }

    public float zoom() {
        return contentScale;
    }

    public NodeWidget nodeWidget(int id) {
        return nodeWidgets.get(id);
    }

    public List<LinkWidget> linkWidgets() {
        return List.copyOf(linkWidgets.values());
    }

    /**
     * A point in this widget's own coordinates, moved into graph space.
     */
    public UiPoint toGraph(double x, double y) {
        return new UiPoint((x - contentX) / contentScale, (y - contentY) / contentScale);
    }

    @Override
    protected void onAttach() {
        syncWidgets();
    }

    @Override
    protected void tick() {
        syncWidgets();
    }

    /**
     * Brings the widgets in line with the graph. New nodes and links get widgets, gone ones lose them.
     */
    public void syncWidgets() {
        NodeGraph graph = graph();
        boolean changed = false;
        Set<Integer> liveNodes = new HashSet<>();
        for (GraphNode node : graph.nodes) {
            liveNodes.add(node.id);
            NodeWidget widget = nodeWidgets.get(node.id);
            if (widget != null && !widget.type().id().equals(node.type)) {
                remove(widget);
                widget = null;
            }
            if (widget == null) {
                GraphNodeType type = palette.type(node.type);
                if (type == null) {
                    continue;
                }
                widget = add(new NodeWidget(this, type, node.id));
                nodeWidgets.put(node.id, widget);
                changed = true;
            } else if (widget.movedFrom(node)) {
                changed = true;
            }
        }
        Iterator<Map.Entry<Integer, NodeWidget>> nodes = nodeWidgets.entrySet().iterator();
        while (nodes.hasNext()) {
            Map.Entry<Integer, NodeWidget> entry = nodes.next();
            if (!liveNodes.contains(entry.getKey())) {
                remove(entry.getValue());
                nodes.remove();
                changed = true;
                if (entry.getKey() == selected) {
                    select(-1);
                }
            }
        }
        Set<String> liveLinks = new HashSet<>();
        for (GraphLink link : graph.links) {
            String key = link.key();
            liveLinks.add(key);
            if (!linkWidgets.containsKey(key) && nodeWidgets.containsKey(link.from) && nodeWidgets.containsKey(link.to)) {
                linkWidgets.put(key, add(new LinkWidget(this, link)));
                changed = true;
            }
        }
        Iterator<Map.Entry<String, LinkWidget>> links = linkWidgets.entrySet().iterator();
        while (links.hasNext()) {
            Map.Entry<String, LinkWidget> entry = links.next();
            if (!liveLinks.contains(entry.getKey())) {
                remove(entry.getValue());
                links.remove();
                changed = true;
            }
        }
        if (changed) {
            requestLayout();
        }
    }

    // Edits

    public GraphNode addNode(String type, float x, float y) {
        GraphNode node = graph().add(type, snap(x), snap(y));
        commit();
        return node;
    }

    public void removeNode(int id) {
        if (graph().remove(id)) {
            commit();
        }
    }

    public void removeLink(GraphLink link) {
        graph().disconnect(link);
        commit();
    }

    void moveNode(int id, float x, float y) {
        GraphNode node = graph().node(id);
        if (node != null) {
            node.x = snap(x);
            node.y = snap(y);
            commit();
        }
    }

    static float snap(float value) {
        return Math.round(value / SNAP) * SNAP;
    }

    /**
     * Whether an output of one node may feed an input of another: matching kinds, different nodes, and no loop unless
     * the palette allows loops.
     */
    public boolean canLink(int from, GraphPort output, int to, GraphPort input) {
        if (from == to || !input.accepts(output)) {
            return false;
        }
        return palette.allowsCycles() || !graph().reaches(to, from);
    }

    // Link dragging, driven by the node widget that holds the pointer

    void beginLink(NodeWidget from, GraphPort port, boolean input, float graphX, float graphY) {
        if (input) {
            GraphLink existing = graph().linkInto(from.nodeId(), port.id());
            if (existing != null) {
                NodeWidget source = nodeWidgets.get(existing.from);
                GraphNodeType sourceType = source == null ? null : source.type();
                GraphPort output = sourceType == null ? null : sourceType.output(existing.output);
                graph().disconnect(existing);
                commit();
                if (output != null) {
                    from = source;
                    port = output;
                    input = false;
                }
            }
        }
        linkFrom = from;
        linkPort = port;
        linkFromInput = input;
        linkX = graphX;
        linkY = graphY;
    }

    void dragLink(float graphX, float graphY) {
        linkX = graphX;
        linkY = graphY;
    }

    void finishLink(float graphX, float graphY) {
        NodeWidget from = linkFrom;
        GraphPort port = linkPort;
        boolean fromInput = linkFromInput;
        linkFrom = null;
        linkPort = null;
        if (from == null) {
            return;
        }
        for (NodeWidget target : nodeWidgets.values()) {
            NodeWidget.PortHit hit = target.portAt(graphX - target.x(), graphY - target.y());
            if (hit == null || hit.input() == fromInput) {
                continue;
            }
            int outputNode = fromInput ? target.nodeId() : from.nodeId();
            int inputNode = fromInput ? from.nodeId() : target.nodeId();
            GraphPort output = fromInput ? hit.port() : port;
            GraphPort input = fromInput ? port : hit.port();
            if (canLink(outputNode, output, inputNode, input) && graph().connect(outputNode, output.id(), inputNode, input.id())) {
                commit();
            }
            return;
        }
    }

    boolean linking() {
        return linkFrom != null;
    }

    // Input

    @Override
    protected boolean interactive() {
        return true;
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        if (button == Keys.RIGHT_BUTTON) {
            // Right clicks on nodes and links bubble up here; let their own menus open instead.
            if (!canEdit() || pick(x, y) != this) {
                return false;
            }
            UiPoint graph = toGraph(x, y);
            NodePicker picker = new NodePicker(this, (float) graph.x(), (float) graph.y());
            UiPoint at = toRoot(x, y);
            root().openPopup(picker, at.x(), at.y());
            root().focus(picker.search());
            return true;
        }
        panning = true;
        select(-1);
        return true;
    }

    @Override
    protected void pointerDrag(double x, double y, double dx, double dy, int button) {
        if (panning) {
            contentX += (float) dx;
            contentY += (float) dy;
        }
    }

    @Override
    protected void pointerUp(double x, double y, int button) {
        panning = false;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        float zoom = (float) Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, contentScale * (amount > 0 ? 1.1 : 1 / 1.1)));
        UiPoint graph = toGraph(x, y);
        contentScale = zoom;
        contentX = (float) (x - graph.x() * zoom);
        contentY = (float) (y - graph.y() * zoom);
        return true;
    }

    /**
     * Moves the view so a graph point sits at the given place in this widget.
     */
    public void view(float graphX, float graphY, float zoom) {
        contentScale = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
        contentX = width() / 2f - graphX * contentScale;
        contentY = height() / 2f - graphY * contentScale;
    }

    // Drawing

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        canvas.fill(0, 0, width(), height(), color("field"));
        float step = GRID * contentScale;
        if (step < 6) {
            return;
        }
        int line = color("track");
        float startX = ((contentX % step) + step) % step;
        float startY = ((contentY % step) + step) % step;
        for (float x = startX; x < width(); x += step) {
            canvas.fill((int) x, 0, (int) x + 1, height(), line);
        }
        for (float y = startY; y < height(); y += step) {
            canvas.fill(0, (int) y, width(), (int) y + 1, line);
        }
    }

    @Override
    protected void drawOver(UiCanvas canvas, float partialTick) {
        if (linkFrom == null) {
            return;
        }
        UiPoint start = linkFrom.portPoint(linkPort, linkFromInput);
        canvas.push();
        canvas.translate(contentX, contentY);
        canvas.scale(contentScale, contentScale);
        if (linkFromInput) {
            LinkWidget.curve(canvas, linkX, linkY, (float) start.x(), (float) start.y(), 1.5f, LinkWidget.kindColor(linkPort.kind()));
        } else {
            LinkWidget.curve(canvas, (float) start.x(), (float) start.y(), linkX, linkY, 1.5f, LinkWidget.kindColor(linkPort.kind()));
        }
        canvas.pop();
    }

    /**
     * Nodes sit where the graph says, and links span their two ports.
     */
    private static final class GraphLayout implements Layout {

        @Override
        public int measureWidth(Widget widget) {
            return 200;
        }

        @Override
        public int measureHeight(Widget widget) {
            return 120;
        }

        @Override
        public void arrange(Widget widget) {
            NodeGraphView view = (NodeGraphView) widget;
            for (NodeWidget node : view.nodeWidgets.values()) {
                node.place();
            }
            for (LinkWidget link : view.linkWidgets.values()) {
                link.place();
            }
        }
    }
}
