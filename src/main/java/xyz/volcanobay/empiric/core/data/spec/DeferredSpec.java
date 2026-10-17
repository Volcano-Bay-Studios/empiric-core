package xyz.volcanobay.empiric.core.data.spec;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.DataException;
import xyz.volcanobay.empiric.core.data.DataPrinter;
import xyz.volcanobay.empiric.core.data.Mode;
import xyz.volcanobay.empiric.core.data.Spec;

import java.util.function.Supplier;

public final class DeferredSpec<T> implements Spec<T> {

    private final Supplier<Spec<T>> source;
    private Spec<T> resolved;

    public DeferredSpec(Supplier<Spec<T>> source) {
        this.source = source;
    }

    private Spec<T> spec() {
        if (resolved == null) {
            resolved = source.get();
            if (resolved == null) {
                throw new DataException("Recursive spec was used before it finished resolving");
            }
        }
        return resolved;
    }

    @Override
    public void write(DataBuffer buffer, T value, DataContext context) {
        spec().write(buffer, value, context);
    }

    @Override
    public T read(DataBuffer buffer, DataContext context) {
        return spec().read(buffer, context);
    }

    @Override
    public T update(DataBuffer buffer, T existing, DataContext context) {
        return spec().update(buffer, existing, context);
    }

    @Override
    public boolean supportsUpdate() {
        Spec<T> spec = resolved != null ? resolved : source.get();
        return spec == null || spec.supportsUpdate();
    }

    @Override
    public Mode defaultMode() {
        Spec<T> spec = resolved != null ? resolved : source.get();
        return spec == null ? Mode.REPLACE : spec.defaultMode();
    }

    @Override
    public T copy(T value) {
        return spec().copy(value);
    }

    @Override
    public T copyInto(T source, T target) {
        return spec().copyInto(source, target);
    }

    @Override
    public boolean same(T first, T second) {
        return spec().same(first, second);
    }

    @Override
    public void print(T value, DataPrinter printer) {
        spec().print(value, printer);
    }
}
