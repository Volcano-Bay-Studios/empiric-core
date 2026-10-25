package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.ui.bind.UiException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The node types one kind of graph can hold. Palettes are registered by id, so layout files can name them, and the
 * server can check graphs against the same palette the client edits with.
 */
public final class GraphPalette {

    private record GraphCategory(String name, String icon, String group) {
    }

    private static final Map<String, GraphPalette> PALETTES = new ConcurrentHashMap<>();

    private final String id;
    private final Map<String, GraphNodeType> types = new LinkedHashMap<>();
    private final Map<String, GraphCategory> categories = new LinkedHashMap<>();
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

    /**
     * Declares a category's order, symbol and group, such as basic and advanced logic. Categories a palette's types use
     * without declaring them follow the declared ones, with no symbol.
     */
    public GraphPalette category(String name, String icon, String group) {
        categories.put(name, new GraphCategory(name, icon, group));
        return this;
    }

    public String categoryIcon(String name) {
        GraphCategory category = categories.get(name);
        return category == null ? "" : category.icon();
    }

    public String categoryGroup(String name) {
        GraphCategory category = categories.get(name);
        return category == null ? "" : category.group();
    }

    public List<String> categories() {
        Set<String> names = new LinkedHashSet<>(categories.keySet());
        for (GraphNodeType type : types.values()) {
            names.add(type.category());
        }
        return new ArrayList<>(names);
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
        GraphNodeType type = types.get(id);
        return type == null && GraphNodeType.ANCHOR_ID.equals(id) ? GraphNodeType.ANCHOR : type;
    }

    public Collection<GraphNodeType> types() {
        return Collections.unmodifiableCollection(types.values());
    }
}
