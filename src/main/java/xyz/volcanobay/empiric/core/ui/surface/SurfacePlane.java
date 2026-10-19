package xyz.volcanobay.empiric.core.ui.surface;

/**
 * A rectangle in world space that widgets are drawn onto. {@code origin} is the top left corner, {@code right} runs along
 * the whole width and {@code down} along the whole height, so the surface can sit under any transform. It faces the
 * side {@code down x right} points to, which is where a viewer sees the corners run left to right. A pointer ray that
 * crosses it becomes a position in the surface's own pixels, the same space a mouse cursor has on a screen.
 */
public record SurfacePlane(double originX, double originY, double originZ,
                           double rightX, double rightY, double rightZ,
                           double downX, double downY, double downZ,
                           int pixelWidth, int pixelHeight) {

    /**
     * Where a ray crosses the plane. {@code distance} is in ray lengths, {@code x} and {@code y} are surface pixels.
     */
    public record Hit(double x, double y, double distance, boolean inside) {
    }



    public Hit intersect(double ox, double oy, double oz, double dx, double dy, double dz) {
        double nx = downY * rightZ - downZ * rightY;
        double ny = downZ * rightX - downX * rightZ;
        double nz = downX * rightY - downY * rightX;
        double facing = nx * dx + ny * dy + nz * dz;
        if (Math.abs(facing) < 1.0e-12) {
            return null;
        }
        double distance = (nx * (originX - ox) + ny * (originY - oy) + nz * (originZ - oz)) / facing;
        if (distance < 0 || facing > 0) {
            return null;
        }
        double px = ox + dx * distance - originX;
        double py = oy + dy * distance - originY;
        double pz = oz + dz * distance - originZ;
        double u = (px * rightX + py * rightY + pz * rightZ) / (rightX * rightX + rightY * rightY + rightZ * rightZ);
        double v = (px * downX + py * downY + pz * downZ) / (downX * downX + downY * downY + downZ * downZ);
        return new Hit(u * pixelWidth, v * pixelHeight, distance, u >= 0 && u < 1 && v >= 0 && v < 1);
    }

    /**
     * Whether a point is on the side of the plane that shows the widgets. The back is never drawn or pointed at.
     */
    public boolean facesPoint(double x, double y, double z) {
        double nx = downY * rightZ - downZ * rightY;
        double ny = downZ * rightX - downX * rightZ;
        double nz = downX * rightY - downY * rightX;
        return nx * (x - originX) + ny * (y - originY) + nz * (z - originZ) > 0;
    }

    /**
     * The world position of a point in surface pixels.
     */
    public double[] toWorld(double x, double y) {
        double u = x / pixelWidth;
        double v = y / pixelHeight;
        return new double[]{
                originX + rightX * u + downX * v,
                originY + rightY * u + downY * v,
                originZ + rightZ * u + downZ * v
        };
    }

    /**
     * The surface facing a viewer looking along {@code +z}'s opposite. Both axes are given per block, so a
     * {@code width} of 2 spans two blocks along {@code right}.
     */
    public static SurfacePlane of(double[] origin, double[] right, double[] down, double width, double height, int pixelWidth, int pixelHeight) {
        return new SurfacePlane(origin[0], origin[1], origin[2],
                right[0] * width, right[1] * width, right[2] * width,
                down[0] * height, down[1] * height, down[2] * height,
                pixelWidth, pixelHeight);
    }
}
