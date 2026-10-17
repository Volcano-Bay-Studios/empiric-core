package xyz.volcanobay.empiric.core.data.spec;

import xyz.volcanobay.empiric.core.data.Data;
import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.DataException;
import xyz.volcanobay.empiric.core.data.DataPrinter;
import xyz.volcanobay.empiric.core.data.Mode;
import xyz.volcanobay.empiric.core.data.Resolution;
import xyz.volcanobay.empiric.core.data.Slot;
import xyz.volcanobay.empiric.core.data.Spec;
import xyz.volcanobay.empiric.core.data.SpecFactory;
import xyz.volcanobay.empiric.core.data.layout.Accessors;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class TypeSpecs {

    public static final SpecFactory ENUMS = (type, raw, resolution, registry) -> raw.isEnum() ? new EnumSpec<>(raw) : null;

    public static final SpecFactory RECORDS = (type, raw, resolution, registry) -> {
        if (!raw.isRecord()) {
            return null;
        }
        RecordComponent[] components = raw.getRecordComponents();
        Slot<?>[] slots = new Slot<?>[components.length];
        Resolution child = resolution.child(Mode.REPLACE);
        for (int i = 0; i < components.length; i++) {
            Slot<?> slot = registry.slot(components[i].getGenericType(), child);
            Data data = data(raw, components[i]);
            slots[i] = data != null && data.optional() ? NullableSpec.wrap(slot) : slot;
        }
        return new RecordSpec<>(raw, components, slots);
    };

    private TypeSpecs() {
    }

    private static Data data(Class<?> raw, RecordComponent component) {
        try {
            return raw.getDeclaredField(component.getName()).getAnnotation(Data.class);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    static final class EnumSpec<E> implements Spec<E> {

        private final Class<?> type;
        private final E[] constants;
        private final Map<String, E> byName = new HashMap<>();

        @SuppressWarnings("unchecked")
        EnumSpec(Class<?> type) {
            this.type = type;
            this.constants = (E[]) type.getEnumConstants();
            for (E constant : constants) {
                byName.put(((Enum<?>) constant).name(), constant);
            }
        }

        @Override
        public void write(DataBuffer buffer, E value, DataContext context) {
            if (context.keyed()) {
                buffer.putString(((Enum<?>) value).name());
            } else {
                buffer.putVarInt(((Enum<?>) value).ordinal());
            }
        }

        @Override
        public E read(DataBuffer buffer, DataContext context) {
            if (context.keyed()) {
                String name = buffer.takeString();
                E constant = byName.get(name);
                if (constant == null) {
                    throw new DataException(type.getName() + " has no constant " + name);
                }
                return constant;
            }
            int ordinal = buffer.takeVarInt();
            if (ordinal < 0 || ordinal >= constants.length) {
                throw new DataException(type.getName() + " has no constant at " + ordinal);
            }
            return constants[ordinal];
        }

        @Override
        public boolean keyed() {
            return true;
        }

        @Override
        public String toKey(E value) {
            return ((Enum<?>) value).name();
        }

        @Override
        public E fromKey(String key) {
            E constant = byName.get(key);
            if (constant == null) {
                throw new DataException(type.getName() + " has no constant " + key);
            }
            return constant;
        }
    }

    static final class RecordSpec<R> implements Spec<R> {

        private final Class<?> type;
        private final String[] ids;
        private final Slot<Object>[] slots;
        private final Accessors.ObjectGetter[] accessors;
        private final Function<Object[], Object> constructor;
        private final Object[] defaults;
        private final Map<String, Integer> byId = new HashMap<>();

        @SuppressWarnings({"unchecked", "rawtypes"})
        RecordSpec(Class<?> type, RecordComponent[] components, Slot<?>[] slots) {
            this.type = type;
            this.ids = new String[components.length];
            this.slots = (Slot<Object>[]) slots;
            this.accessors = new Accessors.ObjectGetter[components.length];
            this.defaults = new Object[components.length];
            Class<?>[] types = new Class<?>[components.length];
            try {
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
                for (int i = 0; i < components.length; i++) {
                    RecordComponent component = components[i];
                    Data data = data(type, component);
                    ids[i] = data != null && !data.id().isEmpty() ? data.id() : component.getName();
                    byId.put(ids[i], i);
                    if (data != null) {
                        for (String alias : data.aliases()) {
                            byId.put(alias, i);
                        }
                    }
                    types[i] = component.getType();
                    accessors[i] = Accessors.bind(Accessors.ObjectGetter.class, lookup.unreflect(component.getAccessor()));
                    defaults[i] = types[i].isPrimitive() ? Array.get(Array.newInstance(types[i], 1), 0) : null;
                }
                this.constructor = Accessors.bind(Function.class, lookup.findConstructor(type, MethodType.methodType(void.class, types))
                        .asSpreader(Object[].class, components.length));
            } catch (ReflectiveOperationException e) {
                throw new DataException("Can't access record " + type.getName(), e);
            }
        }

        private Object component(R value, int index) {
            return accessors[index].get(value);
        }

        @SuppressWarnings("unchecked")
        private R construct(Object[] values) {
            return (R) constructor.apply(values);
        }

        @Override
        public void write(DataBuffer buffer, R value, DataContext context) {
            if (context.keyed()) {
                buffer.beginObject(ids.length);
                for (int i = 0; i < ids.length; i++) {
                    int marker = buffer.beginEntry(ids[i]);
                    slots[i].write(buffer, component(value, i), context);
                    buffer.endEntry(marker);
                }
                buffer.endObject();
                return;
            }
            for (int i = 0; i < slots.length; i++) {
                slots[i].write(buffer, component(value, i), context);
            }
        }

        @Override
        public R read(DataBuffer buffer, DataContext context) {
            Object[] values = defaults.clone();
            if (context.keyed()) {
                int count = buffer.takeObject();
                for (int i = 0; i < count; i++) {
                    DataBuffer.Entry entry = buffer.takeEntry();
                    Integer index = byId.get(entry.key());
                    if (index == null) {
                        buffer.skipEntry(entry);
                        continue;
                    }
                    values[index] = slots[index].read(buffer, null, context);
                    buffer.finishEntry(entry);
                }
                buffer.endObject();
                return construct(values);
            }
            for (int i = 0; i < slots.length; i++) {
                values[i] = slots[i].read(buffer, null, context);
            }
            return construct(values);
        }

        @Override
        public boolean same(R first, R second) {
            for (int i = 0; i < slots.length; i++) {
                Object a = component(first, i);
                Object b = component(second, i);
                if (a == null || b == null ? a != b : !slots[i].spec().same(a, b)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void print(R value, DataPrinter printer) {
            printer.open(type.getSimpleName() + " {");
            for (int i = 0; i < slots.length; i++) {
                printer.entry(ids[i], component(value, i), slots[i].spec());
            }
            printer.close("}");
        }
    }
}
