package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.layout.Align;

import java.util.List;

/**
 * Text. Bound to a non-text field, it shows the field's value read-only. With {@link #wrap}, it wraps to its fixed width, or to the width its layout gives it.
 */
public class Label extends ValueWidget<String> {

    private String color = "text";
    private Align align = Align.START;
    private boolean shadow;
    private boolean wrap;
    private float scale = 1;
    private int laidWidth;

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

    /**
     * @param scale Text size relative to normal, such as 0.5 for half size
     */
    public Label scale(float scale) {
        this.scale = scale;
        requestLayout();
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

    private int wrapWidth() {
        if (!wrap) {
            return 0;
        }
        return preferredWidth().fixed() ? (int) preferredWidth().value() : laidWidth;
    }

    private List<String> lines(int width) {
        return wrap && width > 0 ? font().wrap(text(), Math.round(width / scale)) : List.of(text());
    }

    @Override
    protected void onLayout() {
        if (wrap && !preferredWidth().fixed() && width() != laidWidth) {
            laidWidth = width();
            requestLayout();
        }
    }

    @Override
    protected int contentWidth() {
        if (root() == null) {
            return 0;
        }
        int max = 0;
        for (String line : lines(wrapWidth())) {
            max = Math.max(max, font().width(line));
        }
        return Math.round(max * scale);
    }

    @Override
    protected int contentHeight() {
        return root() == null ? 0 : Math.round(lines(wrapWidth()).size() * font().lineHeight() * scale);
    }

    @Override
    protected void changed(String value) {
        super.changed(value);
        requestLayout();
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        int lineHeight = Math.round(font().lineHeight() * scale);
        int y = 0;
        int textColor = color(isEnabled() ? color : "text_dim");
        for (String line : lines(wrap ? width() : 0)) {
            int x = switch (align) {
                case CENTER -> (width() - Math.round(font().width(line) * scale)) / 2;
                case END -> width() - Math.round(font().width(line) * scale);
                default -> 0;
            };
            canvas.text(line, x, y, textColor, shadow, scale);
            y += lineHeight;
        }
    }
}
