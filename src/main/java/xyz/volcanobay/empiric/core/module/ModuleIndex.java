package xyz.volcanobay.empiric.core.module;

import java.util.Arrays;
import java.util.function.Consumer;

public final class ModuleIndex {

    private final int slot;
    private Module<?>[] modules = new Module<?>[16];
    private int size;
    private int iterating;
    private boolean holes;

    ModuleIndex(int slot) {
        this.slot = slot;
    }

    void add(Module<?> module) {
        if (module.slots[slot] >= 0) {
            return;
        }
        if (size == modules.length) {
            modules = Arrays.copyOf(modules, size * 2);
        }
        module.slots[slot] = size;
        modules[size++] = module;
    }

    void remove(Module<?> module) {
        int index = module.slots[slot];
        if (index < 0) {
            return;
        }
        module.slots[slot] = -1;
        if (iterating > 0) {
            modules[index] = null;
            holes = true;
            return;
        }
        Module<?> last = modules[--size];
        modules[size] = null;
        if (last != module) {
            modules[index] = last;
            last.slots[slot] = index;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void forEach(Consumer<T> action) {
        iterating++;
        int end = size;
        try {
            for (int i = 0; i < end; i++) {
                Module<?> module = modules[i];
                if (module != null) {
                    action.accept((T) module);
                }
            }
        } finally {
            if (--iterating == 0 && holes) {
                compact();
            }
        }
    }

    public Module<?>[] snapshot() {
        Module<?>[] copy = new Module<?>[size];
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (modules[i] != null) {
                copy[count++] = modules[i];
            }
        }
        return count == copy.length ? copy : Arrays.copyOf(copy, count);
    }

    private void compact() {
        int write = 0;
        for (int read = 0; read < size; read++) {
            Module<?> module = modules[read];
            if (module != null) {
                modules[write] = module;
                module.slots[slot] = write;
                write++;
            }
        }
        Arrays.fill(modules, write, size, null);
        size = write;
        holes = false;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
