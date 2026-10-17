package xyz.volcanobay.empiric.core.data.tree;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class TreeDataBuffer implements DataBuffer {

    public static final String VERSION_KEY = "_version";

    private final boolean reading;
    private final Deque<Frame> frames = new ArrayDeque<>();
    private final Slot root;
    private int pendingVersion;

    private TreeDataBuffer(DataNode node) {
        this.reading = node != null;
        this.root = reading ? Slot.reading(node) : Slot.writing();
        frames.push(new SlotFrame(root));
    }

    public static TreeDataBuffer writer() {
        return new TreeDataBuffer(null);
    }

    public static TreeDataBuffer reader(DataNode node) {
        return new TreeDataBuffer(node);
    }

    public DataNode result() {
        if (reading) {
            throw new DataException("A reading buffer has no result");
        }
        return root.materialize();
    }

    @Override
    public boolean structured() {
        return true;
    }

    private Slot slot() {
        Frame frame = frames.peek();
        if (frame instanceof SlotFrame slotFrame) {
            return slotFrame.slot;
        }
        if (frame instanceof ArrayFrame arrayFrame) {
            return arrayFrame.currentOrDirect();
        }
        throw new DataException("Values inside an object need a key");
    }

    private void putScalar(DataNode node) {
        if (reading) {
            throw new DataException("This buffer is for reading");
        }
        slot().scalar(node);
    }

    private DataNode takeScalar() {
        if (!reading) {
            throw new DataException("This buffer is for writing");
        }
        return slot().nextScalar();
    }

    @Override
    public void putByte(int value) {
        putScalar(DataNode.of((long) (byte) value));
    }

    @Override
    public byte takeByte() {
        return takeScalar().asNumber().byteValue();
    }

    @Override
    public void putShort(int value) {
        putScalar(DataNode.of((long) (short) value));
    }

    @Override
    public short takeShort() {
        return takeScalar().asNumber().shortValue();
    }

    @Override
    public void putInt(int value) {
        putScalar(DataNode.of((long) value));
    }

    @Override
    public int takeInt() {
        return takeScalar().asNumber().intValue();
    }

    @Override
    public void putLong(long value) {
        putScalar(DataNode.of(value));
    }

    @Override
    public long takeLong() {
        return takeScalar().asNumber().longValue();
    }

    @Override
    public void putFloat(float value) {
        putScalar(DataNode.of((double) value));
    }

    @Override
    public float takeFloat() {
        return takeScalar().asNumber().floatValue();
    }

    @Override
    public void putDouble(double value) {
        putScalar(DataNode.of(value));
    }

    @Override
    public double takeDouble() {
        return takeScalar().asNumber().doubleValue();
    }

    @Override
    public void putVarInt(int value) {
        putInt(value);
    }

    @Override
    public int takeVarInt() {
        return takeInt();
    }

    @Override
    public void putVarLong(long value) {
        putLong(value);
    }

    @Override
    public long takeVarLong() {
        return takeLong();
    }

    @Override
    public void putSignedVarInt(int value) {
        putInt(value);
    }

    @Override
    public int takeSignedVarInt() {
        return takeInt();
    }

    @Override
    public void putSignedVarLong(long value) {
        putLong(value);
    }

    @Override
    public long takeSignedVarLong() {
        return takeLong();
    }

    @Override
    public void putBoolean(boolean value) {
        putScalar(DataNode.of(value));
    }

    @Override
    public boolean takeBoolean() {
        return takeScalar().asBoolean();
    }

    @Override
    public void putChar(char value) {
        putScalar(DataNode.of(String.valueOf(value)));
    }

    @Override
    public char takeChar() {
        String value = takeScalar().asString();
        return value.isEmpty() ? 0 : value.charAt(0);
    }

    @Override
    public void putString(String value) {
        putScalar(DataNode.of(value));
    }

    @Override
    public String takeString() {
        return takeScalar().asString();
    }

    @Override
    public void putBytes(byte[] bytes, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            putByte(bytes[i]);
        }
    }

    @Override
    public void takeBytes(byte[] target, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            target[i] = takeByte();
        }
    }

    @Override
    public void putBytes(byte[] bytes) {
        beginArray(bytes.length);
        putBytes(bytes, 0, bytes.length);
        endArray();
    }

    @Override
    public byte[] takeBytes() {
        byte[] bytes = new byte[takeArray()];
        takeBytes(bytes, 0, bytes.length);
        endArray();
        return bytes;
    }

    @Override
    public int writePosition() {
        throw new DataException("Structured buffers have no byte positions");
    }

    @Override
    public void patchInt(int position, int value) {
        throw new DataException("Structured buffers have no byte positions");
    }

    @Override
    public int readable() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void skip(int length) {
        throw new DataException("Structured buffers can't skip bytes");
    }

    @Override
    public DataBuffer wrap(byte[] bytes) {
        throw new DataException("Structured buffers can't wrap bytes");
    }

    @Override
    public void putVersion(int version) {
        pendingVersion = version;
    }

    @Override
    public int takeVersion() {
        DataNode node = slot().peek();
        if (node != null && node.isObject()) {
            DataNode version = node.get(VERSION_KEY);
            if (version != null) {
                return version.asNumber().intValue();
            }
        }
        return 0;
    }

    @Override
    public void beginObject(int count) {
        DataNode object = DataNode.object();
        if (pendingVersion != 0) {
            object.put(VERSION_KEY, DataNode.of((long) pendingVersion));
            pendingVersion = 0;
        }
        slot().structure(object);
        frames.push(new ObjectFrame(object));
    }

    @Override
    public int takeObject() {
        DataNode node = slot().take();
        if (!node.isObject()) {
            throw new DataException("Expected an object but found " + node);
        }
        ObjectFrame frame = new ObjectFrame(node);
        frames.push(frame);
        return frame.size;
    }

    @Override
    public void endObject() {
        if (!(frames.pop() instanceof ObjectFrame)) {
            throw new DataException("Ended an object that wasn't open");
        }
    }

    @Override
    public int beginEntry(String key) {
        if (!(frames.peek() instanceof ObjectFrame object)) {
            throw new DataException("Entries need an open object");
        }
        Slot slot = Slot.writing();
        object.pendingKey = key;
        frames.push(new SlotFrame(slot));
        return frames.size();
    }

    @Override
    public void endEntry(int marker) {
        if (!(frames.pop() instanceof SlotFrame slotFrame) || !(frames.peek() instanceof ObjectFrame object)) {
            throw new DataException("Ended an entry that wasn't open");
        }
        object.node.put(object.pendingKey, slotFrame.slot.materialize());
        object.pendingKey = null;
    }

    @Override
    public Entry takeEntry() {
        if (!(frames.peek() instanceof ObjectFrame object)) {
            throw new DataException("Entries need an open object");
        }
        Map.Entry<String, DataNode> entry = object.next();
        frames.push(new SlotFrame(Slot.reading(entry.getValue())));
        return new Entry(entry.getKey(), -1, -1);
    }

    @Override
    public void skipEntry(Entry entry) {
        finishEntry(entry);
    }

    @Override
    public void finishEntry(Entry entry) {
        if (!(frames.pop() instanceof SlotFrame)) {
            throw new DataException("Finished an entry that wasn't open");
        }
    }

    @Override
    public void beginArray(int size) {
        DataNode array = DataNode.array();
        slot().structure(array);
        frames.push(new ArrayFrame(array, false));
    }

    @Override
    public int takeArray() {
        DataNode node = slot().take();
        DataNode array = node.isArray() ? node : DataNode.array(List.of(node));
        frames.push(new ArrayFrame(array, true));
        return array.elements().size();
    }

    @Override
    public void element() {
        if (!(frames.peek() instanceof ArrayFrame array)) {
            throw new DataException("Elements need an open array");
        }
        array.nextElement();
    }

    @Override
    public void takeElement() {
        element();
    }

    @Override
    public void endArray() {
        if (!(frames.pop() instanceof ArrayFrame array)) {
            throw new DataException("Ended an array that wasn't open");
        }
        array.close();
    }

    @Override
    public void putPresence(boolean present) {
        if (!present) {
            slot().structure(DataNode.NULL);
        }
    }

    @Override
    public boolean takePresence() {
        DataNode node = slot().peek();
        if (node == null || node.isNull()) {
            slot().take();
            return false;
        }
        return true;
    }

    @Override
    public void putNode(DataNode node) {
        slot().structure(node);
    }

    @Override
    public DataNode takeNode() {
        return slot().take();
    }

    private interface Frame {
    }

    private record SlotFrame(Slot slot) implements Frame {
    }

    private static final class ObjectFrame implements Frame {

        private final DataNode node;
        private final Iterator<Map.Entry<String, DataNode>> iterator;
        private final int size;
        private String pendingKey;

        private ObjectFrame(DataNode node) {
            this.node = node;
            List<Map.Entry<String, DataNode>> entries = new ArrayList<>();
            for (Map.Entry<String, DataNode> entry : node.entries().entrySet()) {
                if (!entry.getKey().equals(VERSION_KEY)) {
                    entries.add(entry);
                }
            }
            this.iterator = entries.iterator();
            this.size = entries.size();
        }

        private Map.Entry<String, DataNode> next() {
            if (!iterator.hasNext()) {
                throw new DataException("Read past the end of an object");
            }
            return iterator.next();
        }
    }

    private static final class ArrayFrame implements Frame {

        private final DataNode array;
        private final boolean reading;
        private final Slot direct;
        private Slot current;
        private int index;

        private ArrayFrame(DataNode array, boolean reading) {
            this.array = array;
            this.reading = reading;
            this.direct = reading ? Slot.reading(array) : Slot.direct(array);
        }

        private Slot currentOrDirect() {
            return current != null ? current : direct;
        }

        private void nextElement() {
            if (reading) {
                if (index >= array.elements().size()) {
                    throw new DataException("Read past the end of an array");
                }
                current = Slot.reading(array.elements().get(index++));
                return;
            }
            close();
            current = Slot.writing();
        }

        private void close() {
            if (!reading && current != null) {
                array.add(current.materialize());
                current = null;
            }
        }
    }

    private static final class Slot {

        private final DataNode source;
        private final DataNode directArray;
        private final List<DataNode> scalars = new ArrayList<>();
        private DataNode structure;
        private int cursor;
        private boolean taken;

        private Slot(DataNode source, DataNode directArray) {
            this.source = source;
            this.directArray = directArray;
        }

        private static Slot writing() {
            return new Slot(null, null);
        }

        private static Slot reading(DataNode node) {
            return new Slot(node, null);
        }

        private static Slot direct(DataNode array) {
            return new Slot(null, array);
        }

        private void scalar(DataNode node) {
            if (directArray != null) {
                directArray.add(node);
                return;
            }
            if (structure != null) {
                throw new DataException("A value can't mix " + structure + " with " + node);
            }
            scalars.add(node);
        }

        private void structure(DataNode node) {
            if (directArray != null) {
                directArray.add(node);
                return;
            }
            if (structure != null || !scalars.isEmpty()) {
                throw new DataException("A value was written twice");
            }
            structure = node;
        }

        private DataNode materialize() {
            if (structure != null) {
                return structure;
            }
            if (scalars.isEmpty()) {
                return DataNode.NULL;
            }
            if (scalars.size() == 1) {
                return scalars.getFirst();
            }
            return DataNode.array(scalars);
        }

        private DataNode peek() {
            if (source.isArray() && cursor > 0) {
                return cursor < source.elements().size() ? source.elements().get(cursor) : null;
            }
            return taken ? null : source;
        }

        private DataNode take() {
            if (taken) {
                throw new DataException("A value was read twice");
            }
            taken = true;
            return source;
        }

        private DataNode nextScalar() {
            if (source.isArray()) {
                if (cursor >= source.elements().size()) {
                    throw new DataException("Read past the end of " + source);
                }
                return source.elements().get(cursor++);
            }
            return take();
        }
    }
}
