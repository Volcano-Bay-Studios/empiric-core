package xyz.volcanobay.empiric.core.data.spec;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.DataException;
import xyz.volcanobay.empiric.core.data.DataPrinter;
import xyz.volcanobay.empiric.core.data.Spec;
import xyz.volcanobay.empiric.core.data.SpecRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The spec of a base type whose values are one of several registered classes, written as the id the class was registered
 * with followed by the class's own spec. Created by {@link SpecRegistry#polymorphic(Class)}.
 * <p>
 * Ids are strings so they stay valid when mods register in a different order. A saved id nobody registers any more fails
 * to read, which leaves the module's data dormant instead of losing it.
 */
public final class PolymorphicSpec<T> implements Spec<T> {

    private final Class<T> base;
    private final SpecRegistry registry;
    private final Map<String, Class<? extends T>> byId = new ConcurrentHashMap<>();
    private final Map<Class<?>, String> byClass = new ConcurrentHashMap<>();

    public PolymorphicSpec(Class<T> base, SpecRegistry registry) {
        this.base = base;
        this.registry = registry;
    }

    public Class<T> base() {
        return base;
    }

    /**
     * Registers a class of the base type. The class needs a spec of its own, such as {@code @SelfSpec}.
     */
    public PolymorphicSpec<T> register(String id, Class<? extends T> type) {
        if (registry.isFrozen()) {
            throw new DataException("Specs can't be registered after the spec registry is frozen");
        }
        if (!base.isAssignableFrom(type)) {
            throw new DataException(type.getName() + " isn't a " + base.getName());
        }
        if (byId.putIfAbsent(id, type) != null) {
            throw new DataException(base.getName() + " already has a class registered as \"" + id + "\"");
        }
        if (byClass.putIfAbsent(type, id) != null) {
            byId.remove(id);
            throw new DataException(type.getName() + " is already registered as " + base.getName() + " \"" + byClass.get(type) + "\"");
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private Spec<Object> spec(Class<?> type) {
        return (Spec<Object>) registry.spec(type);
    }

    private Class<? extends T> type(String id) {
        Class<? extends T> type = byId.get(id);
        if (type == null) {
            throw new DataException("No " + base.getName() + " is registered as \"" + id + "\"");
        }
        return type;
    }

    private String id(T value) {
        String id = byClass.get(value.getClass());
        if (id == null) {
            throw new DataException(value.getClass().getName() + " isn't a registered " + base.getName());
        }
        return id;
    }

    @Override
    public void write(DataBuffer buffer, T value, DataContext context) {
        if (buffer.structured()) {
            buffer.beginArray(2);
            buffer.element();
            buffer.putString(id(value));
            buffer.element();
            spec(value.getClass()).write(buffer, value, context);
            buffer.endArray();
            return;
        }
        buffer.putString(id(value));
        spec(value.getClass()).write(buffer, value, context);
    }

    private Class<? extends T> takeType(DataBuffer buffer) {
        if (buffer.structured()) {
            buffer.takeArray();
            buffer.takeElement();
            Class<? extends T> type = type(buffer.takeString());
            buffer.takeElement();
            return type;
        }
        return type(buffer.takeString());
    }

    private static void finish(DataBuffer buffer) {
        if (buffer.structured()) {
            buffer.endArray();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T read(DataBuffer buffer, DataContext context) {
        T value = (T) spec(takeType(buffer)).read(buffer, context);
        finish(buffer);
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T update(DataBuffer buffer, T existing, DataContext context) {
        Class<? extends T> type = takeType(buffer);
        Spec<Object> spec = spec(type);
        T value;
        if (existing != null && existing.getClass() == type && spec.supportsUpdate()) {
            value = (T) spec.update(buffer, existing, context);
        } else {
            value = (T) spec.read(buffer, context);
        }
        finish(buffer);
        return value;
    }

    @Override
    public boolean supportsUpdate() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T copy(T value) {
        return (T) spec(value.getClass()).copy(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T copyInto(T source, T target) {
        if (source.getClass() != target.getClass()) {
            return copy(source);
        }
        return (T) spec(source.getClass()).copyInto(source, target);
    }

    @Override
    public boolean same(T first, T second) {
        if (first == second) {
            return true;
        }
        return first != null && second != null && first.getClass() == second.getClass() && spec(first.getClass()).same(first, second);
    }

    @Override
    public void print(T value, DataPrinter printer) {
        if (value == null) {
            printer.value(null);
            return;
        }
        spec(value.getClass()).print(value, printer);
    }
}
