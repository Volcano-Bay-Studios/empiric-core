package xyz.volcanobay.empiric.core.ui;

import xyz.volcanobay.empiric.core.data.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Colours, sprites and sounds that widgets look up by key. Keys a theme leaves out fall back to {@link #FALLBACK}.
 * Colours are written {@code #RRGGBB} or {@code #AARRGGBB}, and sounds are sound event ids.
 */
public class UiTheme {

    public static final String DEFAULT = "empiric:default";
    public static final UiTheme FALLBACK = fallback();

    @Data
    public Map<String, String> colors = new HashMap<>();
    @Data
    public Map<String, UiSprite> sprites = new HashMap<>();
    @Data
    public Map<String, String> sounds = new HashMap<>();

    private final Map<String, Integer> parsed = new HashMap<>();

    public int color(String key) {
        Integer cached = parsed.get(key);
        if (cached != null) {
            return cached;
        }
        String raw = colors.get(key);
        int color;
        if (raw != null) {
            color = parse(raw);
        } else if (key.startsWith("#")) {
            color = parse(key);
        } else if (this != FALLBACK) {
            color = FALLBACK.color(key);
        } else {
            color = 0xFFFF00FF;
        }
        parsed.put(key, color);
        return color;
    }

    public UiSprite sprite(String key) {
        UiSprite sprite = sprites.get(key);
        return sprite != null || this == FALLBACK ? sprite : FALLBACK.sprite(key);
    }

    public String sound(String key) {
        String sound = sounds.get(key);
        return sound != null || this == FALLBACK ? sound : FALLBACK.sound(key);
    }

    public static int parse(String color) {
        String hex = color.startsWith("#") ? color.substring(1) : color;
        long value = Long.parseLong(hex, 16);
        return hex.length() <= 6 ? (int) (0xFF000000L | value) : (int) value;
    }

    private static UiTheme fallback() {
        UiTheme theme = new UiTheme();
        theme.colors.put("text", "#E0E0E0");
        theme.colors.put("text_dim", "#8A8A8A");
        theme.colors.put("panel", "#E0181C20");
        theme.colors.put("panel_border", "#3A4048");
        theme.colors.put("widget", "#2A2F36");
        theme.colors.put("widget_hover", "#353B44");
        theme.colors.put("widget_border", "#4A525C");
        theme.colors.put("accent", "#4651D2");
        theme.colors.put("focus", "#576AD3");
        theme.colors.put("track", "#1A1E22");
        theme.colors.put("knob", "#B3B3B3");
        theme.colors.put("knob_hover", "#919191");
        theme.colors.put("field", "#101214");
        theme.colors.put("selection", "#803D5AA8");
        theme.colors.put("cursor", "#E0E0E0");
        theme.colors.put("scrollbar", "#4A525C");
        theme.colors.put("slot", "#1E2328");
        theme.colors.put("slot_border", "#4A525C");
        theme.colors.put("slot_hover", "#80FFFFFF");
        theme.colors.put("tooltip_line", "#C0FFFFFF");
        theme.colors.put("error", "#FF6666");
        theme.sounds.put("click", "minecraft:ui.button.click");
        theme.sounds.put("toggle", "minecraft:ui.button.click");
        theme.sounds.put("dial", "minecraft:block.wooden_button.click_on");
        return theme;
    }
}
