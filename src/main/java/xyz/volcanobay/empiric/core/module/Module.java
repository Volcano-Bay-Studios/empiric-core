package xyz.volcanobay.empiric.core.module;

import xyz.volcanobay.empiric.core.EmpiricCore;
import xyz.volcanobay.empiric.core.attachment.Attachment;
import xyz.volcanobay.empiric.core.attachment.ModuleAttachment;

import java.util.Arrays;
import java.util.BitSet;

public abstract class Module<A extends Attachment> {

    A attachment;
    ModuleType<?, A> type;
    int[] slots;
    BitSet dirty;
    boolean queued;
    Object[] snapshot;
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
        EmpiricCore.modules().markDirty(this, type.spec().syncIndex(field));
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
