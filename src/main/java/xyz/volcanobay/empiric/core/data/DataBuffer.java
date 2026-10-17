package xyz.volcanobay.empiric.core.data;

import xyz.volcanobay.empiric.core.EmpiricCore;
import xyz.volcanobay.empiric.core.data.tree.DataNode;

import java.nio.charset.StandardCharsets;

public interface DataBuffer {

    void putByte(int value);

    byte takeByte();

    void putShort(int value);

    short takeShort();

    void putInt(int value);

    int takeInt();

    void putLong(long value);

    long takeLong();

    void putFloat(float value);

    float takeFloat();

    void putDouble(double value);

    double takeDouble();

    void putBytes(byte[] bytes, int offset, int length);

    void takeBytes(byte[] target, int offset, int length);

    int writePosition();

    void patchInt(int position, int value);

    int readable();

    void skip(int length);

    DataBuffer wrap(byte[] bytes);

    default void putBoolean(boolean value) {
        putByte(value ? 1 : 0);
    }

    default boolean takeBoolean() {
        return takeByte() != 0;
    }

    default void putChar(char value) {
        putShort(value);
    }

    default char takeChar() {
        return (char) takeShort();
    }

    default void putVarInt(int value) {
        while ((value & ~0x7F) != 0) {
            putByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        putByte(value);
    }

    default int takeVarInt() {
        int value = 0;
        int shift = 0;
        byte b;
        do {
            if (shift >= 35) {
                throw new DataException("VarInt is too long");
            }
            b = takeByte();
            value |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return value;
    }

    default void putVarLong(long value) {
        while ((value & ~0x7FL) != 0) {
            putByte((int) (value & 0x7F) | 0x80);
            value >>>= 7;
        }
        putByte((int) value);
    }

    default long takeVarLong() {
        long value = 0;
        int shift = 0;
        byte b;
        do {
            if (shift >= 70) {
                throw new DataException("VarLong is too long");
            }
            b = takeByte();
            value |= (long) (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return value;
    }

    default void putSignedVarInt(int value) {
        putVarInt((value << 1) ^ (value >> 31));
    }

    default int takeSignedVarInt() {
        int raw = takeVarInt();
        return (raw >>> 1) ^ -(raw & 1);
    }

    default void putSignedVarLong(long value) {
        putVarLong((value << 1) ^ (value >> 63));
    }

    default long takeSignedVarLong() {
        long raw = takeVarLong();
        return (raw >>> 1) ^ -(raw & 1);
    }

    default void putBytes(byte[] bytes) {
        putVarInt(bytes.length);
        putBytes(bytes, 0, bytes.length);
    }

    default byte[] takeBytes() {
        return takeBytes(takeVarInt());
    }

    default byte[] takeBytes(int length) {
        if (length < 0 || length > readable()) {
            throw new DataException("Byte array of length " + length + " doesn't fit in " + readable() + " readable bytes");
        }
        byte[] bytes = new byte[length];
        takeBytes(bytes, 0, length);
        return bytes;
    }

    default void putString(String value) {
        putBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    default String takeString() {
        return new String(takeBytes(), StandardCharsets.UTF_8);
    }

    default boolean structured() {
        return false;
    }

    default void beginObject(int count) {
        putVarInt(count);
    }

    default int takeObject() {
        int count = takeVarInt();
        if (count < 0) {
            throw new DataException("Negative entry count " + count);
        }
        return count;
    }

    default void endObject() {
    }

    default void putVersion(int version) {
        putVarInt(version);
    }

    default int takeVersion() {
        return takeVarInt();
    }

    default int beginEntry(String key) {
        putString(key);
        int position = writePosition();
        putInt(0);
        return position;
    }

    default void endEntry(int marker) {
        patchInt(marker, writePosition() - marker - 4);
    }

    default Entry takeEntry() {
        String key = takeString();
        int length = takeInt();
        return new Entry(key, length, readable());
    }

    default void skipEntry(Entry entry) {
        skip(entry.length());
    }

    default void finishEntry(Entry entry) {
        int used = entry.start() - readable();
        if (used > entry.length()) {
            throw new DataException("Entry \"" + entry.key() + "\" read " + used + " bytes but only " + entry.length() + " were saved");
        }
        if (used < entry.length()) {
            skip(entry.length() - used);
        }
    }

    default void beginArray(int size) {
        putVarInt(size);
    }

    default int takeArray() {
        int size = takeVarInt();
        if (size < 0) {
            throw new DataException("Negative element count " + size);
        }
        return size;
    }

    default void element() {
    }

    default void takeElement() {
    }

    default void endArray() {
    }

    default void putPresence(boolean present) {
        putBoolean(present);
    }

    default boolean takePresence() {
        return takeBoolean();
    }

    default void putNode(DataNode node) {
        throw new DataException("Data nodes can only be written to structured buffers");
    }

    default DataNode takeNode() {
        throw new DataException("Data nodes can only be read from structured buffers");
    }

    record Entry(String key, int length, int start) {
    }

    default <T> void put(T value, Spec<T> spec, DataContext context) {
        spec.write(this, value, context);
    }

    default <T> T take(Spec<T> spec, DataContext context) {
        return spec.read(this, context);
    }

    default void put(Object value, Encoding encoding) {
        EmpiricCore.specs().write(this, value, encoding);
    }

    default <T> T take(Class<T> type, Encoding encoding) {
        return EmpiricCore.specs().read(this, type, encoding);
    }

    default <T> T takeInto(T existing, Encoding encoding) {
        return EmpiricCore.specs().readInto(this, existing, encoding);
    }
}
