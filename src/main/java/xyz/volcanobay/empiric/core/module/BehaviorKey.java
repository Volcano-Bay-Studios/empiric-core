package xyz.volcanobay.empiric.core.module;

public final class BehaviorKey<B extends Behavior> {

    private final Class<B> type;
    private final int id;

    BehaviorKey(Class<B> type, int id) {
        this.type = type;
        this.id = id;
    }

    public Class<B> type() {
        return type;
    }

    public int id() {
        return id;
    }

    int slot() {
        return 2 + id;
    }

    @Override
    public String toString() {
        return type.getSimpleName();
    }
}
