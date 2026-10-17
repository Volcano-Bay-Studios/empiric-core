package xyz.volcanobay.empiric.core.data;

import xyz.volcanobay.empiric.core.data.tree.DataNode;
import xyz.volcanobay.empiric.core.data.tree.TreeDataBuffer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class KeyedData {

    private final Map<String, Object> entries = new LinkedHashMap<>();
    private final DataBuffer source;
    private final DataContext context;
    private int version;

    public KeyedData(DataBuffer source, DataContext context, int version) {
        this.source = source;
        this.context = context;
        this.version = version;
    }

    public int version() {
        return version;
    }

    void version(int version) {
        this.version = version;
    }

    public boolean structured() {
        return source.structured();
    }

    public Set<String> ids() {
        return entries.keySet();
    }

    public boolean has(String id) {
        return entries.containsKey(id);
    }

    public void put(String id, byte[] bytes) {
        entries.put(id, bytes);
    }

    public void put(String id, DataNode node) {
        entries.put(id, node);
    }

    public void rename(String from, String to) {
        Object value = entries.remove(from);
        if (value != null) {
            entries.put(to, value);
        }
    }

    public void remove(String id) {
        entries.remove(id);
    }

    public DataNode node(String id) {
        return entries.get(id) instanceof DataNode node ? node : null;
    }

    public byte[] bytes(String id) {
        return entries.get(id) instanceof byte[] bytes ? bytes : null;
    }

    public <T> T read(String id, Spec<T> spec) {
        DataBuffer buffer = open(id);
        return buffer == null ? null : spec.read(buffer, context);
    }

    public <T> void write(String id, T value, Spec<T> spec) {
        if (structured()) {
            TreeDataBuffer buffer = TreeDataBuffer.writer();
            spec.write(buffer, value, context);
            entries.put(id, buffer.result());
            return;
        }
        DataBuffer buffer = source.wrap(new byte[0]);
        spec.write(buffer, value, context);
        entries.put(id, buffer.takeBytes(buffer.readable()));
    }

    public DataBuffer open(String id) {
        Object value = entries.get(id);
        if (value instanceof byte[] bytes) {
            return source.wrap(bytes);
        }
        if (value instanceof DataNode node) {
            return TreeDataBuffer.reader(node);
        }
        return null;
    }
}
