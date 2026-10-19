package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.data.Data;
import xyz.volcanobay.empiric.core.data.SelfSpec;

import java.util.HashMap;
import java.util.Map;

/**
 * One node in a {@link NodeGraph}: its type, where it sits in graph space, and its settings as strings.
 */
@SelfSpec
public class GraphNode {

    @Data
    public int id;
    @Data
    public String type = "";
    @Data
    public float x;
    @Data
    public float y;
    @Data
    public Map<String, String> values = new HashMap<>();

    public GraphNode() {
    }

    public GraphNode(int id, String type, float x, float y) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public String text(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    public double number(String key, double fallback) {
        String value = values.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean bool(String key, boolean fallback) {
        String value = values.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    public void set(String key, Object value) {
        values.put(key, String.valueOf(value));
    }
}
