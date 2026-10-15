package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.graph.canvas.client.CanvasViewport;

import dev.robotgryphon.screenlib.client.ui.widget.node.NodeWidget;
import dev.robotgryphon.screenlib.graph.canvas.client.ViewportState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Vector2dc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ViewportToolbar {
    private static final int MARGIN = 6;
    private static final int HEIGHT = 22;
    private static final int BUTTON = 18;
    private static final int GAP = 2;
    private static final int RADIUS = 4;
    private static final int MINIMAP_WIDTH = 128;
    private static final int MINIMAP_HEIGHT = 96;
    private static final int MINIMAP_HEADER = 14;
    private static final int MINIMAP_PADDING = 5;
    private static final double FIT_PADDING = 24;
    private static final float[] ZOOM_PRESETS = {0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 3f, 4f};

    private static final int PANEL_BG = 0xFF1F1F23;
    private static final int PANEL_BORDER = 0xFF3A3B44;
    private static final int MAP_BG = 0xFF17171A;
    private static final int BUTTON_HOVER = 0x22FFFFFF;
    private static final int BUTTON_ACTIVE = 0x33FFFFFF;
    private static final int ICON = 0xFFC8CAD4;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int MINIMAP_NODE = 0xFF5A5D6B;
    private static final int MINIMAP_VIEW = 0xFFE0E0E0;
    private static final int MINIMAP_VIEW_FILL = 0x1FFFFFFF;

    private record Bounds(double minX, double minY, double maxX, double maxY) {
        Bounds union(Bounds other) {
            return new Bounds(Math.min(this.minX, other.minX), Math.min(this.minY, other.minY),
                    Math.max(this.maxX, other.maxX), Math.max(this.maxY, other.maxY));
        }

        Bounds grow(double amount) {
            return new Bounds(this.minX - amount, this.minY - amount, this.maxX + amount, this.maxY + amount);
        }

        double width() {
            return this.maxX - this.minX;
        }

        double height() {
            return this.maxY - this.minY;
        }
    }

    private record MapTransform(Bounds world, ScreenRectangle area, double scale, double originX, double originY) {
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

    private final CanvasWidget canvas;
    private boolean minimapOpen;
    private boolean draggingMinimap;

    public ViewportToolbar(CanvasWidget canvas) {
        this.canvas = canvas;
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static int zoomButtonWidth() {
        return font().width("400%") + 14;
    }

    private ScreenRectangle toolbar() {
        int width = GAP + BUTTON + GAP + 1 + GAP + zoomButtonWidth() + GAP + 1 + GAP + BUTTON + GAP;
        return new ScreenRectangle(this.canvas.getRight() - MARGIN - width,
                this.canvas.getBottom() - MARGIN - HEIGHT, width, HEIGHT);
    }

    private ScreenRectangle fitButton() {
        ScreenRectangle bar = this.toolbar();
        return new ScreenRectangle(bar.left() + GAP, bar.top() + (HEIGHT - BUTTON) / 2, BUTTON, BUTTON);
    }

    private ScreenRectangle zoomButton() {
        ScreenRectangle fit = this.fitButton();
        return new ScreenRectangle(fit.right() + GAP + 1 + GAP, fit.top(), zoomButtonWidth(), BUTTON);
    }

    private ScreenRectangle minimapButton() {
        ScreenRectangle zoom = this.zoomButton();
        return new ScreenRectangle(zoom.right() + GAP + 1 + GAP, zoom.top(), BUTTON, BUTTON);
    }

    private ScreenRectangle minimap() {
        ScreenRectangle bar = this.toolbar();
        return new ScreenRectangle(bar.right() - MINIMAP_WIDTH, bar.top() - GAP * 2 - MINIMAP_HEIGHT,
                MINIMAP_WIDTH, MINIMAP_HEIGHT);
    }

    private ScreenRectangle minimapClose() {
        ScreenRectangle map = this.minimap();
        return new ScreenRectangle(map.right() - MINIMAP_HEADER, map.top(), MINIMAP_HEADER, MINIMAP_HEADER);
    }

    private ScreenRectangle minimapArea() {
        ScreenRectangle map = this.minimap();
        return new ScreenRectangle(map.left() + MINIMAP_PADDING, map.top() + MINIMAP_HEADER,
                map.width() - 2 * MINIMAP_PADDING, map.height() - MINIMAP_HEADER - MINIMAP_PADDING);
    }

    private static boolean contains(ScreenRectangle area, double x, double y) {
        return area.containsPoint(Mth.floor(x), Mth.floor(y));
    }

    public boolean contains(double x, double y) {
        return contains(this.toolbar(), x, y) || (this.minimapOpen && contains(this.minimap(), x, y));
    }

    public boolean mouseClicked(double x, double y, boolean primary) {
        if (!this.contains(x, y)) {
            return false;
        }
        if (!primary) {
            return true;
        }
        if (this.minimapOpen && contains(this.minimap(), x, y)) {
            if (contains(this.minimapClose(), x, y)) {
                this.minimapOpen = false;
            } else if (contains(this.minimapArea(), x, y)) {
                this.draggingMinimap = true;
                this.centerFromMinimap(x, y);
            }
            return true;
        }
        if (contains(this.fitButton(), x, y)) {
            this.fitToContent();
        } else if (contains(this.zoomButton(), x, y)) {
            this.canvas.openMenu(this.zoomMenu());
        } else if (contains(this.minimapButton(), x, y)) {
            this.minimapOpen = !this.minimapOpen;
        }
        return true;
    }

    public boolean mouseDragged(double x, double y) {
        if (!this.draggingMinimap) {
            return false;
        }
        this.centerFromMinimap(x, y);
        return true;
    }

    public boolean mouseReleased() {
        if (!this.draggingMinimap) {
            return false;
        }
        this.draggingMinimap = false;
        return true;
    }

    private ContextMenu zoomMenu() {
        List<ContextMenu.Item> items = new ArrayList<>();
        for (float preset : ZOOM_PRESETS) {
            items.add(new ContextMenu.Item(Component.literal(percent(preset)), () -> this.zoomTo(preset)));
        }
        items.add(new ContextMenu.Item(Component.literal("Fit to content"), this::fitToContent));
        ScreenRectangle zoom = this.zoomButton();
        return ContextMenu.above(zoom.left(), this.toolbar().top() - GAP, items);
    }

    private static String percent(float zoom) {
        return Math.round(zoom * 100) + "%";
    }

    private double centerX() {
        return this.canvas.getX() + this.canvas.getWidth() / 2.0;
    }

    private double centerY() {
        return this.canvas.getY() + this.canvas.getHeight() / 2.0;
    }

    private void zoomTo(float zoom) {
        this.canvas.viewport().zoomAround(zoom, this.centerX(), this.centerY());
    }

    private void centerOn(double worldX, double worldY, float zoom) {
        this.canvas.viewport().apply(new ViewportState(worldX, worldY, zoom),
                this.canvas.getWidth(), this.canvas.getHeight());
    }

    private void fitToContent() {
        Optional<Bounds> content = this.contentBounds();
        if (content.isEmpty()) {
            this.centerOn(0, 0, 1f);
            return;
        }
        Bounds bounds = content.get().grow(FIT_PADDING);
        float zoom = (float) Math.min(this.canvas.getWidth() / bounds.width(), this.canvas.getHeight() / bounds.height());
        zoom = Mth.clamp(zoom, CanvasViewport.MIN_ZOOM, 1f);
        this.centerOn((bounds.minX() + bounds.maxX()) / 2, (bounds.minY() + bounds.maxY()) / 2, zoom);
    }

    private Optional<Bounds> contentBounds() {
        Bounds result = null;
        for (NodeWidget widget : this.canvas.nodeWidgets()) {
            Bounds node = new Bounds(widget.getX(), widget.getY(),
                    widget.getX() + widget.getWidth(), widget.getY() + widget.getHeight());
            result = result == null ? node : result.union(node);
        }
        return Optional.ofNullable(result);
    }

    private Bounds viewBounds() {
        CanvasViewport viewport = this.canvas.viewport();
        Vector2dc min = viewport.screenToCanvas(this.canvas.getX(), this.canvas.getY());
        Vector2dc max = viewport.screenToCanvas(this.canvas.getRight(), this.canvas.getBottom());
        return new Bounds(min.x(), min.y(), max.x(), max.y());
    }

    private MapTransform mapTransform() {
        Bounds view = this.viewBounds();
        Bounds world = this.contentBounds().map(content -> content.grow(FIT_PADDING).union(view)).orElse(view);
        ScreenRectangle area = this.minimapArea();
        double scale = Math.min(area.width() / world.width(), area.height() / world.height());
        double originX = area.left() + (area.width() - world.width() * scale) / 2;
        double originY = area.top() + (area.height() - world.height() * scale) / 2;
        return new MapTransform(world, area, scale, originX, originY);
    }

    private void centerFromMinimap(double x, double y) {
        MapTransform map = this.mapTransform();
        this.centerOn(map.worldX(x), map.worldY(y), this.canvas.viewport().zoom());
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.minimapOpen) {
            this.renderMinimap(graphics, mouseX, mouseY);
        }

        ScreenRectangle bar = this.toolbar();
        panel(graphics, bar);

        ScreenRectangle fit = this.fitButton();
        ScreenRectangle zoom = this.zoomButton();
        ScreenRectangle map = this.minimapButton();

        buttonBackground(graphics, fit, contains(fit, mouseX, mouseY), false);
        fitIcon(graphics, fit);
        separator(graphics, fit.right() + GAP, bar);

        buttonBackground(graphics, zoom, contains(zoom, mouseX, mouseY), false);
        Font font = font();
        String label = percent(this.canvas.viewport().zoom());
        int textY = zoom.top() + (zoom.height() - font.lineHeight) / 2 + 1;
        graphics.text(font, Component.literal(label), zoom.left() + 4, textY, TEXT, false);
        chevron(graphics, zoom.right() - 9, zoom.top() + zoom.height() / 2 - 1);
        separator(graphics, zoom.right() + GAP, bar);

        buttonBackground(graphics, map, contains(map, mouseX, mouseY), this.minimapOpen);
        minimapIcon(graphics, map);

        if (contains(fit, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(Component.literal("Fit to content"), mouseX, mouseY);
        } else if (contains(zoom, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(Component.literal("Zoom"), mouseX, mouseY);
        } else if (contains(map, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(Component.literal("Toggle minimap"), mouseX, mouseY);
        }
    }

    private void renderMinimap(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ScreenRectangle panel = this.minimap();
        panel(graphics, panel);

        Font font = font();
        graphics.text(font, Component.literal("Minimap"), panel.left() + MINIMAP_PADDING,
                panel.top() + (MINIMAP_HEADER - font.lineHeight) / 2 + 1, ICON, false);
        ScreenRectangle close = this.minimapClose();
        if (contains(close, mouseX, mouseY)) {
            graphics.fill(close.left() + 2, close.top() + 2, close.right() - 2, close.bottom() - 2, BUTTON_HOVER);
        }
        cross(graphics, close.left() + (close.width() - 5) / 2, close.top() + (close.height() - 5) / 2);

        ScreenRectangle area = this.minimapArea();
        graphics.fill(area.left(), area.top(), area.right(), area.bottom(), MAP_BG);

        MapTransform map = this.mapTransform();
        for (NodeWidget widget : this.canvas.nodeWidgets()) {
            int x1 = map.screenX(widget.getX());
            int y1 = map.screenY(widget.getY());
            int x2 = Math.max(x1 + 1, map.screenX(widget.getX() + widget.getWidth()));
            int y2 = Math.max(y1 + 1, map.screenY(widget.getY() + widget.getHeight()));
            graphics.fill(x1, y1, x2, y2, MINIMAP_NODE);
        }

        Bounds view = this.viewBounds();
        int vx1 = map.screenX(view.minX());
        int vy1 = map.screenY(view.minY());
        int vx2 = Math.max(vx1 + 2, map.screenX(view.maxX()));
        int vy2 = Math.max(vy1 + 2, map.screenY(view.maxY()));
        graphics.fill(vx1, vy1, vx2, vy2, MINIMAP_VIEW_FILL);
        graphics.outline(vx1, vy1, vx2 - vx1, vy2 - vy1, MINIMAP_VIEW);
    }

    private static void panel(GuiGraphicsExtractor graphics, ScreenRectangle area) {
        RoundedShapes.fillRoundedRect(graphics, area.left(), area.top(), area.right(), area.bottom(), RADIUS, PANEL_BORDER);
        RoundedShapes.fillRoundedRect(graphics, area.left() + 1, area.top() + 1, area.right() - 1, area.bottom() - 1,
                RADIUS - 1, PANEL_BG);
    }

    private static void buttonBackground(GuiGraphicsExtractor graphics, ScreenRectangle area, boolean hovered, boolean active) {
        if (active || hovered) {
            RoundedShapes.fillRoundedRect(graphics, area.left(), area.top(), area.right(), area.bottom(), 3,
                    active ? BUTTON_ACTIVE : BUTTON_HOVER);
        }
    }

    private static void separator(GuiGraphicsExtractor graphics, int x, ScreenRectangle bar) {
        graphics.fill(x, bar.top() + 5, x + 1, bar.bottom() - 5, PANEL_BORDER);
    }

    private static void fitIcon(GuiGraphicsExtractor graphics, ScreenRectangle button) {
        int size = 10;
        int arm = 3;
        int x1 = button.left() + (button.width() - size) / 2;
        int y1 = button.top() + (button.height() - size) / 2;
        int x2 = x1 + size;
        int y2 = y1 + size;
        graphics.fill(x1, y1, x1 + arm, y1 + 1, ICON);
        graphics.fill(x1, y1, x1 + 1, y1 + arm, ICON);
        graphics.fill(x2 - arm, y1, x2, y1 + 1, ICON);
        graphics.fill(x2 - 1, y1, x2, y1 + arm, ICON);
        graphics.fill(x1, y2 - 1, x1 + arm, y2, ICON);
        graphics.fill(x1, y2 - arm, x1 + 1, y2, ICON);
        graphics.fill(x2 - arm, y2 - 1, x2, y2, ICON);
        graphics.fill(x2 - 1, y2 - arm, x2, y2, ICON);
        graphics.fill(x1 + 4, y1 + 4, x1 + 6, y1 + 6, ICON);
    }

    private static void minimapIcon(GuiGraphicsExtractor graphics, ScreenRectangle button) {
        int width = 11;
        int height = 9;
        int x = button.left() + (button.width() - width) / 2;
        int y = button.top() + (button.height() - height) / 2;
        graphics.outline(x, y, width, height, ICON);
        graphics.fill(x + 5, y + 4, x + 9, y + 7, ICON);
    }

    private static void chevron(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 5, y + 1, ICON);
        graphics.fill(x + 1, y + 1, x + 4, y + 2, ICON);
        graphics.fill(x + 2, y + 2, x + 3, y + 3, ICON);
    }

    private static void cross(GuiGraphicsExtractor graphics, int x, int y) {
        for (int i = 0; i < 5; i++) {
            graphics.fill(x + i, y + i, x + i + 1, y + i + 1, ICON);
            graphics.fill(x + 4 - i, y + i, x + 5 - i, y + i + 1, ICON);
        }
    }
}
