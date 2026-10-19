package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.ui.bind.Property;

/**
 * What a node's body widgets bind to: the node's settings, as properties that save into the graph and sync it when set.
 */
public final class NodeContext {

    private final NodeGraphView view;
    private final int node;

    NodeContext(NodeGraphView view, int node) {
        this.view = view;
        this.node = node;
    }

    public GraphNode node() {
        return view.graph().node(node);
    }

    public Property<Double> number(String key, double fallback) {
        return new Property<>() {
            @Override
            public Double get() {
                GraphNode current = node();
                return current == null ? fallback : current.number(key, fallback);
            }

            @Override
            public void set(Double value) {
                write(key, value);
            }
        };
    }

    public Property<String> text(String key, String fallback) {
        return new Property<>() {
            @Override
            public String get() {
                GraphNode current = node();
                return current == null ? fallback : current.text(key, fallback);
            }

            @Override
            public void set(String value) {
                write(key, value);
            }
        };
    }

    public Property<Boolean> bool(String key, boolean fallback) {
        return new Property<>() {
            @Override
            public Boolean get() {
                GraphNode current = node();
                return current == null ? fallback : current.bool(key, fallback);
            }

            @Override
            public void set(Boolean value) {
                write(key, value);
            }
        };
    }

    /**
     * A read-only view of a setting, for values the server writes, such as a result.
     */
    public Property<String> shown(String key, String fallback) {
        return new Property<>() {
            @Override
            public String get() {
                GraphNode current = node();
                return current == null ? fallback : current.text(key, fallback);
            }

            @Override
            public void set(String value) {
            }

            @Override
            public boolean writable() {
                return false;
            }
        };
    }

    private void write(String key, Object value) {
        GraphNode current = node();
        if (current != null && view.canEdit()) {
            current.set(key, value);
            view.commit();
        }
    }
}
