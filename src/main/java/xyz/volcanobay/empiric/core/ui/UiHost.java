package xyz.volcanobay.empiric.core.ui;

/**
 * The environment a {@link UiRoot} lives in, such as a GUI screen or a world surface.
 */
public interface UiHost {

    UiFont font();

    String clipboard();

    void setClipboard(String text);

    void playSound(String id, float pitch);

    UiTheme theme(String id);

    void close();

    /**
     * Whether shift is held, for clicks that act differently with it, such as adding several nodes from a picker.
     */
    default boolean shiftDown() {
        return false;
    }

    /**
     * Whether control is held, for pointer gestures that change with it, such as zooming instead of scrolling.
     */
    default boolean controlDown() {
        return false;
    }
}
