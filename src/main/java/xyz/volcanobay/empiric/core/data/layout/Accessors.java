package xyz.volcanobay.empiric.core.data.layout;

import xyz.volcanobay.empiric.core.data.DataException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Accessors {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final String NAME = Accessors.class.getName().replace('.', '/') + "$Bound";
    private static final String HANDLE = "java/lang/invoke/MethodHandle";
    private static final String HANDLES = "java/lang/invoke/MethodHandles";
    private static final String LOOKUP_TYPE = "java/lang/invoke/MethodHandles$Lookup";

    private Accessors() {
    }

    public static <I> I bind(Class<I> type, MethodHandle target) {
        Method method = functionalMethod(type);
        MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
        MethodHandle adapted = target.asType(methodType);
        try {
            byte[] bytes = generate(type, method.getName(), methodType);
            MethodHandles.Lookup hidden = LOOKUP.defineHiddenClassWithClassData(bytes, adapted, true);
            Object instance = hidden.findConstructor(hidden.lookupClass(), MethodType.methodType(void.class)).invoke();
            return type.cast(instance);
        } catch (Throwable e) {
            throw new DataException("Couldn't generate a " + type.getSimpleName() + " for " + target, e);
        }
    }

    private static Method functionalMethod(Class<?> type) {
        if (!type.isInterface()) {
            throw new DataException(type.getName() + " isn't an interface");
        }
        Method found = null;
        for (Method method : type.getMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            if (found != null) {
                throw new DataException(type.getName() + " has more than one abstract method");
            }
            found = method;
        }
        if (found == null) {
            throw new DataException(type.getName() + " has no abstract method");
        }
        return found;
    }

    private static byte[] generate(Class<?> type, String methodName, MethodType methodType) throws IOException {
        Pool pool = new Pool();
        int thisClass = pool.type(NAME);
        int superClass = pool.type("java/lang/Object");
        int iface = pool.type(type.getName().replace('.', '/'));
        int fieldName = pool.utf("HANDLE");
        int fieldType = pool.utf("L" + HANDLE + ";");
        int field = pool.ref(9, NAME, "HANDLE", "L" + HANDLE + ";");
        int objectInit = pool.ref(10, "java/lang/Object", "<init>", "()V");
        int lookup = pool.ref(10, HANDLES, "lookup", "()L" + LOOKUP_TYPE + ";");
        int classData = pool.ref(10, HANDLES, "classData", "(L" + LOOKUP_TYPE + ";Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;");
        int defaultName = pool.string("_");
        int handleClass = pool.type(HANDLE);
        String descriptor = methodType.toMethodDescriptorString();
        int invoke = pool.ref(10, HANDLE, "invokeExact", descriptor);
        int init = pool.utf("<init>");
        int clinit = pool.utf("<clinit>");
        int voidDescriptor = pool.utf("()V");
        int name = pool.utf(methodName);
        int methodDescriptor = pool.utf(descriptor);
        int code = pool.utf("Code");

        ByteArrayOutputStream initCode = new ByteArrayOutputStream();
        initCode.write(0x2A);
        initCode.write(0xB7);
        writeShort(initCode, objectInit);
        initCode.write(0xB1);

        ByteArrayOutputStream staticCode = new ByteArrayOutputStream();
        staticCode.write(0xB8);
        writeShort(staticCode, lookup);
        staticCode.write(0x13);
        writeShort(staticCode, defaultName);
        staticCode.write(0x13);
        writeShort(staticCode, handleClass);
        staticCode.write(0xB8);
        writeShort(staticCode, classData);
        staticCode.write(0xC0);
        writeShort(staticCode, handleClass);
        staticCode.write(0xB3);
        writeShort(staticCode, field);
        staticCode.write(0xB1);

        ByteArrayOutputStream bodyCode = new ByteArrayOutputStream();
        bodyCode.write(0xB2);
        writeShort(bodyCode, field);
        int slot = 1;
        for (Class<?> parameter : methodType.parameterArray()) {
            bodyCode.write(loadOpcode(parameter));
            bodyCode.write(slot);
            slot += size(parameter);
        }
        bodyCode.write(0xB6);
        writeShort(bodyCode, invoke);
        bodyCode.write(returnOpcode(methodType.returnType()));
        int maxStack = Math.max(slot, 2);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(0xCAFEBABE);
        out.writeShort(0);
        out.writeShort(61);
        pool.write(out);
        out.writeShort(0x0001 | 0x0010 | 0x0020);
        out.writeShort(thisClass);
        out.writeShort(superClass);
        out.writeShort(1);
        out.writeShort(iface);
        out.writeShort(1);
        out.writeShort(0x0002 | 0x0008 | 0x0010);
        out.writeShort(fieldName);
        out.writeShort(fieldType);
        out.writeShort(0);
        out.writeShort(3);
        method(out, 0x0001, init, voidDescriptor, code, 1, 1, initCode.toByteArray());
        method(out, 0x0008, clinit, voidDescriptor, code, 3, 0, staticCode.toByteArray());
        method(out, 0x0001 | 0x0010, name, methodDescriptor, code, maxStack, slot, bodyCode.toByteArray());
        out.writeShort(0);
        out.flush();
        return bytes.toByteArray();
    }

    private static void method(DataOutputStream out, int access, int name, int descriptor, int code, int maxStack, int maxLocals, byte[] body) throws IOException {
        out.writeShort(access);
        out.writeShort(name);
        out.writeShort(descriptor);
        out.writeShort(1);
        out.writeShort(code);
        out.writeInt(12 + body.length);
        out.writeShort(maxStack);
        out.writeShort(maxLocals);
        out.writeInt(body.length);
        out.write(body);
        out.writeShort(0);
        out.writeShort(0);
    }

    private static void writeShort(ByteArrayOutputStream out, int value) {
        out.write(value >>> 8);
        out.write(value & 0xFF);
    }

    private static int size(Class<?> type) {
        return type == long.class || type == double.class ? 2 : 1;
    }

    private static int loadOpcode(Class<?> type) {
        if (type == long.class) {
            return 0x16;
        }
        if (type == float.class) {
            return 0x17;
        }
        if (type == double.class) {
            return 0x18;
        }
        if (type.isPrimitive()) {
            return 0x15;
        }
        return 0x19;
    }

    private static int returnOpcode(Class<?> type) {
        if (type == void.class) {
            return 0xB1;
        }
        if (type == long.class) {
            return 0xAD;
        }
        if (type == float.class) {
            return 0xAE;
        }
        if (type == double.class) {
            return 0xAF;
        }
        if (type.isPrimitive()) {
            return 0xAC;
        }
        return 0xB0;
    }

    private static final class Pool {

        private final Map<String, Integer> indices = new LinkedHashMap<>();
        private final List<byte[]> entries = new ArrayList<>();

        private int add(String key, byte[] entry) {
            Integer existing = indices.get(key);
            if (existing != null) {
                return existing;
            }
            entries.add(entry);
            int index = entries.size();
            indices.put(key, index);
            return index;
        }

        int utf(String value) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            try {
                out.writeByte(1);
                out.writeUTF(value);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return add("utf:" + value, bytes.toByteArray());
        }

        int type(String name) {
            int utf = utf(name);
            return add("class:" + name, new byte[]{7, (byte) (utf >>> 8), (byte) utf});
        }

        int string(String value) {
            int utf = utf(value);
            return add("string:" + value, new byte[]{8, (byte) (utf >>> 8), (byte) utf});
        }

        int nameAndType(String name, String descriptor) {
            int nameIndex = utf(name);
            int descriptorIndex = utf(descriptor);
            return add("nat:" + name + ":" + descriptor, new byte[]{12, (byte) (nameIndex >>> 8), (byte) nameIndex, (byte) (descriptorIndex >>> 8), (byte) descriptorIndex});
        }

        int ref(int tag, String owner, String name, String descriptor) {
            int ownerIndex = type(owner);
            int nat = nameAndType(name, descriptor);
            return add("ref" + tag + ":" + owner + "." + name + descriptor, new byte[]{(byte) tag, (byte) (ownerIndex >>> 8), (byte) ownerIndex, (byte) (nat >>> 8), (byte) nat});
        }

        void write(DataOutputStream out) throws IOException {
            out.writeShort(entries.size() + 1);
            for (byte[] entry : entries) {
                out.write(entry);
            }
        }
    }

    public interface ObjectGetter {
        Object get(Object owner);
    }

    public interface ObjectSetter {
        void set(Object owner, Object value);
    }

    public interface IntGetter {
        int get(Object owner);
    }

    public interface IntSetter {
        void set(Object owner, int value);
    }

    public interface LongGetter {
        long get(Object owner);
    }

    public interface LongSetter {
        void set(Object owner, long value);
    }

    public interface DoubleGetter {
        double get(Object owner);
    }

    public interface DoubleSetter {
        void set(Object owner, double value);
    }

    public interface FloatGetter {
        float get(Object owner);
    }

    public interface FloatSetter {
        void set(Object owner, float value);
    }

    public interface BooleanGetter {
        boolean get(Object owner);
    }

    public interface BooleanSetter {
        void set(Object owner, boolean value);
    }

    public interface ByteGetter {
        byte get(Object owner);
    }

    public interface ByteSetter {
        void set(Object owner, byte value);
    }

    public interface ShortGetter {
        short get(Object owner);
    }

    public interface ShortSetter {
        void set(Object owner, short value);
    }

    public interface CharGetter {
        char get(Object owner);
    }

    public interface CharSetter {
        void set(Object owner, char value);
    }
}
