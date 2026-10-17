package xyz.volcanobay.empiric.core.data;

import xyz.volcanobay.empiric.core.data.layout.ClassLayout;
import xyz.volcanobay.empiric.core.data.layout.LayoutCache;
import xyz.volcanobay.empiric.core.data.spec.DeferredSpec;
import xyz.volcanobay.empiric.core.data.spec.ObjectSpec;
import xyz.volcanobay.empiric.core.data.spec.StaticSpec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SpecRegistry {

    private final Map<Class<?>, Spec<?>> exact = new ConcurrentHashMap<>();
    private final List<SpecFactory> factories = new CopyOnWriteArrayList<>();
    private final Map<Key, Spec<?>> resolved = new ConcurrentHashMap<>();
    private final Map<Class<?>, StaticSpec> statics = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> strategies = new ConcurrentHashMap<>();
    private final Map<Class<?>, NavigableMap<Integer, List<DataFixer>>> fixers = new ConcurrentHashMap<>();
    private final ThreadLocal<Set<Key>> resolving = ThreadLocal.withInitial(HashSet::new);
    private final LayoutCache layouts = new LayoutCache();
    private volatile boolean frozen;

    public <T> void register(Class<T> type, Spec<T> spec) {
        checkOpen();
        exact.put(type, spec);
    }

    public void register(SpecFactory factory) {
        checkOpen();
        factories.add(factory);
    }

    public void registerSelf(Class<?> type) {
        checkOpen();
        spec(type, Resolution.DEFAULT);
    }

    public void fixer(Class<?> type, int fromVersion, DataFixer fixer) {
        checkOpen();
        fixers.computeIfAbsent(type, key -> new TreeMap<>()).computeIfAbsent(fromVersion, key -> new ArrayList<>()).add(fixer);
    }

    private void checkOpen() {
        if (frozen) {
            throw new DataException("Specs can't be registered after the spec registry is frozen");
        }
    }

    public void freeze() {
        frozen = true;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean hasFixers(Class<?> type) {
        return fixers.containsKey(type);
    }

    public void fix(Class<?> type, KeyedData data, int targetVersion) {
        NavigableMap<Integer, List<DataFixer>> byVersion = fixers.get(type);
        if (byVersion == null) {
            return;
        }
        for (int version = data.version(); version < targetVersion; version++) {
            List<DataFixer> step = byVersion.get(version);
            if (step != null) {
                for (DataFixer fixer : step) {
                    fixer.fix(data);
                }
            }
            data.version(version + 1);
        }
    }

    public DataContext context(Encoding encoding) {
        return new DataContext(encoding, this);
    }

    public ClassLayout layout(Class<?> type) {
        return layouts.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T> T strategy(Class<T> type) {
        return (T) strategies.computeIfAbsent(type, key -> {
            try {
                Constructor<?> constructor = key.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new DataException(key.getName() + " needs a no-arg constructor", e);
            }
        });
    }

    public Mode mode(Type type, Resolution resolution, boolean supportsUpdate, Mode defaultMode) {
        Mode mode = resolution.mode() == Mode.INHERIT ? defaultMode : resolution.mode();
        mode = resolution.resolver().resolve(type, resolution.level(), mode);
        if (mode == Mode.INHERIT || (mode == Mode.UPDATE && !supportsUpdate)) {
            return Mode.REPLACE;
        }
        return mode;
    }

    public Slot<?> slot(Type type, Resolution resolution) {
        Spec<?> spec = spec(type, resolution);
        return slot(spec, mode(type, resolution, spec.supportsUpdate(), spec.defaultMode()));
    }

    private static <T> Slot<T> slot(Spec<T> spec, Mode mode) {
        return new Slot<>(spec, mode);
    }

    @SuppressWarnings("unchecked")
    public <T> Spec<T> spec(Class<T> type) {
        return (Spec<T>) spec(type, Resolution.DEFAULT);
    }

    @SuppressWarnings("unchecked")
    public <T> Spec<T> specOf(T value) {
        return (Spec<T>) spec(value.getClass(), Resolution.DEFAULT);
    }

    public Spec<?> spec(Type type, Resolution resolution) {
        Class<?> raw = Types.raw(type);
        Spec<?> spec = exact.get(raw);
        if (spec != null) {
            return spec;
        }
        Key key = new Key(type, resolution);
        spec = resolved.get(key);
        if (spec != null) {
            return spec;
        }
        Set<Key> active = resolving.get();
        if (!active.add(key)) {
            return deferred(key);
        }
        try {
            spec = build(type, raw, resolution);
            Spec<?> existing = resolved.putIfAbsent(key, spec);
            return existing != null ? existing : spec;
        } finally {
            active.remove(key);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Spec<T> deferred(Key key) {
        return new DeferredSpec<>(() -> (Spec<T>) resolved.get(key));
    }

    private Spec<?> build(Type type, Class<?> raw, Resolution resolution) {
        for (int i = factories.size() - 1; i >= 0; i--) {
            Spec<?> spec = factories.get(i).create(type, raw, resolution, this);
            if (spec != null) {
                return spec;
            }
        }
        if (raw.isAnnotationPresent(SelfSpec.class)) {
            return new ObjectSpec<>(layout(raw), resolution, this);
        }
        throw new DataException("No spec is registered for " + type.getTypeName());
    }

    public <T> ObjectSpec<T> objectSpec(Class<T> type) {
        return objectSpec(type, Resolution.DEFAULT);
    }

    @SuppressWarnings("unchecked")
    public <T> ObjectSpec<T> objectSpec(Class<T> type, Resolution resolution) {
        Key key = new Key(type, resolution);
        Spec<?> spec = resolved.get(key);
        if (spec instanceof ObjectSpec<?> objectSpec) {
            return (ObjectSpec<T>) objectSpec;
        }
        ObjectSpec<T> created = new ObjectSpec<>(layout(type), resolution, this);
        Spec<?> existing = resolved.putIfAbsent(key, created);
        return existing instanceof ObjectSpec<?> objectSpec ? (ObjectSpec<T>) objectSpec : created;
    }

    public StaticSpec staticSpec(Class<?> type) {
        return statics.computeIfAbsent(type, key -> new StaticSpec(layout(key), this));
    }

    public <T> void write(DataBuffer buffer, T value, Encoding encoding) {
        specOf(value).write(buffer, value, context(encoding));
    }

    public <T> T read(DataBuffer buffer, Class<T> type, Encoding encoding) {
        return spec(type).read(buffer, context(encoding));
    }

    public <T> T readInto(DataBuffer buffer, T existing, Encoding encoding) {
        Spec<T> spec = specOf(existing);
        DataContext context = context(encoding);
        return spec.supportsUpdate() ? spec.update(buffer, existing, context) : spec.read(buffer, context);
    }

    private record Key(Type type, Resolution resolution) {
    }
}
