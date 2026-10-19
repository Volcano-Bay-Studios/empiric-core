package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiFont;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * One line of editable text with a cursor, selection, word jumps and the clipboard. Cursor positions always sit between
 * code points, so surrogate pairs are never split.
 */
public class TextField extends ValueWidget<String> {

    private static final int INSET = 4;

    private String text;
    private int cursor;
    private int anchor;
    private int scroll;
    private int maxLength = 256;
    private Predicate<String> filter = text -> true;
    private String placeholder = "";
    private final List<Consumer<String>> submitted = new ArrayList<>();

    public TextField() {
        this("");
    }

    public TextField(String text) {
        super(String.class, text);
        this.text = text;
        this.cursor = this.anchor = text.length();
    }

    public TextField maxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public TextField filter(Predicate<String> filter) {
        this.filter = filter;
        return this;
    }

    public TextField placeholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public TextField onSubmit(Consumer<String> listener) {
        submitted.add(listener);
        return this;
    }

    public String text() {
        return text;
    }

    public int cursor() {
        return cursor;
    }

    public String selected() {
        return text.substring(Math.min(cursor, anchor), Math.max(cursor, anchor));
    }

    /**
     * Replaces the whole text as if typed, even while the field is being edited.
     */
    public void setText(String text) {
        this.text = text == null ? "" : text;
        cursor = anchor = this.text.length();
        setValue(this.text);
        reveal();
    }

    public void select(int from, int to) {
        anchor = clampIndex(from);
        cursor = clampIndex(to);
        reveal();
    }

    @Override
    protected void changed(String value) {
        super.changed(value);
        if (!isEditing()) {
            text = value == null ? "" : value;
            cursor = clampIndex(cursor);
            anchor = clampIndex(anchor);
            reveal();
        }
    }

    @Override
    protected void focusChanged(boolean focused) {
        if (focused && editable()) {
            beginEdit();
        } else if (!focused) {
            endEdit();
            anchor = cursor;
        }
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public boolean capturesKeyboard() {
        return true;
    }

    @Override
    protected boolean interactive() {
        return true;
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        if (button != Keys.LEFT_BUTTON) {
            return false;
        }
        cursor = anchor = indexAt(x);
        return true;
    }

    @Override
    protected void pointerDrag(double x, double y, double dx, double dy, int button) {
        cursor = indexAt(x);
        reveal();
    }

    @Override
    protected boolean key(int key, int modifiers) {
        boolean shift = Keys.shift(modifiers);
        boolean control = Keys.control(modifiers);
        switch (key) {
            case Keys.LEFT -> move(control ? wordLeft(cursor) : previous(cursor), shift);
            case Keys.RIGHT -> move(control ? wordRight(cursor) : next(cursor), shift);
            case Keys.HOME -> move(0, shift);
            case Keys.END -> move(text.length(), shift);
            case Keys.BACKSPACE -> {
                if (cursor != anchor) {
                    replaceSelection("");
                } else {
                    replace(control ? wordLeft(cursor) : previous(cursor), cursor, "");
                }
            }
            case Keys.DELETE -> {
                if (cursor != anchor) {
                    replaceSelection("");
                } else {
                    replace(cursor, control ? wordRight(cursor) : next(cursor), "");
                }
            }
            case Keys.ENTER, Keys.KEYPAD_ENTER -> {
                for (Consumer<String> listener : submitted) {
                    listener.accept(text);
                }
                // A listener may have closed the popup holding this field, detaching it.
                if (root() != null) {
                    root().focus(null);
                }
            }
            default -> {
                if (!control) {
                    return false;
                }
                switch (key) {
                    case Keys.A -> select(0, text.length());
                    case Keys.C -> copy();
                    case Keys.X -> {
                        copy();
                        replaceSelection("");
                    }
                    case Keys.V -> replaceSelection(root().host().clipboard());
                    default -> {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected boolean character(int codepoint, int modifiers) {
        if (!editable() || Character.isISOControl(codepoint)) {
            return false;
        }
        replaceSelection(Character.toString(codepoint));
        return true;
    }

    private void copy() {
        if (cursor != anchor) {
            root().host().setClipboard(selected());
        }
    }

    private void move(int index, boolean extend) {
        cursor = index;
        if (!extend) {
            anchor = index;
        }
        reveal();
    }

    private void replaceSelection(String insert) {
        replace(Math.min(cursor, anchor), Math.max(cursor, anchor), insert);
    }

    private void replace(int from, int to, String insert) {
        if (!editable()) {
            return;
        }
        StringBuilder clean = new StringBuilder();
        insert.codePoints().filter(codepoint -> !Character.isISOControl(codepoint)).forEach(clean::appendCodePoint);
        String added = clean.toString();
        int room = maxLength - (text.length() - (to - from));
        if (added.length() > room) {
            int end = 0;
            while (end < added.length() && added.offsetByCodePoints(end, 1) <= room) {
                end = added.offsetByCodePoints(end, 1);
            }
            added = added.substring(0, end);
        }
        if (added.isEmpty() && from == to) {
            return;
        }
        String next = text.substring(0, from) + added + text.substring(to);
        if (!filter.test(next)) {
            return;
        }
        text = next;
        cursor = anchor = from + added.length();
        setValue(text);
        reveal();
    }

    private int clampIndex(int index) {
        int clamped = Math.max(0, Math.min(text.length(), index));
        if (clamped > 0 && clamped < text.length() && Character.isLowSurrogate(text.charAt(clamped)) && Character.isHighSurrogate(text.charAt(clamped - 1))) {
            clamped--;
        }
        return clamped;
    }

    private int previous(int index) {
        return index <= 0 ? 0 : text.offsetByCodePoints(index, -1);
    }

    private int next(int index) {
        return index >= text.length() ? text.length() : text.offsetByCodePoints(index, 1);
    }

    private int wordLeft(int index) {
        while (index > 0 && Character.isWhitespace(text.codePointBefore(index))) {
            index = previous(index);
        }
        while (index > 0 && !Character.isWhitespace(text.codePointBefore(index))) {
            index = previous(index);
        }
        return index;
    }

    private int wordRight(int index) {
        while (index < text.length() && Character.isWhitespace(text.codePointAt(index))) {
            index = next(index);
        }
        while (index < text.length() && !Character.isWhitespace(text.codePointAt(index))) {
            index = next(index);
        }
        return index;
    }

    private int indexAt(double x) {
        if (root() == null) {
            return cursor;
        }
        UiFont font = font();
        double target = x - INSET + scroll;
        int index = 0;
        while (index < text.length()) {
            int following = next(index);
            double middle = (font.width(text.substring(0, index)) + font.width(text.substring(0, following))) / 2.0;
            if (target < middle) {
                break;
            }
            index = following;
        }
        return index;
    }

    private void reveal() {
        if (root() == null) {
            return;
        }
        UiFont font = font();
        int inner = width() - INSET * 2;
        int position = font.width(text.substring(0, cursor));
        if (position - scroll > inner) {
            scroll = position - inner;
        }
        if (position < scroll) {
            scroll = position;
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, font.width(text) - inner)));
    }

    @Override
    protected void onLayout() {
        reveal();
    }

    @Override
    protected int contentWidth() {
        return 100;
    }

    @Override
    protected int contentHeight() {
        return root() == null ? 16 : font().lineHeight() + 8;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        UiFont font = font();
        int width = width();
        int height = height();
        Draw.box(canvas, theme(), "field", isFocused() ? "focus" : "widget_border", 0, 0, width, height);
        canvas.pushClip(1, 1, width - 2, height - 2);
        int textY = (height - font.lineHeight()) / 2 + 1;
        if (cursor != anchor) {
            int start = font.width(text.substring(0, Math.min(cursor, anchor))) - scroll + INSET;
            int end = font.width(text.substring(0, Math.max(cursor, anchor))) - scroll + INSET;
            canvas.fill(start, textY - 1, end, textY + font.lineHeight() - 1, color("selection"));
        }
        if (text.isEmpty() && !isFocused()) {
            canvas.text(placeholder, INSET, textY, color("text_dim"), false);
        } else {
            canvas.text(text, INSET - scroll, textY, color(editable() ? "text" : "text_dim"), false);
        }
        if (isFocused() && editable() && root().ticks() / 6 % 2 == 0) {
            int x = font.width(text.substring(0, cursor)) - scroll + INSET;
            canvas.fill(x, textY - 1, x + 1, textY + font.lineHeight() - 1, color("cursor"));
        }
        canvas.popClip();
    }
}
