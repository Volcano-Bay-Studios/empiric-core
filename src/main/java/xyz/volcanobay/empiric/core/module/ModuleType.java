package xyz.volcanobay.empiric.core.module;

import xyz.volcanobay.empiric.core.EmpiricCore;
import xyz.volcanobay.empiric.core.attachment.Attachment;
import xyz.volcanobay.empiric.core.attachment.AttachmentType;
import xyz.volcanobay.empiric.core.data.DataException;
import xyz.volcanobay.empiric.core.data.Track;
import xyz.volcanobay.empiric.core.data.spec.ObjectSpec;
import xyz.volcanobay.empiric.core.data.spec.StaticSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ModuleType<M extends Module<A>, A extends Attachment> {

    private final String id;
    private final Class<M> moduleClass;
    private final AttachmentType<A, ?> attachmentType;
    private final Supplier<M> factory;
    private final Predicate<A> rule;
    private final Runnable onEnable;
    private final Runnable onDisable;
    private final Set<DropPolicy> drops;
    private final List<ModuleType<?, ?>> requires;
    private final Tracking moduleTracking;
    private final Map<String, Tracking> fieldTracking;
    private Tracking[] fieldTrackers = new Tracking[0];
    private final Map<String, EntryTracking> entryTracking;
    private EntryTracking[] entryTrackers = new EntryTracking[0];
    final ModuleEventBus events = new ModuleEventBus();
    int index = -1;
    List<BehaviorKey<?>> behaviors = List.of();
    ObjectSpec<M> spec;
    StaticSpec statics;
    boolean synced;
    volatile boolean enabled;

    protected ModuleType(Builder<M, A, ?> builder) {
        this.id = builder.id.empiriccore$id();
        this.moduleClass = builder.moduleClass;
        this.attachmentType = builder.attachmentType;
        this.factory = builder.factory;
        this.rule = builder.rule;
        this.onEnable = builder.onEnable;
        this.onDisable = builder.onDisable;
        this.drops = Set.copyOf(builder.drops);
        this.requires = List.copyOf(builder.requires);
        this.moduleTracking = builder.moduleTracking;
        this.fieldTracking = Map.copyOf(builder.fieldTracking);
        this.entryTracking = Map.copyOf(builder.entryTracking);
    }

    /**
     * Whether the module or any of its fields decides per viewer what it gets.
     */
    public boolean tracks() {
        return moduleTracking != null || fieldTrackers.length > 0 || entryTrackers.length > 0;
    }

    /**
     * What the viewer gets of the module as a whole. Anything but {@link Track#SYNC} means none of its fields.
     */
    public Track track(M module, Object viewer) {
        return moduleTracking == null ? Track.SYNC : moduleTracking.track(module, viewer);
    }

    /**
     * What the viewer gets of the synced field at {@code index} in {@link ObjectSpec#synced()}.
     */
    public Track track(M module, int index, Object viewer) {
        Tracking tracking = index < fieldTrackers.length ? fieldTrackers[index] : null;
        return tracking == null ? Track.SYNC : tracking.track(module, viewer);
    }

    /**
     * Whether the synced field at {@code index} has a tracker for its entries.
     */
    public boolean tracksEntries(int index) {
        return index < entryTrackers.length && entryTrackers[index] != null;
    }

    /**
     * What the viewer gets of one entry of a delta field, once the field itself is {@link Track#SYNC}.
     */
    public Track track(M module, int index, Object key, Object entry, Object viewer) {
        return entryTrackers[index].track(module, key, entry, viewer);
    }

    void resolveTracking() {
        if (!fieldTracking.isEmpty()) {
            fieldTrackers = new Tracking[spec.synced().length];
            fieldTracking.forEach((field, tracking) -> fieldTrackers[spec.syncIndex(field)] = tracking);
        }
        if (!entryTracking.isEmpty()) {
            entryTrackers = new EntryTracking[spec.synced().length];
            entryTracking.forEach((path, tracking) -> {
                String[] parts = path.split("\\.", -1);
                if (parts.length != 2 || !parts[1].equals("*")) {
                    throw new DataException(id + " tracks entries at \"" + path + "\", which has to be a delta field and \".*\", such as \"signals.*\"");
                }
                int index = spec.syncIndex(parts[0]);
                if (!spec.isDelta(index)) {
                    throw new DataException(id + " tracks the entries of " + parts[0] + ", which isn't a @Data(delta = true) map or set");
                }
                entryTrackers[index] = tracking;
            });
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private record EntryTracking(Class<?> viewer, EntryTracker tracker) {

        Track track(Object module, Object key, Object entry, Object viewer) {
            if (!this.viewer.isInstance(viewer)) {
                throw new IllegalArgumentException("Expected a " + this.viewer.getName() + " viewer, got " + viewer);
            }
            return tracker.track(module, key, entry, viewer);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private record Tracking(Class<?> viewer, Tracker tracker) {

        Track track(Object module, Object viewer) {
            if (!this.viewer.isInstance(viewer)) {
                throw new IllegalArgumentException("Expected a " + this.viewer.getName() + " viewer, got " + viewer);
            }
            return tracker.track(module, viewer);
        }
    }

    public List<ModuleType<?, ?>> requires() {
        return requires;
    }

    public Set<DropPolicy> drops() {
        return drops;
    }

    public boolean dropsOn(DropPolicy policy) {
        return drops.contains(policy);
    }

    public boolean persists() {
        return !drops.contains(DropPolicy.UNLOAD);
    }

    public static <M extends Module<A>, A extends Attachment> Builder<M, A, ModuleType<M, A>> builder(ModuleId id, Class<M> moduleClass, AttachmentType<A, ?> attachmentType, Supplier<M> factory) {
        return new Builder<>(id, moduleClass, attachmentType, factory, ModuleType::new);
    }

    public static <M extends Module<A>, A extends Attachment, T extends ModuleType<M, A>> Builder<M, A, T> builder(ModuleId id, Class<M> moduleClass, AttachmentType<A, ?> attachmentType, Supplier<M> factory, Function<Builder<M, A, T>, T> constructor) {
        return new Builder<>(id, moduleClass, attachmentType, factory, constructor);
    }

    public M get(A attachment) {
        return attachment == null ? null : attachment.modules().get(this);
    }

    public M attach(A attachment) {
        return EmpiricCore.modules().attach(attachment, this);
    }

    public boolean detach(A attachment) {
        return attachment != null && EmpiricCore.modules().detach(attachment, this);
    }

    /**
     * Listens to an event on every module of this type, and subscribes {@code modId} to the type.
     */
    public <E> void listen(String modId, ModuleEvent<E> event, ModuleEventListener<? super M, ? super E> listener) {
        String owner = ModuleSystem.listenerId(modId);
        events.add(owner, event, listener);
        EmpiricCore.modules().subscribe(owner, this);
    }

    public boolean unlisten(String modId) {
        String owner = ModuleSystem.listenerId(modId);
        if (!events.remove(owner)) {
            return false;
        }
        EmpiricCore.modules().unsubscribe(owner, this);
        return true;
    }

    public <E> void fire(M module, ModuleEvent<E> event, E payload) {
        EmpiricCore.modules().fire(module, event, payload);
    }

    public ModuleEventBus events() {
        return events;
    }

    public String id() {
        return id;
    }

    public Class<M> moduleClass() {
        return moduleClass;
    }

    public AttachmentType<A, ?> attachmentType() {
        return attachmentType;
    }

    public int index() {
        return index;
    }

    public List<BehaviorKey<?>> behaviors() {
        return behaviors;
    }

    public boolean has(BehaviorKey<?> behavior) {
        return behaviors.contains(behavior);
    }

    public ObjectSpec<M> spec() {
        return spec;
    }

    /**
     * The object whose {@code @Data} fields hold this module's data. Defaults to the module itself.
     */
    public Object data(M module) {
        return module;
    }

    /**
     * The spec that reads and writes {@link #data(Module)} of this module.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ObjectSpec<Object> spec(M module) {
        return (ObjectSpec) spec;
    }

    public StaticSpec statics() {
        return statics;
    }

    public boolean synced() {
        return synced;
    }

    public boolean watches() {
        return synced && spec.watches();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean autoAttaches() {
        return rule != null;
    }

    boolean accepts(A attachment) {
        return rule != null && rule.test(attachment);
    }

    M create() {
        return factory.get();
    }

    void enabled() {
        if (onEnable != null) {
            onEnable.run();
        }
    }

    void disabled() {
        if (onDisable != null) {
            onDisable.run();
        }
    }

    @Override
    public String toString() {
        return id;
    }

    public static final class Builder<M extends Module<A>, A extends Attachment, T extends ModuleType<M, A>> {

        private final ModuleId id;
        private final Class<M> moduleClass;
        private final AttachmentType<A, ?> attachmentType;
        private final Supplier<M> factory;
        private Predicate<A> rule;
        private Runnable onEnable;
        private Runnable onDisable;
        private final Set<DropPolicy> drops = new HashSet<>();
        private final List<ModuleType<?, ?>> requires = new ArrayList<>();
        private Tracking moduleTracking;
        private final Map<String, Tracking> fieldTracking = new LinkedHashMap<>();
        private final Map<String, EntryTracking> entryTracking = new LinkedHashMap<>();
        private final Function<Builder<M, A, T>, T> constructor;

        private Builder(ModuleId id, Class<M> moduleClass, AttachmentType<A, ?> attachmentType, Supplier<M> factory, Function<Builder<M, A, T>, T> constructor) {
            this.id = id;
            this.moduleClass = moduleClass;
            this.attachmentType = attachmentType;
            this.factory = factory;
            this.constructor = constructor;
        }

        public Builder<M, A, T> autoAttach(Predicate<A> rule) {
            this.rule = rule;
            return this;
        }

        public Builder<M, A, T> autoAttach() {
            return autoAttach(attachment -> true);
        }

        public Builder<M, A, T> onEnable(Runnable onEnable) {
            this.onEnable = onEnable;
            return this;
        }

        public Builder<M, A, T> onDisable(Runnable onDisable) {
            this.onDisable = onDisable;
            return this;
        }

        public Builder<M, A, T> drop(DropPolicy... policies) {
            this.drops.addAll(Arrays.asList(policies));
            return this;
        }

        public Builder<M, A, T> requires(ModuleType<?, ?>... types) {
            this.requires.addAll(Arrays.asList(types));
            return this;
        }

        /**
         * Decides which viewers get this module at all, for example {@code track(ServerPlayer.class, MyModule::inRange)}.
         * A viewer that isn't {@link Track#SYNC} gets none of its fields.
         */
        public <V> Builder<M, A, T> track(Class<V> viewer, Tracker<? super M, ? super V> tracker) {
            this.moduleTracking = new Tracking(viewer, tracker);
            return this;
        }

        /**
         * Decides which viewers get the synced field with this id, for example
         * {@code track("signals", ServerPlayer.class, MyModule::nearby)}. Only checked for viewers the module-level
         * tracker lets through.
         */
        public <V> Builder<M, A, T> track(String field, Class<V> viewer, Tracker<? super M, ? super V> tracker) {
            if (fieldTracking.putIfAbsent(field, new Tracking(viewer, tracker)) != null) {
                throw new IllegalStateException(id + " already tracks " + field);
            }
            return this;
        }

        /**
         * Decides which viewers get each entry of a {@code @Data(delta = true)} map or set field, for example
         * {@code trackEntries("signals.*", ServerPlayer.class, MyModule::inRange)}. The path is the field's id and
         * {@code .*}. Paths into the entries themselves aren't supported.
         */
        public <K, E, V> Builder<M, A, T> trackEntries(String path, Class<V> viewer, EntryTracker<? super M, K, E, ? super V> tracker) {
            if (entryTracking.putIfAbsent(path, new EntryTracking(viewer, tracker)) != null) {
                throw new IllegalStateException(id + " already tracks " + path);
            }
            return this;
        }

        public T build() {
            return constructor.apply(this);
        }
    }
}
