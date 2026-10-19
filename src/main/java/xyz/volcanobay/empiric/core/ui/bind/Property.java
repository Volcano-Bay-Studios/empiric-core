package xyz.volcanobay.empiric.core.ui.bind;

/**
 * A value a widget reads and writes, such as a local field or a synced module field.
 */
public interface Property<T> {

    T get();

    void set(T value);

    default boolean writable() {
        return true;
    }

    /**
     * Marks the value as changed after it was mutated in place, such as an item list edited through a menu slot.
     */
    default void markChanged() {
    }
}
