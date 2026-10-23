package xyz.volcanobay.empiric.core.data;

import java.util.Set;

/**
 * The entries of a {@code @Data(delta = true)} map or set field that are sent instead of the whole field. For a set the
 * keys are its elements.
 *
 * @param replace whether the receiver drops what it has first, so {@code changed} is the whole field
 * @param changed the keys added or whose value changed; any that the field no longer holds are sent as removed
 * @param removed the keys that left the field
 */
public record FieldDelta(boolean replace, Set<Object> changed, Set<Object> removed) {

    public boolean isEmpty() {
        return !replace && changed.isEmpty() && removed.isEmpty();
    }
}
