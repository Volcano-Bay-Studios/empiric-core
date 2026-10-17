package xyz.volcanobay.empiric.core.data.spec;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.Spec;
import xyz.volcanobay.empiric.core.data.SpecRegistry;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.BitSet;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;

public final class CommonSpecs {

    public static final Spec<String> STRING = SimpleSpec.keyed(DataBuffer::putString, DataBuffer::takeString, value -> value, key -> key);
    public static final Spec<UUID> UUID_SPEC = SimpleSpec.keyed((buffer, value) -> {
        if (buffer.structured()) {
            buffer.putString(value.toString());
            return;
        }
        buffer.putLong(value.getMostSignificantBits());
        buffer.putLong(value.getLeastSignificantBits());
    }, buffer -> buffer.structured() ? UUID.fromString(buffer.takeString()) : new UUID(buffer.takeLong(), buffer.takeLong()), UUID::toString, UUID::fromString);
    public static final Spec<BigInteger> BIG_INTEGER = SimpleSpec.of((buffer, value) -> {
        if (buffer.structured()) {
            buffer.putString(value.toString());
            return;
        }
        buffer.putBytes(value.toByteArray());
    }, buffer -> buffer.structured() ? new BigInteger(buffer.takeString()) : new BigInteger(buffer.takeBytes()));
    public static final Spec<BigDecimal> BIG_DECIMAL = SimpleSpec.of((buffer, value) -> {
        if (buffer.structured()) {
            buffer.putString(value.toString());
            return;
        }
        buffer.putSignedVarInt(value.scale());
        buffer.putBytes(value.unscaledValue().toByteArray());
    }, buffer -> {
        if (buffer.structured()) {
            return new BigDecimal(buffer.takeString());
        }
        int scale = buffer.takeSignedVarInt();
        return new BigDecimal(new BigInteger(buffer.takeBytes()), scale);
    });
    public static final Spec<Instant> INSTANT = SimpleSpec.of((buffer, value) -> {
        if (buffer.structured()) {
            buffer.putString(value.toString());
            return;
        }
        buffer.putSignedVarLong(value.getEpochSecond());
        buffer.putVarInt(value.getNano());
    }, buffer -> buffer.structured() ? Instant.parse(buffer.takeString()) : Instant.ofEpochSecond(buffer.takeSignedVarLong(), buffer.takeVarInt()));
    public static final Spec<Duration> DURATION = SimpleSpec.of((buffer, value) -> {
        if (buffer.structured()) {
            buffer.putString(value.toString());
            return;
        }
        buffer.putSignedVarLong(value.getSeconds());
        buffer.putVarInt(value.getNano());
    }, buffer -> buffer.structured() ? Duration.parse(buffer.takeString()) : Duration.ofSeconds(buffer.takeSignedVarLong(), buffer.takeVarInt()));
    public static final Spec<OptionalInt> OPTIONAL_INT = SimpleSpec.of((buffer, value) -> {
        buffer.putPresence(value.isPresent());
        if (value.isPresent()) {
            buffer.putSignedVarInt(value.getAsInt());
        }
    }, buffer -> buffer.takePresence() ? OptionalInt.of(buffer.takeSignedVarInt()) : OptionalInt.empty());
    public static final Spec<OptionalLong> OPTIONAL_LONG = SimpleSpec.of((buffer, value) -> {
        buffer.putPresence(value.isPresent());
        if (value.isPresent()) {
            buffer.putSignedVarLong(value.getAsLong());
        }
    }, buffer -> buffer.takePresence() ? OptionalLong.of(buffer.takeSignedVarLong()) : OptionalLong.empty());
    public static final Spec<OptionalDouble> OPTIONAL_DOUBLE = SimpleSpec.of((buffer, value) -> {
        buffer.putPresence(value.isPresent());
        if (value.isPresent()) {
            buffer.putDouble(value.getAsDouble());
        }
    }, buffer -> buffer.takePresence() ? OptionalDouble.of(buffer.takeDouble()) : OptionalDouble.empty());
    public static final Spec<BitSet> BIT_SET = new BitSetSpec();

    private CommonSpecs() {
    }

    public static void register(SpecRegistry registry) {
        registry.register(String.class, STRING);
        registry.register(UUID.class, UUID_SPEC);
        registry.register(BigInteger.class, BIG_INTEGER);
        registry.register(BigDecimal.class, BIG_DECIMAL);
        registry.register(Instant.class, INSTANT);
        registry.register(Duration.class, DURATION);
        registry.register(OptionalInt.class, OPTIONAL_INT);
        registry.register(OptionalLong.class, OPTIONAL_LONG);
        registry.register(OptionalDouble.class, OPTIONAL_DOUBLE);
        registry.register(BitSet.class, BIT_SET);
    }

    private static final class BitSetSpec implements Spec<BitSet> {

        @Override
        public void write(DataBuffer buffer, BitSet value, DataContext context) {
            long[] words = value.toLongArray();
            buffer.beginArray(words.length);
            for (long word : words) {
                buffer.putLong(word);
            }
            buffer.endArray();
        }

        @Override
        public BitSet read(DataBuffer buffer, DataContext context) {
            long[] words = new long[buffer.takeArray()];
            for (int i = 0; i < words.length; i++) {
                words[i] = buffer.takeLong();
            }
            buffer.endArray();
            return BitSet.valueOf(words);
        }

        @Override
        public BitSet update(DataBuffer buffer, BitSet existing, DataContext context) {
            existing.clear();
            existing.or(read(buffer, context));
            return existing;
        }

        @Override
        public boolean supportsUpdate() {
            return true;
        }

        @Override
        public BitSet copy(BitSet value) {
            return (BitSet) value.clone();
        }

        @Override
        public BitSet copyInto(BitSet source, BitSet target) {
            target.clear();
            target.or(source);
            return target;
        }
    }
}
