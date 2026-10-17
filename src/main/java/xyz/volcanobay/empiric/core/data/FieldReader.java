package xyz.volcanobay.empiric.core.data;

public interface FieldReader {

    Object read(DataBuffer buffer, Object current, Slot<Object> slot, DataContext context);

    final class None implements FieldReader {

        @Override
        public Object read(DataBuffer buffer, Object current, Slot<Object> slot, DataContext context) {
            return slot.read(buffer, current, context);
        }
    }
}
