package xyz.volcanobay.empiric.core;

import xyz.volcanobay.empiric.core.data.SpecRegistry;
import xyz.volcanobay.empiric.core.data.spec.DefaultSpecs;
import xyz.volcanobay.empiric.core.entry.DataEntrySystem;
import xyz.volcanobay.empiric.core.module.ModuleSystem;
import xyz.volcanobay.empiric.core.plugin.EmpiricPlugin;

import java.util.ArrayList;
import java.util.List;

public final class EmpiricCore {

    private static final SpecRegistry SPECS = createSpecs();
    private static final ModuleSystem MODULES = new ModuleSystem(SPECS);
    private static final DataEntrySystem DATA = new DataEntrySystem(SPECS);
    private static boolean bootstrapped;

    private EmpiricCore() {
    }

    private static SpecRegistry createSpecs() {
        SpecRegistry registry = new SpecRegistry();
        DefaultSpecs.register(registry);
        return registry;
    }

    public static SpecRegistry specs() {
        return SPECS;
    }

    public static ModuleSystem modules() {
        return MODULES;
    }

    public static DataEntrySystem data() {
        return DATA;
    }

    public static boolean isBootstrapped() {
        return bootstrapped;
    }

    public static synchronized void bootstrap(EmpiricPlugin platform, List<? extends EmpiricPlugin> plugins) {
        if (bootstrapped) {
            throw new IllegalStateException("Empiric core was already bootstrapped");
        }
        List<EmpiricPlugin> all = new ArrayList<>();
        all.add(platform);
        all.addAll(plugins);
        for (EmpiricPlugin plugin : all) {
            plugin.registerSpecs(SPECS);
        }
        for (EmpiricPlugin plugin : all) {
            plugin.registerData(DATA);
        }
        for (EmpiricPlugin plugin : all) {
            plugin.registerAttachments(MODULES);
        }
        for (EmpiricPlugin plugin : all) {
            plugin.registerBehaviors(MODULES);
        }
        for (EmpiricPlugin plugin : all) {
            plugin.registerModules(MODULES);
        }
        SPECS.freeze();
        MODULES.freeze();
        DATA.freeze();
        bootstrapped = true;
    }
}
