package xyz.volcanobay.empiric.core.data.spec;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.Spec;

import java.util.function.Function;

public record SimpleSpec<T>(Writer<T> writer, Reader<T> reader, Function<T, String> keyWriter, Function<String, T> keyReader) implements Spec<T> {

    public static <T> SimpleSpec<T> of(Writer<T> writer, Reader<T> reader) {
        return new SimpleSpec<>(writer, reader, null, null);
    }

    public static <T> SimpleSpec<T> keyed(Writer<T> writer, Reader<T> reader, Function<T, String> keyWriter, Function<String, T> keyReader) {
        return new SimpleSpec<>(writer, reader, keyWriter, keyReader);
    }

    @Override
    public void write(DataBuffer buffer, T value, DataContext context) {
        writer.write(buffer, value);
    }

    @Override
    public T read(DataBuffer buffer, DataContext context) {
        return reader.read(buffer);
    }

    @Override
    public boolean keyed() {
        return keyWriter != null;
    }

    @Override
    public String toKey(T value) {
        return keyWriter == null ? Spec.super.toKey(value) : keyWriter.apply(value);
    }

    @Override
    public T fromKey(String key) {
        return keyReader == null ? Spec.super.fromKey(key) : keyReader.apply(key);
    }

    public interface Writer<T> {
        void write(DataBuffer buffer, T value);
    }

    public interface Reader<T> {
        T read(DataBuffer buffer);
    }
}
