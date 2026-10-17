package xyz.volcanobay.empiric.core.data;

import java.lang.reflect.Type;

public interface SpecFactory {

    Spec<?> create(Type type, Class<?> raw, Resolution resolution, SpecRegistry registry);
}
