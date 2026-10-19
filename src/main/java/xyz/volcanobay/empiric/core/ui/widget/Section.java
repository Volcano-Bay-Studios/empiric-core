package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.layout.Align;
import xyz.volcanobay.empiric.core.ui.layout.FlexLayout;
import xyz.volcanobay.empiric.core.ui.layout.Insets;

/**
 * A titled group that folds away. Clicking the header shows or hides what was added, which goes into {@link #body()}, a
 * column by default.
 */
public class Section extends Widget {

    private final Header header;
    private final Widget body = new Widget().layout(FlexLayout.column(2).align(Align.STRETCH));
    private boolean open = true;

    public Section(String title) {
        header = new Header(title);
        layout(FlexLayout.column(2).align(Align.STRETCH));
        body.padding(Insets.of(2, 0));
        super.add(header);
        super.add(body);
    }

    @Override
    public <W extends Widget> W add(W child) {
        return body.add(child);
    }

    public Widget body() {
        return body;
    }

    public boolean isOpen() {
        return open;
    }

    public Section open(boolean open) {
        this.open = open;
        body.visible(open);
        return this;
    }

    public Section title(String title) {
        header.title = title;
        requestLayout();
        return this;
    }

    private final class Header extends Widget {

        private String title;

        Header(String title) {
            this.title = title;
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
            if (button != Keys.LEFT_BUTTON) {
                return false;
            }
            open(!open);
            sound("click");
            return true;
        }

        @Override
        protected boolean key(int key, int modifiers) {
            if (key == Keys.ENTER || key == Keys.KEYPAD_ENTER || key == Keys.SPACE) {
                open(!open);
                return true;
            }
            return false;
        }

        @Override
        protected int contentWidth() {
            return root() == null ? 0 : font().width(title) + 14;
        }

        @Override
        protected int contentHeight() {
            return root() == null ? 0 : font().lineHeight() + 5;
        }

        @Override
        protected void draw(UiCanvas canvas, float partialTick) {
            Draw.box(canvas, theme(), isHovered() ? "widget_hover" : "widget", isFocused() ? "focus" : "widget_border", 0, 0, width(), height());
            float cx = 6;
            float cy = height() / 2f;
            int arrow = color("text_dim");
            if (open) {
                canvas.quad(cx - 2.5f, cy - 1.5f, cx, cy + 1.5f, cx, cy + 1.5f, cx + 2.5f, cy - 1.5f, arrow);
            } else {
                canvas.quad(cx - 1.5f, cy - 2.5f, cx - 1.5f, cy + 2.5f, cx + 1.5f, cy, cx + 1.5f, cy, arrow);
            }
            canvas.text(title, 12, (height() - font().lineHeight()) / 2 + 1, color("text"), false);
        }
    }
}
