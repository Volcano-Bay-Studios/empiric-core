package xyz.volcanobay.empiric.core.data;

import java.util.IdentityHashMap;
import java.util.Map;

public final class DataContext {

    private final Encoding encoding;
    private final SpecRegistry registry;
    private final Map<Key<?>, Object> attributes;

    public DataContext(Encoding encoding, SpecRegistry registry) {
        this(encoding, registry, Map.of());
    }

    private DataContext(Encoding encoding, SpecRegistry registry, Map<Key<?>, Object> attributes) {
        this.encoding = encoding;
        this.registry = registry;
        this.attributes = attributes;
    }

    public Encoding encoding() {
        return encoding;
    }

    public boolean keyed() {
        return encoding == Encoding.KEYED;
    }

    public SpecRegistry registry() {
        return registry;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Key<T> key) {
        return (T) attributes.get(key);
    }

    public <T> DataContext with(Key<T> key, T value) {
        Map<Key<?>, Object> changed = new IdentityHashMap<>(attributes);
        changed.put(key, value);
        return new DataContext(encoding, registry, changed);
    }

    public DataContext withEncoding(Encoding encoding) {
        return new DataContext(encoding, registry, attributes);
    }

    public record Key<T>(String name) {
    }
}
