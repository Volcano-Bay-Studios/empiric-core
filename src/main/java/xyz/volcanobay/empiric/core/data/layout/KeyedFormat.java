package xyz.volcanobay.empiric.core.data.layout;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.data.KeyedData;

import java.util.HashMap;
import java.util.Map;

public final class KeyedFormat {

    private KeyedFormat() {
    }

    public static Map<String, FieldIO> index(FieldIO[] fields) {
        Map<String, FieldIO> byId = new HashMap<>();
        for (FieldIO field : fields) {
            byId.put(field.id(), field);
            for (String alias : field.layout().aliases()) {
                byId.put(alias, field);
            }
        }
        return byId;
    }

    public static int begin(DataBuffer buffer, String id) {
        buffer.putString(id);
        int position = buffer.writePosition();
        buffer.putInt(0);
        return position;
    }

    public static void end(DataBuffer buffer, int position) {
        buffer.patchInt(position, buffer.writePosition() - position - 4);
    }

    public static void write(DataBuffer buffer, Object owner, FieldIO[] fields, int version, DataContext context) {
        buffer.putVersion(version);
        buffer.beginObject(fields.length);
        for (FieldIO field : fields) {
            int marker = buffer.beginEntry(field.id());
            field.write(buffer, owner, context);
            buffer.endEntry(marker);
        }
        buffer.endObject();
    }

    public static void read(DataBuffer buffer, Object owner, Map<String, FieldIO> byId, Class<?> type, int version, DataContext context) {
        int savedVersion = buffer.takeVersion();
        int count = buffer.takeObject();
        if (savedVersion < version && context.registry().hasFixers(type)) {
            KeyedData data = new KeyedData(buffer, context, savedVersion);
            for (int i = 0; i < count; i++) {
                if (buffer.structured()) {
                    DataBuffer.Entry entry = buffer.takeEntry();
                    data.put(entry.key(), buffer.takeNode());
                    buffer.finishEntry(entry);
                } else {
                    String id = buffer.takeString();
                    data.put(id, buffer.takeBytes(buffer.takeInt()));
                }
            }
            buffer.endObject();
            context.registry().fix(type, data, version);
            for (String id : data.ids()) {
                FieldIO field = byId.get(id);
                if (field != null) {
                    field.read(data.open(id), owner, context);
                }
            }
            return;
        }
        for (int i = 0; i < count; i++) {
            DataBuffer.Entry entry = buffer.takeEntry();
            FieldIO field = byId.get(entry.key());
            if (field == null) {
                buffer.skipEntry(entry);
                continue;
            }
            field.read(buffer, owner, context);
            buffer.finishEntry(entry);
        }
        buffer.endObject();
    }
}
