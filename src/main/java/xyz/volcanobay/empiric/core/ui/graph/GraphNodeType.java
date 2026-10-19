package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.ui.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * What a kind of node looks like and connects to: a title, ports on either side, a header colour key, and optionally a
 * body of widgets for its settings, built against {@link NodeContext}.
 */
public final class GraphNodeType {

    private final String id;
    private String title;
    private String category = "Nodes";
    private String color = "accent";
    private String description = "";
    private int width = 80;
    private final List<GraphPort> inputs = new ArrayList<>();
    private final List<GraphPort> outputs = new ArrayList<>();
    private Function<NodeContext, Widget> body;

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

    public GraphNodeType width(int width) {
        this.width = width;
        return this;
    }

    public GraphNodeType input(String id, String kind) {
        inputs.add(GraphPort.of(id, kind));
        return this;
    }

    public GraphNodeType output(String id, String kind) {
        outputs.add(GraphPort.of(id, kind));
        return this;
    }

    public GraphNodeType body(Function<NodeContext, Widget> body) {
        this.body = body;
        return this;
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
