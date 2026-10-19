package xyz.volcanobay.empiric.core.data.spec;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.DataException;
import xyz.volcanobay.empiric.core.data.DataPrinter;
import xyz.volcanobay.empiric.core.data.Mode;
import xyz.volcanobay.empiric.core.data.Resolution;
import xyz.volcanobay.empiric.core.data.Spec;
import xyz.volcanobay.empiric.core.data.SpecRegistry;
import xyz.volcanobay.empiric.core.data.layout.ClassLayout;
import xyz.volcanobay.empiric.core.data.layout.FieldIO;
import xyz.volcanobay.empiric.core.data.layout.FieldLayout;
import xyz.volcanobay.empiric.core.data.layout.KeyedFormat;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

public final class ObjectSpec<T> implements Spec<T> {

    private final ClassLayout layout;
    private final FieldIO[] fields;
    private final FieldIO[] saved;
    private final FieldIO[] synced;
    private final int[] watched;
    private final BitSet clientMutable = new BitSet();
    private final Map<String, FieldIO> byId;

    public ObjectSpec(ClassLayout layout, Resolution resolution, SpecRegistry registry) {
        this.layout = layout;
        Mode own = registry.mode(layout.type(), resolution, true, layout.mode());
        Resolution inherited = resolution.child(own);
        List<FieldLayout> layouts = layout.fields();
        this.fields = new FieldIO[layouts.size()];
        List<FieldIO> saved = new ArrayList<>();
        List<FieldIO> synced = new ArrayList<>();
        List<Integer> watched = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            FieldLayout field = layouts.get(i);
            FieldIO io = FieldIO.bind(field, inherited, registry);
            fields[i] = io;
            if (field.save()) {
                saved.add(io);
            }
            if (field.sync()) {
                if (field.watch()) {
                    watched.add(synced.size());
                }
                if (field.clientMutable()) {
                    clientMutable.set(synced.size());
                }
                synced.add(io);
            }
        }
        this.saved = saved.toArray(FieldIO[]::new);
        this.synced = synced.toArray(FieldIO[]::new);
        this.watched = watched.stream().mapToInt(Integer::intValue).toArray();
        this.byId = KeyedFormat.index(this.saved);
    }

    public ClassLayout layout() {
        return layout;
    }

    public FieldIO[] synced() {
        return synced;
    }

    public int syncIndex(String id) {
        for (int i = 0; i < synced.length; i++) {
            if (synced[i].id().equals(id)) {
                return i;
            }
        }
        throw new DataException(layout.type().getName() + " has no synced field \"" + id + "\"");
    }

    public BitSet clientMutable() {
        return (BitSet) clientMutable.clone();
    }

    public boolean isClientMutable(int index) {
        return clientMutable.get(index);
    }

    public boolean hasClientMutable() {
        return !clientMutable.isEmpty();
    }

    public void writeFramed(DataBuffer buffer, T value, BitSet dirty, DataContext context) {
        buffer.putVarInt(dirty.cardinality());
        for (int i = dirty.nextSetBit(0); i >= 0; i = dirty.nextSetBit(i + 1)) {
            buffer.putVarInt(i);
            int position = buffer.writePosition();
            buffer.putInt(0);
            synced[i].write(buffer, value, context);
            buffer.patchInt(position, buffer.writePosition() - position - 4);
        }
    }

    public BitSet readFramed(DataBuffer buffer, T value, DataContext context, IntPredicate allow) {
        BitSet applied = new BitSet();
        int count = buffer.takeVarInt();
        for (int i = 0; i < count; i++) {
            int index = buffer.takeVarInt();
            int length = buffer.takeInt();
            if (length < 0 || length > buffer.readable()) {
                throw new DataException(layout.type().getName() + " sent a field of " + length + " bytes with only " + buffer.readable() + " left");
            }
            if (index < 0 || index >= synced.length || !allow.test(index)) {
                buffer.skip(length);
                continue;
            }
            synced[index].read(buffer.wrap(buffer.takeBytes(length)), value, context);
            applied.set(index);
        }
        return applied;
    }

    public boolean watches() {
        return watched.length > 0;
    }

    public Object[] snapshot(T value) {
        Object[] snapshot = new Object[watched.length];
        for (int i = 0; i < watched.length; i++) {
            snapshot[i] = synced[watched[i]].snapshot(value);
        }
        return snapshot;
    }

    /**
     * Re-snapshots the watched fields among {@code fields}, leaving the others to keep detecting local changes.
     */
    public void refreshSnapshot(T value, Object[] snapshot, BitSet fields) {
        for (int i = 0; i < watched.length; i++) {
            if (fields.get(watched[i])) {
                snapshot[i] = synced[watched[i]].snapshot(value);
            }
        }
    }

    public boolean detectChanges(T value, Object[] snapshot, BitSet dirty) {
        boolean changed = false;
        for (int i = 0; i < watched.length; i++) {
            FieldIO field = synced[watched[i]];
            if (!field.matches(value, snapshot[i])) {
                snapshot[i] = field.snapshot(value);
                dirty.set(watched[i]);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void write(DataBuffer buffer, T value, DataContext context) {
        if (context.keyed()) {
            KeyedFormat.write(buffer, value, saved, layout.version(), context);
            return;
        }
        for (FieldIO field : synced) {
            field.write(buffer, value, context);
        }
    }

    public void writeDirty(DataBuffer buffer, T value, BitSet dirty, DataContext context) {
        buffer.putVarInt(dirty.cardinality());
        for (int i = dirty.nextSetBit(0); i >= 0; i = dirty.nextSetBit(i + 1)) {
            buffer.putVarInt(i);
            synced[i].write(buffer, value, context);
        }
    }

    public BitSet readDirty(DataBuffer buffer, T value, DataContext context) {
        BitSet read = new BitSet();
        int count = buffer.takeVarInt();
        for (int i = 0; i < count; i++) {
            int index = buffer.takeVarInt();
            if (index < 0 || index >= synced.length) {
                throw new DataException(layout.type().getName() + " has no synced field at " + index);
            }
            synced[index].read(buffer, value, context);
            read.set(index);
        }
        return read;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T read(DataBuffer buffer, DataContext context) {
        return update(buffer, (T) layout.newInstance(), context);
    }

    @Override
    public T update(DataBuffer buffer, T existing, DataContext context) {
        if (context.keyed()) {
            KeyedFormat.read(buffer, existing, byId, layout.type(), layout.version(), context);
            return existing;
        }
        for (FieldIO field : synced) {
            field.read(buffer, existing, context);
        }
        return existing;
    }

    @Override
    public boolean supportsUpdate() {
        return true;
    }

    @Override
    public Mode defaultMode() {
        return layout.mode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T copy(T value) {
        if (!layout.constructible()) {
            return value;
        }
        return copyInto(value, (T) layout.newInstance());
    }

    @Override
    public T copyInto(T source, T target) {
        for (FieldIO field : fields) {
            field.copy(source, target);
        }
        return target;
    }

    @Override
    public boolean same(T first, T second) {
        if (first == second) {
            return true;
        }
        for (FieldIO field : fields) {
            if (!field.same(first, second)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void print(T value, DataPrinter printer) {
        printer.open(layout.type().getSimpleName() + " {");
        for (FieldIO field : fields) {
            field.print(value, printer);
        }
        printer.close("}");
    }
}
