package xyz.volcanobay.empiric.core.module;

import xyz.volcanobay.empiric.core.data.Track;

/**
 * Decides what a viewer gets of one entry of a {@code @Data(delta = true)} map or set field. Registered with
 * {@code ModuleType.Builder.trackEntries}, usually as a method reference.
 *
 * @param <K> the key of a map, or the element of a set
 * @param <E> the value of a map, or the element of a set
 */
@FunctionalInterface
public interface EntryTracker<M, K, E, V> {

    Track track(M module, K key, E entry, V viewer);
}
