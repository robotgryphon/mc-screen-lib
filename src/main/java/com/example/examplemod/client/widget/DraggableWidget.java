package com.example.examplemod.client.widget;

import com.example.examplemod.graph.NodeConnection.NodeSide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A panel-style widget that the user can either grab and drag, or use as a
 * connection node. Each widget exposes a list of {@link Port}s; clicking on
 * the body itself drags the widget, while clicking on a port is meant to be
 * intercepted by the parent screen to start a connection between two widgets.
 *
 * <p>Multiple ports on the same side are distributed evenly along that side's
 * body extent (the area below the title bar).
 */
public class DraggableWidget extends AbstractWidget {

    private static final int BACKGROUND_COLOR = 0xCC1F1F23;
    private static final int BACKGROUND_HOVER_COLOR = 0xCC2A2A33;
    private static final int BORDER_COLOR = 0xFF7F7F8C;
    private static final int BORDER_DRAG_COLOR = 0xFFFFD24A;
    private static final int TITLE_BAR_COLOR = 0xFF3A3A45;
    private static final int TITLE_BAR_HEIGHT = 12;

    private static final int PORT_RADIUS = 3;
    private static final int PORT_HIT_RADIUS = 5;
    private static final int PORT_LABEL_GAP = 3;
    private static final int PORT_FILL_COLOR = 0xFF8FA0FF;
    private static final int PORT_HOVER_COLOR = 0xFFFFD24A;
    private static final int PORT_OUTLINE_COLOR = 0xFF101012;

    private final Port[] ports;
    /** Ports grouped by side, in declaration order — used to compute layout positions. */
    private final Map<NodeSide, List<Port>> portsBySide;

    private boolean dragging;
    /** Offset from the widget's top-left to the mouse when the body drag started. */
    private double grabOffsetX;
    private double grabOffsetY;

    public DraggableWidget(int x, int y, int width, int height, Component title, Port[] ports) {
        super(x, y, width, height, title);
        this.ports = ports;
        this.portsBySide = groupBySide(ports);
    }

    private static Map<NodeSide, List<Port>> groupBySide(Port[] ports) {
        Map<NodeSide, List<Port>> map = new EnumMap<>(NodeSide.class);
        for (Port p : ports) {
            map.computeIfAbsent(p.side(), k -> new ArrayList<>()).add(p);
        }
        return map;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public Port[] ports() {
        return this.ports;
    }

    /**
     * The on-screen center of the given port. Ports on the same side share the
     * body extent equally: with N ports on a side, the i-th port (0-indexed)
     * sits at (i+1)/(N+1) along the body.
     */
    public Vector2fc portCenter(Port port) {
        List<Port> sidePorts = this.portsBySide.get(port.side());
        if (sidePorts == null) {
            throw new IllegalArgumentException("Port not on this widget: " + port);
        }
        // Reference equality so duplicate-equals records don't collide.
        int index = -1;
        for (int i = 0; i < sidePorts.size(); i++) {
            if (sidePorts.get(i) == port) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new IllegalArgumentException("Port not on this widget: " + port);
        }
        int count = sidePorts.size();
        float t = (index + 1f) / (count + 1f);

        // Reserve the title-bar slice; ports lay out within the body region.
        int bodyTop = this.getY() + TITLE_BAR_HEIGHT;
        int bodyHeight = this.getHeight() - TITLE_BAR_HEIGHT;
        int left = this.getX();
        int width = this.getWidth();

        switch (port.side()) {
            case LEFT -> {
                return new Vector2f(left, bodyTop + bodyHeight * t);
            }
            case RIGHT -> {
                return new Vector2f(left + width, bodyTop + bodyHeight * t);
            }
            default -> throw new IllegalStateException("Unknown side: " + port.side());
        }
    }

    /**
     * The point a connection line should attach to, sitting just outside the
     * visible port square so the curve doesn't overdraw the port. Matches the
     * edge of the rendered port box plus 1 px of breathing room.
     */
    public Vector2fc portAttachment(Port port) {
        Vector2fc center = this.portCenter(port);
        int offset = PORT_RADIUS + 1;
        switch (port.side()) {
            case LEFT -> {
                return new Vector2f(center.x() - offset, center.y());
            }
            case RIGHT -> {
                return new Vector2f(center.x() + offset, center.y());
            }
            default -> throw new IllegalStateException("Unknown side: " + port.side());
        }
    }

    /**
     * If ({@code mouseX}, {@code mouseY}) lands on one of this widget's ports,
     * return that port; otherwise {@code null}. The hit radius is a few pixels
     * larger than the visible port so it's easy to grab.
     */
    public @Nullable Port portAt(double mouseX, double mouseY) {
        for (Port p : this.ports) {
            Vector2fc center = this.portCenter(p);
            double dx = center.x() - mouseX;
            double dy = center.y() - mouseY;
            if (dx * dx + dy * dy <= PORT_HIT_RADIUS * PORT_HIT_RADIUS) {
                return p;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Don't claim clicks that landed on a port — those are for the screen
        // to interpret as the start of a connection drag. By returning false
        // here we prevent ContainerEventHandler from setting this widget as
        // focused/dragging, which would otherwise hijack the drag.
        if (this.isValidClickButton(event.buttonInfo()) && this.portAt(event.x(), event.y()) != null) {
            return false;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        super.onClick(event, doubleClick);
        this.dragging = true;
        this.grabOffsetX = event.x() - this.getX();
        this.grabOffsetY = event.y() - this.getY();
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        if (!this.dragging) {
            return;
        }
        int newX = (int) Math.round(event.x() - this.grabOffsetX);
        int newY = (int) Math.round(event.y() - this.grabOffsetY);
        // No clamp: nodes live in their parent canvas's coordinate space, which
        // is conceptually unbounded for a graph editor. The host (e.g., Canvas)
        // is responsible for any pan/zoom or boundary policy it wants to apply.
        this.setX(newX);
        this.setY(newY);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        super.onRelease(event);
        this.dragging = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = this.getX();
        int top = this.getY();
        int right = left + this.getWidth();
        int bottom = top + this.getHeight();

        int background = (this.isHovered() || this.dragging) ? BACKGROUND_HOVER_COLOR : BACKGROUND_COLOR;
        int border = this.dragging ? BORDER_DRAG_COLOR : BORDER_COLOR;

        // Drop shadow underneath so the widget feels like it floats.
        graphics.fill(left + 2, top + 2, right + 2, bottom + 2, 0x66000000);

        // Body fill.
        graphics.fill(left, top, right, bottom, ARGB.multiply(background, ARGB.white(this.getAlpha())));

        // Title bar.
        graphics.fill(left, top, right, top + TITLE_BAR_HEIGHT, ARGB.multiply(TITLE_BAR_COLOR, ARGB.white(this.getAlpha())));

        // Outline + title-bar separator.
        graphics.outline(left, top, this.getWidth(), this.getHeight(), border);
        graphics.horizontalLine(left, right - 1, top + TITLE_BAR_HEIGHT - 1, border);

        // Title text — centered in the title bar.
        Font font = Minecraft.getInstance().font;
        int titleColor = ARGB.color((int) (255 * this.getAlpha()), 0xFF, 0xFF, 0xFF);
        graphics.centeredText(font, this.getMessage(), left + this.getWidth() / 2, top + 2, titleColor);

        // Connection ports — small squares + per-port label inside the body.
        for (Port port : this.ports) {
            this.renderPort(graphics, font, port, mouseX, mouseY);
        }
    }

    private void renderPort(GuiGraphicsExtractor graphics, Font font, Port port, int mouseX, int mouseY) {
        Vector2fc center = this.portCenter(port);
        int px = (int) center.x();
        int py = (int) center.y();
        boolean hovered = this.portAt(mouseX, mouseY) == port;
        int fill = hovered ? PORT_HOVER_COLOR : PORT_FILL_COLOR;
        graphics.fill(px - PORT_RADIUS, py - PORT_RADIUS, px + PORT_RADIUS + 1, py + PORT_RADIUS + 1, fill);
        graphics.outline(px - PORT_RADIUS, py - PORT_RADIUS, 2 * PORT_RADIUS + 1, 2 * PORT_RADIUS + 1, PORT_OUTLINE_COLOR);

        Component title = port.title();
        if (title == null || title.getString().isEmpty()) {
            return;
        }
        int labelColor = ARGB.color((int) (255 * this.getAlpha()), 0xCC, 0xCC, 0xD4);
        int textWidth = font.width(title);
        int textY = py - font.lineHeight / 2 + 1;
        switch (port.side()) {
            case LEFT -> graphics.text(font, title, px + PORT_RADIUS + 1 + PORT_LABEL_GAP, textY, labelColor, false);
            case RIGHT -> graphics.text(font, title, px - PORT_RADIUS - PORT_LABEL_GAP - textWidth, textY, labelColor, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
