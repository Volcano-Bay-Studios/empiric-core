package xyz.volcanobay.empiric.core.ui.blueprint;

import xyz.volcanobay.empiric.core.data.tree.DataNode;
import xyz.volcanobay.empiric.core.ui.bind.UiException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One widget in a layout file: its type, an optional id, its properties and its children. It's a plain editable tree,
 * so the editor changes nodes and rebuilds widgets from them rather than editing live widgets.
 * <p>
 * In JSON, {@code type}, {@code id} and {@code children} are structural, and every other key is a property.
 */
public final class BlueprintNode {

    public static final String DEFAULT_TYPE = "widget";

    private String type;
    private String id = "";
    private final Map<String, DataNode> props = new LinkedHashMap<>();
    private final List<BlueprintNode> children = new ArrayList<>();

    public BlueprintNode(String type) {
        this.type = type;
    }

    public static BlueprintNode parse(DataNode node) {
        if (!node.isObject()) {
            throw new UiException("A layout node must be an object, not " + node);
        }
        DataNode type = node.get("type");
        BlueprintNode parsed = new BlueprintNode(type == null ? DEFAULT_TYPE : type.asString());
        for (Map.Entry<String, DataNode> entry : node.entries().entrySet()) {
            switch (entry.getKey()) {
                case "type" -> {
                }
                case "id" -> parsed.id = entry.getValue().asString();
                case "children" -> {
                    for (DataNode child : entry.getValue().elementsOrSelf()) {
                        parsed.children.add(parse(child));
                    }
                }
                default -> parsed.props.put(entry.getKey(), entry.getValue().copy());
            }
        }
        return parsed;
    }

    public DataNode write() {
        DataNode node = DataNode.object();
        node.put("type", DataNode.of(type));
        if (!id.isEmpty()) {
            node.put("id", DataNode.of(id));
        }
        props.forEach((key, value) -> node.put(key, value.copy()));
        if (!children.isEmpty()) {
            DataNode list = DataNode.array();
            for (BlueprintNode child : children) {
                list.add(child.write());
            }
            node.put("children", list);
        }
        return node;
    }

    public BlueprintNode copy() {
        BlueprintNode copy = new BlueprintNode(type);
        copy.id = id;
        props.forEach((key, value) -> copy.props.put(key, value.copy()));
        for (BlueprintNode child : children) {
            copy.children.add(child.copy());
        }
        return copy;
    }

    public String type() {
        return type;
    }

    public BlueprintNode type(String type) {
        this.type = type;
        return this;
    }

    public String id() {
        return id;
    }

    public BlueprintNode id(String id) {
        this.id = id == null ? "" : id;
        return this;
    }

    /**
     * The properties, in file order. Changes go straight into the node.
     */
    public Map<String, DataNode> props() {
        return props;
    }

    public DataNode prop(String name) {
        return props.get(name);
    }

    /**
     * Sets a property, or removes it when the value is null or {@link DataNode#NULL}.
     */
    public BlueprintNode prop(String name, DataNode value) {
        if (value == null || value.isNull()) {
            props.remove(name);
        } else {
            props.put(name, value);
        }
        return this;
    }

    public List<BlueprintNode> children() {
        return children;
    }

    public BlueprintNode add(BlueprintNode child) {
        children.add(child);
        return this;
    }

    public Props read(String path) {
        return new Props(props, path);
    }

    /**
     * A short name for messages and the editor tree, such as {@code slider#gain}.
     */
    public String describe() {
        return id.isEmpty() ? type : type + "#" + id;
    }

    /**
     * The node with this id here or below, or null.
     */
    public BlueprintNode find(String wanted) {
        if (id.equals(wanted)) {
            return this;
        }
        for (BlueprintNode child : children) {
            BlueprintNode found = child.find(wanted);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
