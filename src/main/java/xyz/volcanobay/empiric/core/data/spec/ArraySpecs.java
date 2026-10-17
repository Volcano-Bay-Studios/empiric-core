package xyz.volcanobay.empiric.core.data.spec;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.DataException;
import xyz.volcanobay.empiric.core.data.DataPrinter;
import xyz.volcanobay.empiric.core.data.Mode;
import xyz.volcanobay.empiric.core.data.Resolution;
import xyz.volcanobay.empiric.core.data.Slot;
import xyz.volcanobay.empiric.core.data.Spec;
import xyz.volcanobay.empiric.core.data.SpecFactory;
import xyz.volcanobay.empiric.core.data.SpecRegistry;
import xyz.volcanobay.empiric.core.data.Types;
import xyz.volcanobay.empiric.core.data.layout.Accessors;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.IntFunction;

public final class ArraySpecs {

    public static final Spec<byte[]> BYTES = new PrimitiveArraySpec<>() {

        @Override
        int length(byte[] array) {
            return array.length;
        }

        @Override
        byte[] create(int length) {
            return new byte[length];
        }

        @Override
        void writeElements(DataBuffer buffer, byte[] array) {
            buffer.putBytes(array, 0, array.length);
        }

        @Override
        void readElements(DataBuffer buffer, byte[] array) {
            buffer.takeBytes(array, 0, array.length);
        }

        @Override
        public byte[] copy(byte[] value) {
            return value.clone();
        }

        @Override
        public boolean same(byte[] first, byte[] second) {
            return Arrays.equals(first, second);
        }
    };

    public static final Spec<boolean[]> BOOLEANS = new PrimitiveArraySpec<>() {

        @Override
        int length(boolean[] array) {
            return array.length;
        }

        @Override
        boolean[] create(int length) {
            return new boolean[length];
        }

        @Override
        void writeElements(DataBuffer buffer, boolean[] array) {
            for (boolean value : array) {
                buffer.putBoolean(value);
            }
        }

        @Override
        void readElements(DataBuffer buffer, boolean[] array) {
            for (int i = 0; i < array.length; i++) {
                array[i] = buffer.takeBoolean();
            }
        }

        @Override
        public boolean[] copy(boolean[] value) {
            return value.clone();
        }

        @Override
        public boolean same(boolean[] first, boolean[] second) {
            return Arrays.equals(first, second);
        }
    };

    public static final Spec<short[]> SHORTS = new PrimitiveArraySpec<>() {

        @Override
        int length(short[] array) {
            return array.length;
        }

        @Override
        short[] create(int length) {
            return new short[length];
        }

        @Override
        void writeElements(DataBuffer buffer, short[] array) {
            for (short value : array) {
                buffer.putShort(value);
            }
        }

        @Override
        void readElements(DataBuffer buffer, short[] array) {
            for (int i = 0; i < array.length; i++) {
                array[i] = buffer.takeShort();
            }
        }

        @Override
        public short[] copy(short[] value) {
            return value.clone();
        }

        @Override
        public boolean same(short[] first, short[] second) {
            return Arrays.equals(first, second);
        }
    };

    public static final Spec<char[]> CHARS = new PrimitiveArraySpec<>() {

        @Override
        int length(char[] array) {
            return array.length;
        }

        @Override
        char[] create(int length) {
            return new char[length];
        }

        @Override
        void writeElements(DataBuffer buffer, char[] array) {
            for (char value : array) {
                buffer.putChar(value);
            }
        }

        @Override
        void readElements(DataBuffer buffer, char[] array) {
            for (int i = 0; i < array.length; i++) {
                array[i] = buffer.takeChar();
            }
        }

        @Override
        public char[] copy(char[] value) {
            return value.clone();
        }

        @Override
        public boolean same(char[] first, char[] second) {
            return Arrays.equals(first, second);
        }
    };

    public static final Spec<int[]> INTS = new PrimitiveArraySpec<>() {

        @Override
        int length(int[] array) {
            return array.length;
        }

        @Override
        int[] create(int length) {
            return new int[length];
        }

        @Override
        void writeElements(DataBuffer buffer, int[] array) {
            for (int value : array) {
                buffer.putSignedVarInt(value);
            }
        }

        @Override
        void readElements(DataBuffer buffer, int[] array) {
            for (int i = 0; i < array.length; i++) {
                array[i] = buffer.takeSignedVarInt();
            }
        }

        @Override
        public int[] copy(int[] value) {
            return value.clone();
        }

        @Override
        public boolean same(int[] first, int[] second) {
            return Arrays.equals(first, second);
        }
    };

    public static final Spec<long[]> LONGS = new PrimitiveArraySpec<>() {

        @Override
        int length(long[] array) {
            return array.length;
        }

        @Override
        long[] create(int length) {
            return new long[length];
        }

        @Override
        void writeElements(DataBuffer buffer, long[] array) {
            for (long value : array) {
                buffer.putSignedVarLong(value);
            }
        }

        @Override
        void readElements(DataBuffer buffer, long[] array) {
            for (int i = 0; i < array.length; i++) {
                array[i] = buffer.takeSignedVarLong();
            }
        }

        @Override
        public long[] copy(long[] value) {
            return value.clone();
        }

        @Override
        public boolean same(long[] first, long[] second) {
            return Arrays.equals(first, second);
        }
    };

    public static final Spec<float[]> FLOATS = new PrimitiveArraySpec<>() {

        @Override
        int length(float[] array) {
            return array.length;
        }

        @Override
        float[] create(int length) {
            return new float[length];
        }

        @Override
        void writeElements(DataBuffer buffer, float[] array) {
            for (float value : array) {
                buffer.putFloat(value);
            }
        }

        @Override
        void readElements(DataBuffer buffer, float[] array) {
            for (int i = 0; i < array.length; i++) {
                array[i] = buffer.takeFloat();
            }
        }

        @Override
        public float[] copy(float[] value) {
            return value.clone();
        }

        @Override
        public boolean same(float[] first, float[] second) {
            return Arrays.equals(first, second);
        }
    };

    public static final Spec<double[]> DOUBLES = new PrimitiveArraySpec<>() {

        @Override
        int length(double[] array) {
            return array.length;
        }

        @Override
        double[] create(int length) {
            return new double[length];
        }

        @Override
        void writeElements(DataBuffer buffer, double[] array) {
            for (double value : array) {
                buffer.putDouble(value);
            }
        }

        @Override
        void readElements(DataBuffer buffer, double[] array) {
            for (int i = 0; i < array.length; i++) {
                array[i] = buffer.takeDouble();
            }
        }

        @Override
        public double[] copy(double[] value) {
            return value.clone();
        }

        @Override
        public boolean same(double[] first, double[] second) {
            return Arrays.equals(first, second);
        }
    };

    public static final SpecFactory OBJECT_ARRAYS = (type, raw, resolution, registry) -> {
        if (!raw.isArray() || raw.getComponentType().isPrimitive()) {
            return null;
        }
        Mode own = registry.mode(type, resolution, true, Mode.REPLACE);
        return new ObjectArraySpec<>(raw.getComponentType(), registry.slot(Types.component(type), resolution.child(own)));
    };

    private ArraySpecs() {
    }

    public static void register(SpecRegistry registry) {
        registry.register(byte[].class, BYTES);
        registry.register(boolean[].class, BOOLEANS);
        registry.register(short[].class, SHORTS);
        registry.register(char[].class, CHARS);
        registry.register(int[].class, INTS);
        registry.register(long[].class, LONGS);
        registry.register(float[].class, FLOATS);
        registry.register(double[].class, DOUBLES);
        registry.register(OBJECT_ARRAYS);
    }

    static int checkedLength(DataBuffer buffer) {
        int length = buffer.takeArray();
        if (length < 0 || length > buffer.readable()) {
            throw new DataException("Element count " + length + " doesn't fit in " + buffer.readable() + " readable bytes");
        }
        return length;
    }

    abstract static class PrimitiveArraySpec<A> implements Spec<A> {

        abstract int length(A array);

        abstract A create(int length);

        abstract void writeElements(DataBuffer buffer, A array);

        abstract void readElements(DataBuffer buffer, A array);

        @Override
        public void write(DataBuffer buffer, A value, DataContext context) {
            buffer.beginArray(length(value));
            writeElements(buffer, value);
            buffer.endArray();
        }

        @Override
        public A read(DataBuffer buffer, DataContext context) {
            A array = create(checkedLength(buffer));
            readElements(buffer, array);
            buffer.endArray();
            return array;
        }

        @Override
        public A update(DataBuffer buffer, A existing, DataContext context) {
            int length = checkedLength(buffer);
            A array = length(existing) == length ? existing : create(length);
            readElements(buffer, array);
            buffer.endArray();
            return array;
        }

        @Override
        public boolean supportsUpdate() {
            return true;
        }

        @Override
        @SuppressWarnings("SuspiciousSystemArraycopy")
        public A copyInto(A source, A target) {
            if (length(source) != length(target)) {
                return copy(source);
            }
            System.arraycopy(source, 0, target, 0, length(source));
            return target;
        }
    }

    static final class ObjectArraySpec<E> implements Spec<E[]> {

        private final IntFunction<Object> factory;
        private final Slot<E> element;

        @SuppressWarnings("unchecked")
        ObjectArraySpec(Class<?> component, Slot<?> element) {
            this.factory = Accessors.bind(IntFunction.class, MethodHandles.arrayConstructor(component.arrayType()));
            this.element = (Slot<E>) element;
        }

        @SuppressWarnings("unchecked")
        private E[] create(int length) {
            return (E[]) factory.apply(length);
        }

        @Override
        public void write(DataBuffer buffer, E[] value, DataContext context) {
            buffer.beginArray(value.length);
            for (E entry : value) {
                buffer.element();
                element.write(buffer, entry, context);
            }
            buffer.endArray();
        }

        @Override
        public E[] read(DataBuffer buffer, DataContext context) {
            E[] array = create(checkedLength(buffer));
            for (int i = 0; i < array.length; i++) {
                buffer.takeElement();
                array[i] = element.read(buffer, null, context);
            }
            buffer.endArray();
            return array;
        }

        @Override
        public E[] update(DataBuffer buffer, E[] existing, DataContext context) {
            int length = checkedLength(buffer);
            E[] array = existing.length == length ? existing : create(length);
            for (int i = 0; i < length; i++) {
                buffer.takeElement();
                array[i] = element.read(buffer, i < existing.length ? existing[i] : null, context);
            }
            buffer.endArray();
            return array;
        }

        @Override
        public boolean supportsUpdate() {
            return true;
        }

        @Override
        public E[] copy(E[] value) {
            E[] array = create(value.length);
            for (int i = 0; i < value.length; i++) {
                array[i] = value[i] == null ? null : element.spec().copy(value[i]);
            }
            return array;
        }

        @Override
        public E[] copyInto(E[] source, E[] target) {
            if (source.length != target.length) {
                return copy(source);
            }
            for (int i = 0; i < source.length; i++) {
                target[i] = source[i] == null ? null : element.spec().copy(source[i]);
            }
            return target;
        }

        @Override
        public boolean same(E[] first, E[] second) {
            if (first.length != second.length) {
                return false;
            }
            for (int i = 0; i < first.length; i++) {
                if (first[i] == null || second[i] == null ? first[i] != second[i] : !element.spec().same(first[i], second[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void print(E[] value, DataPrinter printer) {
            printer.open("[");
            for (E entry : value) {
                printer.element(entry, element.spec());
            }
            printer.close("]");
        }
    }
}
