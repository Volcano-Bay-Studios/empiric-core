package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.layout.FlexLayout;
import xyz.volcanobay.empiric.core.ui.layout.Insets;
import xyz.volcanobay.empiric.core.ui.layout.Layout;

/**
 * A clipped frame that scrolls its content vertically. Children added to it go into {@link #content()}, a column by
 * default.
 */
public class ScrollPanel extends Widget {

    private static final int BAR = 4;
    private static final int SPEED = 12;

    private final Widget content = new Widget().layout(FlexLayout.column(2));
    private float scroll;
    private boolean draggingBar;

    public ScrollPanel() {
        super.add(content);
        clip(true);
        layout(new ScrollLayout());
    }

    @Override
    public <W extends Widget> W add(W child) {
        return content.add(child);
    }

    public Widget content() {
        return content;
    }

    public float scrollOffset() {
        return scroll;
    }

    public int maxScroll() {
        return Math.max(0, content.height() - (height() - padding().vertical()));
    }

    public void scrollTo(float offset) {
        scroll = Math.max(0, Math.min(maxScroll(), offset));
        contentY = -scroll;
    }

    @Override
    protected void onLayout() {
        scrollTo(scroll);
    }

    @Override
    protected boolean interactive() {
        return true;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        if (maxScroll() <= 0) {
            return false;
        }
        scrollTo(scroll - (float) amount * SPEED);
        return true;
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        if (button == Keys.LEFT_BUTTON && maxScroll() > 0 && x >= width() - BAR) {
            draggingBar = true;
            return true;
        }
        return false;
    }

    @Override
    protected void pointerDrag(double x, double y, double dx, double dy, int button) {
        if (draggingBar) {
            scrollTo(scroll + (float) (dy * content.height() / Math.max(1, height())));
        }
    }

    @Override
    protected void pointerUp(double x, double y, int button) {
        draggingBar = false;
    }

    @Override
    protected void drawOver(UiCanvas canvas, float partialTick) {
        int max = maxScroll();
        if (max <= 0) {
            return;
        }
        int height = height();
        int thumb = Math.max(8, height * height / Math.max(1, content.height()));
        int top = Math.round(scroll / max * (height - thumb));
        canvas.fill(width() - BAR, 0, width(), height, color("track"));
        canvas.fill(width() - BAR, top, width(), top + thumb, color(draggingBar ? "accent" : "scrollbar"));
    }

    private final class ScrollLayout implements Layout {

        @Override
        public int measureWidth(Widget widget) {
            return content.measuredWidth() + BAR + widget.padding().horizontal();
        }

        @Override
        public int measureHeight(Widget widget) {
            return content.measuredHeight() + widget.padding().vertical();
        }

        @Override
        public void arrange(Widget widget) {
            Insets padding = widget.padding();
            int viewport = widget.height() - padding.vertical();
            content.setBounds(padding.left(), padding.top(), widget.width() - padding.horizontal() - BAR, Math.max(viewport, content.measuredHeight()));
        }
    }
}
