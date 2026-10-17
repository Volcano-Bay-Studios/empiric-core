package xyz.volcanobay.empiric.core.spatial;

public class SpatialHasher {
    private static final long P1 = 73856093L;
    private static final long P2 = 19349663L;
    private static final long P3 = 83492791L;

    public static long computeHash(long x, long y, long z) {
        long h = (x * P1) ^ (y * P2) ^ (z * P3);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }
}
