package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiPoint;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.layout.Align;
import xyz.volcanobay.empiric.core.ui.layout.FlexLayout;
import xyz.volcanobay.empiric.core.ui.layout.Insets;
import xyz.volcanobay.empiric.core.ui.layout.Length;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Shows the chosen option and opens a list of all of them in a popup. Up and down step through the options while it's
 * focused. Long lists scroll.
 */
public class Dropdown<T> extends ValueWidget<T> {

    private static final int MAX_ROWS = 8;

    private final List<T> options;
    private Function<T, String> labels = String::valueOf;
    private Widget popup;

    public Dropdown(Class<T> type, List<T> options) {
        super(type, options.isEmpty() ? null : options.getFirst());
        this.options = List.copyOf(options);
    }

    public static <E extends Enum<E>> Dropdown<E> of(Class<E> type) {
        return new Dropdown<>(type, List.of(type.getEnumConstants()));
    }

    /**
     * Options are indices into {@code labels}, for binding to an int field.
     */
    public static Dropdown<Integer> indexed(String... labels) {
        Dropdown<Integer> dropdown = new Dropdown<>(Integer.class, IntStream.range(0, labels.length).boxed().toList());
        dropdown.labels = index -> index >= 0 && index < labels.length ? labels[index] : String.valueOf(index);
        return dropdown;
    }

    public Dropdown<T> labels(Function<T, String> labels) {
        this.labels = labels;
        requestLayout();
        return this;
    }

    public List<T> options() {
        return options;
    }

    public boolean isOpen() {
        return popup != null && popup.root() != null;
    }

    public void open() {
        if (isOpen() || !editable() || options.isEmpty() || root() == null) {
            return;
        }
        Widget list = new Widget().layout(FlexLayout.column(0).align(Align.STRETCH));
        for (T option : options) {
            list.add(new Option(option));
        }
        Widget content = list;
        if (options.size() > MAX_ROWS) {
            ScrollPanel scroll = new ScrollPanel().size(Length.FILL, Length.px(MAX_ROWS * rowHeight()));
            scroll.content().layout(FlexLayout.column(0).align(Align.STRETCH));
            for (Widget option : List.copyOf(list.children())) {
                scroll.add(option);
            }
            content = scroll;
        }
        popup = new Panel().style("panel", "widget_border").padding(Insets.of(1)).width(Length.px(width()));
        popup.layout(FlexLayout.column(0).align(Align.STRETCH));
        popup.add(content);
        UiPoint below = toRoot(0, height());
        root().openPopup(popup, below.x(), below.y());
        if (popup.y() < below.y()) {
            UiPoint above = toRoot(0, 0);
            popup.setBounds(popup.x(), (int) Math.max(0, above.y() - popup.height()), popup.width(), popup.height());
        }
        sound("click");
    }

    public void close() {
        if (isOpen()) {
            popup.root().closePopup(popup);
        }
        popup = null;
    }

    private void choose(T option) {
        setValue(option);
        sound("click");
        close();
    }

    private void step(int direction) {
        if (!editable() || options.isEmpty()) {
            return;
        }
        int index = options.indexOf(value());
        int next = index < 0 ? 0 : Math.max(0, Math.min(options.size() - 1, index + direction));
        setValue(options.get(next));
    }

    private int rowHeight() {
        return font().lineHeight() + 4;
    }

    @Override
    protected void onDetach() {
        close();
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    protected boolean interactive() {
        return true;
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        if (button != Keys.LEFT_BUTTON || !editable()) {
            return false;
        }
        if (isOpen()) {
            close();
        } else {
            open();
        }
        return true;
    }

    @Override
    protected boolean key(int key, int modifiers) {
        switch (key) {
            case Keys.DOWN -> step(1);
            case Keys.UP -> step(-1);
            case Keys.ENTER, Keys.KEYPAD_ENTER, Keys.SPACE -> open();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected int contentWidth() {
        if (root() == null) {
            return 0;
        }
        int widest = 0;
        for (T option : options) {
            widest = Math.max(widest, font().width(labels.apply(option)));
        }
        return widest + 18;
    }

    @Override
    protected int contentHeight() {
        return root() == null ? 0 : font().lineHeight() + 8;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        int width = width();
        int height = height();
        String fill = isOpen() ? "widget_hover" : isHovered() && editable() ? "widget_hover" : "widget";
        Draw.box(canvas, theme(), fill, isFocused() || isOpen() ? "focus" : "widget_border", 0, 0, width, height);
        int y = (height - font().lineHeight()) / 2 + 1;
        canvas.text(labels.apply(value()), 5, y, color(isEnabled() ? "text" : "text_dim"), false);
        float arrowX = width - 9;
        float arrowY = height / 2f - 1.5f;
        int arrow = color(editable() ? "text_dim" : "track");
        canvas.quad(arrowX, arrowY, arrowX + 2.5f, arrowY + 3, arrowX + 2.5f, arrowY + 3, arrowX + 5, arrowY, arrow);
    }

    private final class Option extends Widget {

        private final T option;

        Option(T option) {
            this.option = option;
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
            choose(option);
            return true;
        }

        @Override
        protected int contentWidth() {
            return font().width(labels.apply(option)) + 8;
        }

        @Override
        protected int contentHeight() {
            return rowHeight();
        }

        @Override
        protected void draw(UiCanvas canvas, float partialTick) {
            boolean chosen = Objects.equals(option, value());
            if (isHovered()) {
                canvas.fill(0, 0, width(), height(), color("widget_hover"));
            }
            canvas.text(labels.apply(option), 4, (height() - font().lineHeight()) / 2 + 1, color(chosen ? "accent" : "text"), false);
        }
    }
}
