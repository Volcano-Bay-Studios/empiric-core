package xyz.volcanobay.empiric.core.data.layout;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.DataException;
import xyz.volcanobay.empiric.core.data.DataPrinter;
import xyz.volcanobay.empiric.core.data.FieldReader;
import xyz.volcanobay.empiric.core.data.Mode;
import xyz.volcanobay.empiric.core.data.ModeResolver;
import xyz.volcanobay.empiric.core.data.Resolution;
import xyz.volcanobay.empiric.core.data.Slot;
import xyz.volcanobay.empiric.core.data.SpecRegistry;
import xyz.volcanobay.empiric.core.data.spec.NullableSpec;

import java.util.Objects;

public abstract class FieldIO {

    protected final FieldLayout layout;

    protected FieldIO(FieldLayout layout) {
        this.layout = layout;
    }

    public static FieldIO bind(FieldLayout field, Resolution inherited, SpecRegistry registry) {
        Resolution resolution = resolve(field, inherited, registry);
        boolean customReader = field.data().reader() != FieldReader.None.class;
        if (field.primitive() && !customReader && !field.optional()) {
            return primitive(field);
        }
        Slot<?> slot = registry.slot(field.genericType(), resolution);
        if (field.optional()) {
            slot = NullableSpec.wrap(slot);
        }
        if (field.isFinal() && slot.mode() != Mode.UPDATE) {
            throw new DataException(field.describe() + " is final but its type can't be updated in place");
        }
        FieldReader reader = customReader ? registry.strategy(field.data().reader()) : null;
        return new ObjectIO(field, slot, reader);
    }

    private static Resolution resolve(FieldLayout field, Resolution inherited, SpecRegistry registry) {
        Mode mode = field.data().mode();
        int depth = field.data().depth();
        if (mode == Mode.INHERIT) {
            mode = inherited.mode();
            depth = inherited.depth();
            if (mode == Mode.INHERIT && field.isFinal()) {
                mode = Mode.UPDATE;
                depth = field.data().depth();
            }
        }
        ModeResolver resolver = field.data().modeResolver() == ModeResolver.None.class ? inherited.resolver() : registry.strategy(field.data().modeResolver());
        return new Resolution(mode, depth, inherited.level(), resolver, field.data().listUpdate());
    }

    private static FieldIO primitive(FieldLayout field) {
        Class<?> type = field.type();
        if (type == int.class) {
            return new IntIO(field);
        }
        if (type == long.class) {
            return new LongIO(field);
        }
        if (type == double.class) {
            return new DoubleIO(field);
        }
        if (type == float.class) {
            return new FloatIO(field);
        }
        if (type == boolean.class) {
            return new BooleanIO(field);
        }
        if (type == byte.class) {
            return new ByteIO(field);
        }
        if (type == short.class) {
            return new ShortIO(field);
        }
        if (type == char.class) {
            return new CharIO(field);
        }
        throw new DataException("Unknown primitive " + type);
    }

    protected static <I> I getter(FieldLayout layout, Class<I> type) {
        return Accessors.bind(type, layout.getter());
    }

    protected static <I> I setter(FieldLayout layout, Class<I> type) {
        return layout.setter() == null ? null : Accessors.bind(type, layout.setter());
    }

    public FieldLayout layout() {
        return layout;
    }

    public String id() {
        return layout.id();
    }

    public abstract void write(DataBuffer buffer, Object owner, DataContext context);

    public abstract void read(DataBuffer buffer, Object owner, DataContext context);

    public abstract Object get(Object owner);

    public abstract void copy(Object from, Object to);

    public abstract boolean same(Object first, Object second);

    public abstract Object snapshot(Object owner);

    public abstract boolean matches(Object owner, Object snapshot);

    public void print(Object owner, DataPrinter printer) {
        printer.entry(id(), get(owner));
    }

    static final class ObjectIO extends FieldIO {

        private final Accessors.ObjectGetter getter;
        private final Accessors.ObjectSetter setter;
        private final Slot<Object> slot;
        private final FieldReader reader;

        @SuppressWarnings("unchecked")
        ObjectIO(FieldLayout layout, Slot<?> slot, FieldReader reader) {
            super(layout);
            this.getter = getter(layout, Accessors.ObjectGetter.class);
            this.setter = setter(layout, Accessors.ObjectSetter.class);
            this.slot = (Slot<Object>) slot;
            this.reader = reader;
        }

        @Override
        public Object get(Object owner) {
            return getter.get(owner);
        }

        private void set(Object owner, Object value) {
            if (setter == null) {
                throw new DataException(layout.describe() + " is final and its value was replaced instead of updated");
            }
            setter.set(owner, value);
        }

        @Override
        public void write(DataBuffer buffer, Object owner, DataContext context) {
            Object value = get(owner);
            if (value == null) {
                throw new DataException(layout.describe() + " is null, mark it optional to allow null");
            }
            slot.write(buffer, value, context);
        }

        @Override
        public void read(DataBuffer buffer, Object owner, DataContext context) {
            Object current = get(owner);
            Object value = reader != null ? reader.read(buffer, current, slot, context) : slot.read(buffer, current, context);
            if (value != current) {
                set(owner, value);
            }
        }

        @Override
        public void copy(Object from, Object to) {
            Object value = get(from);
            if (setter == null) {
                Object target = get(to);
                if (target != null && value != null && slot.spec().copyInto(value, target) != target) {
                    throw new DataException(layout.describe() + " is final and its type can't be copied in place");
                }
                return;
            }
            set(to, value == null ? null : slot.spec().copy(value));
        }

        @Override
        public boolean same(Object first, Object second) {
            Object a = get(first);
            Object b = get(second);
            if (a == null || b == null) {
                return a == b;
            }
            return slot.spec().same(a, b);
        }

        @Override
        public Object snapshot(Object owner) {
            Object value = get(owner);
            return value == null ? null : slot.spec().copy(value);
        }

        @Override
        public boolean matches(Object owner, Object snapshot) {
            Object value = get(owner);
            if (value == null || snapshot == null) {
                return value == snapshot;
            }
            return slot.spec().same(value, snapshot);
        }

        @Override
        public void print(Object owner, DataPrinter printer) {
            printer.entry(id(), get(owner), slot.spec());
        }
    }

    abstract static class PrimitiveIO extends FieldIO {

        PrimitiveIO(FieldLayout layout) {
            super(layout);
        }

        @Override
        public Object snapshot(Object owner) {
            return get(owner);
        }

        @Override
        public boolean matches(Object owner, Object snapshot) {
            return Objects.equals(get(owner), snapshot);
        }

        @Override
        public boolean same(Object first, Object second) {
            return Objects.equals(get(first), get(second));
        }
    }

    static final class IntIO extends PrimitiveIO {

        private final Accessors.IntGetter getter;
        private final Accessors.IntSetter setter;

        IntIO(FieldLayout layout) {
            super(layout);
            this.getter = getter(layout, Accessors.IntGetter.class);
            this.setter = setter(layout, Accessors.IntSetter.class);
        }

        int value(Object owner) {
            return getter.get(owner);
        }

        void set(Object owner, int value) {
            setter.set(owner, value);
        }

        @Override
        public void write(DataBuffer buffer, Object owner, DataContext context) {
            buffer.putSignedVarInt(value(owner));
        }

        @Override
        public void read(DataBuffer buffer, Object owner, DataContext context) {
            set(owner, buffer.takeSignedVarInt());
        }

        @Override
        public Object get(Object owner) {
            return value(owner);
        }

        @Override
        public void copy(Object from, Object to) {
            set(to, value(from));
        }
    }

    static final class LongIO extends PrimitiveIO {

        private final Accessors.LongGetter getter;
        private final Accessors.LongSetter setter;

        LongIO(FieldLayout layout) {
            super(layout);
            this.getter = getter(layout, Accessors.LongGetter.class);
            this.setter = setter(layout, Accessors.LongSetter.class);
        }

        long value(Object owner) {
            return getter.get(owner);
        }

        void set(Object owner, long value) {
            setter.set(owner, value);
        }

        @Override
        public void write(DataBuffer buffer, Object owner, DataContext context) {
            buffer.putSignedVarLong(value(owner));
        }

        @Override
        public void read(DataBuffer buffer, Object owner, DataContext context) {
            set(owner, buffer.takeSignedVarLong());
        }

        @Override
        public Object get(Object owner) {
            return value(owner);
        }

        @Override
        public void copy(Object from, Object to) {
            set(to, value(from));
        }
    }

    static final class DoubleIO extends PrimitiveIO {

        private final Accessors.DoubleGetter getter;
        private final Accessors.DoubleSetter setter;

        DoubleIO(FieldLayout layout) {
            super(layout);
            this.getter = getter(layout, Accessors.DoubleGetter.class);
            this.setter = setter(layout, Accessors.DoubleSetter.class);
        }

        double value(Object owner) {
            return getter.get(owner);
        }

        void set(Object owner, double value) {
            setter.set(owner, value);
        }

        @Override
        public void write(DataBuffer buffer, Object owner, DataContext context) {
            buffer.putDouble(value(owner));
        }

        @Override
        public void read(DataBuffer buffer, Object owner, DataContext context) {
            set(owner, buffer.takeDouble());
        }

        @Override
        public Object get(Object owner) {
            return value(owner);
        }

        @Override
        public void copy(Object from, Object to) {
            set(to, value(from));
        }
    }

    static final class FloatIO extends PrimitiveIO {

        private final Accessors.FloatGetter getter;
        private final Accessors.FloatSetter setter;

        FloatIO(FieldLayout layout) {
            super(layout);
            this.getter = getter(layout, Accessors.FloatGetter.class);
            this.setter = setter(layout, Accessors.FloatSetter.class);
        }

        float value(Object owner) {
            return getter.get(owner);
        }

        void set(Object owner, float value) {
            setter.set(owner, value);
        }

        @Override
        public void write(DataBuffer buffer, Object owner, DataContext context) {
            buffer.putFloat(value(owner));
        }

        @Override
        public void read(DataBuffer buffer, Object owner, DataContext context) {
            set(owner, buffer.takeFloat());
        }

        @Override
        public Object get(Object owner) {
            return value(owner);
        }

        @Override
        public void copy(Object from, Object to) {
            set(to, value(from));
        }
    }

    static final class BooleanIO extends PrimitiveIO {

        private final Accessors.BooleanGetter getter;
        private final Accessors.BooleanSetter setter;

        BooleanIO(FieldLayout layout) {
            super(layout);
            this.getter = getter(layout, Accessors.BooleanGetter.class);
            this.setter = setter(layout, Accessors.BooleanSetter.class);
        }

        boolean value(Object owner) {
            return getter.get(owner);
        }

        void set(Object owner, boolean value) {
            setter.set(owner, value);
        }

        @Override
        public void write(DataBuffer buffer, Object owner, DataContext context) {
            buffer.putBoolean(value(owner));
        }

        @Override
        public void read(DataBuffer buffer, Object owner, DataContext context) {
            set(owner, buffer.takeBoolean());
        }

        @Override
        public Object get(Object owner) {
            return value(owner);
        }

        @Override
        public void copy(Object from, Object to) {
            set(to, value(from));
        }
    }

    static final class ByteIO extends PrimitiveIO {

        private final Accessors.ByteGetter getter;
        private final Accessors.ByteSetter setter;

        ByteIO(FieldLayout layout) {
            super(layout);
            this.getter = getter(layout, Accessors.ByteGetter.class);
            this.setter = setter(layout, Accessors.ByteSetter.class);
        }

        byte value(Object owner) {
            return getter.get(owner);
        }

        void set(Object owner, byte value) {
            setter.set(owner, value);
        }

        @Override
        public void write(DataBuffer buffer, Object owner, DataContext context) {
            buffer.putByte(value(owner));
        }

        @Override
        public void read(DataBuffer buffer, Object owner, DataContext context) {
            set(owner, buffer.takeByte());
        }

        @Override
        public Object get(Object owner) {
            return value(owner);
        }

        @Override
        public void copy(Object from, Object to) {
            set(to, value(from));
        }
    }

    static final class ShortIO extends PrimitiveIO {

        private final Accessors.ShortGetter getter;
        private final Accessors.ShortSetter setter;

        ShortIO(FieldLayout layout) {
            super(layout);
            this.getter = getter(layout, Accessors.ShortGetter.class);
            this.setter = setter(layout, Accessors.ShortSetter.class);
        }

        short value(Object owner) {
            return getter.get(owner);
        }

        void set(Object owner, short value) {
            setter.set(owner, value);
        }

        @Override
        public void write(DataBuffer buffer, Object owner, DataContext context) {
            buffer.putShort(value(owner));
        }

        @Override
        public void read(DataBuffer buffer, Object owner, DataContext context) {
            set(owner, buffer.takeShort());
        }

        @Override
        public Object get(Object owner) {
            return value(owner);
        }

        @Override
        public void copy(Object from, Object to) {
            set(to, value(from));
        }
    }

    static final class CharIO extends PrimitiveIO {

        private final Accessors.CharGetter getter;
        private final Accessors.CharSetter setter;

        CharIO(FieldLayout layout) {
            super(layout);
            this.getter = getter(layout, Accessors.CharGetter.class);
            this.setter = setter(layout, Accessors.CharSetter.class);
        }

        char value(Object owner) {
            return getter.get(owner);
        }

        void set(Object owner, char value) {
            setter.set(owner, value);
        }

        @Override
        public void write(DataBuffer buffer, Object owner, DataContext context) {
            buffer.putChar(value(owner));
        }

        @Override
        public void read(DataBuffer buffer, Object owner, DataContext context) {
            set(owner, buffer.takeChar());
        }

        @Override
        public Object get(Object owner) {
            return value(owner);
        }

        @Override
        public void copy(Object from, Object to) {
            set(to, value(from));
        }
    }
}
