package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.graph.Node;
import dev.robotgryphon.screenlib.graph.Port;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

/**
 * Thin view layer over a {@link Node}. The widget owns no graph state of
 * its own — all geometry, ports, and hit-testing live on the {@code Node}.
 * The widget's responsibilities are limited to:
 *
 * <ul>
 *   <li>Plugging the node into Minecraft's {@code AbstractWidget} input loop
 *       (clicks, drags, narration)</li>
 *   <li>Forwarding draws to the GUI graphics pipeline</li>
 *   <li>Mirroring the node's position into {@link AbstractWidget}'s internal
 *       fields so its hit tests stay consistent with what the user sees</li>
 * </ul>
 *
 * <p>Mirrors the {@code Canvas} / {@code CanvasWidget} split: the data is
 * the node, the widget is the "thing on screen" that you point a mouse at.
 */
public class NodeWidget extends AbstractWidget {

    private static final int BACKGROUND_COLOR = 0xCC1F1F23;
    private static final int BACKGROUND_HOVER_COLOR = 0xCC2A2A33;
    private static final int BORDER_COLOR = 0xFF7F7F8C;
    private static final int BORDER_DRAG_COLOR = 0xFFFFD24A;
    private static final int TITLE_BAR_COLOR = 0xFF3A3A45;

    private static final int PORT_HOVER_COLOR = 0xFFFFD24A;
    private static final int PORT_OUTLINE_COLOR = 0xFF101012;

    private final Node node;

    private boolean dragging;
    /** Offset from the widget's top-left to the mouse when the body drag started. */
    private double grabOffsetX;
    private double grabOffsetY;

    public NodeWidget(Node node) {
        super(node.x(), node.y(), node.width(), node.height(), node.title());
        this.node = node;
    }

    public Node node() {
        return this.node;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    /** Delegates to {@link Node#portAt} — kept here so callers iterating widgets still find ports. */
    public @Nullable Port portAt(double mouseX, double mouseY) {
        return this.node.portAt(mouseX, mouseY);
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
        // Node is the source of truth; AbstractWidget's internal x/y get
        // mirrored so its built-in hit-testing matches what we render.
        this.node.setX(newX);
        this.node.setY(newY);
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
        // Pull straight from the model so anything that mutates the node
        // (e.g., future programmatic moves) shows up immediately.
        int left = this.node.x();
        int top = this.node.y();
        int right = left + this.node.width();
        int bottom = top + this.node.height();

        int background = (this.isHovered() || this.dragging) ? BACKGROUND_HOVER_COLOR : BACKGROUND_COLOR;
        int border = this.dragging ? BORDER_DRAG_COLOR : BORDER_COLOR;

        // Drop shadow underneath so the widget feels like it floats.
        graphics.fill(left + 2, top + 2, right + 2, bottom + 2, 0x66000000);

        // Body fill.
        graphics.fill(left, top, right, bottom, ARGB.multiply(background, ARGB.white(this.getAlpha())));

        // Title bar.
        graphics.fill(left, top, right, top + Node.TITLE_BAR_HEIGHT,
                ARGB.multiply(TITLE_BAR_COLOR, ARGB.white(this.getAlpha())));

        // Outline + title-bar separator.
        graphics.outline(left, top, this.node.width(), this.node.height(), border);
        graphics.horizontalLine(left, right - 1, top + Node.TITLE_BAR_HEIGHT - 1, border);

        // Title text — centered in the title bar.
        Font font = Minecraft.getInstance().font;
        int titleColor = ARGB.color((int) (255 * this.getAlpha()), 0xFF, 0xFF, 0xFF);
        graphics.centeredText(font, this.node.title(), left + this.node.width() / 2, top + 2, titleColor);

        // Connection ports — diamond + per-port label inside the body.
        for (Port port : this.node.ports()) {
            this.renderPort(graphics, font, port, mouseX, mouseY);
        }
    }

    private void renderPort(GuiGraphicsExtractor graphics, Font font, Port port, int mouseX, int mouseY) {
        Vector2fc center = this.node.portCenter(port);
        // Truncate to the integer pixel anchor used by the diamond geometry;
        // the +0.5 in portCenter() is for the bezier endpoint, not for raster.
        int px = (int) center.x();
        int py = (int) center.y();
        boolean hovered = this.portAt(mouseX, mouseY) == port;
        int fill = hovered ? PORT_HOVER_COLOR : port.color();

        // Outline first, fill on top — the outline diamond is one pixel larger
        // on every side, so the fill leaves a 1px dark border that reads cleanly
        // against the panel background regardless of port color.
        fillDiamond(graphics, px, py, Node.PORT_RADIUS + 1, PORT_OUTLINE_COLOR);
        fillDiamond(graphics, px, py, Node.PORT_RADIUS, ARGB.multiply(fill, ARGB.white(this.getAlpha())));

        Component title = port.title();
        if (title == null || title.getString().isEmpty()) {
            return;
        }
        int labelColor = ARGB.color((int) (255 * this.getAlpha()), 0xCC, 0xCC, 0xD4);
        int textWidth = font.width(title);
        int textY = py - font.lineHeight / 2 + 1;
        switch (port.side()) {
            case LEFT -> graphics.text(font, title, px + Node.PORT_RADIUS + 1 + Node.PORT_LABEL_GAP, textY, labelColor, false);
            case RIGHT -> graphics.text(font, title, px - Node.PORT_RADIUS - Node.PORT_LABEL_GAP - textWidth, textY, labelColor, false);
        }
    }

    /**
     * Draws a filled diamond inscribed in the (2r+1)×(2r+1) square centered
     * at ({@code cx}, {@code cy}). Each scanline from the center outward is
     * one pixel narrower per row, producing a 4-corner rhombus.
     */
    private static void fillDiamond(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = r - Math.abs(dy);
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
