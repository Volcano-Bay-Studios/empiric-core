package xyz.volcanobay.empiric.core.plugin;

import xyz.volcanobay.empiric.core.data.SpecRegistry;
import xyz.volcanobay.empiric.core.entry.DataEntrySystem;
import xyz.volcanobay.empiric.core.module.ModuleSystem;

public interface EmpiricPlugin {

    default void registerSpecs(SpecRegistry specs) {
    }

    default void registerData(DataEntrySystem data) {
    }

    default void registerAttachments(ModuleSystem modules) {
    }

    default void registerBehaviors(ModuleSystem modules) {
    }

    default void registerModules(ModuleSystem modules) {
    }
}
