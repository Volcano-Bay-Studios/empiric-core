package xyz.volcanobay.empiric.core.spatial;

import java.util.Arrays;

/**
 * A hash map from three {@code long} coordinates to an object, built for lookups. A lookup that finds nothing costs
 * no more than one that does, which matters because most ask whether a block has anything attached.
 * <p>
 * Null is never stored, and putting null removes the entry. {@link #forEach} walks a snapshot, so the visitor may
 * change the map. The capacity is rounded up to a power of two. Not thread safe.
 */
public class SpatialObjectMap<V> {

    private static final long K1 = 0x9E3779B97F4A7C15L;
    private static final long K2 = 0xC2B2AE3D27D4EB4FL;
    private static final long K3 = 0x165667B19E3779F9L;
    private static final long K4 = 0xD6E8FEB86659FD93L;

    private short[] tags;
    private long[] keys;
    private Object[] values;
    private int capacity;
    private int mask;
    private int shift;
    private int size;
    private int threshold;

    public SpatialObjectMap(int initialCapacity) {
        allocate(Integer.highestOneBit(Math.max(initialCapacity, 4) - 1) << 1);
    }

    private void allocate(int newCapacity) {
        this.capacity = newCapacity;
        this.mask = newCapacity - 1;
        this.shift = 64 - Integer.numberOfTrailingZeros(newCapacity);
        this.threshold = newCapacity >> 1;
        this.tags = new short[newCapacity];
        this.keys = new long[newCapacity * 3];
        this.values = new Object[newCapacity];
    }

    private static long hash(long x, long y, long z) {
        long h = x * K1 + y * K2 + z * K3;
        h ^= h >>> 32;
        h *= K4;
        return h ^ (h >>> 32);
    }

    private static short tag(long hash) {
        return (short) (hash | 1L);
    }

    @SuppressWarnings("unchecked")
    public V get(long x, long y, long z) {
        if (size == 0) {
            return null;
        }
        long hash = hash(x, y, z);
        short tag = tag(hash);
        short[] tags = this.tags;
        int mask = this.mask;
        int slot = (int) (hash >>> shift);
        while (true) {
            short found = tags[slot];
            if (found == 0) {
                return null;
            }
            if (found == tag) {
                int base = slot * 3;
                if (keys[base] == x && keys[base + 1] == y && keys[base + 2] == z) {
                    return (V) values[slot];
                }
            }
            slot = (slot + 1) & mask;
        }
    }

    public boolean contains(long x, long y, long z) {
        return get(x, y, z) != null;
    }

    public void put(long x, long y, long z, V value) {
        if (value == null) {
            remove(x, y, z);
            return;
        }
        if (size >= threshold) {
            resize();
        }
        long hash = hash(x, y, z);
        short tag = tag(hash);
        int slot = (int) (hash >>> shift);
        while (true) {
            short found = tags[slot];
            int base = slot * 3;
            if (found == 0) {
                tags[slot] = tag;
                keys[base] = x;
                keys[base + 1] = y;
                keys[base + 2] = z;
                values[slot] = value;
                size++;
                return;
            }
            if (found == tag && keys[base] == x && keys[base + 1] == y && keys[base + 2] == z) {
                values[slot] = value;
                return;
            }
            slot = (slot + 1) & mask;
        }
    }

    @SuppressWarnings("unchecked")
    public V remove(long x, long y, long z) {
        if (size == 0) {
            return null;
        }
        long hash = hash(x, y, z);
        short tag = tag(hash);
        int slot = (int) (hash >>> shift);
        while (true) {
            short found = tags[slot];
            if (found == 0) {
                return null;
            }
            int base = slot * 3;
            if (found == tag && keys[base] == x && keys[base + 1] == y && keys[base + 2] == z) {
                break;
            }
            slot = (slot + 1) & mask;
        }
        V removed = (V) values[slot];
        int hole = slot;
        int next = (slot + 1) & mask;
        while (tags[next] != 0) {
            int base = next * 3;
            int home = (int) (hash(keys[base], keys[base + 1], keys[base + 2]) >>> shift);
            if (((next - home) & mask) >= ((next - hole) & mask)) {
                tags[hole] = tags[next];
                System.arraycopy(keys, base, keys, hole * 3, 3);
                values[hole] = values[next];
                hole = next;
            }
            next = (next + 1) & mask;
        }
        tags[hole] = 0;
        values[hole] = null;
        size--;
        return removed;
    }

    @SuppressWarnings("unchecked")
    public void forEach(Visitor<V> visitor) {
        Object[] snapshot = values.clone();
        long[] keySnapshot = keys.clone();
        for (int i = 0; i < snapshot.length; i++) {
            if (snapshot[i] != null) {
                int base = i * 3;
                visitor.visit(keySnapshot[base], keySnapshot[base + 1], keySnapshot[base + 2], (V) snapshot[i]);
            }
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        Arrays.fill(tags, (short) 0);
        Arrays.fill(values, null);
        size = 0;
    }

    private void resize() {
        short[] oldTags = tags;
        long[] oldKeys = keys;
        Object[] oldValues = values;
        allocate(capacity << 1);
        for (int i = 0; i < oldTags.length; i++) {
            if (oldTags[i] != 0) {
                int from = i * 3;
                long hash = hash(oldKeys[from], oldKeys[from + 1], oldKeys[from + 2]);
                int slot = (int) (hash >>> shift);
                while (tags[slot] != 0) {
                    slot = (slot + 1) & mask;
                }
                tags[slot] = oldTags[i];
                System.arraycopy(oldKeys, from, keys, slot * 3, 3);
                values[slot] = oldValues[i];
            }
        }
    }

    public interface Visitor<V> {
        void visit(long x, long y, long z, V value);
    }
}
