package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.layout.Align;

import java.util.List;

/**
 * Text. Bound to a non-text field, it shows the field's value read-only. With {@link #wrap}, it wraps to its fixed width.
 */
public class Label extends ValueWidget<String> {

    private String color = "text";
    private Align align = Align.START;
    private boolean shadow;
    private boolean wrap;

    public Label() {
        this("");
    }

    public Label(String text) {
        super(String.class, text);
    }

    public Label textColor(String key) {
        this.color = key;
        return this;
    }

    public Label align(Align align) {
        this.align = align;
        return this;
    }

    public Label shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public Label wrap(boolean wrap) {
        this.wrap = wrap;
        requestLayout();
        return this;
    }

    public String text() {
        return value() == null ? "" : value();
    }

    private List<String> lines(int width) {
        return wrap && preferredWidth().fixed() ? font().wrap(text(), width) : List.of(text());
    }

    @Override
    protected int contentWidth() {
        if (root() == null) {
            return 0;
        }
        int max = 0;
        for (String line : lines((int) preferredWidth().value())) {
            max = Math.max(max, font().width(line));
        }
        return max;
    }

    @Override
    protected int contentHeight() {
        return root() == null ? 0 : lines((int) preferredWidth().value()).size() * font().lineHeight();
    }

    @Override
    protected void changed(String value) {
        super.changed(value);
        requestLayout();
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        int lineHeight = font().lineHeight();
        int y = 0;
        int textColor = color(isEnabled() ? color : "text_dim");
        for (String line : lines(width())) {
            int x = switch (align) {
                case CENTER -> (width() - font().width(line)) / 2;
                case END -> width() - font().width(line);
                default -> 0;
            };
            canvas.text(line, x, y, textColor, shadow);
            y += lineHeight;
        }
    }
}
