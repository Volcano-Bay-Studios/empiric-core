package xyz.volcanobay.empiric.core.ui.graph;

/**
 * An input or output on a node type. Links join ports of the same kind, and {@link #ANY} joins anything.
 */
public record GraphPort(String id, String label, String kind, String toggle) {

    public static final String ANY = "any";

    public GraphPort(String id, String label, String kind) {
        this(id, label, kind, null);
    }

    /**
     * A port a node only has while its toggle setting is on, such as a setting that can be fed by a link instead.
     */
    public static GraphPort optional(String id, String label, String kind, String toggle) {
        return new GraphPort(id, label, kind, toggle);
    }

    public boolean shownOn(GraphNode node) {
        return toggle == null || node != null && node.bool(toggle, false);
    }

    public static GraphPort of(String id, String kind) {
        return new GraphPort(id, id, kind);
    }

    public boolean accepts(GraphPort other) {
        return kind.equals(ANY) || other.kind.equals(ANY) || kind.equals(other.kind);
    }
}
