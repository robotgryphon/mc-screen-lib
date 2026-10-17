package dev.robotgryphon.screenlib.nodes.client.widget.canvas;

import dev.robotgryphon.screenlib.nodes.client.widget.CanvasWidget;
import dev.robotgryphon.screenlib.nodes.client.widget.node.NodeWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;

public final class CanvasMinimap {
    private static final double PADDING = 24;
    private static final int BACKGROUND = 0xFF17171A;
    private static final int NODE = 0xFF5A5D6B;
    private static final int VIEW = 0xFFE0E0E0;
    private static final int VIEW_FILL = 0x1FFFFFFF;

    private CanvasMinimap() {}

    private record Transform(CanvasBounds world, double scale, double originX, double originY) {
        static Transform of(CanvasWidget canvas, ScreenRectangle area) {
            CanvasBounds view = canvas.viewBounds();
            CanvasBounds world = canvas.contentBounds().map(content -> content.grow(PADDING).union(view)).orElse(view);
            double scale = Math.min(area.width() / world.width(), area.height() / world.height());
            return new Transform(world, scale,
                    area.left() + (area.width() - world.width() * scale) / 2,
                    area.top() + (area.height() - world.height() * scale) / 2);
        }

        int screenX(double worldX) {
            return Mth.floor(this.originX + (worldX - this.world.minX()) * this.scale);
        }

        int screenY(double worldY) {
            return Mth.floor(this.originY + (worldY - this.world.minY()) * this.scale);
        }

        double worldX(double screenX) {
            return this.world.minX() + (screenX - this.originX) / this.scale;
        }

        double worldY(double screenY) {
            return this.world.minY() + (screenY - this.originY) / this.scale;
        }
    }

    public static void centerOn(CanvasWidget canvas, ScreenRectangle area, double x, double y) {
        Transform map = Transform.of(canvas, area);
        canvas.centerOn(map.worldX(x), map.worldY(y), canvas.viewport().zoom());
    }

    public static void render(GuiGraphicsExtractor graphics, ScreenRectangle area, CanvasWidget canvas) {
        graphics.fill(area.left(), area.top(), area.right(), area.bottom(), BACKGROUND);

        Transform map = Transform.of(canvas, area);
        for (NodeWidget widget : canvas.nodeWidgets()) {
            int x1 = map.screenX(widget.getX());
            int y1 = map.screenY(widget.getY());
            int x2 = Math.max(x1 + 1, map.screenX(widget.getX() + widget.getWidth()));
            int y2 = Math.max(y1 + 1, map.screenY(widget.getY() + widget.getHeight()));
            graphics.fill(x1, y1, x2, y2, NODE);
        }

        CanvasBounds view = canvas.viewBounds();
        int vx1 = map.screenX(view.minX());
        int vy1 = map.screenY(view.minY());
        int vx2 = Math.max(vx1 + 2, map.screenX(view.maxX()));
        int vy2 = Math.max(vy1 + 2, map.screenY(view.maxY()));
        graphics.fill(vx1, vy1, vx2, vy2, VIEW_FILL);
        graphics.outline(vx1, vy1, vx2 - vx1, vy2 - vy1, VIEW);
    }
}
