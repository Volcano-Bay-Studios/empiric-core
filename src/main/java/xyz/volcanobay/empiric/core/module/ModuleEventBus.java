package xyz.volcanobay.empiric.core.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Listeners by event, copied on write so firing never locks and costs nothing when no one listens.
 */
public final class ModuleEventBus {

    private static final Entry[] NONE = new Entry[0];

    private volatile Entry[][] byEvent = new Entry[0][];

    synchronized void add(String owner, ModuleEvent<?> event, ModuleEventListener<?, ?> listener) {
        Entry[][] current = byEvent;
        int index = event.index();
        Entry[][] next = index < current.length ? current.clone() : Arrays.copyOf(current, index + 1);
        Entry[] entries = next[index] == null ? NONE : next[index];
        Entry[] grown = Arrays.copyOf(entries, entries.length + 1);
        grown[entries.length] = new Entry(owner, event, listener);
        next[index] = grown;
        byEvent = next;
    }

    synchronized boolean remove(String owner) {
        Entry[][] next = byEvent.clone();
        boolean removed = false;
        for (int i = 0; i < next.length; i++) {
            if (next[i] == null) {
                continue;
            }
            Entry[] kept = Arrays.stream(next[i]).filter(entry -> !owner.equals(entry.owner())).toArray(Entry[]::new);
            if (kept.length != next[i].length) {
                removed = true;
                next[i] = kept.length == 0 ? null : kept;
            }
        }
        if (removed) {
            byEvent = next;
        }
        return removed;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    <E> void fire(Module<?> module, ModuleEvent<E> event, E payload, BiConsumer<String, Throwable> errors) {
        Entry[][] current = byEvent;
        int index = event.index();
        if (index >= current.length) {
            return;
        }
        Entry[] entries = current[index];
        if (entries == null) {
            return;
        }
        for (Entry entry : entries) {
            try {
                ((ModuleEventListener) entry.listener()).on(module, payload);
            } catch (RuntimeException e) {
                errors.accept(entry.owner() + " failed handling " + event + " on " + module.type, e);
            }
        }
    }

    public List<Entry> entries() {
        List<Entry> all = new ArrayList<>();
        for (Entry[] entries : byEvent) {
            if (entries != null) {
                all.addAll(Arrays.asList(entries));
            }
        }
        return all;
    }

    public record Entry(String owner, ModuleEvent<?> event, ModuleEventListener<?, ?> listener) {
    }
}
