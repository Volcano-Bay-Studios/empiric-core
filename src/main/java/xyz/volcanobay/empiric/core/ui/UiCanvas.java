package xyz.volcanobay.empiric.core.ui;

/**
 * What widgets draw onto. Coordinates are local to the widget being drawn, and the backend decides where the pixels end
 * up: a GUI screen, the HUD, or a texture placed in the world.
 */
public interface UiCanvas {

    UiFont font();

    void fill(int x0, int y0, int x1, int y1, int color);

    void gradient(int x0, int y0, int x1, int y1, int top, int bottom);

    /**
     * A quad with a colour per corner, blended across it.
     */
    void quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int c0, int c1, int c2, int c3);

    default void quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        quad(x0, y0, x1, y1, x2, y2, x3, y3, color, color, color, color);
    }

    default void horizontalGradient(int x0, int y0, int x1, int y1, int left, int right) {
        quad(x0, y0, x0, y1, x1, y1, x1, y0, left, left, right, right);
    }

    void text(String text, int x, int y, int color, boolean shadow);

    void blit(String texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, int textureWidth, int textureHeight, int tint);

    void push();

    void pop();

    void translate(float x, float y);

    void scale(float x, float y);

    void rotate(float radians);

    void pushClip(int x, int y, int width, int height);

    /**
     * Moves everything drawn from here on in front of everything drawn so far. Widgets call it once each in draw order,
     * so a later widget always covers an earlier one, including text and items that backends draw slightly raised.
     */
    default void nextLayer() {
    }

    /**
     * Like {@link #nextLayer}, but far enough forward to cover items too, for popups.
     */
    default void overlay() {
    }

    void popClip();

    default void outline(int x, int y, int width, int height, int color) {
        fill(x, y, x + width, y + 1, color);
        fill(x, y + height - 1, x + width, y + height, color);
        fill(x, y + 1, x + 1, y + height - 1, color);
        fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    default void line(float x0, float y0, float x1, float y1, float thickness, int color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0) {
            return;
        }
        float nx = -dy / length * thickness / 2;
        float ny = dx / length * thickness / 2;
        quad(x0 + nx, y0 + ny, x0 - nx, y0 - ny, x1 - nx, y1 - ny, x1 + nx, y1 + ny, color);
    }

    default void circle(float x, float y, float radius, int color) {
        arc(x, y, 0, radius, 0, (float) (Math.PI * 2), color);
    }

    /**
     * Fills the ring between {@code inner} and {@code outer} from {@code start} to {@code end}, in radians clockwise from
     * the right.
     */
    default void arc(float x, float y, float inner, float outer, float start, float end, int color) {
        int segments = Math.max(4, Math.min(64, (int) (Math.abs(end - start) * outer / 3)));
        float step = (end - start) / segments;
        for (int i = 0; i < segments; i++) {
            float a = start + step * i;
            float b = a + step;
            float ca = (float) Math.cos(a);
            float sa = (float) Math.sin(a);
            float cb = (float) Math.cos(b);
            float sb = (float) Math.sin(b);
            quad(x + ca * inner, y + sa * inner, x + ca * outer, y + sa * outer, x + cb * outer, y + sb * outer, x + cb * inner, y + sb * inner, color);
        }
    }

    default void sprite(UiSprite sprite, int x, int y, int width, int height, int tint) {
        int border = sprite.border;
        if (border <= 0 || (width == sprite.width && height == sprite.height)) {
            blit(sprite.texture, x, y, width, height, sprite.u, sprite.v, sprite.width, sprite.height, sprite.textureWidth, sprite.textureHeight, tint);
            return;
        }
        int innerWidth = width - border * 2;
        int innerHeight = height - border * 2;
        int sourceInnerWidth = sprite.width - border * 2;
        int sourceInnerHeight = sprite.height - border * 2;
        for (int row = 0; row < 3; row++) {
            int destY = row == 0 ? y : row == 1 ? y + border : y + height - border;
            int destHeight = row == 1 ? innerHeight : border;
            int sourceV = row == 0 ? sprite.v : row == 1 ? sprite.v + border : sprite.v + sprite.height - border;
            int sourceHeight = row == 1 ? sourceInnerHeight : border;
            for (int column = 0; column < 3; column++) {
                int destX = column == 0 ? x : column == 1 ? x + border : x + width - border;
                int destWidth = column == 1 ? innerWidth : border;
                int sourceU = column == 0 ? sprite.u : column == 1 ? sprite.u + border : sprite.u + sprite.width - border;
                int sourceWidth = column == 1 ? sourceInnerWidth : border;
                if (destWidth > 0 && destHeight > 0) {
                    blit(sprite.texture, destX, destY, destWidth, destHeight, sourceU, sourceV, sourceWidth, sourceHeight, sprite.textureWidth, sprite.textureHeight, tint);
                }
            }
        }
    }

    default void centeredText(String text, int x, int y, int color, boolean shadow) {
        text(text, x - font().width(text) / 2, y, color, shadow);
    }
}
