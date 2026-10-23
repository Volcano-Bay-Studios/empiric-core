package xyz.volcanobay.empiric.core.data.spec;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.FieldDelta;

/**
 * A spec for a map or set that can write just some of its entries, for {@code @Data(delta = true)} fields. The frames
 * are only for syncing, so they use whatever the buffer gives for arrays and elements and are never saved.
 */
public interface DeltaSpec<C> {

    /**
     * Whether this spec's type can be sent as a delta, such as a set but not a list.
     */
    boolean supportsDelta();

    void writeDelta(DataBuffer buffer, C value, FieldDelta delta, DataContext context);

    void readDelta(DataBuffer buffer, C existing, DataContext context);

    /**
     * What a tracker sees for {@code key}: the value of a map, or the element of a set. Null if the key isn't there.
     */
    Object entry(C value, Object key);

    /**
     * Every key now in the field.
     */
    Iterable<?> keys(C value);
}
