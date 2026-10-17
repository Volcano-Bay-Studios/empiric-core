package xyz.volcanobay.empiric.core.data;

import java.util.Arrays;

public class ByteArrayDataBuffer implements DataBuffer {

    private byte[] data;
    private int writeIndex;
    private int readIndex;

    public ByteArrayDataBuffer() {
        this(64);
    }

    public ByteArrayDataBuffer(int capacity) {
        this.data = new byte[Math.max(capacity, 8)];
    }

    public ByteArrayDataBuffer(byte[] data) {
        this.data = data;
        this.writeIndex = data.length;
    }

    private void ensure(int extra) {
        int needed = writeIndex + extra;
        if (needed > data.length) {
            data = Arrays.copyOf(data, Math.max(needed, data.length * 2));
        }
    }

    private void check(int length) {
        if (readable() < length) {
            throw new DataException("Tried to read " + length + " bytes with only " + readable() + " readable");
        }
    }

    @Override
    public void putByte(int value) {
        ensure(1);
        data[writeIndex++] = (byte) value;
    }

    @Override
    public byte takeByte() {
        check(1);
        return data[readIndex++];
    }

    @Override
    public void putShort(int value) {
        ensure(2);
        data[writeIndex++] = (byte) (value >>> 8);
        data[writeIndex++] = (byte) value;
    }

    @Override
    public short takeShort() {
        check(2);
        return (short) (((data[readIndex++] & 0xFF) << 8) | (data[readIndex++] & 0xFF));
    }

    @Override
    public void putInt(int value) {
        ensure(4);
        writeInt(writeIndex, value);
        writeIndex += 4;
    }

    private void writeInt(int index, int value) {
        data[index] = (byte) (value >>> 24);
        data[index + 1] = (byte) (value >>> 16);
        data[index + 2] = (byte) (value >>> 8);
        data[index + 3] = (byte) value;
    }

    @Override
    public int takeInt() {
        check(4);
        int value = ((data[readIndex] & 0xFF) << 24)
                | ((data[readIndex + 1] & 0xFF) << 16)
                | ((data[readIndex + 2] & 0xFF) << 8)
                | (data[readIndex + 3] & 0xFF);
        readIndex += 4;
        return value;
    }

    @Override
    public void putLong(long value) {
        putInt((int) (value >>> 32));
        putInt((int) value);
    }

    @Override
    public long takeLong() {
        return ((long) takeInt() << 32) | (takeInt() & 0xFFFFFFFFL);
    }

    @Override
    public void putFloat(float value) {
        putInt(Float.floatToRawIntBits(value));
    }

    @Override
    public float takeFloat() {
        return Float.intBitsToFloat(takeInt());
    }

    @Override
    public void putDouble(double value) {
        putLong(Double.doubleToRawLongBits(value));
    }

    @Override
    public double takeDouble() {
        return Double.longBitsToDouble(takeLong());
    }

    @Override
    public void putBytes(byte[] bytes, int offset, int length) {
        ensure(length);
        System.arraycopy(bytes, offset, data, writeIndex, length);
        writeIndex += length;
    }

    @Override
    public void takeBytes(byte[] target, int offset, int length) {
        check(length);
        System.arraycopy(data, readIndex, target, offset, length);
        readIndex += length;
    }

    @Override
    public int writePosition() {
        return writeIndex;
    }

    @Override
    public void patchInt(int position, int value) {
        if (position < 0 || position + 4 > writeIndex) {
            throw new DataException("Can't patch at " + position);
        }
        writeInt(position, value);
    }

    @Override
    public int readable() {
        return writeIndex - readIndex;
    }

    @Override
    public void skip(int length) {
        check(length);
        readIndex += length;
    }

    @Override
    public DataBuffer wrap(byte[] bytes) {
        return new ByteArrayDataBuffer(bytes);
    }

    public byte[] toByteArray() {
        return Arrays.copyOfRange(data, readIndex, writeIndex);
    }
}
