package xyz.volcanobay.empiric.core.ui.blueprint;

import xyz.volcanobay.empiric.core.ui.UiSprite;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.bind.UiException;
import xyz.volcanobay.empiric.core.ui.graph.GraphPalette;
import xyz.volcanobay.empiric.core.ui.graph.NodeGraphView;
import xyz.volcanobay.empiric.core.ui.layout.Align;
import xyz.volcanobay.empiric.core.ui.layout.Direction;
import xyz.volcanobay.empiric.core.ui.widget.Button;
import xyz.volcanobay.empiric.core.ui.widget.Checkbox;
import xyz.volcanobay.empiric.core.ui.widget.ColorPicker;
import xyz.volcanobay.empiric.core.ui.widget.Cycle;
import xyz.volcanobay.empiric.core.ui.widget.Dial;
import xyz.volcanobay.empiric.core.ui.widget.Dropdown;
import xyz.volcanobay.empiric.core.ui.widget.Graph;
import xyz.volcanobay.empiric.core.ui.widget.Image;
import xyz.volcanobay.empiric.core.ui.widget.Label;
import xyz.volcanobay.empiric.core.ui.widget.Panel;
import xyz.volcanobay.empiric.core.ui.widget.ProgressBar;
import xyz.volcanobay.empiric.core.ui.widget.ScrollPanel;
import xyz.volcanobay.empiric.core.ui.widget.Slider;
import xyz.volcanobay.empiric.core.ui.widget.Switch;
import xyz.volcanobay.empiric.core.ui.widget.Tabs;
import xyz.volcanobay.empiric.core.ui.widget.TextField;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The widget types layout files can use. The core widgets are always here; mods add their own with
 * {@link #register}.
 */
public final class WidgetTypes {

    private static final Map<String, WidgetType<?>> TYPES = new LinkedHashMap<>();

    static {
        registerCore();
    }

    private WidgetTypes() {
    }

    public static synchronized <W extends Widget> WidgetType<W> register(WidgetType<W> type) {
        if (TYPES.containsKey(type.id())) {
            throw new UiException("Widget type " + type.id() + " is already registered");
        }
        TYPES.put(type.id(), type);
        return type;
    }

    public static WidgetType<?> get(String id) {
        return TYPES.get(id);
    }

    public static Collection<WidgetType<?>> all() {
        return Collections.unmodifiableCollection(TYPES.values());
    }

    private static void registerCore() {
        register(WidgetType.of(BlueprintNode.DEFAULT_TYPE, props -> new Widget()).category("Containers").container());
        register(WidgetType.of("panel", props -> new Panel().style(props.string("style", "panel"), props.string("border", "panel_border")))
                .category("Containers").container()
                .property(PropertyDef.themeKey("style", "panel", "Background colour or sprite"), PropertyDef.themeKey("border", "panel_border", "Border colour")));
        register(WidgetType.of("scroll", props -> new ScrollPanel()).category("Containers").container());
        register(WidgetType.of("tabs", props -> new Tabs()).category("Containers")
                .container((tabs, page, props) -> tabs.tab(props.string("title", page.id().isEmpty() ? "Tab" : page.id()), page),
                        PropertyDef.string("title", "", "The tab's title")));

        register(WidgetType.of("label", props -> new Label(props.string("text", ""))
                        .textColor(props.string("color", "text"))
                        .align(props.choice("align", Align.START))
                        .shadow(props.bool("shadow", false))
                        .wrap(props.bool("wrap", false)))
                .category("Display")
                .property(PropertyDef.string("text", "", "Shown when not bound"), PropertyDef.themeKey("color", "text", "Text colour"),
                        PropertyDef.choice("align", Align.START, "Horizontal alignment"), PropertyDef.bool("shadow", false, "Drop shadow"),
                        PropertyDef.bool("wrap", false, "Wrap to the width"),
                        PropertyDef.string("format", "", "String.format pattern for a bound value, such as %.1f")));
        register(WidgetType.of("progress", props -> new ProgressBar(props.number("min", 0), props.number("max", 1))
                        .direction(props.choice("direction", Direction.ROW))
                        .fill(props.string("fill", "accent")))
                .category("Display")
                .property(PropertyDef.number("min", 0, "Empty at"), PropertyDef.number("max", 1, "Full at"),
                        PropertyDef.choice("direction", Direction.ROW, "Fill direction"), PropertyDef.themeKey("fill", "accent", "Fill colour")));
        register(WidgetType.of("graph", props -> new Graph(props.integer("history", 100))
                        .range(props.number("min", 0), props.number("max", 1))
                        .line(props.string("line", "accent")))
                .category("Display")
                .property(PropertyDef.integer("history", 100, "Samples kept"), PropertyDef.number("min", 0, "Bottom"),
                        PropertyDef.number("max", 1, "Top"), PropertyDef.themeKey("line", "accent", "Line colour")));
        register(WidgetType.of("image", props -> new Image(new UiSprite(props.string("texture", ""), props.integer("u", 0), props.integer("v", 0),
                        props.integer("sprite_width", 16), props.integer("sprite_height", 16), props.integer("texture_width", 256),
                        props.integer("texture_height", 256), props.integer("border", 0))).tint(props.color("tint", 0xFFFFFFFF)))
                .category("Display")
                .property(PropertyDef.string("texture", "", "Texture id"), PropertyDef.integer("u", 0, "Left in the texture"),
                        PropertyDef.integer("v", 0, "Top in the texture"), PropertyDef.integer("sprite_width", 16, "Width in the texture"),
                        PropertyDef.integer("sprite_height", 16, "Height in the texture"), PropertyDef.integer("texture_width", 256, "Texture width"),
                        PropertyDef.integer("texture_height", 256, "Texture height"), PropertyDef.integer("border", 0, "Nine-slice border"),
                        PropertyDef.color("tint", "#FFFFFFFF", "Tint")));

        register(WidgetType.of("button", props -> new Button(props.string("text", "")))
                .category("Input")
                .property(PropertyDef.string("text", "", "Caption"), PropertyDef.actions("on_click", "Actions run when pressed")));
        register(WidgetType.of("switch", props -> new Switch()).category("Input"));
        register(WidgetType.of("checkbox", props -> new Checkbox(props.string("text", "")))
                .category("Input")
                .property(PropertyDef.string("text", "", "Caption")));
        register(WidgetType.of("slider", props -> {
                    Slider slider = new Slider(props.number("min", 0), props.number("max", 1), props.number("step", 0));
                    String format = props.string("format", "");
                    if (!format.isEmpty()) {
                        slider.format(value -> String.format(format, value));
                    }
                    return slider;
                })
                .category("Input")
                .property(PropertyDef.number("min", 0, "Lowest value"), PropertyDef.number("max", 1, "Highest value"),
                        PropertyDef.number("step", 0, "Snap step, or 0 for none"), PropertyDef.string("format", "", "String.format pattern, such as %.1f")));
        register(WidgetType.of("dial", props -> new Dial(props.number("min", 0), props.number("max", 1), props.number("step", 0)))
                .category("Input")
                .property(PropertyDef.number("min", 0, "Lowest value"), PropertyDef.number("max", 1, "Highest value"),
                        PropertyDef.number("step", 0, "Snap step, or 0 for none")));
        register(WidgetType.of("text_field", props -> new TextField(props.string("text", ""))
                        .maxLength(props.integer("max_length", 256))
                        .placeholder(props.string("placeholder", "")))
                .category("Input")
                .property(PropertyDef.string("text", "", "Starting text when not bound"), PropertyDef.integer("max_length", 256, "Longest text"),
                        PropertyDef.string("placeholder", "", "Shown while empty")));
        register(WidgetType.of("cycle", props -> Cycle.indexed(props.strings("options").toArray(String[]::new)))
                .category("Input")
                .property(PropertyDef.strings("options", "Labels; the value is the chosen index")));
        register(WidgetType.of("dropdown", props -> Dropdown.indexed(props.strings("options").toArray(String[]::new)))
                .category("Input")
                .property(PropertyDef.strings("options", "Labels; the value is the chosen index")));
        register(WidgetType.of("node_graph", props -> {
                    GraphPalette palette = GraphPalette.get(props.string("palette", ""));
                    if (palette == null) {
                        throw new UiException(props.path() + ": there's no graph palette \"" + props.string("palette", "") + "\"");
                    }
                    return new NodeGraphView(palette);
                })
                .category("Input")
                .property(PropertyDef.string("palette", "", "Registered graph palette id; bind a NodeGraph field")));
        register(WidgetType.of("color", props -> new ColorPicker().alpha(props.bool("alpha", true)))
                .category("Input")
                .property(PropertyDef.bool("alpha", true, "Edit transparency too")));
    }
}
