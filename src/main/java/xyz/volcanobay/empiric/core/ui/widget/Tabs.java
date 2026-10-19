package xyz.volcanobay.empiric.core.ui.widget;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.layout.Align;
import xyz.volcanobay.empiric.core.ui.layout.FlexLayout;
import xyz.volcanobay.empiric.core.ui.layout.Length;

/**
 * A row of tabs over a stack of pages, showing the page at the selected index. Children added to it become pages, titled
 * by their id. Binding the index to a module field shares the open tab between viewers.
 */
public class Tabs extends ValueWidget<Integer> {

    private final Widget header = new Widget().layout(FlexLayout.row(1));
    private final Widget pages = new Pages();

    public Tabs() {
        super(Integer.class, 0);
        super.add(header);
        super.add(pages);
        pages.fill();
        layout(FlexLayout.column(0).align(Align.STRETCH));
    }

    @Override
    public <W extends Widget> W add(W page) {
        return tab(page.id(), page);
    }

    public <W extends Widget> W tab(String title, W page) {
        int index = pages.children().size();
        header.add(new Tab(index, title.isEmpty() ? "Tab " + (index + 1) : title));
        if (page.preferredWidth() == Length.FIT) {
            page.width(Length.FILL);
        }
        if (page.preferredHeight() == Length.FIT) {
            page.height(Length.FILL);
        }
        pages.add(page);
        page.visible(index == selected());
        return page;
    }

    public int selected() {
        return value() == null ? 0 : value();
    }

    public Widget page(int index) {
        return index >= 0 && index < pages.children().size() ? pages.children().get(index) : null;
    }

    @Override
    protected void changed(Integer value) {
        super.changed(value);
        for (int i = 0; i < pages.children().size(); i++) {
            pages.children().get(i).visible(i == selected());
        }
    }

    private static final class Pages extends Widget {

        @Override
        protected int contentWidth() {
            int max = 0;
            for (Widget page : children()) {
                max = Math.max(max, page.measuredWidth() + page.margin().horizontal());
            }
            return max + padding().horizontal();
        }

        @Override
        protected int contentHeight() {
            int max = 0;
            for (Widget page : children()) {
                max = Math.max(max, page.measuredHeight() + page.margin().vertical());
            }
            return max + padding().vertical();
        }
    }

    private final class Tab extends Widget {

        private final int index;
        private final String title;

        Tab(int index, String title) {
            this.index = index;
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
            if (button != Keys.LEFT_BUTTON || !editable()) {
                return false;
            }
            select();
            return true;
        }

        @Override
        protected boolean key(int key, int modifiers) {
            if (key == Keys.ENTER || key == Keys.SPACE) {
                select();
                return true;
            }
            return false;
        }

        private void select() {
            if (editable() && selected() != index) {
                setValue(index);
                sound("click");
            }
        }

        @Override
        protected int contentWidth() {
            return font().width(title) + 10;
        }

        @Override
        protected int contentHeight() {
            return font().lineHeight() + 6;
        }

        @Override
        protected void draw(UiCanvas canvas, float partialTick) {
            boolean active = selected() == index;
            String fill = active ? "panel" : isHovered() && editable() ? "widget_hover" : "widget";
            Draw.box(canvas, theme(), fill, isFocused() ? "focus" : "panel_border", 0, 0, width(), height());
            canvas.centeredText(title, width() / 2, (height() - font().lineHeight()) / 2 + 1, color(active ? "text" : "text_dim"), false);
        }
    }
}
