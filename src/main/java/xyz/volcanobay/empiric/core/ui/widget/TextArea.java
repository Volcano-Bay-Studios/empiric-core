package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiFont;

import java.util.ArrayList;
import java.util.List;

/**
 * Several lines of editable text, without wrapping. The cursor and selection are offsets into the whole text, so they
 * never point into a line that no longer exists, and they always sit between code points. Enter starts a new line; the
 * wheel scrolls.
 */
public class TextArea extends ValueWidget<String> {

    private static final int INSET = 4;
    private static final int SCROLL_LINES = 3;

    private String text;
    private int cursor;
    private int anchor;
    private int scrollX;
    private int scrollY;
    private int rows = 5;
    private int maxLength = 8192;
    private int goalX = -1;
    private List<Integer> lineStarts = List.of(0);

    public TextArea() {
        this("");
    }

    public TextArea(String text) {
        super(String.class, text);
        this.text = text;
        this.cursor = this.anchor = text.length();
        index();
    }

    /**
     * How many lines tall it is when its height isn't set.
     */
    public TextArea rows(int rows) {
        this.rows = Math.max(1, rows);
        requestLayout();
        return this;
    }

    public TextArea maxLength(int maxLength) {
        this.maxLength = maxLength;
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
     * Replaces the whole text as if typed, even while it's being edited.
     */
    public void setText(String text) {
        this.text = text == null ? "" : text;
        index();
        cursor = anchor = this.text.length();
        goalX = -1;
        setValue(this.text);
        reveal();
    }

    public void select(int from, int to) {
        anchor = clampIndex(from);
        cursor = clampIndex(to);
        goalX = -1;
        reveal();
    }

    public int lineCount() {
        return lineStarts.size();
    }

    @Override
    protected void changed(String value) {
        super.changed(value);
        if (!isEditing()) {
            text = value == null ? "" : value;
            index();
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
        cursor = anchor = indexAt(x, y);
        goalX = -1;
        return true;
    }

    @Override
    protected void pointerDrag(double x, double y, double dx, double dy, int button) {
        cursor = indexAt(x, y);
        goalX = -1;
        reveal();
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        int max = maxScrollY();
        if (max <= 0 || root() == null) {
            return false;
        }
        scrollY = (int) Math.max(0, Math.min(max, scrollY - amount * SCROLL_LINES * font().lineHeight()));
        return true;
    }

    @Override
    protected boolean key(int key, int modifiers) {
        boolean shift = Keys.shift(modifiers);
        boolean control = Keys.control(modifiers);
        switch (key) {
            case Keys.LEFT -> move(control ? wordLeft(cursor) : previous(cursor), shift);
            case Keys.RIGHT -> move(control ? wordRight(cursor) : next(cursor), shift);
            case Keys.UP -> vertical(-1, shift);
            case Keys.DOWN -> vertical(1, shift);
            case Keys.HOME -> move(control ? 0 : lineStarts.get(lineOf(cursor)), shift);
            case Keys.END -> move(control ? text.length() : lineEnd(lineOf(cursor)), shift);
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
            case Keys.ENTER, Keys.KEYPAD_ENTER -> replaceSelection("\n");
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
        goalX = -1;
        reveal();
    }

    /**
     * Moves up or down a line, keeping to the column the cursor was at before a run of vertical moves.
     */
    private void vertical(int direction, boolean extend) {
        if (root() == null) {
            return;
        }
        int line = lineOf(cursor);
        if (goalX < 0) {
            goalX = font().width(text.substring(lineStarts.get(line), cursor));
        }
        int goal = goalX;
        int target = line + direction;
        int index;
        if (target < 0) {
            index = 0;
        } else if (target >= lineStarts.size()) {
            index = text.length();
        } else {
            index = indexInLine(target, goal);
        }
        cursor = index;
        if (!extend) {
            anchor = index;
        }
        reveal();
        goalX = goal;
    }

    private void replaceSelection(String insert) {
        replace(Math.min(cursor, anchor), Math.max(cursor, anchor), insert);
    }

    private void replace(int from, int to, String insert) {
        if (!editable()) {
            return;
        }
        StringBuilder clean = new StringBuilder();
        insert.replace("\r\n", "\n").replace('\r', '\n').codePoints()
                .filter(codepoint -> codepoint == '\n' || !Character.isISOControl(codepoint))
                .forEach(clean::appendCodePoint);
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
        text = text.substring(0, from) + added + text.substring(to);
        cursor = anchor = from + added.length();
        goalX = -1;
        index();
        setValue(text);
        reveal();
    }

    private void index() {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                starts.add(i + 1);
            }
        }
        lineStarts = starts;
    }

    private int lineOf(int index) {
        int low = 0;
        int high = lineStarts.size() - 1;
        while (low < high) {
            int middle = (low + high + 1) / 2;
            if (lineStarts.get(middle) <= index) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    private int lineEnd(int line) {
        return line + 1 < lineStarts.size() ? lineStarts.get(line + 1) - 1 : text.length();
    }

    private String line(int line) {
        return text.substring(lineStarts.get(line), lineEnd(line));
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

    /**
     * The offset in a line closest to {@code x} pixels from its start.
     */
    private int indexInLine(int line, double x) {
        UiFont font = font();
        int start = lineStarts.get(line);
        String content = line(line);
        int index = 0;
        while (index < content.length()) {
            int following = content.offsetByCodePoints(index, 1);
            double middle = (font.width(content.substring(0, index)) + font.width(content.substring(0, following))) / 2.0;
            if (x < middle) {
                break;
            }
            index = following;
        }
        return start + index;
    }

    private int indexAt(double x, double y) {
        if (root() == null) {
            return cursor;
        }
        int line = (int) Math.floor((y - INSET + scrollY) / font().lineHeight());
        if (line < 0) {
            return 0;
        }
        if (line >= lineStarts.size()) {
            return text.length();
        }
        return indexInLine(line, x - INSET + scrollX);
    }

    private int maxScrollY() {
        return root() == null ? 0 : Math.max(0, lineStarts.size() * font().lineHeight() - (height() - INSET * 2));
    }

    private void reveal() {
        if (root() == null) {
            return;
        }
        UiFont font = font();
        int line = lineOf(cursor);
        int innerWidth = width() - INSET * 2;
        int innerHeight = height() - INSET * 2;
        int top = line * font.lineHeight();
        if (top < scrollY) {
            scrollY = top;
        }
        if (top + font.lineHeight() - scrollY > innerHeight) {
            scrollY = top + font.lineHeight() - innerHeight;
        }
        scrollY = Math.max(0, Math.min(scrollY, maxScrollY()));
        int position = font.width(text.substring(lineStarts.get(line), cursor));
        if (position - scrollX > innerWidth - 1) {
            scrollX = position - innerWidth + 1;
        }
        if (position < scrollX) {
            scrollX = position;
        }
        scrollX = Math.max(0, scrollX);
    }

    @Override
    protected void onLayout() {
        reveal();
    }

    @Override
    protected int contentWidth() {
        return 120;
    }

    @Override
    protected int contentHeight() {
        return root() == null ? rows * 9 + INSET * 2 : rows * font().lineHeight() + INSET * 2;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        UiFont font = font();
        int width = width();
        int height = height();
        int lineHeight = font.lineHeight();
        Draw.box(canvas, theme(), "field", isFocused() ? "focus" : "widget_border", 0, 0, width, height);
        canvas.pushClip(1, 1, width - 2, height - 2);
        int first = Math.max(0, scrollY / lineHeight);
        int last = Math.min(lineStarts.size() - 1, (scrollY + height) / lineHeight);
        int from = Math.min(cursor, anchor);
        int to = Math.max(cursor, anchor);
        int textColor = color(editable() ? "text" : "text_dim");
        for (int line = first; line <= last; line++) {
            int start = lineStarts.get(line);
            int end = lineEnd(line);
            String content = text.substring(start, end);
            int y = INSET + line * lineHeight - scrollY;
            if (from != to && from <= end && to >= start) {
                int selectionStart = font.width(text.substring(start, Math.max(start, from)));
                int selectionEnd = to > end ? font.width(content) + 3 : font.width(text.substring(start, to));
                canvas.fill(INSET - scrollX + selectionStart, y - 1, INSET - scrollX + selectionEnd, y + lineHeight - 1, color("selection"));
            }
            canvas.text(content, INSET - scrollX, y, textColor, false);
        }
        if (isFocused() && editable() && root().ticks() / 6 % 2 == 0) {
            int line = lineOf(cursor);
            int x = INSET - scrollX + font.width(text.substring(lineStarts.get(line), cursor));
            int y = INSET + line * lineHeight - scrollY;
            canvas.fill(x, y - 1, x + 1, y + lineHeight - 1, color("cursor"));
        }
        canvas.popClip();
        int max = maxScrollY();
        if (max > 0) {
            int thumb = Math.max(6, height * height / (height + max));
            int top = Math.round((float) scrollY / max * (height - thumb));
            canvas.fill(width - 3, top, width - 1, top + thumb, color("scrollbar"));
        }
    }
}
