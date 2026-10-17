package xyz.volcanobay.empiric.core.data.spec;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.DataException;
import xyz.volcanobay.empiric.core.data.DataPrinter;
import xyz.volcanobay.empiric.core.data.Resolution;
import xyz.volcanobay.empiric.core.data.SpecRegistry;
import xyz.volcanobay.empiric.core.data.layout.ClassLayout;
import xyz.volcanobay.empiric.core.data.layout.FieldIO;
import xyz.volcanobay.empiric.core.data.layout.KeyedFormat;

import java.util.Map;

public final class StaticSpec {

    private final ClassLayout layout;
    private final FieldIO[] fields;
    private final Map<String, FieldIO> byId;

    public StaticSpec(ClassLayout layout, SpecRegistry registry) {
        this.layout = layout;
        this.fields = layout.statics().stream()
                .filter(field -> field.save())
                .map(field -> FieldIO.bind(field, Resolution.DEFAULT, registry))
                .toArray(FieldIO[]::new);
        this.byId = KeyedFormat.index(fields);
    }

    public boolean isEmpty() {
        return fields.length == 0;
    }

    public void write(DataBuffer buffer, DataContext context) {
        requireKeyed(context);
        KeyedFormat.write(buffer, null, fields, layout.version(), context);
    }

    public void read(DataBuffer buffer, DataContext context) {
        requireKeyed(context);
        KeyedFormat.read(buffer, null, byId, layout.type(), layout.version(), context);
    }

    private void requireKeyed(DataContext context) {
        if (!context.keyed()) {
            throw new DataException("Static data of " + layout.type().getName() + " can only be saved, never synced");
        }
    }

    public String print() {
        DataPrinter printer = new DataPrinter();
        printer.open(layout.type().getSimpleName() + " (static) {");
        for (FieldIO field : fields) {
            field.print(null, printer);
        }
        printer.close("}");
        return printer.toString();
    }
}
