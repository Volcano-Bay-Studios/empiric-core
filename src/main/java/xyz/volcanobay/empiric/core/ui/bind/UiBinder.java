package xyz.volcanobay.empiric.core.ui.bind;

import xyz.volcanobay.empiric.core.attachment.ModuleAttachment;
import xyz.volcanobay.empiric.core.module.Module;
import xyz.volcanobay.empiric.core.ui.ContextEntry;
import xyz.volcanobay.empiric.core.ui.UiTheme;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.data.tree.DataNode;
import xyz.volcanobay.empiric.core.ui.annotation.Flex;
import xyz.volcanobay.empiric.core.ui.blueprint.BlueprintNode;
import xyz.volcanobay.empiric.core.ui.blueprint.BlueprintView;
import xyz.volcanobay.empiric.core.ui.blueprint.Blueprints;
import xyz.volcanobay.empiric.core.ui.annotation.Margin;
import xyz.volcanobay.empiric.core.ui.annotation.Padding;
import xyz.volcanobay.empiric.core.ui.annotation.Size;
import xyz.volcanobay.empiric.core.ui.layout.FlexLayout;
import xyz.volcanobay.empiric.core.ui.layout.Insets;
import xyz.volcanobay.empiric.core.ui.layout.Length;
import xyz.volcanobay.empiric.core.ui.widget.Button;
import xyz.volcanobay.empiric.core.ui.widget.ValueWidget;

/**
 * Turns a widget's annotations into children, layout settings and bindings when it joins a {@code UiRoot}.
 */
public final class UiBinder {

    private UiBinder() {
    }

    public interface Invoker {
        void invoke(Object owner);
    }

    public interface ValueInvoker {
        void invoke(Object owner, Object value);
    }

    public static String theme(Class<? extends Widget> type) {
        String theme = UiClass.of(type).theme;
        return theme == null ? UiTheme.DEFAULT : theme;
    }

    /**
     * The theme a root widget asks for: its layout file's {@code theme}, else its {@code @Theme}, else the default.
     */
    public static String theme(Widget content) {
        String layout = layout(content);
        BlueprintNode node = layout == null ? null : Blueprints.find(layout);
        DataNode theme = node == null ? null : node.prop("theme");
        if (theme != null && !theme.isNull() && !theme.asString().isEmpty()) {
            return theme.asString();
        }
        return theme(content.getClass());
    }

    /**
     * The layout file a widget takes its content from, or null.
     */
    public static String layout(Widget widget) {
        if (widget instanceof BlueprintView view) {
            return view.blueprint();
        }
        return UiClass.of(widget.getClass()).layout;
    }

    public static void assemble(Widget widget, Module<?> source) {
        UiClass type = UiClass.of(widget.getClass());
        String layout = layout(widget);
        if (type.empty() && layout == null) {
            return;
        }
        if (type.size != null) {
            widget.size(width(type.size), height(type.size));
        }
        if (type.padding != null) {
            widget.padding(padding(type.padding));
        }
        if (type.flex != null) {
            widget.layout(flex(type.flex));
        }
        if (type.place != null) {
            widget.anchor(type.place.value(), type.place.x(), type.place.y());
        }
        if (type.clip) {
            widget.clip(true);
        }
        if (widget instanceof BlueprintView) {
            widget.add(Blueprints.build(Blueprints.require(layout, widget.getClass().getName()), Blueprints.Context.strict(source)));
        } else if (layout != null) {
            String user = widget.getClass().getName();
            Blueprints.apply(widget, Blueprints.require(layout, user), Blueprints.Context.strict(source), layout);
        }
        for (UiClass.ChildField field : type.children) {
            if (field.property()) {
                field.setter().set(widget, property(source, field.bind().value(), Object.class, field.describe()));
                continue;
            }
            Widget child = (Widget) field.getter().get(widget);
            if (child == null) {
                throw new UiException(field.describe() + " is null when its widget is attached");
            }
            configure(child, field);
            if (field.bind() != null) {
                if (!(child instanceof ValueWidget<?> value)) {
                    throw new UiException(field.describe() + " has @Bind but " + child.getClass().getSimpleName() + " doesn't hold a value");
                }
                value.bindUnchecked(property(source, field.bind().value(), value.valueType(), field.describe()));
            }
            widget.add(child);
        }
        for (UiClass.Handler handler : type.clicks) {
            Widget target = child(widget, handler.target());
            if (!(target instanceof Button button)) {
                throw new UiException(handler.describe() + " is an @OnClick for \"" + handler.target() + "\", which isn't a Button child");
            }
            button.onPress(() -> handler.invoker().invoke(widget));
        }
        for (UiClass.Handler handler : type.contextActions) {
            Widget target = handler.target().isEmpty() ? widget : child(widget, handler.target());
            if (target == null) {
                throw new UiException(handler.describe() + " is a @ContextAction for \"" + handler.target() + "\", which isn't a child");
            }
            target.contextEntry(ContextEntry.of(handler.label(), () -> handler.invoker().invoke(widget)));
        }
        for (UiClass.Handler handler : type.changes) {
            Property<Object> property = property(source, handler.target(), Object.class, handler.describe());
            if (handler.valueInvoker() != null) {
                widget.watch(property::get, value -> handler.valueInvoker().invoke(widget, value));
            } else {
                widget.watch(property::get, value -> handler.invoker().invoke(widget));
            }
        }
        for (UiClass.RefField ref : type.refs) {
            Widget found = child(widget, ref.id());
            if (found == null) {
                throw new UiException(ref.describe() + " is a @Ref to \"" + ref.id() + "\", which isn't in the tree");
            }
            if (!ref.type().isInstance(found)) {
                throw new UiException(ref.describe() + " is a @Ref to \"" + ref.id() + "\", which is a " + found.getClass().getSimpleName());
            }
            ref.setter().set(widget, found);
        }
    }

    private static void configure(Widget child, UiClass.ChildField field) {
        if (child.id().isEmpty()) {
            child.id(field.id());
        }
        if (field.size() != null) {
            child.size(width(field.size()), height(field.size()));
        }
        if (field.place() != null) {
            child.anchor(field.place().value(), field.place().x(), field.place().y());
        }
        if (field.margin() != null) {
            Margin margin = field.margin();
            child.margin(insets(margin.value(), margin.left(), margin.top(), margin.right(), margin.bottom()));
        }
        if (field.padding() != null) {
            child.padding(padding(field.padding()));
        }
        if (field.layer() != null) {
            child.layer(field.layer().value());
        }
        if (field.flex() != null) {
            child.layout(flex(field.flex()));
        }
        if (field.clip()) {
            child.clip(true);
        }
        if (field.hint() != null) {
            child.hint(field.hint().value());
        }
    }

    private static Widget child(Widget widget, String id) {
        for (Widget child : widget.children()) {
            if (child.id().equals(id)) {
                return child;
            }
            Widget nested = child(child, id);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    /**
     * Finds the field on {@code source}, or on the first module above it, and makes a property for it.
     */
    public static <T> Property<T> property(Module<?> source, String id, Class<T> type, String user) {
        if (source == null) {
            throw new UiException(user + " binds \"" + id + "\" but the UI wasn't opened for a module");
        }
        for (Module<?> module = source; module != null; module = parent(module)) {
            ModuleField field = ModuleField.find(module.type(), id);
            if (field != null) {
                return field.property(module, type, user);
            }
        }
        throw new UiException(user + " binds \"" + id + "\" but " + source.type().id() + " and the modules above it have no synced field with that id");
    }

    private static Module<?> parent(Module<?> module) {
        return module.attachment() instanceof ModuleAttachment child ? child.parent() : null;
    }

    private static Length width(Size size) {
        return length(size.width(), size.weight());
    }

    private static Length height(Size size) {
        return length(size.height(), size.weight());
    }

    private static Length length(int value, float weight) {
        if (value == Size.FIT) {
            return Length.FIT;
        }
        if (value == Size.FILL) {
            return Length.fill(weight);
        }
        return Length.px(value);
    }

    private static Insets padding(Padding padding) {
        return insets(padding.value(), padding.left(), padding.top(), padding.right(), padding.bottom());
    }

    private static Insets insets(int all, int left, int top, int right, int bottom) {
        return new Insets(left < 0 ? all : left, top < 0 ? all : top, right < 0 ? all : right, bottom < 0 ? all : bottom);
    }

    private static FlexLayout flex(Flex flex) {
        return new FlexLayout(flex.value(), flex.gap(), flex.align(), flex.justify());
    }
}
