package xyz.volcanobay.empiric.core.module;

public final class ModuleEvents {

    public static final ModuleEvent<Void> ATTACHED = ModuleEvent.create(() -> "empiric_core:attached");
    public static final ModuleEvent<Void> LOADED = ModuleEvent.create(() -> "empiric_core:loaded");
    public static final ModuleEvent<Void> DETACHED = ModuleEvent.create(() -> "empiric_core:detached");
    public static final ModuleEvent<Void> UNLOADED = ModuleEvent.create(() -> "empiric_core:unloaded");
    /**
     * Fired after data from the other side was applied to a module, on whichever side received it.
     */
    public static final ModuleEvent<SyncedFields> SYNCED = ModuleEvent.create(() -> "empiric_core:synced");

    private ModuleEvents() {
    }
}
