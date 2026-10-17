package xyz.volcanobay.empiric.core.data;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

public final class Types {

    private Types() {
    }

    public static Class<?> raw(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterized) {
            return (Class<?>) parameterized.getRawType();
        }
        if (type instanceof GenericArrayType array) {
            return Array.newInstance(raw(array.getGenericComponentType()), 0).getClass();
        }
        if (type instanceof WildcardType wildcard) {
            return raw(wildcard.getUpperBounds()[0]);
        }
        if (type instanceof TypeVariable<?> variable) {
            throw new DataException("Type variable " + variable.getName() + " can't be resolved to a spec");
        }
        throw new DataException("Unsupported type " + type);
    }

    public static Type argument(Type type, int index) {
        if (type instanceof ParameterizedType parameterized) {
            Type argument = parameterized.getActualTypeArguments()[index];
            if (argument instanceof WildcardType wildcard) {
                return wildcard.getUpperBounds()[0];
            }
            return argument;
        }
        throw new DataException("Missing type argument " + index + " on " + type);
    }

    public static Type component(Type type) {
        if (type instanceof GenericArrayType array) {
            return array.getGenericComponentType();
        }
        if (type instanceof Class<?> clazz && clazz.isArray()) {
            return clazz.getComponentType();
        }
        throw new DataException(type + " is not an array");
    }
}
