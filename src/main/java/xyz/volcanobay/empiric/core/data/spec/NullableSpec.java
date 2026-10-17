package xyz.volcanobay.empiric.core.data.spec;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.DataPrinter;
import xyz.volcanobay.empiric.core.data.Mode;
import xyz.volcanobay.empiric.core.data.Slot;
import xyz.volcanobay.empiric.core.data.Spec;

public record NullableSpec<T>(Spec<T> inner) implements Spec<T> {

    public static <T> Slot<T> wrap(Slot<T> slot) {
        return new Slot<>(new NullableSpec<>(slot.spec()), slot.mode());
    }

    @Override
    public void write(DataBuffer buffer, T value, DataContext context) {
        buffer.putPresence(value != null);
        if (value != null) {
            inner.write(buffer, value, context);
        }
    }

    @Override
    public T read(DataBuffer buffer, DataContext context) {
        return buffer.takePresence() ? inner.read(buffer, context) : null;
    }

    @Override
    public T update(DataBuffer buffer, T existing, DataContext context) {
        if (!buffer.takePresence()) {
            return null;
        }
        return existing == null ? inner.read(buffer, context) : inner.update(buffer, existing, context);
    }

    @Override
    public boolean supportsUpdate() {
        return inner.supportsUpdate();
    }

    @Override
    public Mode defaultMode() {
        return inner.defaultMode();
    }

    @Override
    public T copy(T value) {
        return value == null ? null : inner.copy(value);
    }

    @Override
    public T copyInto(T source, T target) {
        if (source == null || target == null) {
            return copy(source);
        }
        return inner.copyInto(source, target);
    }

    @Override
    public boolean same(T first, T second) {
        if (first == null || second == null) {
            return first == second;
        }
        return inner.same(first, second);
    }

    @Override
    public void print(T value, DataPrinter printer) {
        if (value == null) {
            printer.value(null);
        } else {
            inner.print(value, printer);
        }
    }
}
