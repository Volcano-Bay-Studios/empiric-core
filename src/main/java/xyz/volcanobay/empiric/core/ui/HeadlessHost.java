package xyz.volcanobay.empiric.core.ui;

/**
 * A host with nothing to draw to, for building a widget tree where there's no screen, such as on the server to find a
 * menu's slots. Its font is fixed width.
 */
public class HeadlessHost implements UiHost {

    public static final HeadlessHost INSTANCE = new HeadlessHost();

    private static final UiFont FONT = new UiFont() {
        @Override
        public int width(String text) {
            return text.codePointCount(0, text.length()) * 6;
        }

        @Override
        public int lineHeight() {
            return 9;
        }
    };

    @Override
    public UiFont font() {
        return FONT;
    }

    @Override
    public String clipboard() {
        return "";
    }

    @Override
    public void setClipboard(String text) {
    }

    @Override
    public void playSound(String id, float pitch) {
    }

    @Override
    public UiTheme theme(String id) {
        return UiTheme.FALLBACK;
    }

    @Override
    public void close() {
    }
}
