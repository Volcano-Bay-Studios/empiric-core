package xyz.volcanobay.empiric.core.ui.blueprint;

import xyz.volcanobay.empiric.core.data.tree.DataNode;
import xyz.volcanobay.empiric.core.module.Module;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.bind.Property;
import xyz.volcanobay.empiric.core.ui.bind.UiBinder;
import xyz.volcanobay.empiric.core.ui.bind.UiException;
import xyz.volcanobay.empiric.core.ui.layout.Align;
import xyz.volcanobay.empiric.core.ui.layout.Anchor;
import xyz.volcanobay.empiric.core.ui.layout.AnchorLayout;
import xyz.volcanobay.empiric.core.ui.layout.Direction;
import xyz.volcanobay.empiric.core.ui.layout.FlexLayout;
import xyz.volcanobay.empiric.core.ui.layout.FormLayout;
import xyz.volcanobay.empiric.core.ui.layout.GridLayout;
import xyz.volcanobay.empiric.core.ui.layout.Insets;
import xyz.volcanobay.empiric.core.ui.layout.Justify;
import xyz.volcanobay.empiric.core.ui.layout.Length;
import xyz.volcanobay.empiric.core.ui.widget.Button;
import xyz.volcanobay.empiric.core.ui.widget.Label;
import xyz.volcanobay.empiric.core.ui.widget.ValueWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builds widgets from layout files. Every node can set the {@link #COMMON} properties; its widget type reads the rest.
 * Bindings and actions resolve against the module the UI was opened for, so a layout file can be a whole screen.
 */
public final class Blueprints {

    public static final List<PropertyDef> COMMON = List.of(
            PropertyDef.length("width", "fit", "fit, fill, fill:<weight> or pixels"),
            PropertyDef.length("height", "fit", "fit, fill, fill:<weight> or pixels"),
            PropertyDef.insets("padding", "Space inside the edge"),
            PropertyDef.insets("margin", "Space outside the edge"),
            PropertyDef.choice("layout", "none", List.of("none", "row", "column", "grid", "form", "anchor"), "How children are placed"),
            PropertyDef.integer("gap", 0, "Space between children (column gap in a form)"),
            PropertyDef.integer("row_gap", 3, "Space between form rows"),
            PropertyDef.integer("columns", 2, "Grid columns"),
            PropertyDef.choice("align", Align.START, "Cross-axis alignment in a row or column"),
            PropertyDef.choice("justify", Justify.START, "Main-axis spacing in a row or column"),
            PropertyDef.choice("anchor", Anchor.TOP_LEFT, "Where it sits in an anchor layout"),
            PropertyDef.integer("offset_x", 0, "Offset from the anchor"),
            PropertyDef.integer("offset_y", 0, "Offset from the anchor"),
            PropertyDef.integer("layer", 0, "Draw order among siblings"),
            PropertyDef.bool("clip", false, "Cut off children outside the bounds"),
            PropertyDef.bool("visible", true, "Shown and laid out"),
            PropertyDef.bool("enabled", true, "Takes input"),
            PropertyDef.string("hint", "", "Hover hint"),
            PropertyDef.field("bind", "Synced module field this widget shows and edits"),
            PropertyDef.string("theme", "", "Theme id, read on the root only"));

    private static final Map<String, ActionType> ACTIONS = new ConcurrentHashMap<>();
    private static volatile Function<String, BlueprintNode> provider = id -> null;

    /**
     * Turns one action object from a layout file into what runs when it fires.
     */
    public interface ActionType {
        Runnable create(Widget widget, Props action, Context context);
    }

    /**
     * What a build binds against, and what happens to problems: strict builds throw, lenient ones (the editor's
     * preview) report them and leave the bad part out.
     */
    public record Context(Module<?> source, Consumer<String> problems, BiConsumer<BlueprintNode, Widget> built) {

        public static Context strict(Module<?> source) {
            return new Context(source, null, null);
        }

        public static Context lenient(Module<?> source, Consumer<String> problems) {
            return new Context(source, problems, null);
        }

        /**
         * Also reports which widget each node became, so a preview can map clicks back to nodes.
         */
        public static Context lenient(Module<?> source, Consumer<String> problems, BiConsumer<BlueprintNode, Widget> built) {
            return new Context(source, problems, built);
        }

        void problem(UiException e) {
            if (problems == null) {
                throw e;
            }
            problems.accept(e.getMessage());
        }
    }

    static {
        registerAction("set", Blueprints::setAction);
        registerAction("toggle", (widget, action, context) -> {
            Property<Boolean> property = UiBinder.property(context.source(), action.string("field", ""), Boolean.class, action.path());
            return () -> property.set(!Boolean.TRUE.equals(property.get()));
        });
        registerAction("add", (widget, action, context) -> {
            Property<Double> property = UiBinder.property(context.source(), action.string("field", ""), Double.class, action.path());
            double amount = action.number("amount", 1);
            double min = action.number("min", Double.NEGATIVE_INFINITY);
            double max = action.number("max", Double.POSITIVE_INFINITY);
            return () -> property.set(Math.max(min, Math.min(max, (property.get() == null ? 0 : property.get()) + amount)));
        });
        registerAction("close", (widget, action, context) -> () -> {
            if (widget.root() != null) {
                widget.root().close();
            }
        });
        registerAction("sound", (widget, action, context) -> {
            String key = action.string("key", "click");
            float pitch = (float) action.number("pitch", 1);
            return () -> widget.sound(key, pitch);
        });
    }

    private Blueprints() {
    }

    /**
     * Where layouts are looked up by id. The mod points this at its loaded layout files.
     */
    public static void provider(Function<String, BlueprintNode> provider) {
        Blueprints.provider = provider;
    }

    public static BlueprintNode find(String id) {
        return provider.apply(id);
    }

    public static BlueprintNode require(String id, String user) {
        BlueprintNode node = find(id);
        if (node == null) {
            throw new UiException(user + " uses layout " + id + ", which isn't loaded");
        }
        return node;
    }

    public static void registerAction(String name, ActionType type) {
        ACTIONS.put(name, type);
    }

    public static boolean hasAction(String name) {
        return ACTIONS.containsKey(name);
    }

    public static List<String> actions() {
        return List.copyOf(ACTIONS.keySet());
    }

    /**
     * Builds a node and everything below it.
     */
    public static Widget build(BlueprintNode node, Context context) {
        return build(node, context, node.describe());
    }

    private static Widget build(BlueprintNode node, Context context, String path) {
        WidgetType<?> type = WidgetTypes.get(node.type());
        if (type == null) {
            throw new UiException(path + ": there's no widget type \"" + node.type() + "\"");
        }
        Props props = node.read(path);
        Widget widget = type.create(props);
        configure(widget, node, props, context);
        if (context.built() != null) {
            context.built().accept(node, widget);
        }
        if (!node.children().isEmpty() && !type.isContainer()) {
            context.problem(new UiException(path + ": a " + type.id() + " can't hold children"));
            return widget;
        }
        for (BlueprintNode child : node.children()) {
            String childPath = path + " > " + child.describe();
            try {
                type.addChild(widget, build(child, context, childPath), child.read(childPath));
            } catch (UiException e) {
                context.problem(e);
            }
        }
        return widget;
    }

    /**
     * Applies a node's common properties and children to a widget that already exists, such as a Java screen class
     * whose layout comes from a file.
     */
    public static void apply(Widget target, BlueprintNode node, Context context, String path) {
        configure(target, node, node.read(path), context);
        if (context.built() != null) {
            context.built().accept(node, target);
        }
        for (BlueprintNode child : node.children()) {
            String childPath = path + " > " + child.describe();
            try {
                target.add(build(child, context, childPath));
            } catch (UiException e) {
                context.problem(e);
            }
        }
    }

    private static void configure(Widget widget, BlueprintNode node, Props props, Context context) {
        try {
            if (!node.id().isEmpty()) {
                widget.id(node.id());
            }
            Length width = props.length("width", null);
            Length height = props.length("height", null);
            if (width != null) {
                widget.width(width);
            }
            if (height != null) {
                widget.height(height);
            }
            Insets padding = props.insets("padding", null);
            Insets margin = props.insets("margin", null);
            if (padding != null) {
                widget.padding(padding);
            }
            if (margin != null) {
                widget.margin(margin);
            }
            if (props.has("anchor") || props.has("offset_x") || props.has("offset_y")) {
                widget.anchor(props.choice("anchor", widget.anchor()), props.integer("offset_x", widget.offsetX()), props.integer("offset_y", widget.offsetY()));
            }
            if (props.has("layer")) {
                widget.layer(props.integer("layer", 0));
            }
            if (props.has("clip")) {
                widget.clip(props.bool("clip", false));
            }
            if (props.has("visible")) {
                widget.visible(props.bool("visible", true));
            }
            if (props.has("enabled")) {
                widget.enabled(props.bool("enabled", true));
            }
            if (props.has("hint")) {
                widget.hint(props.string("hint", ""));
            }
            layout(widget, props);
        } catch (UiException e) {
            context.problem(e);
        }
        if (props.has("bind")) {
            try {
                bind(widget, props, context);
            } catch (UiException e) {
                context.problem(e);
            }
        }
        if (widget instanceof Button button && props.has("on_click")) {
            Runnable actions = actions(widget, props, "on_click", context);
            button.onPress(actions);
        }
    }

    private static void layout(Widget widget, Props props) {
        if (!props.has("layout")) {
            return;
        }
        int gap = props.integer("gap", 0);
        switch (props.string("layout", "none").toLowerCase(Locale.ROOT)) {
            case "row" -> widget.layout(new FlexLayout(Direction.ROW, gap, props.choice("align", Align.START), props.choice("justify", Justify.START)));
            case "column" -> widget.layout(new FlexLayout(Direction.COLUMN, gap, props.choice("align", Align.START), props.choice("justify", Justify.START)));
            case "grid" -> widget.layout(new GridLayout(props.integer("columns", 2), gap));
            case "form" -> widget.layout(new FormLayout(props.integer("gap", 6), props.integer("row_gap", 3)));
            case "anchor" -> widget.layout(AnchorLayout.INSTANCE);
            case "none" -> {
            }
            default -> throw new UiException(props.path() + ": \"layout\" must be none, row, column, grid, form or anchor");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bind(Widget widget, Props props, Context context) {
        String field = props.string("bind", "");
        String format = props.string("format", "");
        if (widget instanceof Label label && !format.isEmpty()) {
            Property<Object> value = UiBinder.property(context.source(), field, Object.class, props.path());
            label.bind(readOnly(() -> String.format(format, value.get())));
            return;
        }
        if (!(widget instanceof ValueWidget<?> valueWidget)) {
            throw new UiException(props.path() + " binds \"" + field + "\" but a " + widget.getClass().getSimpleName() + " doesn't hold a value");
        }
        valueWidget.bindUnchecked(UiBinder.property(context.source(), field, (Class) valueWidget.valueType(), props.path()));
    }

    /**
     * The actions in a list property, run in order. Actions that fail to resolve are reported and left out.
     */
    public static Runnable actions(Widget widget, Props props, String name, Context context) {
        List<Runnable> runs = new ArrayList<>();
        List<DataNode> list = props.raw(name).elementsOrSelf();
        for (int i = 0; i < list.size(); i++) {
            String path = props.path() + " " + name + "[" + i + "]";
            try {
                DataNode entry = list.get(i);
                if (!entry.isObject()) {
                    throw new UiException(path + " must be an object with an \"action\"");
                }
                Props action = new Props(entry.entries(), path);
                String kind = action.string("action", "");
                ActionType type = ACTIONS.get(kind);
                if (type == null) {
                    throw new UiException(path + ": there's no action \"" + kind + "\"");
                }
                runs.add(type.create(widget, action, context));
            } catch (UiException e) {
                context.problem(e);
            }
        }
        return () -> runs.forEach(Runnable::run);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Runnable setAction(Widget widget, Props action, Context context) {
        String field = action.string("field", "");
        DataNode value = action.raw("value");
        return switch (value.kind()) {
            case BOOLEAN -> {
                Property<Boolean> property = UiBinder.property(context.source(), field, Boolean.class, action.path());
                boolean set = value.asBoolean();
                yield () -> property.set(set);
            }
            case NUMBER -> {
                Property<Double> property = UiBinder.property(context.source(), field, Double.class, action.path());
                double set = value.asNumber().doubleValue();
                yield () -> property.set(set);
            }
            case STRING -> {
                Property<Object> property = UiBinder.property(context.source(), field, Object.class, action.path());
                String set = value.asString();
                yield () -> {
                    Object current = property.get();
                    if (current instanceof Enum<?> constant) {
                        property.set(Enum.valueOf((Class) constant.getDeclaringClass(), set.toUpperCase(Locale.ROOT)));
                    } else {
                        property.set(set);
                    }
                };
            }
            default -> throw new UiException(action.path() + ": \"value\" must be a boolean, number or string");
        };
    }

    private static <T> Property<T> readOnly(Supplier<T> supplier) {
        return new Property<>() {
            @Override
            public T get() {
                return supplier.get();
            }

            @Override
            public void set(T value) {
            }

            @Override
            public boolean writable() {
                return false;
            }
        };
    }
}
