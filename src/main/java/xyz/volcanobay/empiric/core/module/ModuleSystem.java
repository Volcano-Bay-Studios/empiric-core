package xyz.volcanobay.empiric.core.module;

import xyz.volcanobay.empiric.core.attachment.Attachment;
import xyz.volcanobay.empiric.core.attachment.AttachmentType;
import xyz.volcanobay.empiric.core.attachment.ModuleAttachment;
import xyz.volcanobay.empiric.core.attachment.ModuleAttachmentType;
import xyz.volcanobay.empiric.core.data.ByteArrayDataBuffer;
import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.DataException;
import xyz.volcanobay.empiric.core.data.Encoding;
import xyz.volcanobay.empiric.core.data.SpecRegistry;
import xyz.volcanobay.empiric.core.data.layout.KeyedFormat;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ModuleSystem {

    private final SpecRegistry specs;
    private final List<AttachmentType<?, ?>> attachmentTypes = new ArrayList<>();
    private final Map<String, AttachmentType<?, ?>> attachmentsById = new HashMap<>();
    private final List<BehaviorKey<?>> behaviors = new ArrayList<>();
    private final List<ModuleType<?, ?>> types = new ArrayList<>();
    private final Map<String, ModuleType<?, ?>> typesById = new HashMap<>();
    private final Map<ModuleType<?, ?>, Set<String>> subscribers = new HashMap<>();
    private final SideState[] sides = {new SideState(), new SideState()};
    private final ModuleAttachmentType moduleAttachments;
    private final Map<String, byte[]> unreadStatics = new HashMap<>();
    private final ModuleEventBus listeners = new ModuleEventBus();
    private Function<byte[], DataBuffer> buffers = ByteArrayDataBuffer::new;
    private BiConsumer<String, Throwable> errors = (message, error) -> {
        System.err.println(message);
        error.printStackTrace();
    };
    private boolean frozen;

    public ModuleSystem(SpecRegistry specs) {
        this.specs = specs;
        this.moduleAttachments = registerAttachment(new ModuleAttachmentType(this));
    }

    public SpecRegistry specs() {
        return specs;
    }

    public ModuleAttachmentType moduleAttachments() {
        return moduleAttachments;
    }

    public void bufferFactory(Function<byte[], DataBuffer> buffers) {
        this.buffers = buffers;
    }

    /**
     * Listens to an event on modules of every type. Unlike {@link ModuleType#listen}, this subscribes nothing.
     */
    public <E> void listen(ModuleEvent<E> event, ModuleEventListener<Module<?>, ? super E> listener) {
        listeners.add("global", event, listener);
    }

    public <E> void fire(Module<?> module, ModuleEvent<E> event, E payload) {
        listeners.fire(module, event, payload, errors);
        module.type.events.fire(module, event, payload, errors);
    }

    public void errorHandler(BiConsumer<String, Throwable> errors) {
        this.errors = errors;
    }

    public DataBuffer newBuffer() {
        return buffers.apply(new byte[0]);
    }

    private void checkOpen() {
        if (frozen) {
            throw new IllegalStateException("Modules can't be registered after the module system is frozen");
        }
    }

    public synchronized <T extends AttachmentType<?, ?>> T registerAttachment(T type) {
        checkOpen();
        if (attachmentsById.putIfAbsent(type.id(), type) != null) {
            throw new IllegalStateException("Attachment type " + type.id() + " is already registered");
        }
        type.assignIndex(attachmentTypes.size());
        attachmentTypes.add(type);
        return type;
    }

    public synchronized <B extends Behavior> BehaviorKey<B> registerBehavior(Class<B> type) {
        checkOpen();
        if (!types.isEmpty()) {
            throw new IllegalStateException("Behaviours must be registered before any module type, " + type.getName() + " was too late");
        }
        for (BehaviorKey<?> key : behaviors) {
            if (key.type() == type) {
                throw new IllegalStateException("Behaviour " + type.getName() + " is already registered");
            }
        }
        BehaviorKey<B> key = new BehaviorKey<>(type, behaviors.size());
        behaviors.add(key);
        for (SideState side : sides) {
            side.addBehavior(key);
        }
        return key;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized <T extends ModuleType<?, ?>> T register(T type) {
        registerType((ModuleType) type);
        return type;
    }

    private <M extends Module<A>, A extends Attachment> void registerType(ModuleType<M, A> type) {
        checkOpen();
        if (typesById.putIfAbsent(type.id(), type) != null) {
            throw new IllegalStateException("Module type " + type.id() + " is already registered");
        }
        for (ModuleType<?, ?> required : type.requires()) {
            if (typesById.get(required.id()) != required) {
                throw new IllegalStateException(type.id() + " requires " + required.id() + ", which has to be registered first");
            }
        }
        if (type.attachmentType().index() < 0) {
            throw new IllegalStateException(type.id() + " uses the unregistered attachment type " + type.attachmentType().id());
        }
        List<BehaviorKey<?>> owned = new ArrayList<>();
        for (BehaviorKey<?> behavior : behaviors) {
            if (behavior.type().isAssignableFrom(type.moduleClass())) {
                owned.add(behavior);
            }
        }
        type.index = types.size();
        type.behaviors = List.copyOf(owned);
        type.synced = SyncBehavior.class.isAssignableFrom(type.moduleClass());
        type.spec = specs.objectSpec(type.moduleClass());
        type.statics = specs.staticSpec(type.moduleClass());
        types.add(type);
        for (SideState side : sides) {
            side.addType();
        }
    }

    public synchronized void freeze() {
        frozen = true;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public List<ModuleType<?, ?>> types() {
        return Collections.unmodifiableList(types);
    }

    public ModuleType<?, ?> type(String id) {
        return typesById.get(id);
    }

    public List<BehaviorKey<?>> behaviors() {
        return Collections.unmodifiableList(behaviors);
    }

    public AttachmentType<?, ?> attachmentType(String id) {
        return attachmentsById.get(id);
    }

    public List<AttachmentType<?, ?>> attachmentTypes() {
        return Collections.unmodifiableList(attachmentTypes);
    }

    private SideState state(Side side) {
        return side == Side.NONE ? null : sides[side.ordinal()];
    }

    public synchronized boolean subscribe(String modId, ModuleType<?, ?> type) {
        Set<String> set = subscribers.computeIfAbsent(type, key -> new LinkedHashSet<>());
        if (!set.add(modId)) {
            return false;
        }
        if (set.size() == 1) {
            for (ModuleType<?, ?> required : type.requires()) {
                subscribe(requiredBy(type), required);
            }
            type.enabled = true;
            type.enabled();
            schedule(type);
        }
        return true;
    }

    public synchronized boolean subscribe(String modId, String typeId) {
        return subscribe(modId, require(typeId));
    }

    public synchronized boolean unsubscribe(String modId, ModuleType<?, ?> type) {
        Set<String> set = subscribers.get(type);
        if (set == null || !set.remove(modId)) {
            return false;
        }
        if (set.isEmpty()) {
            subscribers.remove(type);
            type.enabled = false;
            type.disabled();
            schedule(type);
            for (ModuleType<?, ?> required : type.requires()) {
                unsubscribe(requiredBy(type), required);
            }
        }
        return true;
    }

    public synchronized boolean unsubscribe(String modId, String typeId) {
        return unsubscribe(modId, require(typeId));
    }

    public static String requiredBy(ModuleType<?, ?> type) {
        return "required_by:" + type.id();
    }

    public static String listenerId(String modId) {
        return "listener:" + modId;
    }

    private ModuleType<?, ?> require(String typeId) {
        ModuleType<?, ?> type = typesById.get(typeId);
        if (type == null) {
            throw new IllegalArgumentException("No module type " + typeId);
        }
        return type;
    }

    public synchronized Set<String> subscribers(ModuleType<?, ?> type) {
        Set<String> set = subscribers.get(type);
        return set == null ? Set.of() : Set.copyOf(set);
    }

    public boolean isEnabled(ModuleType<?, ?> type) {
        return type.enabled;
    }

    private void schedule(ModuleType<?, ?> type) {
        for (SideState side : sides) {
            synchronized (side.pending) {
                side.pending.add(type);
            }
        }
    }

    public void processPending(Side side) {
        SideState state = state(side);
        if (state == null) {
            return;
        }
        List<ModuleType<?, ?>> pending;
        synchronized (state.pending) {
            if (state.pending.isEmpty()) {
                return;
            }
            pending = new ArrayList<>(state.pending);
            state.pending.clear();
        }
        for (ModuleType<?, ?> type : pending) {
            if (type.enabled) {
                enable(side, state, type);
            } else {
                disable(state, type);
            }
        }
    }

    private <M extends Module<A>, A extends Attachment> void enable(Side side, SideState state, ModuleType<M, A> type) {
        Set<ModuleHolder> holders = state.dormantHolders.remove(type.id());
        if (holders != null) {
            for (ModuleHolder holder : new ArrayList<>(holders)) {
                if (holder.dormant().containsKey(type.id())) {
                    attachSafely(holder.attachment(), type);
                }
            }
        }
        if (type.autoAttaches()) {
            type.attachmentType().forEachLoaded(side, attachment -> {
                if (type.enabled && !attachment.modules().has(type) && type.accepts(attachment)) {
                    attachSafely(attachment, type);
                }
            });
        }
    }

    private void disable(SideState state, ModuleType<?, ?> type) {
        for (Module<?> module : state.types.get(type.index).snapshot()) {
            ModuleHolder holder = module.attachment.modules();
            holder.putDormant(type.id(), encode(module));
            trackDormant(type.id(), holder);
            remove(module, false);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void attachSafely(Attachment attachment, ModuleType type) {
        if (attachment.attachmentType() != type.attachmentType()) {
            return;
        }
        try {
            attach(attachment, type);
        } catch (DataException e) {
            errors.accept("Couldn't revive " + type.id() + ", its data stays dormant", e);
        }
    }

    public <M extends Module<A>, A extends Attachment> M get(A attachment, ModuleType<M, A> type) {
        return attachment.modules().get(type);
    }

    @SuppressWarnings("unchecked")
    public <M extends Module<A>, A extends Attachment> M attach(A attachment, ModuleType<M, A> type) {
        if (attachment.attachmentType() != type.attachmentType()) {
            throw new IllegalArgumentException(type.id() + " attaches to " + type.attachmentType().id() + ", not " + attachment.attachmentType().id());
        }
        if (!type.enabled) {
            throw new IllegalStateException(type.id() + " isn't enabled, a mod has to subscribe to it first");
        }
        ModuleHolder holder = attachment.modules();
        M existing = holder.get(type);
        if (existing != null) {
            return existing;
        }
        byte[] dormant = holder.takeDormant(type.id());
        if (dormant != null) {
            M module = (M) readSafely(attachment, type, dormant, specs.context(Encoding.KEYED));
            if (module == null) {
                throw new DataException(type.id() + " has saved data that can't be read, it was kept dormant");
            }
            return module;
        }
        M module = type.create();
        install(attachment, type, module);
        module.onAttach();
        fire(module, ModuleEvents.ATTACHED, null);
        return module;
    }

    public <M extends Module<A>, A extends Attachment> boolean detach(A attachment, ModuleType<M, A> type) {
        ModuleHolder holder = attachment.modules();
        holder.takeDormant(type.id());
        M module = holder.get(type);
        if (module == null) {
            return false;
        }
        remove(module, true);
        return true;
    }

    public void detachAll(Attachment attachment) {
        attachment.modules().forEach(module -> remove(module, true));
    }

    public void unload(Attachment attachment) {
        attachment.modules().forEach(module -> remove(module, false));
    }

    public int drop(Attachment attachment, DropPolicy policy) {
        int[] count = {0};
        attachment.modules().forEach(module -> {
            if (module.type.dropsOn(policy)) {
                remove(module, true);
                count[0]++;
            } else {
                ModuleAttachment children = module.childrenIfPresent();
                if (children != null) {
                    count[0] += drop(children, policy);
                }
            }
        });
        return count[0];
    }

    public void unloadSide(Side side) {
        SideState state = state(side);
        if (state == null) {
            return;
        }
        for (ModuleIndex index : state.types) {
            for (Module<?> module : index.snapshot()) {
                remove(module, false);
            }
        }
        state.dirty.clear();
        state.dormantHolders.clear();
    }

    public void applyRules(Attachment attachment) {
        for (ModuleType<?, ?> type : types) {
            if (type.enabled && type.autoAttaches() && type.attachmentType() == attachment.attachmentType()) {
                applyRule(attachment, type);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void applyRule(Attachment attachment, ModuleType type) {
        if (!attachment.modules().has(type) && type.accepts(attachment)) {
            attachSafely(attachment, type);
        }
    }

    private <M extends Module<A>, A extends Attachment> void install(A attachment, ModuleType<M, A> type, M module) {
        module.init(attachment, type, behaviors.size());
        attachment.modules().set(type.index, module);
        module.attached = true;
        SideState state = state(attachment.side());
        if (state == null) {
            return;
        }
        state.types.get(type.index).add(module);
        for (BehaviorKey<?> behavior : type.behaviors) {
            state.behaviors.get(behavior.id()).add(module);
        }
        if (type.watches()) {
            module.snapshot = type.spec.snapshot(module);
            state.watchers.add(module);
        }
    }

    private void remove(Module<?> module, boolean destroy) {
        if (!module.attached) {
            return;
        }
        ModuleAttachment children = module.childrenIfPresent();
        if (children != null) {
            children.modules().forEach(child -> remove(child, destroy));
        }
        if (destroy) {
            module.onDetach();
            fire(module, ModuleEvents.DETACHED, null);
        } else {
            module.onUnload();
            fire(module, ModuleEvents.UNLOADED, null);
        }
        module.attached = false;
        module.attachment.modules().set(module.type.index, null);
        SideState state = state(module.attachment.side());
        if (state == null) {
            return;
        }
        state.types.get(module.type.index).remove(module);
        for (BehaviorKey<?> behavior : module.type.behaviors) {
            state.behaviors.get(behavior.id()).remove(module);
        }
        state.watchers.remove(module);
        module.queued = false;
    }

    private void trackDormant(String id, ModuleHolder holder) {
        SideState state = state(holder.attachment().side());
        if (state != null) {
            state.dormantHolders.computeIfAbsent(id, key -> Collections.newSetFromMap(new WeakHashMap<>())).add(holder);
        }
    }

    private byte[] encode(Module<?> module) {
        DataBuffer buffer = newBuffer();
        writeModule(buffer, module, specs.context(Encoding.KEYED));
        return buffer.takeBytes(buffer.readable());
    }

    public void save(Attachment attachment, DataBuffer buffer, DataContext context) {
        save(attachment, buffer, context, type -> true);
    }

    public int countSaved(Attachment attachment, Predicate<ModuleType<?, ?>> filter) {
        int[] count = {0};
        attachment.modules().forEach(module -> {
            if (filter.test(module.type)) {
                count[0]++;
            }
        });
        for (String id : attachment.modules().dormant().keySet()) {
            ModuleType<?, ?> type = typesById.get(id);
            if (type == null || filter.test(type)) {
                count[0]++;
            }
        }
        return count[0];
    }

    public void save(Attachment attachment, DataBuffer buffer, DataContext context, Predicate<ModuleType<?, ?>> filter) {
        ModuleHolder holder = attachment.modules();
        buffer.putVarInt(countSaved(attachment, filter));
        holder.forEach(module -> {
            if (!filter.test(module.type)) {
                return;
            }
            int position = KeyedFormat.begin(buffer, module.type.id());
            writeModule(buffer, module, context);
            KeyedFormat.end(buffer, position);
        });
        for (Map.Entry<String, byte[]> entry : holder.dormant().entrySet()) {
            ModuleType<?, ?> type = typesById.get(entry.getKey());
            if (type != null && !filter.test(type)) {
                continue;
            }
            buffer.putString(entry.getKey());
            buffer.putInt(entry.getValue().length);
            buffer.putBytes(entry.getValue(), 0, entry.getValue().length);
        }
    }

    @SuppressWarnings("unchecked")
    private <M extends Module<?>> void writeModule(DataBuffer buffer, M module, DataContext context) {
        ((ModuleType<M, ?>) (ModuleType<?, ?>) module.type).spec.write(buffer, module, context);
        ModuleAttachment children = module.childrenIfPresent();
        if (children == null || children.modules().isEmpty()) {
            buffer.putVarInt(0);
        } else {
            save(children, buffer, context);
        }
    }

    public void load(Attachment attachment, DataBuffer buffer, DataContext context) {
        ModuleHolder holder = attachment.modules();
        int count = buffer.takeVarInt();
        for (int i = 0; i < count; i++) {
            String id = buffer.takeString();
            int length = buffer.takeInt();
            ModuleType<?, ?> type = typesById.get(id);
            if (type == null || !type.enabled || type.attachmentType() != attachment.attachmentType() || holder.has(type)) {
                holder.putDormant(id, buffer.takeBytes(length));
                trackDormant(id, holder);
                continue;
            }
            readSafely(attachment, type, buffer.takeBytes(length), context);
        }
    }

    private Module<?> readSafely(Attachment attachment, ModuleType<?, ?> type, byte[] bytes, DataContext context) {
        try {
            return readModule(attachment, type, buffers.apply(bytes), context);
        } catch (RuntimeException e) {
            Module<?> partial = attachment.modules().get(type);
            if (partial != null) {
                remove(partial, false);
            }
            attachment.modules().putDormant(type.id(), bytes);
            trackDormant(type.id(), attachment.modules());
            errors.accept("Couldn't read " + type.id() + " on " + attachment.attachmentType().id() + ", its data was kept dormant", e);
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Module readModule(Attachment attachment, ModuleType type, DataBuffer buffer, DataContext context) {
        Module module = type.create();
        type.spec.update(buffer, module, context);
        install(attachment, type, module);
        load(module.children(), buffer, context);
        module.onLoad();
        fire(module, ModuleEvents.LOADED, null);
        return module;
    }

    public void saveStatics(DataBuffer buffer, DataContext context) {
        List<ModuleType<?, ?>> withStatics = types.stream().filter(type -> !type.statics.isEmpty()).toList();
        List<String> unknown = unreadStatics.keySet().stream().filter(id -> !typesById.containsKey(id)).toList();
        buffer.putVarInt(withStatics.size() + unknown.size());
        for (ModuleType<?, ?> type : withStatics) {
            byte[] unread = unreadStatics.get(type.id());
            if (unread != null) {
                putRaw(buffer, type.id(), unread);
                continue;
            }
            int position = KeyedFormat.begin(buffer, type.id());
            type.statics.write(buffer, context);
            KeyedFormat.end(buffer, position);
        }
        for (String id : unknown) {
            putRaw(buffer, id, unreadStatics.get(id));
        }
    }

    private static void putRaw(DataBuffer buffer, String id, byte[] bytes) {
        buffer.putString(id);
        buffer.putInt(bytes.length);
        buffer.putBytes(bytes, 0, bytes.length);
    }

    public void loadStatics(DataBuffer buffer, DataContext context) {
        int count = buffer.takeVarInt();
        for (int i = 0; i < count; i++) {
            String id = buffer.takeString();
            int length = buffer.takeInt();
            byte[] bytes = buffer.takeBytes(length);
            ModuleType<?, ?> type = typesById.get(id);
            if (type == null || type.statics.isEmpty()) {
                unreadStatics.put(id, bytes);
                continue;
            }
            try {
                type.statics.read(buffers.apply(bytes), context);
                unreadStatics.remove(id);
            } catch (RuntimeException e) {
                unreadStatics.put(id, bytes);
                errors.accept("Couldn't read the static data of " + id + ", it will be saved unchanged", e);
            }
        }
    }

    public <B extends Behavior> void forEach(Side side, BehaviorKey<B> behavior, Consumer<B> action) {
        SideState state = state(side);
        if (state != null) {
            state.behaviors.get(behavior.id()).forEach(action);
        }
    }

    public <M extends Module<?>> void forEach(Side side, ModuleType<M, ?> type, Consumer<M> action) {
        SideState state = state(side);
        if (state != null) {
            state.types.get(type.index).forEach(action);
        }
    }

    public int count(Side side, ModuleType<?, ?> type) {
        SideState state = state(side);
        return state == null ? 0 : state.types.get(type.index).size();
    }

    public int count(Side side, BehaviorKey<?> behavior) {
        SideState state = state(side);
        return state == null ? 0 : state.behaviors.get(behavior.id()).size();
    }

    void markDirty(Module<?> module, int index) {
        if (!module.attached || !module.type.synced) {
            return;
        }
        SideState state = state(module.attachment.side());
        if (state == null) {
            return;
        }
        if (module.dirty == null) {
            module.dirty = new BitSet();
        }
        if (index < 0) {
            module.dirty.set(0, module.type.spec.synced().length);
        } else {
            module.dirty.set(index);
        }
        queue(state, module);
    }

    private void queue(SideState state, Module<?> module) {
        if (!module.queued) {
            module.queued = true;
            state.dirty.add(module);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void flushDirty(Side side, DirtyConsumer consumer) {
        SideState state = state(side);
        if (state == null) {
            return;
        }
        state.watchers.forEach((Module module) -> {
            if (module.dirty == null) {
                module.dirty = new BitSet();
            }
            if (((ModuleType) module.type).spec.detectChanges(module, module.snapshot, module.dirty)) {
                queue(state, module);
            }
        });
        if (state.dirty.isEmpty()) {
            return;
        }
        List<Module<?>> pending = new ArrayList<>(state.dirty);
        state.dirty.clear();
        for (Module<?> module : pending) {
            module.queued = false;
            if (side == Side.CLIENT) {
                module.dirty.and(module.type.spec.clientMutable());
            }
            if (module.attached && !module.dirty.isEmpty()) {
                consumer.accept(module, module.dirty);
            }
            module.dirty.clear();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void acceptRemote(Module<?> module, BitSet fields) {
        if (module.snapshot != null) {
            ((ModuleType) module.type).spec.refreshSnapshot(module, module.snapshot, fields);
        }
        if (module.dirty != null) {
            module.dirty.andNot(fields);
        }
        fire(module, ModuleEvents.SYNCED, new SyncedFields(module.type.spec, fields));
    }

    public void writeLocator(DataBuffer buffer, Module<?> module, DataContext context) {
        writeTarget(buffer, module.attachment, module.type, context);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void writeTarget(DataBuffer buffer, Attachment attachment, ModuleType<?, ?> type, DataContext context) {
        AttachmentType attachmentType = attachment.attachmentType();
        buffer.putVarInt(attachmentType.index());
        attachmentType.writeKey(buffer, attachment, context);
        buffer.putVarInt(type.index);
    }

    public Target readTarget(DataBuffer buffer, Side side, DataContext context) {
        int attachmentIndex = buffer.takeVarInt();
        if (attachmentIndex < 0 || attachmentIndex >= attachmentTypes.size()) {
            throw new DataException("Unknown attachment type " + attachmentIndex);
        }
        Attachment attachment = attachmentTypes.get(attachmentIndex).readKey(buffer, side, context);
        int typeIndex = buffer.takeVarInt();
        if (typeIndex < 0 || typeIndex >= types.size()) {
            throw new DataException("Unknown module type " + typeIndex);
        }
        return new Target(attachment, types.get(typeIndex));
    }

    public Module<?> readLocator(DataBuffer buffer, Side side, DataContext context) {
        Target target = readTarget(buffer, side, context);
        return target.attachment() == null ? null : target.attachment().modules().get(target.type());
    }

    public record Target(Attachment attachment, ModuleType<?, ?> type) {
    }

    private static final class SideState {

        private final List<ModuleIndex> behaviors = new ArrayList<>();
        private final List<ModuleIndex> types = new ArrayList<>();
        private final ModuleIndex watchers = new ModuleIndex(1);
        private final List<Module<?>> dirty = new ArrayList<>();
        private final Set<ModuleType<?, ?>> pending = new LinkedHashSet<>();
        private final Map<String, Set<ModuleHolder>> dormantHolders = new HashMap<>();

        void addBehavior(BehaviorKey<?> key) {
            behaviors.add(new ModuleIndex(key.slot()));
        }

        void addType() {
            types.add(new ModuleIndex(0));
        }
    }
}
