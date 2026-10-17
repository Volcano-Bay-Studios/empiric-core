package xyz.volcanobay.empiric.core.module;

public interface ModuleListener {

    default void attached(Module<?> module) {
    }

    default void loaded(Module<?> module) {
    }

    default void detached(Module<?> module) {
    }

    default void unloaded(Module<?> module) {
    }
}
