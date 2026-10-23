package xyz.volcanobay.empiric.core.module;

import xyz.volcanobay.empiric.core.EmpiricCore;
import xyz.volcanobay.empiric.core.attachment.Attachment;
import xyz.volcanobay.empiric.core.attachment.ModuleAttachment;
import xyz.volcanobay.empiric.core.data.FieldDelta;
import xyz.volcanobay.empiric.core.data.spec.ObjectSpec;

import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class Module<A extends Attachment> {

    A attachment;
    ModuleType<?, A> type;
    int[] slots;
    BitSet dirty;
    boolean queued;
    Object[] snapshot;
    long[] sentAt;
    Pending[] pending;
    boolean attached;
    private ModuleAttachment children;

    void init(A attachment, ModuleType<?, A> type, int behaviorCount) {
        this.attachment = attachment;
        this.type = type;
        this.slots = new int[2 + behaviorCount];
        Arrays.fill(slots, -1);
    }

    public final A attachment() {
        return attachment;
    }

    public final ModuleType<?, A> type() {
        return type;
    }

    public final Side side() {
        return attachment.side();
    }

    public final boolean isAttached() {
        return attached;
    }

    public final ModuleAttachment children() {
        if (children == null) {
            children = new ModuleAttachment(this);
        }
        return children;
    }

    public final boolean hasChildren() {
        return children != null && !children.modules().isEmpty();
    }

    final ModuleAttachment childrenIfPresent() {
        return children;
    }

    public final void markDirty() {
        EmpiricCore.modules().markDirty(this, -1);
    }

    public final void markDirty(String field) {
        EmpiricCore.modules().markDirty(this, spec().syncIndex(field));
    }

    /**
     * Marks one entry of a {@code @Data(delta = true)} map or set field as added or changed, so only it is synced.
     */
    public final void markDirty(String field, Object key) {
        EmpiricCore.modules().markKey(this, spec().syncIndex(field), key, false);
    }

    /**
     * Marks one entry of a delta field as removed.
     */
    public final void markRemoved(String field, Object key) {
        EmpiricCore.modules().markKey(this, spec().syncIndex(field), key, true);
    }

    /**
     * The marked entries of the delta field at {@code index} waiting to be synced, or null if there are none or the whole
     * field is waiting.
     */
    public final FieldDelta delta(int index) {
        Pending entry = pending == null ? null : pending[index];
        if (entry == null || entry.full) {
            return null;
        }
        return new FieldDelta(false, new LinkedHashSet<>(entry.changed), new LinkedHashSet<>(entry.removed));
    }

    /**
     * Whether the delta field at {@code index} is waiting to be synced whole.
     */
    public final boolean pendingFull(int index) {
        return pending != null && pending[index] != null && pending[index].full;
    }

    static final class Pending {

        boolean full;
        final Set<Object> changed = new LinkedHashSet<>();
        final Set<Object> removed = new LinkedHashSet<>();
    }

    public final boolean isDirty(String field) {
        return dirty != null && dirty.get(spec().syncIndex(field));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public final ObjectSpec<Object> spec() {
        return ((ModuleType) type).spec(this);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public final Object data() {
        return ((ModuleType) type).data(this);
    }

    protected void onAttach() {
    }

    protected void onDetach() {
    }

    protected void onLoad() {
    }

    protected void onUnload() {
    }
}
