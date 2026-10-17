package xyz.volcanobay.empiric.core.module;

@FunctionalInterface
public interface ModuleEventListener<M, E> {

    void on(M module, E payload);
}
