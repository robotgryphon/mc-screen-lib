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
import org.joml.Vector2fc;

import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Clickable preview tile used in the "Add Node" picker grid. Bundles up the
 * rendering of a half-scale {@link NodeWidget} plus a name label below it,
 * and behaves like a regular {@link AbstractWidget} so a {@code GridLayout}
 * can position it and Minecraft's event loop can dispatch clicks to it.
 *
 * <p>Selection state is queried from the parent screen (rather than stored
 * locally) so that toggling the selected tile only has to flip a single
 * field on the screen — every tile re-evaluates {@link #isSelected} during
 * its next render pass.
 */
public class NodePreviewWidget extends AbstractWidget {

    /** Scale applied to the embedded {@link NodeWidget}. */
    public static final float PREVIEW_SCALE = 0.5f;
    /** Scale applied to the name label below the preview. */
    public static final float LABEL_SCALE = 0.5f;
    /** Pixels of breathing room between scaled node / label and the cell edge. */
    public static final int CELL_PADDING = 6;
    /** Vertical gap between the preview and its name label. */
    public static final int LABEL_GAP = 4;

    private static final int CELL_BG_NORMAL = 0x00000000;
    /** Subtle darken painted under the cell on hover; sits between normal and selected. */
    private static final int CELL_BG_HOVER = 0x66000000;
    private static final int CELL_BG_SELECTED = 0xCC000000;
    private static final int CELL_BORDER_NORMAL = 0x44FFFFFF;
    private static final int CELL_BORDER_HOVER = 0x88FFFFFF;
    private static final int CELL_BORDER_SELECTED = 0xFFFFD24A;
    private static final int LABEL_COLOR = 0xFFE0E0E0;

    /** Color of the highlight halo painted around a port the parent filter matched. */
    private static final int HIGHLIGHT_HALO_COLOR = 0xFFFFD24A;
    /**
     * Halo radius (in node-local pixels). Larger than {@link Node#PORT_RADIUS}
     * so the halo extends past the port outline and reads as a glow around it.
     */
    private static final int HIGHLIGHT_HALO_RADIUS = Node.PORT_RADIUS + 4;

    private final Node node;
    private final NodeWidget widget;
    private final Component name;
    /**
     * The tallest scaled-node height across all sibling previews. Each tile
     * vertically centers its own node within this shared band so a short
     * node doesn't pin to the top of a row sized for the tallest node, and
     * the labels stay aligned across the row.
     */
    private final int scaledNodeMaxH;
    private final BooleanSupplier isSelected;
    private final Runnable onSelect;
    /**
     * Ports the parent screen wants visually emphasized — e.g. inputs whose
     * type matched the current filter. Defaults to empty; the parent pushes
     * a fresh set on every filter change via {@link #setHighlightedPorts}.
     */
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

    /**
     * Replaces the set of ports that should render with a highlight halo.
     * Called by the parent screen whenever the active filter changes — the
     * tile itself doesn't care which filter produced the set, only which
     * ports landed in it.
     */
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
        // Selected wins over hover: combining the two darkenings would only
        // muddy the selected fill that already reads as "this is the picked
        // tile". Hover applies only to unselected tiles.
        boolean hovered = !selected && this.isHovered();
        int bg = selected ? CELL_BG_SELECTED : (hovered ? CELL_BG_HOVER : CELL_BG_NORMAL);
        int border = selected ? CELL_BORDER_SELECTED : (hovered ? CELL_BORDER_HOVER : CELL_BORDER_NORMAL);

        if ((bg >>> 24) != 0) {
            graphics.fill(x, y, x + w, y + h, bg);
        }
        graphics.outline(x, y, w, h, border);

        // Scaled NodeWidget — translate to cell, scale, render. The preview is
        // centered vertically within the shared scaledNodeMaxH band so a short
        // node doesn't pin to the top of a row sized for the tallest one.
        int nodeRenderW = scale(this.node.width());
        int nodeRenderH = scale(this.node.height());
        int nodeRenderX = x + (w - nodeRenderW) / 2;
        int nodeRenderY = y + CELL_PADDING + (this.scaledNodeMaxH - nodeRenderH) / 2;

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) nodeRenderX, (float) nodeRenderY);
        graphics.pose().scale(PREVIEW_SCALE, PREVIEW_SCALE);

        // Highlight halos go down BEFORE the NodeWidget render. The widget's
        // body fill covers the inside half of each halo, leaving a colored
        // glow emanating outward from the port — and the port itself draws
        // on top, so the halo never obscures the diamond's color.
        if (!this.highlightedPorts.isEmpty()) {
            for (Port port : this.node.ports()) {
                if (this.highlightedPorts.contains(port)) {
                    Vector2fc center = this.node.portCenter(port);
                    fillDiamond(graphics, (int) center.x(), (int) center.y(),
                            HIGHLIGHT_HALO_RADIUS, HIGHLIGHT_HALO_COLOR);
                }
            }
        }

        // Pass mouse coordinates well outside the node's logical bounds so the
        // preview never enters port-hover state — selection is a tile-level
        // concern, not an internal-port one.
        this.widget.extractRenderState(graphics, -10000, -10000, partialTick);
        graphics.pose().popMatrix();

        // Name label, half-scale font, centered below the preview slot.
        Font font = Minecraft.getInstance().font;
        int labelY = y + CELL_PADDING + this.scaledNodeMaxH + LABEL_GAP;
        scaledCenteredText(graphics, font, this.name, x + w / 2, labelY, LABEL_COLOR, LABEL_SCALE);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    /**
     * Convenience for the {@link #PREVIEW_SCALE} multiplication used in both
     * sizing math (on the parent screen) and rendering math (here).
     */
    public static int scale(int v) {
        return Math.round(v * PREVIEW_SCALE);
    }

    /**
     * Renders {@code text} centered horizontally on {@code centerX} with its
     * top at {@code y}, at {@code scale}× the font's natural size. The
     * translation accounts for the on-screen width shrinking with scale so
     * the text stays centered on {@code centerX} after the matrix transform.
     */
    private static void scaledCenteredText(GuiGraphicsExtractor graphics, Font font, Component text,
                                           int centerX, int y, int color, float scale) {
        int textWidth = font.width(text);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX - textWidth * scale / 2f, (float) y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    /**
     * Filled-diamond rasterizer matching the one in {@code NodeWidget}.
     * Duplicated here so the halo can be drawn in the same logical coordinate
     * space as the node — the parent matrix translates and scales both the
     * widget's ports and our halos together.
     */
    private static void fillDiamond(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = r - Math.abs(dy);
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }
}
