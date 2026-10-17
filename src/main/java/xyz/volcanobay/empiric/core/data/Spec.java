package xyz.volcanobay.empiric.core.data;

import java.util.Objects;

public interface Spec<T> {

    void write(DataBuffer buffer, T value, DataContext context);

    T read(DataBuffer buffer, DataContext context);

    default T update(DataBuffer buffer, T existing, DataContext context) {
        return read(buffer, context);
    }

    default boolean supportsUpdate() {
        return false;
    }

    default Mode defaultMode() {
        return Mode.REPLACE;
    }

    default T copy(T value) {
        return value;
    }

    default T copyInto(T source, T target) {
        return copy(source);
    }

    default boolean same(T first, T second) {
        return Objects.equals(first, second);
    }

    default boolean keyed() {
        return false;
    }

    default String toKey(T value) {
        throw new DataException(value + " can't be used as a key");
    }

    default T fromKey(String key) {
        throw new DataException("\"" + key + "\" can't be read as a key");
    }

    default void print(T value, DataPrinter printer) {
        printer.value(value);
    }
}
