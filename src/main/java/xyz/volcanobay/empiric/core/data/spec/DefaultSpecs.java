package xyz.volcanobay.empiric.core.data.spec;

import xyz.volcanobay.empiric.core.data.SpecRegistry;

public final class DefaultSpecs {

    private DefaultSpecs() {
    }

    public static void register(SpecRegistry registry) {
        PrimitiveSpecs.register(registry);
        CommonSpecs.register(registry);
        ArraySpecs.register(registry);
        registry.register(TypeSpecs.ENUMS);
        registry.register(TypeSpecs.RECORDS);
        registry.register(CollectionSpecs.OPTIONALS);
        registry.register(CollectionSpecs.COLLECTIONS);
        registry.register(CollectionSpecs.MAPS);
        FastutilSpecs.register(registry);
    }
}
