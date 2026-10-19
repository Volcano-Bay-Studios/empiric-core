package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.ui.bind.UiException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The node types one kind of graph can hold. Palettes are registered by id, so layout files can name them, and the
 * server can check graphs against the same palette the client edits with.
 */
public final class GraphPalette {

    private static final Map<String, GraphPalette> PALETTES = new ConcurrentHashMap<>();

    private final String id;
    private final Map<String, GraphNodeType> types = new LinkedHashMap<>();
    private boolean cycles;

    private GraphPalette(String id) {
        this.id = id;
    }

    public static GraphPalette register(String id) {
        GraphPalette palette = new GraphPalette(id);
        if (PALETTES.putIfAbsent(id, palette) != null) {
            throw new UiException("Graph palette " + id + " is already registered");
        }
        return palette;
    }

    public static GraphPalette get(String id) {
        return PALETTES.get(id);
    }

    public GraphPalette add(GraphNodeType type) {
        types.put(type.id(), type);
        return this;
    }

    /**
     * Lets links form loops. Off by default, which suits graphs that are evaluated like dataflow.
     */
    public GraphPalette allowCycles(boolean cycles) {
        this.cycles = cycles;
        return this;
    }

    public boolean allowsCycles() {
        return cycles;
    }

    public String id() {
        return id;
    }

    public GraphNodeType type(String id) {
        return types.get(id);
    }

    public Collection<GraphNodeType> types() {
        return Collections.unmodifiableCollection(types.values());
    }
}
