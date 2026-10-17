package xyz.volcanobay.empiric.core.data;

import java.lang.reflect.Array;

public final class DataPrinter {

    private final StringBuilder out = new StringBuilder();
    private int indent;

    public static <T> String print(T value, Spec<T> spec) {
        DataPrinter printer = new DataPrinter();
        if (value == null) {
            printer.value(null);
        } else {
            spec.print(value, printer);
        }
        return printer.toString();
    }

    public static String print(Object value, SpecRegistry registry) {
        return print(value, registry.specOf(value));
    }

    public static <T> String printKeyed(DataBuffer buffer, Spec<T> spec, SpecRegistry registry) {
        return print(spec.read(buffer, registry.context(Encoding.KEYED)), spec);
    }

    public void value(Object value) {
        if (value != null && value.getClass().isArray()) {
            out.append('[');
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    out.append(", ");
                }
                out.append(Array.get(value, i));
            }
            out.append(']');
            return;
        }
        if (value instanceof String string) {
            out.append('"').append(string).append('"');
            return;
        }
        out.append(value);
    }

    public <T> void entry(String key, T value, Spec<T> spec) {
        newline();
        out.append(key).append(" = ");
        if (value == null) {
            out.append("null");
        } else {
            spec.print(value, this);
        }
    }

    public void entry(String key, Object value) {
        newline();
        out.append(key).append(" = ");
        value(value);
    }

    public <T> void element(T value, Spec<T> spec) {
        newline();
        if (value == null) {
            out.append("null");
        } else {
            spec.print(value, this);
        }
    }

    public void open(String bracket) {
        out.append(bracket);
        indent++;
    }

    public void close(String bracket) {
        indent--;
        newline();
        out.append(bracket);
    }

    private void newline() {
        out.append('\n');
        out.append("  ".repeat(Math.max(indent, 0)));
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
