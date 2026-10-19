package xyz.volcanobay.empiric.core.ui;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * One line of a context menu: an action, a submenu built when it opens, or a separator (no action and no submenu).
 *
 * @param hint    shown when the pointer rests on the line, or null
 * @param submenu the entries of a submenu opened from this line, or null
 */
public record ContextEntry(String label, Runnable action, BooleanSupplier enabled, String hint, Supplier<List<ContextEntry>> submenu) {

    public static final ContextEntry SEPARATOR = new ContextEntry("", null, () -> false);

    public ContextEntry(String label, Runnable action, BooleanSupplier enabled) {
        this(label, action, enabled, null, null);
    }

    public static ContextEntry of(String label, Runnable action) {
        return new ContextEntry(label, action, () -> true);
    }

    public static ContextEntry of(String label, String hint, Runnable action) {
        return new ContextEntry(label, action, () -> true, hint, null);
    }

    public static ContextEntry submenu(String label, String hint, Supplier<List<ContextEntry>> entries) {
        return new ContextEntry(label, null, () -> true, hint, entries);
    }

    public ContextEntry enabledWhen(BooleanSupplier enabled) {
        return new ContextEntry(label, action, enabled, hint, submenu);
    }

    public boolean separator() {
        return action == null && submenu == null;
    }

    public boolean isSubmenu() {
        return submenu != null;
    }
}
