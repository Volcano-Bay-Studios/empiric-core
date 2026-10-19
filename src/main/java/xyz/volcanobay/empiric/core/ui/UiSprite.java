package xyz.volcanobay.empiric.core.ui;

import xyz.volcanobay.empiric.core.data.Data;
import xyz.volcanobay.empiric.core.data.SelfSpec;

/**
 * A region of a texture. A non-zero {@link #border} draws it as a nine-slice, stretching only the middle.
 */
@SelfSpec
public class UiSprite {

    @Data
    public String texture = "";
    @Data
    public int u;
    @Data
    public int v;
    @Data
    public int width = 16;
    @Data
    public int height = 16;
    @Data(id = "texture_width")
    public int textureWidth = 256;
    @Data(id = "texture_height")
    public int textureHeight = 256;
    @Data
    public int border;

    public UiSprite() {
    }

    public UiSprite(String texture, int u, int v, int width, int height, int textureWidth, int textureHeight, int border) {
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.border = border;
    }
}
