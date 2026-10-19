package xyz.volcanobay.empiric.core.ui.graph;

/**
 * An input or output on a node type. Links join ports of the same kind, and {@link #ANY} joins anything.
 */
public record GraphPort(String id, String label, String kind) {

    public static final String ANY = "any";

    public static GraphPort of(String id, String kind) {
        return new GraphPort(id, id, kind);
    }

    public boolean accepts(GraphPort other) {
        return kind.equals(ANY) || other.kind.equals(ANY) || kind.equals(other.kind);
    }
}
