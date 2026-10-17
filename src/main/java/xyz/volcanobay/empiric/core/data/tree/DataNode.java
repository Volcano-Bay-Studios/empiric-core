package xyz.volcanobay.empiric.core.data.tree;

import xyz.volcanobay.empiric.core.data.DataException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DataNode {

    public static final DataNode NULL = new DataNode(Kind.NULL, null, null, null);

    private final Kind kind;
    private final Object value;
    private final Map<String, DataNode> entries;
    private final List<DataNode> elements;

    private DataNode(Kind kind, Object value, Map<String, DataNode> entries, List<DataNode> elements) {
        this.kind = kind;
        this.value = value;
        this.entries = entries;
        this.elements = elements;
    }

    public static DataNode object() {
        return new DataNode(Kind.OBJECT, null, new LinkedHashMap<>(), null);
    }

    public static DataNode array() {
        return new DataNode(Kind.ARRAY, null, null, new ArrayList<>());
    }

    public static DataNode array(List<DataNode> elements) {
        return new DataNode(Kind.ARRAY, null, null, new ArrayList<>(elements));
    }

    public static DataNode of(String value) {
        return new DataNode(Kind.STRING, value, null, null);
    }

    public static DataNode of(boolean value) {
        return new DataNode(Kind.BOOLEAN, value, null, null);
    }

    public static DataNode of(long value) {
        return new DataNode(Kind.NUMBER, value, null, null);
    }

    public static DataNode of(double value) {
        return new DataNode(Kind.NUMBER, value, null, null);
    }

    public static DataNode of(Number value) {
        return new DataNode(Kind.NUMBER, value, null, null);
    }

    public Kind kind() {
        return kind;
    }

    public boolean isObject() {
        return kind == Kind.OBJECT;
    }

    public boolean isArray() {
        return kind == Kind.ARRAY;
    }

    public boolean isNull() {
        return kind == Kind.NULL;
    }

    public Map<String, DataNode> entries() {
        require(Kind.OBJECT);
        return entries;
    }

    public List<DataNode> elements() {
        require(Kind.ARRAY);
        return elements;
    }

    public DataNode get(String key) {
        return entries().get(key);
    }

    public DataNode put(String key, DataNode node) {
        entries().put(key, node);
        return this;
    }

    public DataNode add(DataNode node) {
        elements().add(node);
        return this;
    }

    public DataNode remove(String key) {
        return entries().remove(key);
    }

    public String asString() {
        if (kind == Kind.STRING) {
            return (String) value;
        }
        if (kind == Kind.NUMBER || kind == Kind.BOOLEAN) {
            return String.valueOf(value);
        }
        throw new DataException("Expected a string but found " + this);
    }

    public Number asNumber() {
        if (kind == Kind.NUMBER) {
            return (Number) value;
        }
        if (kind == Kind.BOOLEAN) {
            return (Boolean) value ? 1 : 0;
        }
        if (kind == Kind.STRING) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new DataException("Expected a number but found \"" + value + "\"");
            }
        }
        throw new DataException("Expected a number but found " + this);
    }

    public boolean asBoolean() {
        if (kind == Kind.BOOLEAN) {
            return (Boolean) value;
        }
        if (kind == Kind.NUMBER) {
            return ((Number) value).doubleValue() != 0;
        }
        if (kind == Kind.STRING) {
            return Boolean.parseBoolean((String) value);
        }
        throw new DataException("Expected a boolean but found " + this);
    }

    public Object rawValue() {
        return value;
    }

    public DataNode copy() {
        return switch (kind) {
            case OBJECT -> {
                DataNode copy = object();
                entries.forEach((key, node) -> copy.put(key, node.copy()));
                yield copy;
            }
            case ARRAY -> {
                DataNode copy = array();
                elements.forEach(node -> copy.add(node.copy()));
                yield copy;
            }
            default -> this;
        };
    }

    private void require(Kind expected) {
        if (kind != expected) {
            throw new DataException("Expected " + expected.name().toLowerCase() + " but found " + this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DataNode node) || node.kind != kind) {
            return false;
        }
        return switch (kind) {
            case OBJECT -> entries.equals(node.entries);
            case ARRAY -> elements.equals(node.elements);
            case NUMBER -> ((Number) value).doubleValue() == ((Number) node.value).doubleValue();
            default -> Objects.equals(value, node.value);
        };
    }

    @Override
    public int hashCode() {
        return switch (kind) {
            case OBJECT -> entries.hashCode();
            case ARRAY -> elements.hashCode();
            case NUMBER -> Double.hashCode(((Number) value).doubleValue());
            default -> Objects.hashCode(value);
        };
    }

    @Override
    public String toString() {
        return switch (kind) {
            case OBJECT -> entries.toString();
            case ARRAY -> elements.toString();
            case STRING -> "\"" + value + "\"";
            case NULL -> "null";
            default -> String.valueOf(value);
        };
    }

    public List<DataNode> elementsOrSelf() {
        return kind == Kind.ARRAY ? elements : Collections.singletonList(this);
    }

    public enum Kind {
        OBJECT,
        ARRAY,
        STRING,
        NUMBER,
        BOOLEAN,
        NULL
    }
}
