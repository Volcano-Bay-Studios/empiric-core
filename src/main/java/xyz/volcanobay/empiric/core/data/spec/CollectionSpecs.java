package xyz.volcanobay.empiric.core.data.spec;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.DataPrinter;
import xyz.volcanobay.empiric.core.data.FieldDelta;
import xyz.volcanobay.empiric.core.data.ListUpdate;
import xyz.volcanobay.empiric.core.data.Mode;
import xyz.volcanobay.empiric.core.data.Slot;
import xyz.volcanobay.empiric.core.data.Spec;
import xyz.volcanobay.empiric.core.data.SpecFactory;
import xyz.volcanobay.empiric.core.data.Types;
import xyz.volcanobay.empiric.core.data.layout.Accessors;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

public final class CollectionSpecs {

    public static final SpecFactory COLLECTIONS = (type, raw, resolution, registry) -> {
        if (!Collection.class.isAssignableFrom(raw)) {
            return null;
        }
        Supplier<Collection<Object>> factory = collectionFactory(raw);
        if (factory == null) {
            return null;
        }
        Mode own = registry.mode(type, resolution, true, Mode.REPLACE);
        return new CollectionSpec<>(factory, registry.slot(Types.argument(type, 0), resolution.child(own)), resolution.listUpdate());
    };

    public static final SpecFactory MAPS = (type, raw, resolution, registry) -> {
        if (!Map.class.isAssignableFrom(raw)) {
            return null;
        }
        Supplier<Map<Object, Object>> factory = mapFactory(raw);
        if (factory == null) {
            return null;
        }
        Mode own = registry.mode(type, resolution, true, Mode.REPLACE);
        Slot<?> key = registry.slot(Types.argument(type, 0), resolution.child(Mode.REPLACE).withMode(Mode.REPLACE, 0));
        Slot<?> value = registry.slot(Types.argument(type, 1), resolution.child(own));
        return new MapSpec<>(factory, key, value, resolution.listUpdate());
    };

    public static final SpecFactory OPTIONALS = (type, raw, resolution, registry) -> {
        if (raw != Optional.class) {
            return null;
        }
        return new OptionalSpec<>(registry.slot(Types.argument(type, 0), resolution.child(Mode.REPLACE)));
    };

    private CollectionSpecs() {
    }

    @SuppressWarnings("unchecked")
    private static <T> Supplier<T> constructor(Class<?> raw) {
        if (raw.isInterface() || Modifier.isAbstract(raw.getModifiers())) {
            return null;
        }
        try {
            return Accessors.bind(Supplier.class, MethodHandles.publicLookup().findConstructor(raw, MethodType.methodType(void.class)));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    private static Supplier<Collection<Object>> collectionFactory(Class<?> raw) {
        if (raw == List.class || raw == Collection.class) {
            return ArrayList::new;
        }
        if (raw == Set.class) {
            return LinkedHashSet::new;
        }
        if (raw == SortedSet.class || raw == NavigableSet.class) {
            return TreeSet::new;
        }
        if (raw == Queue.class || raw == Deque.class) {
            return ArrayDeque::new;
        }
        if (raw.getTypeParameters().length != 1) {
            return null;
        }
        return constructor(raw);
    }

    private static Supplier<Map<Object, Object>> mapFactory(Class<?> raw) {
        if (raw == Map.class) {
            return LinkedHashMap::new;
        }
        if (raw == java.util.SortedMap.class || raw == java.util.NavigableMap.class) {
            return TreeMap::new;
        }
        if (raw.getTypeParameters().length != 2) {
            return null;
        }
        return constructor(raw);
    }

    static final class CollectionSpec<E, C extends Collection<E>> implements Spec<C>, DeltaSpec<C> {

        private final Supplier<C> factory;
        private final Slot<E> element;
        private final ListUpdate update;

        @SuppressWarnings("unchecked")
        CollectionSpec(Supplier<? extends Collection<Object>> factory, Slot<?> element, ListUpdate update) {
            this.factory = (Supplier<C>) (Supplier<?>) factory;
            this.element = (Slot<E>) element;
            this.update = update;
        }

        @Override
        public void write(DataBuffer buffer, C value, DataContext context) {
            buffer.beginArray(value.size());
            for (E entry : value) {
                buffer.element();
                element.write(buffer, entry, context);
            }
            buffer.endArray();
        }

        private E readElement(DataBuffer buffer, E existing, DataContext context) {
            buffer.takeElement();
            return element.read(buffer, existing, context);
        }

        @Override
        public C read(DataBuffer buffer, DataContext context) {
            C collection = factory.get();
            int count = buffer.takeArray();
            for (int i = 0; i < count; i++) {
                collection.add(readElement(buffer, null, context));
            }
            buffer.endArray();
            return collection;
        }

        @Override
        @SuppressWarnings("unchecked")
        public C update(DataBuffer buffer, C existing, DataContext context) {
            int count = buffer.takeArray();
            if (update == ListUpdate.APPEND) {
                for (int i = 0; i < count; i++) {
                    existing.add(readElement(buffer, null, context));
                }
                buffer.endArray();
                return existing;
            }
            if (element.updates() && existing instanceof List<?>) {
                List<E> list = (List<E>) existing;
                for (int i = 0; i < count; i++) {
                    if (i < list.size()) {
                        E current = list.get(i);
                        E value = readElement(buffer, current, context);
                        if (value != current) {
                            list.set(i, value);
                        }
                    } else {
                        list.add(readElement(buffer, null, context));
                    }
                }
                if (list.size() > count) {
                    list.subList(count, list.size()).clear();
                }
                buffer.endArray();
                return existing;
            }
            existing.clear();
            for (int i = 0; i < count; i++) {
                existing.add(readElement(buffer, null, context));
            }
            buffer.endArray();
            return existing;
        }

        @Override
        public boolean supportsUpdate() {
            return true;
        }

        @Override
        public C copy(C value) {
            C collection = factory.get();
            for (E entry : value) {
                collection.add(element.spec().copy(entry));
            }
            return collection;
        }

        @Override
        public C copyInto(C source, C target) {
            target.clear();
            for (E entry : source) {
                target.add(element.spec().copy(entry));
            }
            return target;
        }

        @Override
        public boolean same(C first, C second) {
            if (first.size() != second.size()) {
                return false;
            }
            if (first instanceof Set<?>) {
                return first.equals(second);
            }
            Iterator<E> a = first.iterator();
            Iterator<E> b = second.iterator();
            while (a.hasNext()) {
                if (!element.spec().same(a.next(), b.next())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean supportsDelta() {
            return factory.get() instanceof Set;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void writeDelta(DataBuffer buffer, C value, FieldDelta delta, DataContext context) {
            List<E> added = new ArrayList<>();
            List<E> removed = new ArrayList<>();
            for (Object entry : delta.changed()) {
                (value.contains(entry) ? added : removed).add((E) entry);
            }
            for (Object entry : delta.removed()) {
                if (!value.contains(entry)) {
                    removed.add((E) entry);
                }
            }
            buffer.putBoolean(delta.replace());
            buffer.beginArray(removed.size());
            for (E entry : removed) {
                buffer.element();
                element.write(buffer, entry, context);
            }
            buffer.endArray();
            buffer.beginArray(added.size());
            for (E entry : added) {
                buffer.element();
                element.write(buffer, entry, context);
            }
            buffer.endArray();
        }

        @Override
        public void readDelta(DataBuffer buffer, C existing, DataContext context) {
            if (buffer.takeBoolean()) {
                existing.clear();
            }
            int removed = buffer.takeArray();
            for (int i = 0; i < removed; i++) {
                existing.remove(readElement(buffer, null, context));
            }
            buffer.endArray();
            int added = buffer.takeArray();
            for (int i = 0; i < added; i++) {
                existing.add(readElement(buffer, null, context));
            }
            buffer.endArray();
        }

        @Override
        public Object entry(C value, Object key) {
            return value.contains(key) ? key : null;
        }

        @Override
        public Iterable<?> keys(C value) {
            return value;
        }

        @Override
        public void print(C value, DataPrinter printer) {
            printer.open("[");
            for (E entry : value) {
                printer.element(entry, element.spec());
            }
            printer.close("]");
        }
    }

    static final class MapSpec<K, V, M extends Map<K, V>> implements Spec<M>, DeltaSpec<M> {

        private final Supplier<M> factory;
        private final Slot<K> key;
        private final Slot<V> value;
        private final ListUpdate update;

        @SuppressWarnings("unchecked")
        MapSpec(Supplier<? extends Map<Object, Object>> factory, Slot<?> key, Slot<?> value, ListUpdate update) {
            this.factory = (Supplier<M>) (Supplier<?>) factory;
            this.key = (Slot<K>) key;
            this.value = (Slot<V>) value;
            this.update = update;
        }

        private boolean keyedObject(DataContext context) {
            return context.keyed() && key.spec().keyed();
        }

        @Override
        public void write(DataBuffer buffer, M map, DataContext context) {
            if (keyedObject(context)) {
                buffer.beginObject(map.size());
                for (Map.Entry<K, V> entry : map.entrySet()) {
                    int marker = buffer.beginEntry(key.spec().toKey(entry.getKey()));
                    value.write(buffer, entry.getValue(), context);
                    buffer.endEntry(marker);
                }
                buffer.endObject();
                return;
            }
            buffer.beginArray(map.size());
            for (Map.Entry<K, V> entry : map.entrySet()) {
                buffer.element();
                if (context.keyed()) {
                    buffer.beginArray(2);
                    buffer.element();
                    key.write(buffer, entry.getKey(), context);
                    buffer.element();
                    value.write(buffer, entry.getValue(), context);
                    buffer.endArray();
                } else {
                    key.write(buffer, entry.getKey(), context);
                    value.write(buffer, entry.getValue(), context);
                }
            }
            buffer.endArray();
        }

        private interface EntryReader<K> {
            void read(K key, DataBuffer buffer);
        }

        private void readEntries(DataBuffer buffer, DataContext context, EntryReader<K> reader) {
            if (keyedObject(context)) {
                int count = buffer.takeObject();
                for (int i = 0; i < count; i++) {
                    DataBuffer.Entry entry = buffer.takeEntry();
                    reader.read(key.spec().fromKey(entry.key()), buffer);
                    buffer.finishEntry(entry);
                }
                buffer.endObject();
                return;
            }
            int count = buffer.takeArray();
            for (int i = 0; i < count; i++) {
                buffer.takeElement();
                if (context.keyed()) {
                    buffer.takeArray();
                    buffer.takeElement();
                    K k = key.read(buffer, null, context);
                    buffer.takeElement();
                    reader.read(k, buffer);
                    buffer.endArray();
                } else {
                    reader.read(key.read(buffer, null, context), buffer);
                }
            }
            buffer.endArray();
        }

        @Override
        public M read(DataBuffer buffer, DataContext context) {
            M map = factory.get();
            readEntries(buffer, context, (k, input) -> map.put(k, value.read(input, null, context)));
            return map;
        }

        @Override
        public M update(DataBuffer buffer, M existing, DataContext context) {
            if (update == ListUpdate.APPEND) {
                readEntries(buffer, context, (k, input) -> existing.put(k, value.read(input, existing.get(k), context)));
                return existing;
            }
            if (value.updates()) {
                Set<K> seen = new LinkedHashSet<>();
                readEntries(buffer, context, (k, input) -> {
                    seen.add(k);
                    V current = existing.get(k);
                    V read = value.read(input, current, context);
                    if (read != current) {
                        existing.put(k, read);
                    }
                });
                existing.keySet().retainAll(seen);
                return existing;
            }
            existing.clear();
            readEntries(buffer, context, (k, input) -> existing.put(k, value.read(input, null, context)));
            return existing;
        }

        @Override
        public boolean supportsUpdate() {
            return true;
        }

        @Override
        public M copy(M map) {
            M copy = factory.get();
            for (Map.Entry<K, V> entry : map.entrySet()) {
                copy.put(entry.getKey(), value.spec().copy(entry.getValue()));
            }
            return copy;
        }

        @Override
        public M copyInto(M source, M target) {
            target.clear();
            for (Map.Entry<K, V> entry : source.entrySet()) {
                target.put(entry.getKey(), value.spec().copy(entry.getValue()));
            }
            return target;
        }

        @Override
        public boolean same(M first, M second) {
            if (first.size() != second.size()) {
                return false;
            }
            for (Map.Entry<K, V> entry : first.entrySet()) {
                V other = second.get(entry.getKey());
                if (other == null || !value.spec().same(entry.getValue(), other)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean supportsDelta() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void writeDelta(DataBuffer buffer, M map, FieldDelta delta, DataContext context) {
            List<K> upserts = new ArrayList<>();
            List<K> removed = new ArrayList<>();
            for (Object entry : delta.changed()) {
                (map.containsKey(entry) ? upserts : removed).add((K) entry);
            }
            for (Object entry : delta.removed()) {
                if (!map.containsKey(entry)) {
                    removed.add((K) entry);
                }
            }
            buffer.putBoolean(delta.replace());
            buffer.beginArray(removed.size());
            for (K entry : removed) {
                buffer.element();
                key.write(buffer, entry, context);
            }
            buffer.endArray();
            buffer.beginArray(upserts.size());
            for (K entry : upserts) {
                buffer.element();
                key.write(buffer, entry, context);
                value.write(buffer, map.get(entry), context);
            }
            buffer.endArray();
        }

        @Override
        public void readDelta(DataBuffer buffer, M existing, DataContext context) {
            if (buffer.takeBoolean()) {
                existing.clear();
            }
            int removed = buffer.takeArray();
            for (int i = 0; i < removed; i++) {
                buffer.takeElement();
                existing.remove(key.read(buffer, null, context));
            }
            buffer.endArray();
            int upserts = buffer.takeArray();
            for (int i = 0; i < upserts; i++) {
                buffer.takeElement();
                K k = key.read(buffer, null, context);
                V current = existing.get(k);
                V read = value.read(buffer, current, context);
                if (read != current) {
                    existing.put(k, read);
                }
            }
            buffer.endArray();
        }

        @Override
        public Object entry(M map, Object key) {
            return map.get(key);
        }

        @Override
        public Iterable<?> keys(M map) {
            return map.keySet();
        }

        @Override
        public void print(M map, DataPrinter printer) {
            printer.open("{");
            for (Map.Entry<K, V> entry : map.entrySet()) {
                printer.entry(String.valueOf(entry.getKey()), entry.getValue(), value.spec());
            }
            printer.close("}");
        }
    }

    static final class OptionalSpec<T> implements Spec<Optional<T>> {

        private final Slot<T> inner;

        @SuppressWarnings("unchecked")
        OptionalSpec(Slot<?> inner) {
            this.inner = (Slot<T>) inner;
        }

        @Override
        public void write(DataBuffer buffer, Optional<T> value, DataContext context) {
            buffer.putPresence(value.isPresent());
            value.ifPresent(present -> inner.write(buffer, present, context));
        }

        @Override
        public Optional<T> read(DataBuffer buffer, DataContext context) {
            return buffer.takePresence() ? Optional.of(inner.read(buffer, null, context)) : Optional.empty();
        }

        @Override
        public boolean same(Optional<T> first, Optional<T> second) {
            if (first.isEmpty() || second.isEmpty()) {
                return first.isEmpty() == second.isEmpty();
            }
            return inner.spec().same(first.get(), second.get());
        }

        @Override
        public void print(Optional<T> value, DataPrinter printer) {
            if (value.isEmpty()) {
                printer.value("empty");
            } else {
                inner.spec().print(value.get(), printer);
            }
        }
    }
}
