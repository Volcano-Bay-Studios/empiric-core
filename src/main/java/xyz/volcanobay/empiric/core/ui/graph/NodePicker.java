package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiPoint;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.layout.Align;
import xyz.volcanobay.empiric.core.ui.layout.FlexLayout;
import xyz.volcanobay.empiric.core.ui.layout.Insets;
import xyz.volcanobay.empiric.core.ui.layout.Length;
import xyz.volcanobay.empiric.core.ui.widget.Label;
import xyz.volcanobay.empiric.core.ui.widget.Panel;
import xyz.volcanobay.empiric.core.ui.widget.ScrollPanel;
import xyz.volcanobay.empiric.core.ui.widget.TextField;

import java.util.Locale;

/**
 * Every node type in the palette, filtered by typing.
 */
public class NodePicker extends Panel {

    private static final int WIDTH = 150;
    private static final int ROW = 14;
    private static final int MAX_ROWS = 10;

    private final NodeGraphView view;
    private final float openedX;
    private final float openedY;
    private final TextField search = new TextField().placeholder("Search");
    private final ScrollPanel list = new ScrollPanel();

    NodePicker(NodeGraphView view, float graphX, float graphY) {
        this.view = view;
        this.openedX = graphX;
        this.openedY = graphY;
        padding(Insets.of(3));
        layout(FlexLayout.column(3).align(Align.STRETCH));
        width(Length.px(WIDTH));
        list.content().layout(FlexLayout.column(0).align(Align.STRETCH));
        search.onChange(text -> refill());
        search.onSubmit(text -> pickFirst());
        add(search);
        add(list);
        refill();
    }

    /**
     * The search field, focused when the picker opens so typing filters straight away.
     */
    public TextField search() {
        return search;
    }

    private void refill() {
        list.content().clear();
        String query = search.text().toLowerCase(Locale.ROOT).trim();
        int rows = 0;
        for (GraphNodeType type : view.palette().types()) {
            if (query.isEmpty() || matches(type, query)) {
                list.add(new Row(type));
                rows++;
            }
        }
        if (rows == 0) {
            list.add(new Label("No matches").textColor("text_dim"));
            rows = 1;
        }
        list.height(Length.px(Math.min(rows, MAX_ROWS) * ROW));
        list.scrollTo(0);
        requestLayout();
    }

    private static boolean matches(GraphNodeType type, String query) {
        return type.title().toLowerCase(Locale.ROOT).contains(query)
                || type.category().toLowerCase(Locale.ROOT).contains(query)
                || type.id().toLowerCase(Locale.ROOT).contains(query);
    }

    private void pickFirst() {
        for (Widget child : list.content().children()) {
            if (child instanceof Row row) {
                view.addNode(row.type.id(), openedX, openedY);
                root().closePopup(this);
                return;
            }
        }
    }

    private void place(GraphNodeType type, boolean keepOpen) {
        UiPoint local = view.toLocal(root().pointerX(), root().pointerY());
        UiPoint graph = view.toGraph(local.x(), local.y());
        view.addNode(type.id(), (float) graph.x() - 8, (float) graph.y() - NodeWidget.TITLE / 2f);
        if (!keepOpen) {
            root().closePopup(this);
        }
    }

    private final class Row extends Widget {

        private final GraphNodeType type;

        private Row(GraphNodeType type) {
            this.type = type;
            if (!type.description().isEmpty()) {
                hint(type.description());
            }
        }

        @Override
        protected int contentWidth() {
            return WIDTH - 10;
        }

        @Override
        protected int contentHeight() {
            return ROW;
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
            place(type, root().host().shiftDown());
            return true;
        }

        @Override
        protected void draw(UiCanvas canvas, float partialTick) {
            if (isHovered()) {
                canvas.fill(0, 0, width(), height(), color("selection"));
            }
            canvas.fill(2, 3, 5, height() - 3, color(type.color()));
            int textY = (height() - font().lineHeight()) / 2 + 1;
            String category = type.category();
            int categoryWidth = font().width(category);
            canvas.text(font().trim(type.title(), width() - categoryWidth - 14), 8, textY, color("text"), false);
            canvas.text(category, width() - categoryWidth - 2, textY, color("text_dim"), false);
        }
    }
}
