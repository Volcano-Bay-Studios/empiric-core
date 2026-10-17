package xyz.volcanobay.empiric.core.module;

import xyz.volcanobay.empiric.core.data.spec.ObjectSpec;

import java.util.BitSet;

/**
 * The synced fields of a module that were just received, as indices into {@link ObjectSpec#synced()}.
 */
public record SyncedFields(ObjectSpec<?> spec, BitSet fields) {

    public static SyncedFields full(ObjectSpec<?> spec) {
        BitSet fields = new BitSet();
        fields.set(0, spec.synced().length);
        return new SyncedFields(spec, fields);
    }

    public boolean has(String field) {
        return fields.get(spec.syncIndex(field));
    }

    public boolean isFull() {
        return fields.cardinality() == spec.synced().length;
    }
}
