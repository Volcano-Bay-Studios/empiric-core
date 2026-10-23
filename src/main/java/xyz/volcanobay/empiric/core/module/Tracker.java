package xyz.volcanobay.empiric.core.module;

import xyz.volcanobay.empiric.core.data.Track;

/**
 * Decides what a viewer, a player on the server, gets of a module or of one of its synced fields.
 * Registered with {@code ModuleType.Builder.track}, usually as a method reference.
 */
@FunctionalInterface
public interface Tracker<M, V> {

    Track track(M module, V viewer);
}
