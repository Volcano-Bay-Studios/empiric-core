package xyz.volcanobay.empiric.core.module;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A key for something that happens to a module. Any mod can create one and fire it through
 * {@link ModuleType#fire}, and other mods listen to it per module type with {@link ModuleType#listen}.
 */
public final class ModuleEvent<E> {

    private static final AtomicInteger COUNT = new AtomicInteger();

    private final String id;
    private final int index;

    private ModuleEvent(String id) {
        this.id = id;
        this.index = COUNT.getAndIncrement();
    }

    public static <E> ModuleEvent<E> create(ModuleId id) {
        return new ModuleEvent<>(id.empiriccore$id());
    }

    public String id() {
        return id;
    }

    int index() {
        return index;
    }

    @Override
    public String toString() {
        return id;
    }
}
