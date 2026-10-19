package xyz.volcanobay.empiric.core.ui.blueprint;

import xyz.volcanobay.empiric.core.data.DataException;
import xyz.volcanobay.empiric.core.data.tree.DataNode;
import xyz.volcanobay.empiric.core.ui.bind.UiException;
import xyz.volcanobay.empiric.core.ui.layout.Insets;
import xyz.volcanobay.empiric.core.ui.layout.Length;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Typed reads of a blueprint node's properties, each with the value to use when it's left out. A value of the wrong
 * shape fails with the node's path, so a bad layout file points at itself.
 */
public final class Props {

    private final Map<String, DataNode> values;
    private final String path;

    public Props(Map<String, DataNode> values, String path) {
        this.values = values;
        this.path = path;
    }

    public String path() {
        return path;
    }

    public boolean has(String name) {
        DataNode value = values.get(name);
        return value != null && !value.isNull();
    }

    public DataNode raw(String name) {
        DataNode value = values.get(name);
        return value == null ? DataNode.NULL : value;
    }

    public int integer(String name, int fallback) {
        return has(name) ? read(name, node -> (int) Math.round(node.asNumber().doubleValue())) : fallback;
    }

    public double number(String name, double fallback) {
        return has(name) ? read(name, node -> node.asNumber().doubleValue()) : fallback;
    }

    public boolean bool(String name, boolean fallback) {
        return has(name) ? read(name, DataNode::asBoolean) : fallback;
    }

    public String string(String name, String fallback) {
        return has(name) ? read(name, DataNode::asString) : fallback;
    }

    public int color(String name, int fallback) {
        return has(name) ? read(name, Props::parseColor) : fallback;
    }

    public <E extends Enum<E>> E choice(String name, E fallback) {
        if (!has(name)) {
            return fallback;
        }
        String wanted = string(name, "");
        for (E option : fallback.getDeclaringClass().getEnumConstants()) {
            if (key(option).equals(wanted.toLowerCase(Locale.ROOT))) {
                return option;
            }
        }
        throw fail(name, "isn't one of " + PropertyDef.choice(name, fallback, "").options());
    }

    public Length length(String name, Length fallback) {
        return has(name) ? read(name, Props::parseLength) : fallback;
    }

    public Insets insets(String name, Insets fallback) {
        return has(name) ? read(name, Props::parseInsets) : fallback;
    }

    public List<String> strings(String name) {
        if (!has(name)) {
            return List.of();
        }
        return read(name, node -> {
            List<String> strings = new ArrayList<>();
            for (DataNode element : node.elementsOrSelf()) {
                strings.add(element.asString());
            }
            return strings;
        });
    }

    private interface Reader<T> {
        T read(DataNode node);
    }

    private <T> T read(String name, Reader<T> reader) {
        try {
            return reader.read(values.get(name));
        } catch (DataException | IllegalArgumentException e) {
            throw fail(name, e.getMessage());
        }
    }

    private UiException fail(String name, String problem) {
        return new UiException(path + ": \"" + name + "\" " + problem);
    }

    static String key(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    public static int parseColor(DataNode node) {
        if (node.kind() == DataNode.Kind.NUMBER) {
            return node.asNumber().intValue();
        }
        String text = node.asString().trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.length() != 6 && text.length() != 8) {
            throw new IllegalArgumentException("must be #RRGGBB or #AARRGGBB, not \"" + node.asString() + "\"");
        }
        int value = (int) Long.parseLong(text, 16);
        return text.length() == 6 ? 0xFF000000 | value : value;
    }

    public static Length parseLength(DataNode node) {
        if (node.kind() == DataNode.Kind.NUMBER) {
            return Length.px(node.asNumber().intValue());
        }
        String text = node.asString().trim().toLowerCase(Locale.ROOT);
        if (text.equals("fit")) {
            return Length.FIT;
        }
        if (text.equals("fill")) {
            return Length.FILL;
        }
        if (text.startsWith("fill:")) {
            return Length.fill(Float.parseFloat(text.substring(5)));
        }
        return Length.px(Integer.parseInt(text));
    }

    public static DataNode writeLength(Length length) {
        return switch (length.kind()) {
            case FIT -> DataNode.of("fit");
            case FILL -> DataNode.of(length.value() == 1 ? "fill" : "fill:" + length.value());
            default -> DataNode.of((long) length.value());
        };
    }

    public static Insets parseInsets(DataNode node) {
        if (!node.isArray()) {
            return Insets.of(node.asNumber().intValue());
        }
        List<DataNode> sides = node.elements();
        return switch (sides.size()) {
            case 1 -> Insets.of(sides.get(0).asNumber().intValue());
            case 2 -> Insets.of(sides.get(0).asNumber().intValue(), sides.get(1).asNumber().intValue());
            case 4 -> new Insets(sides.get(0).asNumber().intValue(), sides.get(1).asNumber().intValue(), sides.get(2).asNumber().intValue(), sides.get(3).asNumber().intValue());
            default -> throw new IllegalArgumentException("needs 1, 2 or 4 numbers");
        };
    }
}
