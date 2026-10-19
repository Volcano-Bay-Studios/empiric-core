package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiSprite;
import xyz.volcanobay.empiric.core.ui.UiTheme;

public final class Draw {

    private Draw() {
    }

    /**
     * Draws the theme's sprite for {@code key} if it has one, otherwise a box in the {@code key} colour with a
     * {@code border} coloured outline.
     */
    public static void box(UiCanvas canvas, UiTheme theme, String key, String border, int x, int y, int width, int height) {
        UiSprite sprite = theme.sprite(key);
        if (sprite != null) {
            canvas.sprite(sprite, x, y, width, height, 0xFFFFFFFF);
            return;
        }
        canvas.fill(x, y, x + width, y + height, theme.color(key));
        if (border != null) {
            canvas.outline(x, y, width, height, theme.color(border));
        }
    }

    public static int lerp(int from, int to, float t) {
        int a = channel(from >>> 24, to >>> 24, t);
        int r = channel(from >> 16 & 0xFF, to >> 16 & 0xFF, t);
        int g = channel(from >> 8 & 0xFF, to >> 8 & 0xFF, t);
        int b = channel(from & 0xFF, to & 0xFF, t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int channel(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps to the range and rounds to the nearest step from {@code min}.
     */
    public static double snap(double value, double min, double max, double step) {
        double clamped = clamp(value, min, max);
        if (step > 0) {
            clamped = clamp(min + Math.round((clamped - min) / step) * step, min, max);
        }
        return Math.round(clamped * 1e6) / 1e6;
    }

    public static String format(double value, double step) {
        if (step >= 1 && step == Math.rint(step)) {
            return Long.toString(Math.round(value));
        }
        return String.format("%.2f", value);
    }
}
