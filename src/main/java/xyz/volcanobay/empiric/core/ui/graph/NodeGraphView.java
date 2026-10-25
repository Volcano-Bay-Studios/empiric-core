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
    private int grid = 4;
    private GraphNodeStyle nodeStyle = GraphNodeStyle.DEFAULT;
    private GraphLinkStyle linkStyle = GraphLinkStyle.DEFAULT;

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
    private float linkStartX;
    private float linkStartY;
    private boolean linkDetached;
    private int dragged = -1;

    public NodeGraphView(GraphPalette palette) {
        super(NodeGraph.class, new NodeGraph());
        this.palette = palette;
        layout(new GraphLayout());
        clip(true);
        fill();
    }

    /**
     * The size of one grid cell. Nodes snap to it and their ports sit on its lines.
     */
    public NodeGraphView grid(int grid) {
        this.grid = Math.max(1, grid);
        requestLayout();
        return this;
    }

    public int grid() {
        return grid;
    }

    public NodeGraphView nodeStyle(GraphNodeStyle style) {
        this.nodeStyle = style == null ? GraphNodeStyle.DEFAULT : style;
        return this;
    }

    public GraphNodeStyle nodeStyle() {
        return nodeStyle;
    }

    public NodeGraphView linkStyle(GraphLinkStyle style) {
        this.linkStyle = style == null ? GraphLinkStyle.DEFAULT : style;
        requestLayout();
        return this;
    }

    public GraphLinkStyle linkStyle() {
        return linkStyle;
    }

    public float panX() {
        return contentX;
    }

    public float panY() {
        return contentY;
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

    /**
     * The node under the pointer, including through its body widgets, or null.
     */
    public GraphNode hoveredNode() {
        if (dragged >= 0) {
            return graph().node(dragged);
        }
        if (root() == null) {
            return null;
        }
        for (Widget widget = root().hovered(); widget != null; widget = widget.parent()) {
            if (widget instanceof NodeWidget node && nodeWidgets.get(node.nodeId()) == node) {
                return node.node();
            }
        }
        return null;
    }

    /**
     * Every node's rectangle in graph space as a flat {@code x, y, width, height, ...} array.
     */
    public float[] obstacles() {
        float[] rects = new float[nodeWidgets.size() * 4];
        int i = 0;
        for (NodeWidget node : nodeWidgets.values()) {
            if (node.type().isAnchor()) {
                continue;
            }
            rects[i++] = node.x();
            rects[i++] = node.y();
            rects[i++] = node.width();
            rects[i++] = node.height();
        }
        return i == rects.length ? rects : java.util.Arrays.copyOf(rects, i);
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
    protected void onDetach() {
        super.onDetach();
        if (property().writable()) {
            property().markChanged();
        }
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
        GraphNode node = graph().node(id);
        if (node != null && GraphNodeType.ANCHOR_ID.equals(node.type)) {
            dissolveAnchor(id);
            commit();
            return;
        }
        if (graph().remove(id)) {
            pruneAnchors();
            commit();
        }
    }

    public void removeLink(GraphLink link) {
        graph().disconnect(link);
        pruneAnchors();
        commit();
    }

    /**
     * Puts an anchor on a link, so the link runs through it.
     */
    void splitLink(GraphLink link, float graphX, float graphY) {
        if (!graph().links.contains(link)) {
            return;
        }
        GraphNode anchor = graph().add(GraphNodeType.ANCHOR_ID, snap(graphX), snap(graphY));
        graph().disconnect(link);
        graph().connect(link.from, link.output, anchor.id, "in");
        graph().connect(anchor.id, "out", link.to, link.input);
        commit();
    }

    /**
     * Removes anchors that no longer sit between a source and a target, so deleting a link takes its bend points along.
     */
    void dragging(int id) {
        dragged = id;
    }

    /**
     * Removes an anchor and joins whatever fed it to whatever it fed, so the path stays.
     */
    private void dissolveAnchor(int id) {
        List<GraphLink> in = new ArrayList<>();
        List<GraphLink> out = new ArrayList<>();
        for (GraphLink link : graph().links) {
            if (link.to == id) {
                in.add(link);
            } else if (link.from == id) {
                out.add(link);
            }
        }
        graph().remove(id);
        for (GraphLink source : in) {
            for (GraphLink target : out) {
                NodeWidget from = nodeWidgets.get(source.from);
                NodeWidget to = nodeWidgets.get(target.to);
                GraphPort output = from == null ? null : from.type().output(source.output);
                GraphPort input = to == null ? null : to.type().input(target.input);
                if (output != null && input != null && (canLink(source.from, output, target.to, input) || from.type().isAnchor() || to.type().isAnchor())) {
                    graph().connect(source.from, source.output, target.to, target.input);
                }
            }
        }
    }

    void pruneAnchors() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (GraphNode node : new ArrayList<>(graph().nodes)) {
                if (!GraphNodeType.ANCHOR_ID.equals(node.type)) {
                    continue;
                }
                boolean in = false;
                boolean out = false;
                for (GraphLink link : graph().links) {
                    in |= link.to == node.id;
                    out |= link.from == node.id;
                }
                if (!in || !out) {
                    graph().remove(node.id);
                    changed = true;
                }
            }
        }
    }

    /**
     * After an anchor is moved: if it landed on a free port, joins it.
     */
    void connectAnchor(int id) {
        NodeWidget anchor = nodeWidgets.get(id);
        GraphNode node = graph().node(id);
        if (anchor == null || node == null) {
            return;
        }
        for (NodeWidget target : nodeWidgets.values()) {
            if (target.type().isAnchor()) {
                continue;
            }
            NodeWidget.PortHit hit = target.portAt(node.x - target.x(), node.y - target.y());
            if (hit == null) {
                continue;
            }
            boolean joined = false;
            if (hit.input()) {
                joined = canLink(id, anchor.type().outputs().getFirst(), target.nodeId(), hit.port()) && graph().connect(id, "out", target.nodeId(), hit.port().id());
            } else if (graph().linkInto(id, "in") == null) {
                joined = canLink(target.nodeId(), hit.port(), id, anchor.type().inputs().getFirst()) && graph().connect(target.nodeId(), hit.port().id(), id, "in");
            }
            if (joined) {
                dissolveAnchor(id);
                commit();
            }
            return;
        }
    }

    void moveNode(int id, float x, float y) {
        GraphNode node = graph().node(id);
        if (node != null) {
            node.x = snap(x);
            node.y = snap(y);
            commit();
        }
    }

    boolean overlaps(NodeWidget self, float x, float y, int width, int height) {
        if (self.type().isAnchor()) {
            return false;
        }
        for (NodeWidget other : nodeWidgets.values()) {
            if (other == self || other.type().isAnchor() || other.width() == 0) {
                continue;
            }
            if (x < other.x() + other.width() + grid && x + width > other.x() - grid && y < other.y() + other.height() + grid && y + height > other.y() - grid) {
                return true;
            }
        }
        return false;
    }

    /**
     * The closest grid position to a point where a node of this size overlaps no other.
     */
    float[] freeSpot(NodeWidget self, float x, float y, int width, int height) {
        float baseX = snap(x);
        float baseY = snap(y);
        if (!overlaps(self, baseX, baseY, width, height)) {
            return new float[]{baseX, baseY};
        }
        for (int ring = 1; ring <= 64; ring++) {
            float[] best = null;
            float bestDistance = Float.MAX_VALUE;
            for (int dy = -ring; dy <= ring; dy++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != ring) {
                        continue;
                    }
                    float cx = baseX + dx * grid;
                    float cy = baseY + dy * grid;
                    float distance = dx * dx + dy * dy;
                    if (distance < bestDistance && !overlaps(self, cx, cy, width, height)) {
                        best = new float[]{cx, cy};
                        bestDistance = distance;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return new float[]{baseX, baseY};
    }

    float snap(float value) {
        return Math.round(value / grid) * grid;
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
        linkDetached = false;
        linkStartX = graphX;
        linkStartY = graphY;
        if (input) {
            GraphLink existing = graph().linkInto(from.nodeId(), port.id());
            if (existing != null) {
                NodeWidget source = nodeWidgets.get(existing.from);
                GraphNodeType sourceType = source == null ? null : source.type();
                GraphPort output = sourceType == null ? null : sourceType.output(existing.output);
                graph().disconnect(existing);
                linkDetached = true;
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
            NodeWidget.PortHit hit = target.type().isAnchor()
                    ? target.anchorHit(!fromInput, graphX - target.x(), graphY - target.y())
                    : target.portAt(graphX - target.x(), graphY - target.y());
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
        dropAnchor(from, port, fromInput, graphX, graphY);
    }

    private void dropAnchor(NodeWidget from, GraphPort port, boolean fromInput, float graphX, float graphY) {
        if (linkDetached) {
            pruneAnchors();
            commit();
            return;
        }
        if (!canEdit() || Math.hypot(graphX - linkStartX, graphY - linkStartY) < grid) {
            return;
        }
        for (NodeWidget node : nodeWidgets.values()) {
            if (!node.type().isAnchor() && graphX >= node.x() && graphX < node.x() + node.width() && graphY >= node.y() && graphY < node.y() + node.height()) {
                return;
            }
        }
        GraphNode anchor = graph().add(GraphNodeType.ANCHOR_ID, snap(graphX), snap(graphY));
        if (fromInput) {
            graph().connect(anchor.id, "out", from.nodeId(), port.id());
        } else {
            graph().connect(from.nodeId(), port.id(), anchor.id, "in");
        }
        commit();
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
        nodeStyle.drawBackground(canvas, this);
        drawLink(canvas);
    }

    private void drawLink(UiCanvas canvas) {
        if (linkFrom == null) {
            return;
        }
        UiPoint start = linkFrom.portPoint(linkPort, linkFromInput);
        float[] route = linkFromInput
                ? linkStyle.route(snap(linkX), snap(linkY), (float) start.x(), (float) start.y(), grid, obstacles())
                : linkStyle.route((float) start.x(), (float) start.y(), snap(linkX), snap(linkY), grid, obstacles());
        canvas.pushClip(0, 0, width(), height());
        canvas.push();
        canvas.translate(contentX, contentY);
        canvas.scale(contentScale, contentScale);
        linkStyle.draw(canvas, route, linkStyle.color(linkPort.kind(), true, theme()), true);
        canvas.pop();
        canvas.popClip();
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
