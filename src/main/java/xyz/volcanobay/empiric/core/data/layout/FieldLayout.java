package xyz.volcanobay.empiric.core.data.layout;

import xyz.volcanobay.empiric.core.data.Data;
import xyz.volcanobay.empiric.core.data.DataException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

public final class FieldLayout {

    private final Field field;
    private final Data data;
    private final String id;
    private final String[] aliases;
    private final boolean isStatic;
    private final boolean isFinal;
    private final MethodHandle getter;
    private final MethodHandle setter;

    FieldLayout(Field field, Data data) {
        this.field = field;
        this.data = data;
        this.id = data.id().isEmpty() ? field.getName() : data.id();
        this.aliases = data.aliases();
        this.isStatic = Modifier.isStatic(field.getModifiers());
        this.isFinal = Modifier.isFinal(field.getModifiers());
        if (isStatic && data.watch()) {
            throw new DataException(describe() + " is static and can't be watched, static data is never synced");
        }
        if (data.clientMutable() && isStatic) {
            throw new DataException(describe() + " is static and can't be client mutable, static data is never synced");
        }
        if (data.clientMutable() && !data.sync()) {
            throw new DataException(describe() + " is client mutable but doesn't sync");
        }
        if (isFinal && field.getType().isPrimitive()) {
            throw new DataException(describe() + " is a final primitive and can't be read into");
        }
        MethodHandles.Lookup lookup = lookup(field);
        Class<?> storage = field.getType().isPrimitive() ? field.getType() : Object.class;
        try {
            MethodHandle get = lookup.unreflectGetter(field);
            get = isStatic ? MethodHandles.dropArguments(get, 0, Object.class) : get.asType(get.type().changeParameterType(0, Object.class));
            this.getter = get.asType(MethodType.methodType(storage, Object.class));
            if (isFinal) {
                this.setter = null;
            } else {
                MethodHandle set = lookup.unreflectSetter(field);
                set = isStatic ? MethodHandles.dropArguments(set, 0, Object.class) : set.asType(set.type().changeParameterType(0, Object.class));
                this.setter = set.asType(MethodType.methodType(void.class, Object.class, storage));
            }
        } catch (IllegalAccessException e) {
            throw new DataException("Can't access " + describe(), e);
        }
    }

    private static MethodHandles.Lookup lookup(Field field) {
        try {
            return MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup());
        } catch (IllegalAccessException e) {
            field.setAccessible(true);
            return MethodHandles.lookup();
        }
    }

    public String describe() {
        return field.getDeclaringClass().getName() + "." + field.getName();
    }

    public Field field() {
        return field;
    }

    public Data data() {
        return data;
    }

    public String id() {
        return id;
    }

    public String[] aliases() {
        return aliases;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public boolean primitive() {
        return field.getType().isPrimitive();
    }

    public Class<?> type() {
        return field.getType();
    }

    public Type genericType() {
        return field.getGenericType();
    }

    public boolean save() {
        return data.save();
    }

    public boolean sync() {
        return data.sync() && !isStatic;
    }

    public boolean watch() {
        return data.watch();
    }

    public boolean clientMutable() {
        return data.clientMutable();
    }

    public boolean optional() {
        return data.optional();
    }

    public MethodHandle getter() {
        return getter;
    }

    public MethodHandle setter() {
        return setter;
    }
}
