package xyz.volcanobay.empiric.core.module;

import xyz.volcanobay.empiric.core.EmpiricCore;
import xyz.volcanobay.empiric.core.attachment.Attachment;
import xyz.volcanobay.empiric.core.attachment.AttachmentType;
import xyz.volcanobay.empiric.core.data.spec.ObjectSpec;
import xyz.volcanobay.empiric.core.data.spec.StaticSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

        public T build() {
            return constructor.apply(this);
        }
    }
}
