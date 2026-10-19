package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiPoint;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.layout.Align;
import xyz.volcanobay.empiric.core.ui.layout.FlexLayout;
import xyz.volcanobay.empiric.core.ui.layout.Insets;
import xyz.volcanobay.empiric.core.ui.layout.Length;

/**
 * An ARGB colour. Clicking the swatch opens a popup with a saturation and brightness square, a hue strip, an alpha strip
 * and a hex field. Bind it to an int field.
 */
public class ColorPicker extends ValueWidget<Integer> {

    private static final int AREA = 80;
    private static final int STRIP = 8;

    private boolean alpha = true;
    private Widget popup;
    private float hue;
    private float saturation;
    private float brightness;
    private float opacity;

    public ColorPicker() {
        this(0xFFFFFFFF);
    }

    public ColorPicker(int color) {
        super(Integer.class, color);
    }

    public ColorPicker alpha(boolean alpha) {
        this.alpha = alpha;
        return this;
    }

    public int argb() {
        return value() == null ? 0xFFFFFFFF : value();
    }

    public boolean isOpen() {
        return popup != null && popup.root() != null;
    }

    public void open() {
        if (isOpen() || !editable() || root() == null) {
            return;
        }
        int color = argb();
        float[] hsv = toHsv(color);
        hue = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
        opacity = (color >>> 24) / 255f;
        beginEdit();
        popup = new Popup();
        UiPoint below = toRoot(0, height());
        root().openPopup(popup, below.x(), below.y());
        sound("click");
    }

    public void close() {
        if (isOpen()) {
            popup.root().closePopup(popup);
        }
        popup = null;
    }

    private void apply() {
        int rgb = toRgb(hue, saturation, brightness);
        int a = alpha ? Math.round(opacity * 255) : 255;
        setValue(a << 24 | rgb);
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
        if (key == Keys.ENTER || key == Keys.SPACE) {
            open();
            return true;
        }
        return false;
    }

    @Override
    protected int contentWidth() {
        return root() == null ? 60 : font().width("#FFFFFFFF") + 22;
    }

    @Override
    protected int contentHeight() {
        return root() == null ? 16 : font().lineHeight() + 8;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        int height = height();
        String fill = isHovered() && editable() ? "widget_hover" : "widget";
        Draw.box(canvas, theme(), fill, isFocused() || isOpen() ? "focus" : "widget_border", 0, 0, width(), height);
        checker(canvas, 3, 3, height - 6, height - 6);
        canvas.fill(3, 3, height - 3, height - 3, argb());
        canvas.text(hex(argb(), alpha), height + 2, (height - font().lineHeight()) / 2 + 1, color(isEnabled() ? "text" : "text_dim"), false);
    }

    private static void checker(UiCanvas canvas, int x, int y, int width, int height) {
        for (int cy = 0; cy < height; cy += 4) {
            for (int cx = 0; cx < width; cx += 4) {
                int shade = ((cx + cy) / 4) % 2 == 0 ? 0xFF9A9A9A : 0xFF5A5A5A;
                canvas.fill(x + cx, y + cy, x + Math.min(width, cx + 4), y + Math.min(height, cy + 4), shade);
            }
        }
    }

    public static String hex(int color, boolean alpha) {
        return alpha ? String.format("#%08X", color) : String.format("#%06X", color & 0xFFFFFF);
    }

    public static float[] toHsv(int color) {
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue = 0;
        if (delta > 0) {
            if (max == r) {
                hue = ((g - b) / delta) % 6;
            } else if (max == g) {
                hue = (b - r) / delta + 2;
            } else {
                hue = (r - g) / delta + 4;
            }
            hue /= 6;
            if (hue < 0) {
                hue += 1;
            }
        }
        return new float[]{hue, max == 0 ? 0 : delta / max, max};
    }

    public static int toRgb(float hue, float saturation, float brightness) {
        float h = (hue % 1 + 1) % 1 * 6;
        int sector = (int) Math.floor(h);
        float f = h - sector;
        float p = brightness * (1 - saturation);
        float q = brightness * (1 - saturation * f);
        float t = brightness * (1 - saturation * (1 - f));
        float r;
        float g;
        float b;
        switch (sector % 6) {
            case 0 -> {
                r = brightness;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = brightness;
                b = p;
            }
            case 2 -> {
                r = p;
                g = brightness;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = brightness;
            }
            case 4 -> {
                r = t;
                g = p;
                b = brightness;
            }
            default -> {
                r = brightness;
                g = p;
                b = q;
            }
        }
        return Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
    }

    private final class Popup extends Panel {

        private final TextField field = new TextField().maxLength(9);

        Popup() {
            style("panel", "widget_border");
            padding(Insets.of(4));
            layout(FlexLayout.column(4).align(Align.STRETCH));
            add(new Area());
            add(new Strip(false));
            if (alpha) {
                add(new Strip(true));
            }
            field.filter(text -> text.matches("#?[0-9a-fA-F]{0,8}"));
            field.onSubmit(this::submit);
            field.width(Length.px(AREA));
            add(field);
            refresh();
        }

        private void refresh() {
            if (!field.isFocused()) {
                field.setValue(hex(argb(), alpha));
            }
        }

        private void submit(String text) {
            String digits = text.startsWith("#") ? text.substring(1) : text;
            if (digits.length() != 6 && digits.length() != 8) {
                return;
            }
            int parsed = (int) Long.parseLong(digits, 16);
            if (digits.length() == 6) {
                parsed |= 0xFF000000;
            }
            float[] hsv = toHsv(parsed);
            hue = hsv[0];
            saturation = hsv[1];
            brightness = hsv[2];
            opacity = (parsed >>> 24) / 255f;
            apply();
        }

        @Override
        protected void tick() {
            refresh();
        }

        @Override
        protected void onDetach() {
            endEdit();
        }
    }

    private final class Area extends Widget {

        Area() {
            size(AREA, AREA);
        }

        @Override
        protected boolean interactive() {
            return true;
        }

        @Override
        protected boolean pointerDown(double x, double y, int button) {
            choose(x, y);
            return true;
        }

        @Override
        protected void pointerDrag(double x, double y, double dx, double dy, int button) {
            choose(x, y);
        }

        private void choose(double x, double y) {
            saturation = (float) Draw.clamp(x / (width() - 1), 0, 1);
            brightness = (float) Draw.clamp(1 - y / (height() - 1), 0, 1);
            apply();
        }

        @Override
        protected void draw(UiCanvas canvas, float partialTick) {
            int width = width();
            int height = height();
            canvas.horizontalGradient(0, 0, width, height, 0xFFFFFFFF, 0xFF000000 | toRgb(hue, 1, 1));
            canvas.gradient(0, 0, width, height, 0x00000000, 0xFF000000);
            float x = saturation * width;
            float y = (1 - brightness) * height;
            canvas.arc(x, y, 2.5f, 3.5f, 0, (float) (Math.PI * 2), brightness > 0.5f ? 0xFF000000 : 0xFFFFFFFF);
        }
    }

    private final class Strip extends Widget {

        private final boolean alphaStrip;

        Strip(boolean alphaStrip) {
            this.alphaStrip = alphaStrip;
            size(AREA, STRIP);
        }

        @Override
        protected boolean interactive() {
            return true;
        }

        @Override
        protected boolean pointerDown(double x, double y, int button) {
            choose(x);
            return true;
        }

        @Override
        protected void pointerDrag(double x, double y, double dx, double dy, int button) {
            choose(x);
        }

        private void choose(double x) {
            float t = (float) Draw.clamp(x / (width() - 1), 0, 1);
            if (alphaStrip) {
                opacity = t;
            } else {
                hue = Math.min(t, 0.9999f);
            }
            apply();
        }

        @Override
        protected void draw(UiCanvas canvas, float partialTick) {
            int width = width();
            int height = height();
            if (alphaStrip) {
                checker(canvas, 0, 0, width, height);
                int rgb = toRgb(hue, saturation, brightness);
                canvas.horizontalGradient(0, 0, width, height, rgb, 0xFF000000 | rgb);
            } else {
                for (int i = 0; i < 6; i++) {
                    int x0 = i * width / 6;
                    int x1 = (i + 1) * width / 6;
                    canvas.horizontalGradient(x0, 0, x1, height, 0xFF000000 | toRgb(i / 6f, 1, 1), 0xFF000000 | toRgb((i + 1) / 6f, 1, 1));
                }
            }
            int marker = Math.round((alphaStrip ? opacity : hue) * (width - 1));
            canvas.fill(marker - 1, -1, marker + 2, height + 1, 0xFF000000);
            canvas.fill(marker, 0, marker + 1, height, 0xFFFFFFFF);
            canvas.outline(0, 0, width, height, color("widget_border"));
        }
    }
}
