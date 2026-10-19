package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.ui.ContextEntry;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiPoint;
import xyz.volcanobay.empiric.core.ui.widget.ColorPicker;
import xyz.volcanobay.empiric.core.ui.Widget;

/**
 * One link of a {@link NodeGraphView}, drawn as a curve between its ports behind the nodes. Its hit area is the curve
 * itself, so it can be hovered and right clicked to delete.
 */
public class LinkWidget extends Widget {

    private static final int SEGMENTS = 24;
    private static final float MARGIN = 4;
    private static final float REACH = 3.5f;

    private final NodeGraphView view;
    private final GraphLink link;
    private float startX;
    private float startY;
    private float endX;
    private float endY;
    private String kind = GraphPort.ANY;

    LinkWidget(NodeGraphView view, GraphLink link) {
        this.view = view;
        this.link = link;
        layer(-1);
        contextEntry(ContextEntry.of("Delete link", () -> {
            if (view.canEdit()) {
                view.removeLink(link);
            }
        }));
    }

    public GraphLink link() {
        return link;
    }

    void place() {
        NodeWidget from = view.nodeWidget(link.from);
        NodeWidget to = view.nodeWidget(link.to);
        GraphPort output = from == null ? null : from.type().output(link.output);
        GraphPort input = to == null ? null : to.type().input(link.input);
        if (output == null || input == null) {
            setBounds(0, 0, 0, 0);
            return;
        }
        kind = output.kind();
        UiPoint start = from.portPoint(output, false);
        UiPoint end = to.portPoint(input, true);
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i <= SEGMENTS; i++) {
            float[] point = point((float) start.x(), (float) start.y(), (float) end.x(), (float) end.y(), i / (float) SEGMENTS);
            minX = Math.min(minX, point[0]);
            minY = Math.min(minY, point[1]);
            maxX = Math.max(maxX, point[0]);
            maxY = Math.max(maxY, point[1]);
        }
        int left = (int) Math.floor(minX - MARGIN);
        int top = (int) Math.floor(minY - MARGIN);
        startX = (float) start.x() - left;
        startY = (float) start.y() - top;
        endX = (float) end.x() - left;
        endY = (float) end.y() - top;
        setBounds(left, top, (int) Math.ceil(maxX + MARGIN) - left, (int) Math.ceil(maxY + MARGIN) - top);
    }

    @Override
    protected boolean interactive() {
        return true;
    }

    @Override
    public boolean contains(double x, double y) {
        if (!super.contains(x, y)) {
            return false;
        }
        float[] previous = point(startX, startY, endX, endY, 0);
        for (int i = 1; i <= SEGMENTS; i++) {
            float[] next = point(startX, startY, endX, endY, i / (float) SEGMENTS);
            if (distance(x, y, previous, next) <= REACH) {
                return true;
            }
            previous = next;
        }
        return false;
    }

    private static double distance(double x, double y, float[] a, float[] b) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double length = dx * dx + dy * dy;
        double t = length == 0 ? 0 : Math.max(0, Math.min(1, ((x - a[0]) * dx + (y - a[1]) * dy) / length));
        double px = a[0] + t * dx - x;
        double py = a[1] + t * dy - y;
        return Math.sqrt(px * px + py * py);
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        curve(canvas, startX, startY, endX, endY, isHovered() ? 2.5f : 1.5f, kindColor(kind));
    }

    /**
     * Draws a link-shaped curve that leaves the output to the right and enters the input from the left.
     */
    public static void curve(UiCanvas canvas, float x0, float y0, float x1, float y1, float thickness, int color) {
        float[] previous = point(x0, y0, x1, y1, 0);
        for (int i = 1; i <= SEGMENTS; i++) {
            float[] next = point(x0, y0, x1, y1, i / (float) SEGMENTS);
            canvas.line(previous[0], previous[1], next[0], next[1], thickness, color);
            previous = next;
        }
    }

    private static float[] point(float x0, float y0, float x1, float y1, float t) {
        float reach = Math.max(24, Math.abs(x1 - x0) * 0.5f);
        float u = 1 - t;
        float x = u * u * u * x0 + 3 * u * u * t * (x0 + reach) + 3 * u * t * t * (x1 - reach) + t * t * t * x1;
        float y = u * u * u * y0 + 3 * u * u * t * y0 + 3 * u * t * t * y1 + t * t * t * y1;
        return new float[]{x, y};
    }

    /**
     * A steady colour per port kind, so matching ports and their links read at a glance.
     */
    public static int kindColor(String kind) {
        if (kind.equals(GraphPort.ANY)) {
            return 0xFFD0D0D0;
        }
        float hue = kind.hashCode() * 0.618034f;
        return 0xFF000000 | ColorPicker.toRgb(hue - (float) Math.floor(hue), 0.55f, 0.95f);
    }
}
