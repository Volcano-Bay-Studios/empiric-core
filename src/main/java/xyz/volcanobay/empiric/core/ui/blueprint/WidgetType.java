package xyz.volcanobay.empiric.core.ui.blueprint;

import xyz.volcanobay.empiric.core.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * A widget that layout files can name. It builds the widget from a node's properties and lists the properties it
 * reads, beyond the ones every widget has, so the editor can offer them.
 */
public final class WidgetType<W extends Widget> {

    public interface Factory<W extends Widget> {
        W create(Props props);
    }

    public interface ChildAdder<W extends Widget> {
        /**
         * Adds a built child, given the child's own properties, for containers like tabs that need more than
         * {@link Widget#add}.
         */
        void add(W parent, Widget child, Props childProps);
    }

    private final String id;
    private final Factory<W> factory;
    private final List<PropertyDef> properties = new ArrayList<>();
    private final List<PropertyDef> childProperties = new ArrayList<>();
    private String category = "Other";
    private boolean container;
    private ChildAdder<W> adder = (parent, child, props) -> parent.add(child);

    private WidgetType(String id, Factory<W> factory) {
        this.id = id;
        this.factory = factory;
    }

    public static <W extends Widget> WidgetType<W> of(String id, Factory<W> factory) {
        return new WidgetType<>(id, factory);
    }

    public WidgetType<W> property(PropertyDef... properties) {
        this.properties.addAll(List.of(properties));
        return this;
    }

    public WidgetType<W> category(String category) {
        this.category = category;
        return this;
    }

    /**
     * Lets the type hold children in layout files.
     */
    public WidgetType<W> container() {
        this.container = true;
        return this;
    }

    /**
     * Lets the type hold children, added its own way, and gives each child extra properties to set.
     */
    public WidgetType<W> container(ChildAdder<W> adder, PropertyDef... childProperties) {
        this.container = true;
        this.adder = adder;
        this.childProperties.addAll(List.of(childProperties));
        return this;
    }

    public String id() {
        return id;
    }

    public String category() {
        return category;
    }

    public boolean isContainer() {
        return container;
    }

    public List<PropertyDef> properties() {
        return List.copyOf(properties);
    }

    /**
     * Properties this type reads from each of its children, such as a tab's title.
     */
    public List<PropertyDef> childProperties() {
        return List.copyOf(childProperties);
    }

    public W create(Props props) {
        return factory.create(props);
    }

    @SuppressWarnings("unchecked")
    void addChild(Widget parent, Widget child, Props childProps) {
        adder.add((W) parent, child, childProps);
    }

    @Override
    public String toString() {
        return id;
    }
}
