package xyz.volcanobay.empiric.core.spatial;

import java.util.Arrays;

public class SpatialMap {
    private long[] keysX, keysY, keysZ;
    private int[] networkIds;
    private int capacity;
    private int size = 0;
    private final float loadFactor = 0.7f;
    private int threshold;
    private static final int EMPTY = -1;

    public SpatialMap(int initialCapacity) {
        this.capacity = initialCapacity;
        this.threshold = (int) (capacity * loadFactor);
        initArrays(capacity);
    }

    private void initArrays(int cap) {
        this.keysX = new long[cap];
        this.keysY = new long[cap];
        this.keysZ = new long[cap];
        this.networkIds = new int[cap];
        Arrays.fill(networkIds, EMPTY);
    }

    public int get(long x, long y, long z) {
        int index = Math.abs((int)(SpatialHasher.computeHash(x, y, z) % capacity));
        while (networkIds[index] != EMPTY) {
            if (keysX[index] == x && keysY[index] == y && keysZ[index] == z) {
                return networkIds[index];
            }
            index = (index + 1) % capacity;
        }
        return EMPTY;
    }

    public void put(long x, long y, long z, int netId) {
        if (size >= threshold) {
            resize();
        }
        if (insertInternal(x, y, z, netId)) {
            size++;
        }
    }

    public void remove(long x, long y, long z) {
        int i = Math.abs((int) (SpatialHasher.computeHash(x, y, z) % capacity));
        while (networkIds[i] != EMPTY) {
            if (keysX[i] == x && keysY[i] == y && keysZ[i] == z) {
                networkIds[i] = EMPTY;
                size--;
                // Backshift elements in the same cluster to fill the gap [105]
                int j = i;
                while (true) {
                    j = (j + 1) % capacity;
                    if (networkIds[j] == EMPTY) {
                        break;
                    }
                    long kx = keysX[j], ky = keysY[j], kz = keysZ[j];
                    int kid = networkIds[j];
                    networkIds[j] = EMPTY;
                    size--;
                    put(kx, ky, kz, kid);
                }
                return;
            }
            i = (i + 1) % capacity;
        }
    }

    public void clear() {
        initArrays(capacity);
        size = 0;
    }

    private void resize() {
        long[] oldX = keysX, oldY = keysY, oldZ = keysZ;
        int[] oldIds = networkIds;
        int oldCap = capacity;

        this.capacity *= 2;
        this.threshold = (int) (capacity * loadFactor);
        this.size = 0;
        initArrays(capacity);

        for (int i = 0; i < oldCap; i++) {
            if (oldIds[i] != EMPTY) {
                insertInternal(oldX[i], oldY[i], oldZ[i], oldIds[i]);
                size++;
            }
        }
    }

    private boolean insertInternal(long x, long y, long z, int netId) {
        int index = Math.abs((int)(SpatialHasher.computeHash(x, y, z) % capacity));
        while (networkIds[index] != EMPTY) {
            if (keysX[index] == x && keysY[index] == y && keysZ[index] == z) {
                networkIds[index] = netId;
                return false;
            }
            index = (index + 1) % capacity;
        }
        keysX[index] = x;
        keysY[index] = y;
        keysZ[index] = z;
        networkIds[index] = netId;
        return true;
    }
}
