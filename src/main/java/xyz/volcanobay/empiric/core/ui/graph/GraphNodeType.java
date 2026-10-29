package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.ui.Widget;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * What a kind of node looks like and connects to: a title, ports on either side, a header colour key, and optionally a
 * body of widgets for its settings, built against {@link NodeContext}.
 */
public final class GraphNodeType {

    public static final String ANCHOR_ID = "empiric:anchor";
    public static final GraphNodeType ANCHOR = GraphNodeType.of(ANCHOR_ID)
            .title("Anchor")
            .category("Anchor")
            .description("A point to organize links. Drag a link onto empty space to make one, drag it to move it, shift-drag from it to continue a link")
            .input("in", GraphPort.ANY)
            .output("out", GraphPort.ANY)
            .anchor();

    private final String id;
    private boolean anchor;
    private String title;
    private String category = "Nodes";
    private String color = "accent";
    private String description = "";
    private String icon = "";
    private int width = 80;
    private final List<GraphPort> inputs = new ArrayList<>();
    private final List<GraphPort> outputs = new ArrayList<>();
    private Function<NodeContext, Widget> body;
    private final Map<String, Function<NodeContext, Widget>> portWidgets = new LinkedHashMap<>();

    private GraphNodeType(String id) {
        this.id = id;
        this.title = id;
    }

    public static GraphNodeType of(String id) {
        return new GraphNodeType(id);
    }

    public GraphNodeType title(String title) {
        this.title = title;
        return this;
    }

    /**
     * What the node does, shown as its hint in the picker and by inspectors.
     */
    public GraphNodeType description(String description) {
        this.description = description;
        return this;
    }

    /**
     * A short symbol drawn in the header and picker, instead of the category's.
     */
    public GraphNodeType icon(String icon) {
        this.icon = icon;
        return this;
    }

    public String icon() {
        return icon;
    }

    public String description() {
        return description;
    }

    public GraphNodeType category(String category) {
        this.category = category;
        return this;
    }

    /**
     * A theme colour key for the header.
     */
    public GraphNodeType color(String color) {
        this.color = color;
        return this;
    }

    /**
     * Marks the type as a bare link point: no header or body, one input and one output at the same spot, and kept out
     * of pickers.
     */
    GraphNodeType anchor() {
        this.anchor = true;
        return this;
    }

    public boolean isAnchor() {
        return anchor;
    }

    public GraphNodeType width(int width) {
        this.width = width;
        return this;
    }

    public GraphNodeType input(String id, String kind) {
        inputs.add(GraphPort.of(id, kind));
        return this;
    }

    /**
     * An input that exists only while the node's {@code toggle} setting is on.
     */
    public GraphNodeType optionalInput(String id, String label, String kind, String toggle) {
        inputs.add(GraphPort.optional(id, label, kind, toggle));
        return this;
    }

    public List<GraphPort> inputs(GraphNode node) {
        List<GraphPort> shown = new ArrayList<>();
        for (GraphPort port : inputs) {
            if (port.shownOn(node)) {
                shown.add(port);
            }
        }
        return shown;
    }

    public GraphNodeType output(String id, String kind) {
        outputs.add(GraphPort.of(id, kind));
        return this;
    }

    public GraphNodeType body(Function<NodeContext, Widget> body) {
        this.body = body;
        return this;
    }

    /**
     * A widget that sits on the same row as a port, opposite its label, such as a toggle for that port.
     */
    public GraphNodeType portWidget(String port, Function<NodeContext, Widget> widget) {
        portWidgets.put(port, widget);
        return this;
    }

    public Map<String, Widget> createPortWidgets(NodeContext context) {
        Map<String, Widget> made = new LinkedHashMap<>();
        portWidgets.forEach((port, widget) -> made.put(port, widget.apply(context)));
        return made;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String category() {
        return category;
    }

    public String color() {
        return color;
    }

    public int width() {
        return width;
    }

    public List<GraphPort> inputs() {
        return List.copyOf(inputs);
    }

    public List<GraphPort> outputs() {
        return List.copyOf(outputs);
    }

    public GraphPort input(String id) {
        for (GraphPort port : inputs) {
            if (port.id().equals(id)) {
                return port;
            }
        }
        return null;
    }

    public GraphPort output(String id) {
        for (GraphPort port : outputs) {
            if (port.id().equals(id)) {
                return port;
            }
        }
        return null;
    }

    public Widget createBody(NodeContext context) {
        return body == null ? null : body.apply(context);
    }
}
