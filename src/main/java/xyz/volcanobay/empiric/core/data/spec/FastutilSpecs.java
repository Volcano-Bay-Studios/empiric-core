package xyz.volcanobay.empiric.core.data.spec;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.ListUpdate;
import xyz.volcanobay.empiric.core.data.Mode;
import xyz.volcanobay.empiric.core.data.Slot;
import xyz.volcanobay.empiric.core.data.Spec;
import xyz.volcanobay.empiric.core.data.SpecFactory;
import xyz.volcanobay.empiric.core.data.SpecRegistry;
import xyz.volcanobay.empiric.core.data.Types;

import java.util.Map;
import java.util.function.Supplier;

public final class FastutilSpecs {

    private static final Slot<Integer> INT = new Slot<>(PrimitiveSpecs.INT, Mode.REPLACE);
    private static final Slot<Long> LONG = new Slot<>(PrimitiveSpecs.LONG, Mode.REPLACE);
    private static final Slot<Float> FLOAT = new Slot<>(PrimitiveSpecs.FLOAT, Mode.REPLACE);
    private static final Slot<Double> DOUBLE = new Slot<>(PrimitiveSpecs.DOUBLE, Mode.REPLACE);

    public static final Spec<IntList> INT_LIST = new IntListSpec();
    public static final Spec<LongList> LONG_LIST = new LongListSpec();
    public static final Spec<FloatList> FLOAT_LIST = new FloatListSpec();
    public static final Spec<DoubleList> DOUBLE_LIST = new DoubleListSpec();
    public static final Spec<IntSet> INT_SET = new IntSetSpec();
    public static final Spec<LongSet> LONG_SET = new LongSetSpec();

    public static final SpecFactory OBJECT_MAPS = (type, raw, resolution, registry) -> {
        Mode own = registry.mode(type, resolution, true, Mode.REPLACE);
        if (Int2ObjectMap.class.isAssignableFrom(raw)) {
            return map(Int2ObjectOpenHashMap::new, INT, registry.slot(Types.argument(type, 0), resolution.child(own)), resolution.listUpdate());
        }
        if (Long2ObjectMap.class.isAssignableFrom(raw)) {
            return map(Long2ObjectOpenHashMap::new, LONG, registry.slot(Types.argument(type, 0), resolution.child(own)), resolution.listUpdate());
        }
        if (Object2IntMap.class.isAssignableFrom(raw)) {
            return map(Object2IntOpenHashMap::new, registry.slot(Types.argument(type, 0), resolution.child(Mode.REPLACE)), INT, resolution.listUpdate());
        }
        if (Object2LongMap.class.isAssignableFrom(raw)) {
            return map(Object2LongOpenHashMap::new, registry.slot(Types.argument(type, 0), resolution.child(Mode.REPLACE)), LONG, resolution.listUpdate());
        }
        if (Object2FloatMap.class.isAssignableFrom(raw)) {
            return map(Object2FloatOpenHashMap::new, registry.slot(Types.argument(type, 0), resolution.child(Mode.REPLACE)), FLOAT, resolution.listUpdate());
        }
        if (Object2DoubleMap.class.isAssignableFrom(raw)) {
            return map(Object2DoubleOpenHashMap::new, registry.slot(Types.argument(type, 0), resolution.child(Mode.REPLACE)), DOUBLE, resolution.listUpdate());
        }
        return null;
    };

    private FastutilSpecs() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Spec<?> map(Supplier<? extends Map> factory, Slot<?> key, Slot<?> value, ListUpdate update) {
        return new CollectionSpecs.MapSpec<>((Supplier) factory, key, value, update);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <M> void registerMap(SpecRegistry registry, Class<M> type, Class<?> implementation, Supplier<? extends Map> factory, Slot<?> key, Slot<?> value) {
        Spec spec = map(factory, key, value, ListUpdate.SET);
        registry.register(type, spec);
        registry.register((Class) implementation, spec);
    }

    public static void register(SpecRegistry registry) {
        registry.register(IntList.class, INT_LIST);
        registry.register(IntArrayList.class, cast(INT_LIST));
        registry.register(LongList.class, LONG_LIST);
        registry.register(LongArrayList.class, cast(LONG_LIST));
        registry.register(FloatList.class, FLOAT_LIST);
        registry.register(FloatArrayList.class, cast(FLOAT_LIST));
        registry.register(DoubleList.class, DOUBLE_LIST);
        registry.register(DoubleArrayList.class, cast(DOUBLE_LIST));
        registry.register(IntSet.class, INT_SET);
        registry.register(IntOpenHashSet.class, cast(INT_SET));
        registry.register(LongSet.class, LONG_SET);
        registry.register(LongOpenHashSet.class, cast(LONG_SET));
        registerMap(registry, Int2IntMap.class, Int2IntOpenHashMap.class, Int2IntOpenHashMap::new, INT, INT);
        registerMap(registry, Int2LongMap.class, Int2LongOpenHashMap.class, Int2LongOpenHashMap::new, INT, LONG);
        registerMap(registry, Int2DoubleMap.class, Int2DoubleOpenHashMap.class, Int2DoubleOpenHashMap::new, INT, DOUBLE);
        registerMap(registry, Long2IntMap.class, Long2IntOpenHashMap.class, Long2IntOpenHashMap::new, LONG, INT);
        registerMap(registry, Long2LongMap.class, Long2LongOpenHashMap.class, Long2LongOpenHashMap::new, LONG, LONG);
        registerMap(registry, Long2DoubleMap.class, Long2DoubleOpenHashMap.class, Long2DoubleOpenHashMap::new, LONG, DOUBLE);
        registry.register(OBJECT_MAPS);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Spec<T> cast(Spec<?> spec) {
        return (Spec) spec;
    }

    abstract static class PrimitiveCollectionSpec<C> implements Spec<C> {

        abstract int size(C value);

        abstract C create(int size);

        abstract void clear(C value);

        abstract void writeElements(DataBuffer buffer, C value);

        abstract void readElements(DataBuffer buffer, C value, int count);

        abstract void addAll(C target, C source);

        @Override
        public void write(DataBuffer buffer, C value, DataContext context) {
            buffer.beginArray(size(value));
            writeElements(buffer, value);
            buffer.endArray();
        }

        @Override
        public C read(DataBuffer buffer, DataContext context) {
            int count = ArraySpecs.checkedLength(buffer);
            C value = create(count);
            readElements(buffer, value, count);
            buffer.endArray();
            return value;
        }

        @Override
        public C update(DataBuffer buffer, C existing, DataContext context) {
            int count = ArraySpecs.checkedLength(buffer);
            clear(existing);
            readElements(buffer, existing, count);
            buffer.endArray();
            return existing;
        }

        @Override
        public boolean supportsUpdate() {
            return true;
        }

        @Override
        public C copy(C value) {
            C copy = create(size(value));
            addAll(copy, value);
            return copy;
        }

        @Override
        public C copyInto(C source, C target) {
            clear(target);
            addAll(target, source);
            return target;
        }
    }

    static final class IntListSpec extends PrimitiveCollectionSpec<IntList> {

        @Override
        int size(IntList value) {
            return value.size();
        }

        @Override
        IntList create(int size) {
            return new IntArrayList(size);
        }

        @Override
        void clear(IntList value) {
            value.clear();
        }

        @Override
        void writeElements(DataBuffer buffer, IntList value) {
            for (int i = 0; i < value.size(); i++) {
                buffer.putSignedVarInt(value.getInt(i));
            }
        }

        @Override
        void readElements(DataBuffer buffer, IntList value, int count) {
            for (int i = 0; i < count; i++) {
                value.add(buffer.takeSignedVarInt());
            }
        }

        @Override
        void addAll(IntList target, IntList source) {
            target.addAll(source);
        }
    }

    static final class LongListSpec extends PrimitiveCollectionSpec<LongList> {

        @Override
        int size(LongList value) {
            return value.size();
        }

        @Override
        LongList create(int size) {
            return new LongArrayList(size);
        }

        @Override
        void clear(LongList value) {
            value.clear();
        }

        @Override
        void writeElements(DataBuffer buffer, LongList value) {
            for (int i = 0; i < value.size(); i++) {
                buffer.putSignedVarLong(value.getLong(i));
            }
        }

        @Override
        void readElements(DataBuffer buffer, LongList value, int count) {
            for (int i = 0; i < count; i++) {
                value.add(buffer.takeSignedVarLong());
            }
        }

        @Override
        void addAll(LongList target, LongList source) {
            target.addAll(source);
        }
    }

    static final class FloatListSpec extends PrimitiveCollectionSpec<FloatList> {

        @Override
        int size(FloatList value) {
            return value.size();
        }

        @Override
        FloatList create(int size) {
            return new FloatArrayList(size);
        }

        @Override
        void clear(FloatList value) {
            value.clear();
        }

        @Override
        void writeElements(DataBuffer buffer, FloatList value) {
            for (int i = 0; i < value.size(); i++) {
                buffer.putFloat(value.getFloat(i));
            }
        }

        @Override
        void readElements(DataBuffer buffer, FloatList value, int count) {
            for (int i = 0; i < count; i++) {
                value.add(buffer.takeFloat());
            }
        }

        @Override
        void addAll(FloatList target, FloatList source) {
            target.addAll(source);
        }
    }

    static final class DoubleListSpec extends PrimitiveCollectionSpec<DoubleList> {

        @Override
        int size(DoubleList value) {
            return value.size();
        }

        @Override
        DoubleList create(int size) {
            return new DoubleArrayList(size);
        }

        @Override
        void clear(DoubleList value) {
            value.clear();
        }

        @Override
        void writeElements(DataBuffer buffer, DoubleList value) {
            for (int i = 0; i < value.size(); i++) {
                buffer.putDouble(value.getDouble(i));
            }
        }

        @Override
        void readElements(DataBuffer buffer, DoubleList value, int count) {
            for (int i = 0; i < count; i++) {
                value.add(buffer.takeDouble());
            }
        }

        @Override
        void addAll(DoubleList target, DoubleList source) {
            target.addAll(source);
        }
    }

    static final class IntSetSpec extends PrimitiveCollectionSpec<IntSet> {

        @Override
        int size(IntSet value) {
            return value.size();
        }

        @Override
        IntSet create(int size) {
            return new IntOpenHashSet(size);
        }

        @Override
        void clear(IntSet value) {
            value.clear();
        }

        @Override
        void writeElements(DataBuffer buffer, IntSet value) {
            value.forEach((int entry) -> buffer.putSignedVarInt(entry));
        }

        @Override
        void readElements(DataBuffer buffer, IntSet value, int count) {
            for (int i = 0; i < count; i++) {
                value.add(buffer.takeSignedVarInt());
            }
        }

        @Override
        void addAll(IntSet target, IntSet source) {
            target.addAll(source);
        }
    }

    static final class LongSetSpec extends PrimitiveCollectionSpec<LongSet> {

        @Override
        int size(LongSet value) {
            return value.size();
        }

        @Override
        LongSet create(int size) {
            return new LongOpenHashSet(size);
        }

        @Override
        void clear(LongSet value) {
            value.clear();
        }

        @Override
        void writeElements(DataBuffer buffer, LongSet value) {
            value.forEach((long entry) -> buffer.putSignedVarLong(entry));
        }

        @Override
        void readElements(DataBuffer buffer, LongSet value, int count) {
            for (int i = 0; i < count; i++) {
                value.add(buffer.takeSignedVarLong());
            }
        }

        @Override
        void addAll(LongSet target, LongSet source) {
            target.addAll(source);
        }
    }
}
