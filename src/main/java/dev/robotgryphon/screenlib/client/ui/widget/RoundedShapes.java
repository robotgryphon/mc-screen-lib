package dev.robotgryphon.screenlib.client.ui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Stateless rasterizer for rounded-corner rectangles. The renderer's
 * native primitives are flat-edged {@code fill} and {@code outline}; this
 * class adds three small additions on top of them — a fully-rounded fill,
 * a top-only-rounded fill (useful for headers that abut a flat body
 * below), and a fully-rounded 1-pixel outline.
 *
 * <p>The corner shape is a pixel-art quarter-circle: each pixel in the
 * radius × radius corner box is included in the fill only when its center
 * point sits within the corner radius of the rounded-arc center. For the
 * outline, the leftmost / topmost "inside" pixel per row and per column
 * is what gets drawn, so the perimeter stays connected without
 * any gaps where a single-axis sweep would have left them.
 *
 * <p>Radii are clamped to {@code min(width, height) / 2} so callers can
 * pass an oversized value without breaking the math; passing 0 short-
 * circuits to the equivalent flat-edge call.
 */
public final class RoundedShapes {

    private RoundedShapes() {}

    /**
     * Fills the rectangle from ({@code x1}, {@code y1}) (inclusive) to
     * ({@code x2}, {@code y2}) (exclusive) with the four corners rounded
     * to {@code radius} pixels.
     */
    public static void fillRoundedRect(GuiGraphicsExtractor g,
                                       int x1, int y1, int x2, int y2,
                                       int radius, int color) {
        int w = x2 - x1;
        int h = y2 - y1;
        radius = clampRadius(radius, w, h);
        if (radius == 0) {
            g.fill(x1, y1, x2, y2, color);
            return;
        }

        // Three flat strips. The middle band is full-width; the top and
        // bottom bands span only the non-corner middle so the corner
        // boxes are left untouched for the quarter-circle fill.
        g.fill(x1, y1 + radius, x2, y2 - radius, color);
        g.fill(x1 + radius, y1, x2 - radius, y1 + radius, color);
        g.fill(x1 + radius, y2 - radius, x2 - radius, y2, color);

        // Four corner quarter-circles.
        fillCorner(g, x1,          y1,          radius, color, +1, +1);
        fillCorner(g, x2 - radius, y1,          radius, color, -1, +1);
        fillCorner(g, x1,          y2 - radius, radius, color, +1, -1);
        fillCorner(g, x2 - radius, y2 - radius, radius, color, -1, -1);
    }

    /**
     * Like {@link #fillRoundedRect} but only the top two corners are
     * rounded; the bottom edge stays flat. Used by the title bar so it
     * butts up cleanly against a separator line / body below.
     */
    public static void fillTopRoundedRect(GuiGraphicsExtractor g,
                                          int x1, int y1, int x2, int y2,
                                          int radius, int color) {
        int w = x2 - x1;
        int h = y2 - y1;
        radius = clampRadius(radius, w, h);
        if (radius == 0) {
            g.fill(x1, y1, x2, y2, color);
            return;
        }

        // Bottom band (down to y2) is full-width; only the very top
        // corners get the rounded treatment.
        g.fill(x1, y1 + radius, x2, y2, color);
        g.fill(x1 + radius, y1, x2 - radius, y1 + radius, color);

        fillCorner(g, x1,          y1, radius, color, +1, +1);
        fillCorner(g, x2 - radius, y1, radius, color, -1, +1);
    }

    /**
     * 1-pixel rounded outline running ({@code x1}, {@code y1}) inclusive
     * to ({@code x2}, {@code y2}) exclusive. Straight edges between the
     * corners, quarter-circle arcs at the corners.
     */
    public static void outlineRoundedRect(GuiGraphicsExtractor g,
                                          int x1, int y1, int x2, int y2,
                                          int radius, int color) {
        int w = x2 - x1;
        int h = y2 - y1;
        radius = clampRadius(radius, w, h);
        if (radius == 0) {
            // Standard square outline — emulate via four 1-pixel strips
            // because the surrounding helpers don't all share the same
            // exclusive/inclusive convention.
            g.fill(x1, y1, x2, y1 + 1, color);
            g.fill(x1, y2 - 1, x2, y2, color);
            g.fill(x1, y1, x1 + 1, y2, color);
            g.fill(x2 - 1, y1, x2, y2, color);
            return;
        }

        // Straight edges between the corner arcs.
        g.fill(x1 + radius, y1,         x2 - radius, y1 + 1,     color); // top
        g.fill(x1 + radius, y2 - 1,     x2 - radius, y2,         color); // bottom
        g.fill(x1,          y1 + radius, x1 + 1,     y2 - radius, color); // left
        g.fill(x2 - 1,      y1 + radius, x2,         y2 - radius, color); // right

        // Corner arcs.
        outlineCorner(g, x1,          y1,          radius, color, +1, +1);
        outlineCorner(g, x2 - radius, y1,          radius, color, -1, +1);
        outlineCorner(g, x1,          y2 - radius, radius, color, +1, -1);
        outlineCorner(g, x2 - radius, y2 - radius, radius, color, -1, -1);
    }

    /**
     * Fills the quarter-circle corner block whose outer corner sits at
     * ({@code x}, {@code y}). {@code signX} / {@code signY} say which way
     * the corner curves in (+1 toward higher coords, -1 toward lower).
     */
    private static void fillCorner(GuiGraphicsExtractor g,
                                   int x, int y, int radius, int color,
                                   int signX, int signY) {
        // Pivot is the arc center — pulled "inward" by `radius` along
        // each sign axis. A pixel center at (px+0.5, py+0.5) is inside
        // the rounded shape when it's within radius of the pivot.
        double rr = (double) radius * radius;
        for (int v = 0; v < radius; v++) {
            for (int u = 0; u < radius; u++) {
                double dx = radius - u - 0.5;
                double dy = radius - v - 0.5;
                if (dx * dx + dy * dy > rr) continue;
                // The sign-axis flip means we render the same quarter-
                // circle but reflected for the three non-TL corners.
                int px = x + (signX > 0 ? u : (radius - 1 - u));
                int py = y + (signY > 0 ? v : (radius - 1 - v));
                g.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    /**
     * 1-pixel outline of the corner arc. Drawn as the leftmost-inside
     * pixel per row plus the topmost-inside pixel per column; the union
     * keeps the perimeter connected without a sweep direction biasing it.
     */
    private static void outlineCorner(GuiGraphicsExtractor g,
                                      int x, int y, int radius, int color,
                                      int signX, int signY) {
        double rr = (double) radius * radius;

        // Per-row: smallest u such that (radius-u-0.5)² + (radius-v-0.5)² <= rr.
        for (int v = 0; v < radius; v++) {
            double dy = radius - v - 0.5;
            double maxDx = Math.sqrt(rr - dy * dy);
            int uMin = (int) Math.ceil(radius - 0.5 - maxDx);
            uMin = Math.max(0, Math.min(uMin, radius - 1));
            int px = x + (signX > 0 ? uMin : (radius - 1 - uMin));
            int py = y + (signY > 0 ? v : (radius - 1 - v));
            g.fill(px, py, px + 1, py + 1, color);
        }
        // Per-column: smallest v that's inside. Catches the rows where
        // the per-row sweep already chose a non-zero u — without this,
        // the topmost slice (one or two pixels along the y axis) would
        // be missing.
        for (int u = 0; u < radius; u++) {
            double dx = radius - u - 0.5;
            double maxDy = Math.sqrt(rr - dx * dx);
            int vMin = (int) Math.ceil(radius - 0.5 - maxDy);
            vMin = Math.max(0, Math.min(vMin, radius - 1));
            int px = x + (signX > 0 ? u : (radius - 1 - u));
            int py = y + (signY > 0 ? vMin : (radius - 1 - vMin));
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    private static int clampRadius(int radius, int w, int h) {
        if (radius < 0) return 0;
        int half = Math.min(w, h) / 2;
        return Math.min(radius, half);
    }
}
