package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.ui.ContextEntry;
import xyz.volcanobay.empiric.core.ui.Keys;
import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiPoint;
import xyz.volcanobay.empiric.core.ui.widget.ColorPicker;
import xyz.volcanobay.empiric.core.ui.Widget;

/**
 * One link of a {@link NodeGraphView}, drawn along the route its {@link GraphLinkStyle} gives between its ports, behind
 * the nodes. Its hit area is the route itself, so it can be hovered and right clicked to delete.
 */
public class LinkWidget extends Widget {

    private static final int SEGMENTS = 24;
    private static final float MARGIN = 4;
    private static final float REACH = 3.5f;

    private final NodeGraphView view;
    private final GraphLink link;
    private float[] points = new float[0];
    private String kind = GraphPort.ANY;
    private long lastDown;
    private float lastX;
    private float lastY;

    LinkWidget(NodeGraphView view, GraphLink link) {
        this.view = view;
        this.link = link;
        layer(-2);
        contextEntry(ContextEntry.of("Add anchor", () -> {
            if (view.canEdit()) {
                view.splitLink(link, x() + lastX, y() + lastY);
            }
        }));
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
        if (output == null || input == null || !to.shows(input)) {
            setBounds(0, 0, 0, 0);
            return;
        }
        kind = output.kind();
        UiPoint start = from.portPoint(output, false);
        UiPoint end = to.portPoint(input, true);
        float[] route = view.linkStyle().route((float) start.x(), (float) start.y(), (float) end.x(), (float) end.y(), view.grid(), view.obstacles());
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < route.length; i += 2) {
            minX = Math.min(minX, route[i]);
            minY = Math.min(minY, route[i + 1]);
            maxX = Math.max(maxX, route[i]);
            maxY = Math.max(maxY, route[i + 1]);
        }
        int left = (int) Math.floor(minX - MARGIN);
        int top = (int) Math.floor(minY - MARGIN);
        points = new float[route.length];
        for (int i = 0; i < route.length; i += 2) {
            points[i] = route[i] - left;
            points[i + 1] = route[i + 1] - top;
        }
        setBounds(left, top, (int) Math.ceil(maxX + MARGIN) - left, (int) Math.ceil(maxY + MARGIN) - top);
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        long now = System.currentTimeMillis();
        boolean twice = button == Keys.LEFT_BUTTON && now - lastDown < 350 && Math.abs(x - lastX) < 4 && Math.abs(y - lastY) < 4;
        lastDown = now;
        lastX = (float) x;
        lastY = (float) y;
        if (twice && view.canEdit()) {
            lastDown = 0;
            view.splitLink(link, this.x() + lastX, this.y() + lastY);
            return true;
        }
        return false;
    }

    @Override
    protected void tick() {
        int want = isHovered() ? -1 : -2;
        if (layer() != want) {
            layer(want);
        }
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
        for (int i = 2; i < points.length; i += 2) {
            if (distance(x, y, points[i - 2], points[i - 1], points[i], points[i + 1]) <= REACH) {
                return true;
            }
        }
        return false;
    }

    private static double distance(double x, double y, float ax, float ay, float bx, float by) {
        double dx = bx - ax;
        double dy = by - ay;
        double length = dx * dx + dy * dy;
        double t = length == 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / length));
        double px = ax + t * dx - x;
        double py = ay + t * dy - y;
        return Math.sqrt(px * px + py * py);
    }

    @Override
    protected void draw(UiCanvas canvas, float partialTick) {
        GraphLinkStyle style = view.linkStyle();
        style.draw(canvas, points, style.color(kind, isHovered(), theme()), isHovered());
    }

    static float[] bezier(float x0, float y0, float x1, float y1) {
        float[] route = new float[(SEGMENTS + 1) * 2];
        float reach = Math.max(24, Math.abs(x1 - x0) * 0.5f);
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = i / (float) SEGMENTS;
            float u = 1 - t;
            route[i * 2] = u * u * u * x0 + 3 * u * u * t * (x0 + reach) + 3 * u * t * t * (x1 - reach) + t * t * t * x1;
            route[i * 2 + 1] = u * u * u * y0 + 3 * u * u * t * y0 + 3 * u * t * t * y1 + t * t * t * y1;
        }
        return route;
    }

    private static final java.util.Map<String, Integer> KIND_COLORS = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Picks the colour of a port kind, instead of the one made from its name.
     */
    public static void kindColor(String kind, int argb) {
        KIND_COLORS.put(kind, argb);
    }

    /**
     * A steady colour per port kind, so matching ports and their links read at a glance.
     */
    public static int kindColor(String kind) {
        Integer chosen = KIND_COLORS.get(kind);
        if (chosen != null) {
            return chosen;
        }
        if (kind.equals(GraphPort.ANY)) {
            return 0xFFD0D0D0;
        }
        float hue = kind.hashCode() * 0.618034f;
        return 0xFF000000 | ColorPicker.toRgb(hue - (float) Math.floor(hue), 0.55f, 0.95f);
    }
}
