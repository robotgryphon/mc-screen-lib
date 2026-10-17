package dev.robotgryphon.screenlib.ui.api.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class RoundedShapes {
    private RoundedShapes() {}

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

        g.fill(x1, y1 + radius, x2, y2 - radius, color);
        g.fill(x1 + radius, y1, x2 - radius, y1 + radius, color);
        g.fill(x1 + radius, y2 - radius, x2 - radius, y2, color);

        fillCorner(g, x1,          y1,          radius, color, +1, +1);
        fillCorner(g, x2 - radius, y1,          radius, color, -1, +1);
        fillCorner(g, x1,          y2 - radius, radius, color, +1, -1);
        fillCorner(g, x2 - radius, y2 - radius, radius, color, -1, -1);
    }

    private static void fillCorner(GuiGraphicsExtractor g,
                                   int x, int y, int radius, int color,
                                   int signX, int signY) {
        double rr = (double) radius * radius;
        for (int v = 0; v < radius; v++) {
            for (int u = 0; u < radius; u++) {
                double dx = radius - u - 0.5;
                double dy = radius - v - 0.5;
                if (dx * dx + dy * dy > rr) continue;
                int px = x + (signX > 0 ? u : (radius - 1 - u));
                int py = y + (signY > 0 ? v : (radius - 1 - v));
                g.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    private static int clampRadius(int radius, int w, int h) {
        if (radius < 0) return 0;
        int half = Math.min(w, h) / 2;
        return Math.min(radius, half);
    }
}
