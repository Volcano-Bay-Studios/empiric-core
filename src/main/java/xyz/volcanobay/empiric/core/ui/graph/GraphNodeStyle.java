package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.widget.Draw;

import java.util.List;

/**
 * How a graph looks. Positions are in the node's or view's own
 * coordinates, and a style must not change where ports sit, only how they are drawn.
 */
public interface GraphNodeStyle {

    GraphNodeStyle DEFAULT = new GraphNodeStyle() {
    };

    default void drawBackground(UiCanvas canvas, NodeGraphView view) {
        canvas.fill(0, 0, view.width(), view.height(), view.color("field"));
        float step = view.grid() * view.zoom();
        while (step < 6) {
            step *= 2;
        }
        int line = view.color("track");
        float startX = ((view.panX() % step) + step) % step;
        float startY = ((view.panY() % step) + step) % step;
        for (float x = startX; x < view.width(); x += step) {
            canvas.fill((int) x, 0, (int) x + 1, view.height(), line);
        }
        for (float y = startY; y < view.height(); y += step) {
            canvas.fill(0, (int) y, view.width(), (int) y + 1, line);
        }
    }

    default void drawNode(UiCanvas canvas, NodeWidget node) {
        GraphNodeType type = node.type();
        int title = node.titleHeight();
        Draw.box(canvas, node.theme(), "panel", node.selected() ? "focus" : "panel_border", 0, 0, node.width(), node.height());
        canvas.fill(1, 1, node.width() - 1, title, node.color(type.color()));
        int textY = (title - node.font().lineHeight()) / 2 + 1;
        String icon = node.icon();
        int textX = NodeWidget.PAD;
        if (!icon.isEmpty()) {
            canvas.text(icon, textX, textY, 0xFFFFFFFF, true);
            textX += node.font().width(icon) + 3;
        }
        canvas.text(node.font().trim(type.title(), node.width() - textX - NodeWidget.PAD), textX, textY, 0xFFFFFFFF, true);
        drawPorts(canvas, node, node.inputs(), true);
        drawPorts(canvas, node, type.outputs(), false);
    }

    default void drawAnchor(UiCanvas canvas, NodeWidget node) {
        canvas.fill(0, 0, node.width(), node.height(), node.selected() ? node.color("focus") : node.color("panel_border"));
        canvas.fill(1, 1, node.width() - 1, node.height() - 1, node.color("accent"));
    }

    default void drawPorts(UiCanvas canvas, NodeWidget node, List<GraphPort> ports, boolean input) {
        int text = node.color("text_dim");
        for (int i = 0; i < ports.size(); i++) {
            GraphPort port = ports.get(i);
            int portY = node.portY(i);
            int portX = input ? 0 : node.width();
            drawPort(canvas, node, port, portX, portY);
            int labelY = Math.round(portY - node.font().lineHeight() / 2f);
            if (input) {
                canvas.text(port.label(), NodeWidget.PAD + 2, labelY, text, false);
            } else {
                canvas.text(port.label(), node.width() - NodeWidget.PAD - 2 - node.font().width(port.label()), labelY, text, false);
            }
        }
    }

    default void drawPort(UiCanvas canvas, NodeWidget node, GraphPort port, int x, int y) {
        int half = 3;
        canvas.fill(x - half - 1, y - half - 1, x + half + 1, y + half + 1, 0xFF000000);
        canvas.fill(x - half, y - half, x + half, y + half, LinkWidget.kindColor(port.kind()));
    }
}
