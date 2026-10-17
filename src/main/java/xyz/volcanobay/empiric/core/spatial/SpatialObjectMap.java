package xyz.volcanobay.empiric.core.spatial;

import java.util.Arrays;

public class SpatialObjectMap<V> {
    private long[] keysX, keysY, keysZ;
    private Object[] values;
    private int capacity;
    private int size = 0;
    private final float loadFactor = 0.7f;
    private int threshold;

    public SpatialObjectMap(int initialCapacity) {
        this.capacity = Math.max(initialCapacity, 4);
        this.threshold = (int) (capacity * loadFactor);
        initArrays(capacity);
    }

    private void initArrays(int cap) {
        this.keysX = new long[cap];
        this.keysY = new long[cap];
        this.keysZ = new long[cap];
        this.values = new Object[cap];
    }

    private int indexOf(long x, long y, long z) {
        return Math.abs((int) (SpatialHasher.computeHash(x, y, z) % capacity));
    }

    @SuppressWarnings("unchecked")
    public V get(long x, long y, long z) {
        int index = indexOf(x, y, z);
        while (values[index] != null) {
            if (keysX[index] == x && keysY[index] == y && keysZ[index] == z) {
                return (V) values[index];
            }
            index = (index + 1) % capacity;
        }
        return null;
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
        if (insertInternal(x, y, z, value)) {
            size++;
        }
    }

    @SuppressWarnings("unchecked")
    public V remove(long x, long y, long z) {
        int i = indexOf(x, y, z);
        while (values[i] != null) {
            if (keysX[i] == x && keysY[i] == y && keysZ[i] == z) {
                V removed = (V) values[i];
                values[i] = null;
                size--;
                int j = i;
                while (true) {
                    j = (j + 1) % capacity;
                    if (values[j] == null) {
                        break;
                    }
                    long kx = keysX[j], ky = keysY[j], kz = keysZ[j];
                    Object kv = values[j];
                    values[j] = null;
                    size--;
                    if (insertInternal(kx, ky, kz, kv)) {
                        size++;
                    }
                }
                return removed;
            }
            i = (i + 1) % capacity;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void forEach(Visitor<V> visitor) {
        Object[] snapshot = values.clone();
        long[] sx = keysX.clone(), sy = keysY.clone(), sz = keysZ.clone();
        for (int i = 0; i < snapshot.length; i++) {
            if (snapshot[i] != null) {
                visitor.visit(sx[i], sy[i], sz[i], (V) snapshot[i]);
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
        initArrays(capacity);
        size = 0;
    }

    private void resize() {
        long[] oldX = keysX, oldY = keysY, oldZ = keysZ;
        Object[] oldValues = values;
        int oldCap = capacity;

        this.capacity *= 2;
        this.threshold = (int) (capacity * loadFactor);
        this.size = 0;
        initArrays(capacity);

        for (int i = 0; i < oldCap; i++) {
            if (oldValues[i] != null) {
                insertInternal(oldX[i], oldY[i], oldZ[i], oldValues[i]);
                size++;
            }
        }
    }

    private boolean insertInternal(long x, long y, long z, Object value) {
        int index = indexOf(x, y, z);
        while (values[index] != null) {
            if (keysX[index] == x && keysY[index] == y && keysZ[index] == z) {
                values[index] = value;
                return false;
            }
            index = (index + 1) % capacity;
        }
        keysX[index] = x;
        keysY[index] = y;
        keysZ[index] = z;
        values[index] = value;
        return true;
    }

    public interface Visitor<V> {
        void visit(long x, long y, long z, V value);
    }
}
