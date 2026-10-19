package xyz.volcanobay.empiric.core.ui.bind;

public final class LocalProperty<T> implements Property<T> {

    private T value;

    public LocalProperty(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public void set(T value) {
        this.value = value;
    }
}
