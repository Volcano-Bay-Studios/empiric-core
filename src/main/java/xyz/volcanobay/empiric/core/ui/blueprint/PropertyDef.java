package xyz.volcanobay.empiric.core.ui.blueprint;

import xyz.volcanobay.empiric.core.data.tree.DataNode;

import java.util.Arrays;
import java.util.List;

/**
 * One property a blueprint node can set, so the editor knows how to show it and what it falls back to.
 *
 * @param fallback the value used when the node leaves it out, or {@link DataNode#NULL} for none
 * @param options  the choices of a {@link Kind#CHOICE} property
 */
public record PropertyDef(String name, Kind kind, DataNode fallback, List<String> options, String help) {

    public enum Kind {
        INT,
        NUMBER,
        BOOL,
        STRING,
        /**
         * {@code "#RRGGBB"} or {@code "#AARRGGBB"}.
         */
        COLOR,
        /**
         * A theme colour or sprite key.
         */
        THEME_KEY,
        CHOICE,
        /**
         * {@code "fit"}, {@code "fill"}, {@code "fill:2"} for a weighted fill, or pixels.
         */
        LENGTH,
        /**
         * One number for every side, {@code [horizontal, vertical]} or {@code [left, top, right, bottom]}.
         */
        INSETS,
        /**
         * The id of a synced module field.
         */
        FIELD,
        STRINGS,
        /**
         * A list of action objects, run in order.
         */
        ACTIONS
    }

    public static PropertyDef integer(String name, int fallback, String help) {
        return new PropertyDef(name, Kind.INT, DataNode.of(fallback), List.of(), help);
    }

    public static PropertyDef number(String name, double fallback, String help) {
        return new PropertyDef(name, Kind.NUMBER, DataNode.of(fallback), List.of(), help);
    }

    public static PropertyDef bool(String name, boolean fallback, String help) {
        return new PropertyDef(name, Kind.BOOL, DataNode.of(fallback), List.of(), help);
    }

    public static PropertyDef string(String name, String fallback, String help) {
        return new PropertyDef(name, Kind.STRING, DataNode.of(fallback), List.of(), help);
    }

    public static PropertyDef color(String name, String fallback, String help) {
        return new PropertyDef(name, Kind.COLOR, DataNode.of(fallback), List.of(), help);
    }

    public static PropertyDef themeKey(String name, String fallback, String help) {
        return new PropertyDef(name, Kind.THEME_KEY, DataNode.of(fallback), List.of(), help);
    }

    public static PropertyDef length(String name, String fallback, String help) {
        return new PropertyDef(name, Kind.LENGTH, DataNode.of(fallback), List.of(), help);
    }

    public static PropertyDef insets(String name, String help) {
        return new PropertyDef(name, Kind.INSETS, DataNode.of(0), List.of(), help);
    }

    public static PropertyDef field(String name, String help) {
        return new PropertyDef(name, Kind.FIELD, DataNode.NULL, List.of(), help);
    }

    public static PropertyDef strings(String name, String help) {
        return new PropertyDef(name, Kind.STRINGS, DataNode.array(), List.of(), help);
    }

    public static PropertyDef actions(String name, String help) {
        return new PropertyDef(name, Kind.ACTIONS, DataNode.array(), List.of(), help);
    }

    public static <E extends Enum<E>> PropertyDef choice(String name, E fallback, String help) {
        List<String> options = Arrays.stream(fallback.getDeclaringClass().getEnumConstants()).map(Props::key).toList();
        return new PropertyDef(name, Kind.CHOICE, DataNode.of(Props.key(fallback)), options, help);
    }

    public static PropertyDef choice(String name, String fallback, List<String> options, String help) {
        return new PropertyDef(name, Kind.CHOICE, DataNode.of(fallback), List.copyOf(options), help);
    }
}
