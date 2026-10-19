package xyz.volcanobay.empiric.core.ui.bind;

import xyz.volcanobay.empiric.core.data.layout.Accessors;
import xyz.volcanobay.empiric.core.data.layout.FieldLayout;
import xyz.volcanobay.empiric.core.module.Module;
import xyz.volcanobay.empiric.core.module.ModuleType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Generated access to one synced {@code @Data} field of a module type, made once per type and field.
 */
final class ModuleField {

    private static final Map<ModuleType<?, ?>, Map<String, ModuleField>> CACHE = new ConcurrentHashMap<>();

    private final String id;
    private final Class<?> type;
    private final Accessors.ObjectGetter getter;
    private final Accessors.ObjectSetter setter;

    private ModuleField(FieldLayout layout) {
        this.id = layout.id();
        this.type = layout.type();
        this.getter = Accessors.bind(Accessors.ObjectGetter.class, layout.getter());
        this.setter = layout.data().clientMutable() && layout.setter() != null ? Accessors.bind(Accessors.ObjectSetter.class, layout.setter()) : null;
    }

    static ModuleField find(ModuleType<?, ?> type, String id) {
        return CACHE.computeIfAbsent(type, ModuleField::index).get(id);
    }

    private static Map<String, ModuleField> index(ModuleType<?, ?> type) {
        Map<String, ModuleField> fields = new HashMap<>();
        for (FieldLayout layout : type.spec().layout().fields()) {
            if (layout.sync()) {
                fields.put(layout.id(), new ModuleField(layout));
            }
        }
        return fields;
    }

    @SuppressWarnings("unchecked")
    <T> Property<T> property(Module<?> module, Class<T> wanted, String user) {
        Class<?> boxed = box(type);
        if (wanted == Object.class || wanted.isAssignableFrom(boxed)) {
            return (Property<T>) new FieldProperty(module, this, Function.identity(), Function.identity());
        }
        if (Number.class.isAssignableFrom(wanted) && Number.class.isAssignableFrom(boxed)) {
            return (Property<T>) new FieldProperty(module, this, value -> number((Number) value, wanted), value -> number((Number) value, boxed));
        }
        if (wanted == String.class) {
            return (Property<T>) new FieldProperty(module, this, String::valueOf, null);
        }
        throw new UiException(user + " wants a " + wanted.getSimpleName() + " but \"" + id + "\" on " + module.type().id() + " is a " + type.getSimpleName());
    }

    private static Object number(Number value, Class<?> type) {
        if (type == Double.class) {
            return value.doubleValue();
        }
        if (type == Float.class) {
            return value.floatValue();
        }
        if (type == Integer.class) {
            return (int) Math.round(value.doubleValue());
        }
        if (type == Long.class) {
            return Math.round(value.doubleValue());
        }
        if (type == Short.class) {
            return (short) Math.round(value.doubleValue());
        }
        if (type == Byte.class) {
            return (byte) Math.round(value.doubleValue());
        }
        return value;
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        return Character.class;
    }

    private record FieldProperty(Module<?> module, ModuleField field, Function<Object, Object> read, Function<Object, Object> write) implements Property<Object> {

        @Override
        public Object get() {
            return read.apply(field.getter.get(module));
        }

        @Override
        public void set(Object value) {
            if (!writable()) {
                throw new UiException("\"" + field.id + "\" on " + module.type().id() + " isn't client mutable");
            }
            field.setter.set(module, write.apply(value));
            module.markDirty(field.id);
        }

        @Override
        public boolean writable() {
            return field.setter != null && write != null;
        }

        @Override
        public void markChanged() {
            module.markDirty(field.id);
        }
    }
}
