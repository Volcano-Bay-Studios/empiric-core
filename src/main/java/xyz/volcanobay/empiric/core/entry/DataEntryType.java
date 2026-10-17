package xyz.volcanobay.empiric.core.entry;

import xyz.volcanobay.empiric.core.data.DataException;
import xyz.volcanobay.empiric.core.data.Encoding;
import xyz.volcanobay.empiric.core.data.SpecRegistry;
import xyz.volcanobay.empiric.core.data.spec.ObjectSpec;
import xyz.volcanobay.empiric.core.data.tree.DataNode;
import xyz.volcanobay.empiric.core.data.tree.TreeDataBuffer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public class DataEntryType<T> {

    private final String id;
    private final Class<T> type;
    private final Supplier<T> defaults;
    private final Set<String> reserved;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private ObjectSpec<T> spec;
    private SpecRegistry specs;
    private T defaultValue;
    private volatile Map<String, DataEntry<T>> entries = Map.of();
    int index = -1;

    public DataEntryType(String id, Class<T> type, Supplier<T> defaults, String... reserved) {
        this.id = id;
        this.type = type;
        this.defaults = defaults;
        this.reserved = Set.of(reserved);
    }

    void bind(SpecRegistry specs, int index) {
        this.specs = specs;
        this.spec = specs.objectSpec(type);
        this.index = index;
    }

    public String id() {
        return id;
    }

    public Class<T> type() {
        return type;
    }

    public int index() {
        return index;
    }

    public String directory() {
        return id.replace(':', '/');
    }

    public Set<String> reserved() {
        return reserved;
    }

    public T defaultValue() {
        if (defaultValue == null) {
            defaultValue = defaults.get();
        }
        return defaultValue;
    }

    private void checkBound() {
        if (spec == null) {
            throw new DataException("Data entry type " + id + " isn't registered");
        }
    }

    public DataEntry<T> parse(String entryId, DataNode node) {
        checkBound();
        if (!node.isObject()) {
            throw new DataException(entryId + " must be an object");
        }
        DataNode values = node.copy();
        DataNode extras = DataNode.object();
        for (String key : reserved) {
            DataNode extra = values.remove(key);
            if (extra != null) {
                extras.put(key, extra);
            }
        }
        T value = defaults.get();
        spec.update(TreeDataBuffer.reader(values), value, specs.context(Encoding.KEYED));
        return new DataEntry<>(entryId, value, extras);
    }

    public DataNode write(T value) {
        checkBound();
        TreeDataBuffer buffer = TreeDataBuffer.writer();
        spec.write(buffer, value, specs.context(Encoding.KEYED));
        return buffer.result();
    }

    public Map<String, Exception> load(Map<String, DataNode> files) {
        Map<String, DataEntry<T>> loaded = new LinkedHashMap<>();
        Map<String, Exception> errors = new LinkedHashMap<>();
        for (Map.Entry<String, DataNode> file : files.entrySet()) {
            try {
                loaded.put(file.getKey(), parse(file.getKey(), file.getValue()));
            } catch (RuntimeException e) {
                errors.put(file.getKey(), e);
            }
        }
        entries = Map.copyOf(loaded);
        for (Runnable listener : listeners) {
            listener.run();
        }
        return errors;
    }

    public T get(String entryId) {
        DataEntry<T> entry = entries.get(entryId);
        return entry == null ? null : entry.value();
    }

    public DataEntry<T> entry(String entryId) {
        return entries.get(entryId);
    }

    public Collection<DataEntry<T>> entries() {
        return entries.values();
    }

    public void onReload(Runnable listener) {
        listeners.add(listener);
    }

    @Override
    public String toString() {
        return id;
    }
}
