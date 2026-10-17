package xyz.volcanobay.empiric.core.data.spec;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.Spec;
import xyz.volcanobay.empiric.core.data.SpecRegistry;

public final class PrimitiveSpecs {

    public static final Spec<Boolean> BOOLEAN = SimpleSpec.of(DataBuffer::putBoolean, DataBuffer::takeBoolean);
    public static final Spec<Byte> BYTE = SimpleSpec.of((buffer, value) -> buffer.putByte(value), DataBuffer::takeByte);
    public static final Spec<Short> SHORT = SimpleSpec.of((buffer, value) -> buffer.putShort(value), DataBuffer::takeShort);
    public static final Spec<Character> CHAR = SimpleSpec.of(DataBuffer::putChar, DataBuffer::takeChar);
    public static final Spec<Integer> INT = SimpleSpec.keyed(DataBuffer::putSignedVarInt, DataBuffer::takeSignedVarInt, String::valueOf, Integer::parseInt);
    public static final Spec<Long> LONG = SimpleSpec.keyed(DataBuffer::putSignedVarLong, DataBuffer::takeSignedVarLong, String::valueOf, Long::parseLong);
    public static final Spec<Float> FLOAT = SimpleSpec.of(DataBuffer::putFloat, DataBuffer::takeFloat);
    public static final Spec<Double> DOUBLE = SimpleSpec.of(DataBuffer::putDouble, DataBuffer::takeDouble);

    private PrimitiveSpecs() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register(SpecRegistry registry) {
        registry.register(Boolean.class, BOOLEAN);
        registry.register((Class) boolean.class, BOOLEAN);
        registry.register(Byte.class, BYTE);
        registry.register((Class) byte.class, BYTE);
        registry.register(Short.class, SHORT);
        registry.register((Class) short.class, SHORT);
        registry.register(Character.class, CHAR);
        registry.register((Class) char.class, CHAR);
        registry.register(Integer.class, INT);
        registry.register((Class) int.class, INT);
        registry.register(Long.class, LONG);
        registry.register((Class) long.class, LONG);
        registry.register(Float.class, FLOAT);
        registry.register((Class) float.class, FLOAT);
        registry.register(Double.class, DOUBLE);
        registry.register((Class) double.class, DOUBLE);
    }
}
