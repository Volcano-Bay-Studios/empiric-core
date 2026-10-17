package xyz.volcanobay.empiric.core.module;

public final class DropPolicy {

    public static final DropPolicy UNLOAD = new DropPolicy("unload");

    private final String id;

    public DropPolicy(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
