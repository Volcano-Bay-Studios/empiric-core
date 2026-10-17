package xyz.volcanobay.empiric.core.entry;

import xyz.volcanobay.empiric.core.data.SpecRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DataEntrySystem {

    private final SpecRegistry specs;
    private final List<DataEntryType<?>> types = new ArrayList<>();
    private final Map<String, DataEntryType<?>> byId = new HashMap<>();
    private boolean frozen;

    public DataEntrySystem(SpecRegistry specs) {
        this.specs = specs;
    }

    public synchronized <T, D extends DataEntryType<T>> D register(D type) {
        if (frozen) {
            throw new IllegalStateException("Data entry types can't be registered after bootstrap");
        }
        if (byId.putIfAbsent(type.id(), type) != null) {
            throw new IllegalStateException("Data entry type " + type.id() + " is already registered");
        }
        type.bind(specs, types.size());
        types.add(type);
        return type;
    }

    public void freeze() {
        frozen = true;
    }

    public List<DataEntryType<?>> types() {
        return Collections.unmodifiableList(types);
    }

    public DataEntryType<?> type(String id) {
        return byId.get(id);
    }
}
