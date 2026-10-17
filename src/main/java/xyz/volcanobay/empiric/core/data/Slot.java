package xyz.volcanobay.empiric.core.data;

public record Slot<T>(Spec<T> spec, Mode mode) {

    public void write(DataBuffer buffer, T value, DataContext context) {
        spec.write(buffer, value, context);
    }

    public T read(DataBuffer buffer, T existing, DataContext context) {
        if (mode == Mode.UPDATE && existing != null) {
            return spec.update(buffer, existing, context);
        }
        return spec.read(buffer, context);
    }

    public boolean updates() {
        return mode == Mode.UPDATE;
    }
}
