package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiSprite;
import xyz.volcanobay.empiric.core.ui.Widget;

/**
 * Draws a sprite, stretched or nine-sliced to the widget's size.
 */
public class Image extends Widget {

    private UiSprite sprite;
    private int tint = 0xFFFFFFFF;

    public Image(UiSprite sprite) {
        this.sprite = sprite;
    }

    public Image sprite(UiSprite sprite) {
        this.sprite = sprite;
        requestLayout();
        return this;
    }

    public Image tint(int tint) {
        this.tint = tint;
        return this;
    }

    @Override
    protected int contentWidth() {
        return sprite == null ? 0 : sprite.width;
    }

    @Override
    protected int contentHeight() {
        return sprite == null ? 0 : sprite.height;
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        if (sprite != null) {
            canvas.sprite(sprite, 0, 0, width(), height(), tint);
        }
    }
}
