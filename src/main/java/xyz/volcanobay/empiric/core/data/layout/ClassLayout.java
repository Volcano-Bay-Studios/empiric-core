package xyz.volcanobay.empiric.core.data.layout;

import xyz.volcanobay.empiric.core.data.Data;
import xyz.volcanobay.empiric.core.data.DataException;
import xyz.volcanobay.empiric.core.data.Mode;
import xyz.volcanobay.empiric.core.data.SelfSpec;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public final class ClassLayout {

    private final Class<?> type;
    private final int version;
    private final Mode mode;
    private final List<FieldLayout> fields;
    private final List<FieldLayout> statics;
    private final Supplier<Object> constructor;

    private ClassLayout(Class<?> type, int version, Mode mode, List<FieldLayout> fields, List<FieldLayout> statics, Supplier<Object> constructor) {
        this.type = type;
        this.version = version;
        this.mode = mode;
        this.fields = List.copyOf(fields);
        this.statics = List.copyOf(statics);
        this.constructor = constructor;
    }

    static ClassLayout parse(Class<?> type) {
        if (type.isRecord() || type.isInterface() || type.isPrimitive() || type.isArray()) {
            throw new DataException(type.getName() + " can't have a field layout");
        }
        Deque<Class<?>> hierarchy = new ArrayDeque<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            hierarchy.push(current);
        }
        List<FieldLayout> fields = new ArrayList<>();
        List<FieldLayout> statics = new ArrayList<>();
        Set<String> instanceIds = new HashSet<>();
        Set<String> staticIds = new HashSet<>();
        for (Class<?> current : hierarchy) {
            Field[] declared;
            try {
                declared = current.getDeclaredFields();
            } catch (LinkageError | RuntimeException e) {
                System.getLogger(ClassLayout.class.getName()).log(System.Logger.Level.WARNING, current.getName() + " has field types that can't load here, so its fields are skipped: " + e);
                continue;
            }
            for (Field field : declared) {
                Data data = field.getAnnotation(Data.class);
                if (data == null) {
                    continue;
                }
                boolean isStatic = Modifier.isStatic(field.getModifiers());
                if (isStatic && current != type) {
                    continue;
                }
                FieldLayout layout = new FieldLayout(field, data);
                Set<String> ids = isStatic ? staticIds : instanceIds;
                claim(ids, layout.id(), layout);
                for (String alias : layout.aliases()) {
                    claim(ids, alias, layout);
                }
                (isStatic ? statics : fields).add(layout);
            }
        }
        SelfSpec selfSpec = type.getAnnotation(SelfSpec.class);
        int version = selfSpec == null ? 0 : selfSpec.version();
        Mode mode = selfSpec == null ? Mode.REPLACE : selfSpec.mode();
        return new ClassLayout(type, version, mode, fields, statics, findConstructor(type));
    }

    private static void claim(Set<String> ids, String id, FieldLayout layout) {
        if (!ids.add(id)) {
            throw new DataException(layout.describe() + " reuses the id or alias \"" + id + "\"");
        }
    }

    @SuppressWarnings("unchecked")
    private static Supplier<Object> findConstructor(Class<?> type) {
        if (Modifier.isAbstract(type.getModifiers())) {
            return null;
        }
        Constructor<?> constructor;
        try {
            constructor = type.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            return null;
        }
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
            return Accessors.bind(Supplier.class, lookup.unreflectConstructor(constructor));
        } catch (IllegalAccessException e) {
            constructor.setAccessible(true);
            try {
                return Accessors.bind(Supplier.class, MethodHandles.lookup().unreflectConstructor(constructor));
            } catch (IllegalAccessException inaccessible) {
                throw new DataException("Can't access the constructor of " + type.getName(), inaccessible);
            }
        }
    }

    public Class<?> type() {
        return type;
    }

    public int version() {
        return version;
    }

    public Mode mode() {
        return mode;
    }

    public List<FieldLayout> fields() {
        return fields;
    }

    public List<FieldLayout> statics() {
        return statics;
    }

    public boolean constructible() {
        return constructor != null;
    }

    public Object newInstance() {
        if (constructor == null) {
            throw new DataException(type.getName() + " needs a no-arg constructor to be read as a new value");
        }
        return constructor.get();
    }
}
