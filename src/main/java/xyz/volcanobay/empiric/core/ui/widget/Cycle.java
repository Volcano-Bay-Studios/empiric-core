package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Steps through a fixed list of options. Left click and right arrow go forward, right click and left arrow go back.
 */
public class Cycle<T> extends ValueWidget<T> {

    private final List<T> options;
    private Function<T, String> labels = String::valueOf;

    public Cycle(Class<T> type, List<T> options) {
        super(type, options.isEmpty() ? null : options.getFirst());
        this.options = List.copyOf(options);
    }

    public static <E extends Enum<E>> Cycle<E> of(Class<E> type) {
        return new Cycle<>(type, List.of(type.getEnumConstants()));
    }

    /**
     * Options are indices into {@code labels}, for binding to an int field.
     */
    public static Cycle<Integer> indexed(String... labels) {
        Cycle<Integer> cycle = new Cycle<>(Integer.class, IntStream.range(0, labels.length).boxed().toList());
        cycle.labels = index -> index >= 0 && index < labels.length ? labels[index] : String.valueOf(index);
        return cycle;
    }

    public Cycle<T> labels(Function<T, String> labels) {
        this.labels = labels;
        requestLayout();
        return this;
    }

    public List<T> options() {
        return options;
    }

    public void step(int direction) {
        if (!editable() || options.isEmpty()) {
            return;
        }
        int index = options.indexOf(value());
        int next = index < 0 ? 0 : Math.floorMod(index + direction, options.size());
        setValue(options.get(next));
        sound("click");
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
        if (!editable() || (button != Keys.LEFT_BUTTON && button != Keys.RIGHT_BUTTON)) {
            return false;
        }
        step(button == Keys.LEFT_BUTTON ? 1 : -1);
        return true;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        if (!editable()) {
            return false;
        }
        step(amount > 0 ? -1 : 1);
        return true;
    }

    @Override
    protected boolean key(int key, int modifiers) {
        if (key == Keys.RIGHT || key == Keys.ENTER || key == Keys.SPACE) {
            step(1);
            return true;
        }
        if (key == Keys.LEFT) {
            step(-1);
            return true;
        }
        return false;
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
        return widest + 24;
    }

    @Override
    protected int contentHeight() {
        return root() == null ? 0 : font().lineHeight() + 8;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        int width = width();
        int height = height();
        String fill = isHovered() && editable() ? "widget_hover" : "widget";
        Draw.box(canvas, theme(), fill, isFocused() ? "focus" : "widget_border", 0, 0, width, height);
        int y = (height - font().lineHeight()) / 2 + 1;
        int arrows = color(editable() ? "text_dim" : "track");
        canvas.text("<", 4, y, arrows, false);
        canvas.text(">", width - 4 - font().width(">"), y, arrows, false);
        canvas.centeredText(labels.apply(value()), width / 2, y, color(isEnabled() ? "text" : "text_dim"), false);
    }
}
