package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.layout.Align;
import xyz.volcanobay.empiric.core.ui.layout.FlexLayout;
import xyz.volcanobay.empiric.core.ui.layout.Length;

import java.util.Locale;

/**
 * A number typed into a field, with buttons that step it down and up. Each valid number typed is applied straight
 * away.
 */
public class NumberField extends ValueWidget<Double> {

    private final TextField field = new TextField();
    private final Button down = new Button("-", () -> nudge(-1));
    private final Button up = new Button("+", () -> nudge(1));
    private double step = 1;
    private double min = -Double.MAX_VALUE;
    private double max = Double.MAX_VALUE;
    private boolean integer;

    public NumberField() {
        this(0);
    }

    public NumberField(double value) {
        super(Double.class, value);
        layout(FlexLayout.row(1).align(Align.STRETCH));
        field.width(Length.FILL);
        field.filter(text -> text.matches("-?[0-9]*\\.?[0-9]*([eE]-?[0-9]*)?"));
        field.onChange(this::typed);
        down.width(Length.px(11));
        up.width(Length.px(11));
        down.hint("Step down");
        up.hint("Step up");
        super.add(field);
        super.add(down);
        super.add(up);
        field.setValue(format(value));
    }

    public NumberField step(double step) {
        this.step = step;
        return this;
    }

    public NumberField range(double min, double max) {
        this.min = min;
        this.max = max;
        return this;
    }

    /**
     * Whole numbers only: typed decimals are rounded and the text has no decimal point.
     */
    public NumberField integer(boolean integer) {
        this.integer = integer;
        field.setValue(format(number()));
        return this;
    }

    /**
     * Hides the step buttons, for narrow spaces.
     */
    public NumberField compact() {
        down.visible(false);
        up.visible(false);
        return this;
    }

    public TextField field() {
        return field;
    }

    public double number() {
        return value() == null ? 0 : value();
    }

    private double clamp(double value) {
        double clamped = Math.max(min, Math.min(max, value));
        return integer ? Math.round(clamped) : clamped;
    }

    private void typed(String text) {
        if (!field.isEditing()) {
            return;
        }
        try {
            setValue(clamp(Double.parseDouble(text)));
        } catch (NumberFormatException e) {
            // Half-typed, like "-" or "1e": wait for more.
        }
    }

    private void nudge(int direction) {
        if (editable()) {
            setValue(clamp(number() + step * direction));
        }
    }

    private String format(double value) {
        if (integer || value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString(Math.round(value));
        }
        String text = String.format(Locale.ROOT, "%.4f", value);
        return text.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @Override
    protected void changed(Double value) {
        super.changed(value);
        if (!field.isEditing()) {
            field.setValue(format(value == null ? 0 : value));
        }
    }

    @Override
    protected void tick() {
        boolean editable = editable();
        field.enabled(editable);
        down.enabled(editable);
        up.enabled(editable);
        if (!field.isEditing()) {
            String shown = format(number());
            if (!field.text().equals(shown)) {
                field.setValue(shown);
            }
        }
    }

    @Override
    protected boolean key(int key, int modifiers) {
        if (key == Keys.UP || key == Keys.DOWN) {
            nudge(key == Keys.UP ? 1 : -1);
            if (field.isEditing()) {
                field.setText(format(number()));
                field.select(0, field.text().length());
            }
            return true;
        }
        return false;
    }

    @Override
    public <W extends Widget> W add(W child) {
        throw new UnsupportedOperationException("A number field has no room for other widgets");
    }
}
