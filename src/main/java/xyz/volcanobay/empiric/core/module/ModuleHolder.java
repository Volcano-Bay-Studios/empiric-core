package xyz.volcanobay.empiric.core.module;

import xyz.volcanobay.empiric.core.attachment.Attachment;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ModuleHolder {

    private static final Module<?>[] EMPTY = new Module<?>[0];

    private final Attachment attachment;
    private Module<?>[] modules = EMPTY;
    private int count;
    private Map<String, byte[]> dormant;

    public ModuleHolder(Attachment attachment) {
        this.attachment = attachment;
    }

    public Attachment attachment() {
        return attachment;
    }

    @SuppressWarnings("unchecked")
    public <M extends Module<?>> M get(ModuleType<M, ?> type) {
        int index = type.index();
        return index >= 0 && index < modules.length ? (M) modules[index] : null;
    }

    public boolean has(ModuleType<?, ?> type) {
        return get(type) != null;
    }

    void set(int index, Module<?> module) {
        if (index >= modules.length) {
            modules = Arrays.copyOf(modules, Math.max(index + 1, modules.length * 2));
        }
        if (modules[index] == null && module != null) {
            count++;
        } else if (modules[index] != null && module == null) {
            count--;
        }
        modules[index] = module;
    }

    public void forEach(Consumer<Module<?>> action) {
        for (Module<?> module : modules.clone()) {
            if (module != null) {
                action.accept(module);
            }
        }
    }

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0 && (dormant == null || dormant.isEmpty());
    }

    public boolean hasModules() {
        return count > 0;
    }

    public Map<String, byte[]> dormant() {
        return dormant == null ? Collections.emptyMap() : Collections.unmodifiableMap(dormant);
    }

    public void clearDormant() {
        dormant = null;
    }

    void putDormant(String id, byte[] bytes) {
        if (dormant == null) {
            dormant = new LinkedHashMap<>();
        }
        dormant.put(id, bytes);
    }

    byte[] takeDormant(String id) {
        if (dormant == null) {
            return null;
        }
        byte[] bytes = dormant.remove(id);
        if (dormant.isEmpty()) {
            dormant = null;
        }
        return bytes;
    }
}
