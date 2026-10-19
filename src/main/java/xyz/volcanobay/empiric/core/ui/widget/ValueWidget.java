package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.bind.LocalProperty;
import xyz.volcanobay.empiric.core.ui.bind.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A widget that shows and edits one value. The value lives in a {@link Property}, which is local until it's bound to a
 * module field. Remote changes are picked up each tick, except while the user is editing, so a sync arriving mid-drag
 * or mid-typing doesn't snap the widget back.
 */
public abstract class ValueWidget<T> extends Widget {

    private final Class<T> type;
    private Property<T> property;
    private T value;
    private boolean editing;
    private List<Consumer<T>> listeners;

    protected ValueWidget(Class<T> type, T initial) {
        this.type = type;
        this.property = new LocalProperty<>(initial);
        this.value = initial;
        watch(() -> editing ? value : property.get(), this::received);
    }

    public Class<T> valueType() {
        return type;
    }

    public T value() {
        return value;
    }

    public Property<T> property() {
        return property;
    }

    @SuppressWarnings("unchecked")
    public <W extends ValueWidget<T>> W bind(Property<T> property) {
        this.property = property;
        T current = property.get();
        if (!Objects.equals(current, value)) {
            value = current;
            changed(current);
        }
        return (W) this;
    }

    @SuppressWarnings("unchecked")
    public void bindUnchecked(Property<?> property) {
        bind((Property<T>) property);
    }

    public boolean readOnly() {
        return !property.writable();
    }

    protected boolean editable() {
        return isEnabled() && !readOnly();
    }

    /**
     * Sets the value as the user would, writing it to the bound property.
     */
    public void setValue(T value) {
        if (Objects.equals(value, this.value)) {
            return;
        }
        this.value = value;
        if (property.writable()) {
            property.set(value);
        }
        changed(value);
    }

    @SuppressWarnings("unchecked")
    public <W extends ValueWidget<T>> W onChange(Consumer<T> listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
        return (W) this;
    }

    protected void beginEdit() {
        editing = true;
    }

    protected void endEdit() {
        editing = false;
    }

    public boolean isEditing() {
        return editing;
    }

    private void received(T value) {
        if (!Objects.equals(value, this.value)) {
            this.value = value;
            changed(value);
        }
    }

    protected void changed(T value) {
        if (listeners != null) {
            for (Consumer<T> listener : listeners) {
                listener.accept(value);
            }
        }
    }
}
