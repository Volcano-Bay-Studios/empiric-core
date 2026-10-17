package xyz.volcanobay.empiric.core.attachment;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.module.Side;

import java.util.function.Consumer;

/**
 * An attachment type is the type of {@link Attachment} a module can form to another object.
 */
public abstract class AttachmentType<A extends Attachment, K> {

    private final String id;
    int index = -1;

    protected AttachmentType(String id) {
        this.id = id;
    }

    public final String id() {
        return id;
    }

    public final int index() {
        return index;
    }

    public final void assignIndex(int index) {
        if (this.index >= 0) {
            throw new IllegalStateException(id + " is already registered");
        }
        this.index = index;
    }

    public abstract A get(K key);

    public A find(K key) {
        return get(key);
    }

    public abstract void writeKey(DataBuffer buffer, A attachment, DataContext context);

    public abstract A readKey(DataBuffer buffer, Side side, DataContext context);

    public void forEachLoaded(Side side, Consumer<A> action) {
    }

    @Override
    public String toString() {
        return id;
    }
}
