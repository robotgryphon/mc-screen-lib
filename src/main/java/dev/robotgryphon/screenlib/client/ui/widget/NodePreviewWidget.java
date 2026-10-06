package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.client.NodeRenderer;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.ports.Port;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Vector2fc;

import java.util.Set;
import java.util.function.BooleanSupplier;

public class NodePreviewWidget extends AbstractWidget {
    public static final float PREVIEW_SCALE = 0.5f;
    public static final float LABEL_SCALE = 0.5f;
    public static final int CELL_PADDING = 6;
    public static final int LABEL_GAP = 4;

    private static final int CELL_BG_NORMAL = 0x00000000;
    private static final int CELL_BG_HOVER = 0x66000000;
    private static final int CELL_BG_SELECTED = 0xCC000000;
    private static final int CELL_BORDER_NORMAL = 0x44FFFFFF;
    private static final int CELL_BORDER_HOVER = 0x88FFFFFF;
    private static final int CELL_BORDER_SELECTED = 0xFFFFD24A;
    private static final int LABEL_COLOR = 0xFFE0E0E0;

    private static final int HIGHLIGHT_HALO_COLOR = 0xFFFFD24A;
    private static final int HIGHLIGHT_HALO_RADIUS = NodeRenderer.PORT_RADIUS + 4;

    private final Node node;
    private final NodeWidget widget;
    private final Component name;
    private final int scaledNodeMaxH;
    private final BooleanSupplier isSelected;
    private final Runnable onSelect;
    private Set<Port> highlightedPorts = Set.of();

    public NodePreviewWidget(int width, int height, int scaledNodeMaxH,
                             Node node, NodeWidget widget, Component name,
                             BooleanSupplier isSelected, Runnable onSelect) {
        super(0, 0, width, height, name);
        this.node = node;
        this.widget = widget;
        this.name = name;
        this.scaledNodeMaxH = scaledNodeMaxH;
        this.isSelected = isSelected;
        this.onSelect = onSelect;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        super.onClick(event, doubleClick);
        this.onSelect.run();
    }

    public void setHighlightedPorts(Set<Port> ports) {
        this.highlightedPorts = ports;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        boolean selected = this.isSelected.getAsBoolean();
        boolean hovered = !selected && this.isHovered();
        int bg = selected ? CELL_BG_SELECTED : (hovered ? CELL_BG_HOVER : CELL_BG_NORMAL);
        int border = selected ? CELL_BORDER_SELECTED : (hovered ? CELL_BORDER_HOVER : CELL_BORDER_NORMAL);

        if ((bg >>> 24) != 0) {
            graphics.fill(x, y, x + w, y + h, bg);
        }
        graphics.outline(x, y, w, h, border);

        int nodeRenderW = scale(this.widget.getWidth());
        int nodeRenderH = scale(this.widget.getHeight());
        int nodeRenderX = x + (w - nodeRenderW) / 2;
        int nodeRenderY = y + CELL_PADDING + (this.scaledNodeMaxH - nodeRenderH) / 2;

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) nodeRenderX, (float) nodeRenderY);
        graphics.pose().scale(PREVIEW_SCALE, PREVIEW_SCALE);

        if (!this.highlightedPorts.isEmpty()) {
            for (Port port : this.node.portManager().ports()) {
                if (this.highlightedPorts.contains(port)) {
                    Vector2fc center = this.widget.ports().renderedPort(port).center();
                    fillDiamond(graphics, (int) center.x(), (int) center.y(),
                            HIGHLIGHT_HALO_RADIUS, HIGHLIGHT_HALO_COLOR);
                }
            }
        }

        this.widget.extractRenderState(graphics, -10000, -10000, partialTick);
        graphics.pose().popMatrix();

        Font font = Minecraft.getInstance().font;
        int labelY = y + CELL_PADDING + this.scaledNodeMaxH + LABEL_GAP;
        scaledCenteredText(graphics, font, this.name, x + w / 2, labelY, LABEL_COLOR, LABEL_SCALE);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    public static int scale(int v) {
        return Math.round(v * PREVIEW_SCALE);
    }

    private static void scaledCenteredText(GuiGraphicsExtractor graphics, Font font, Component text,
                                           int centerX, int y, int color, float scale) {
        int textWidth = font.width(text);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX - textWidth * scale / 2f, (float) y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    private static void fillDiamond(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = r - Math.abs(dy);
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }
}
