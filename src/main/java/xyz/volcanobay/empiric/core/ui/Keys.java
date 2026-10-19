package xyz.volcanobay.empiric.core.ui;

/**
 * Key codes and modifier bits, with the same values as GLFW.
 */
public final class Keys {

    public static final int SPACE = 32;
    public static final int A = 65;
    public static final int C = 67;
    public static final int S = 83;
    public static final int V = 86;
    public static final int X = 88;
    public static final int Y = 89;
    public static final int Z = 90;
    public static final int ESCAPE = 256;
    public static final int ENTER = 257;
    public static final int TAB = 258;
    public static final int BACKSPACE = 259;
    public static final int DELETE = 261;
    public static final int RIGHT = 262;
    public static final int LEFT = 263;
    public static final int DOWN = 264;
    public static final int UP = 265;
    public static final int HOME = 268;
    public static final int END = 269;
    public static final int KEYPAD_ENTER = 335;

    public static final int SHIFT = 1;
    public static final int CONTROL = 2;
    public static final int ALT = 4;
    public static final int SUPER = 8;

    public static final int LEFT_BUTTON = 0;
    public static final int RIGHT_BUTTON = 1;
    public static final int MIDDLE_BUTTON = 2;

    private Keys() {
    }

    public static boolean shift(int modifiers) {
        return (modifiers & SHIFT) != 0;
    }

    public static boolean control(int modifiers) {
        return (modifiers & (CONTROL | SUPER)) != 0;
    }
}
