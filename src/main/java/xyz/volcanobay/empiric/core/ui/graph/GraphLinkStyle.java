package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.ui.UiCanvas;
import xyz.volcanobay.empiric.core.ui.UiTheme;

/**
 * How links are routed and drawn.
 */
public interface GraphLinkStyle {

    GraphLinkStyle DEFAULT = new GraphLinkStyle() {
    };

    default float[] route(float x0, float y0, float x1, float y1, int grid) {
        return LinkWidget.bezier(x0, y0, x1, y1);
    }

    /**
     * Routes around node rectangles, given as a flat {@code x, y, width, height, ...} array in graph space.
     */
    default float[] route(float x0, float y0, float x1, float y1, int grid, float[] obstacles) {
        return route(x0, y0, x1, y1, grid);
    }

    default int color(String kind, boolean hovered, UiTheme theme) {
        return LinkWidget.kindColor(kind);
    }

    default void draw(UiCanvas canvas, float[] points, int color, boolean hovered) {
        float thickness = hovered ? 2.5f : 1.5f;
        for (int i = 2; i < points.length; i += 2) {
            canvas.line(points[i - 2], points[i - 1], points[i], points[i + 1], thickness, color);
        }
    }
}
