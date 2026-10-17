package dev.robotgryphon.screenlib.nodes.client.widget.node;

import dev.robotgryphon.screenlib.nodes.client.NodeRenderer;
import dev.robotgryphon.screenlib.ui.api.render.RoundedRectRenderState;
import dev.robotgryphon.screenlib.ui.api.render.RoundedRect;
import dev.robotgryphon.screenlib.nodes.client.canvas.CanvasViewport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Vector2dc;

import java.util.ArrayList;
import java.util.List;

public final class NodeBackgrounds {
    private NodeBackgrounds() {}

    public static void nodes(GuiGraphicsExtractor graphics,
                             CanvasViewport viewport,
                             List<NodeWidget> nodes,
                             boolean dropShadow) {
        if (nodes.isEmpty()) {
            return;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (NodeWidget widget : nodes) {
            ScreenRectangle b = widget.screenBounds(viewport);
            minX = Math.min(minX, b.left());
            minY = Math.min(minY, b.top());
            maxX = Math.max(maxX, b.right());
            maxY = Math.max(maxY, b.bottom());
        }
        int basePad = 1;
        int shadowPad = dropShadow
                ? Math.max(2, Math.round(NodeWidget.NODE_CORNER_RADIUS * viewport.zoom() * 2f))
                : 0;
        boolean anyGlow = false;
        for (NodeWidget widget : nodes) {
            if (widget.hasVisibleGlow()) {
                anyGlow = true;
                break;
            }
        }
        int glowPad = anyGlow
                ? Math.max(2, Math.round(NodeWidget.NODE_CORNER_RADIUS * viewport.zoom()))
                : 0;
        int leftPad = basePad + Math.max(shadowPad, glowPad);
        int topPad = basePad + Math.max(shadowPad, glowPad);
        int rightPad = basePad + Math.max(shadowPad, glowPad);
        int bottomPad = basePad + Math.max(shadowPad, glowPad);
        minX -= leftPad; minY -= topPad;
        maxX += rightPad; maxY += bottomPad;
        ScreenRectangle textureBounds = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY);

        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        float cornerRadiusScaled = NodeWidget.NODE_CORNER_RADIUS * viewport.zoom() * guiScale;
        float featherScaled = 1f;
        float borderThicknessScaled = Math.max(1f,
                0.5f * viewport.zoom() * guiScale);

        List<RoundedRect> entries = new ArrayList<>(nodes.size());
        for (NodeWidget widget : nodes) {
            entries.add(widget.buildBackgroundEntry(viewport, minX, minY, guiScale, dropShadow));
        }

        var state = new RoundedRectRenderState(textureBounds, entries,
                cornerRadiusScaled, featherScaled, borderThicknessScaled, graphics.peekScissorStack());
        graphics.submitPictureInPictureRenderState(state);
    }

    public static void ports(GuiGraphicsExtractor graphics,
                             CanvasViewport viewport,
                             List<NodeWidget> nodes,
                             Vector2dc canvasMouse) {
        if (nodes.isEmpty()) return;

        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        float radiusScaled = NodeRenderer.PORT_RADIUS * viewport.zoom() * guiScale;
        float hoverRadiusScaled = radiusScaled * NodePortsRenderer.HOVER_RADIUS_FACTOR;
        float featherScaled = 1f;
        float borderThicknessScaled = Math.max(1f, radiusScaled * 0.2f);

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (NodeWidget widget : nodes) {
            ScreenRectangle b = widget.screenBounds(viewport);
            minX = Math.min(minX, b.left());
            minY = Math.min(minY, b.top());
            maxX = Math.max(maxX, b.right());
            maxY = Math.max(maxY, b.bottom());
        }
        int pad = (int) Math.ceil(hoverRadiusScaled / guiScale) + 1;
        minX -= pad; minY -= pad;
        maxX += pad; maxY += pad;
        ScreenRectangle textureBounds = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY);

        List<RoundedRect> entries = new ArrayList<>();
        for (NodeWidget widget : nodes) {
            widget.ports().appendPortEntries(viewport, minX, minY, guiScale,
                    canvasMouse.x(), canvasMouse.y(), entries);
        }
        if (entries.isEmpty()) return;

        int max = RoundedRectRenderState.MAX_ENTRIES;
        for (int from = 0; from < entries.size(); from += max) {
            int to = Math.min(from + max, entries.size());
            List<RoundedRect> chunk = entries.subList(from, to);
            var state = new RoundedRectRenderState(textureBounds, List.copyOf(chunk),
                    radiusScaled, featherScaled, borderThicknessScaled, graphics.peekScissorStack());
            graphics.submitPictureInPictureRenderState(state);
        }
    }
}
