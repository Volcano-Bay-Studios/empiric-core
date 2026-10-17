package xyz.volcanobay.empiric.core.data.layout;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LayoutCache {

    private final Map<Class<?>, ClassLayout> layouts = new ConcurrentHashMap<>();

    public ClassLayout get(Class<?> type) {
        ClassLayout layout = layouts.get(type);
        if (layout == null) {
            layout = ClassLayout.parse(type);
            ClassLayout existing = layouts.putIfAbsent(type, layout);
            if (existing != null) {
                layout = existing;
            }
        }
        return layout;
    }
}
