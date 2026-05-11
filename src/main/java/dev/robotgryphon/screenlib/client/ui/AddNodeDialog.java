package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.graph.Node;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Picker screen for choosing a {@link NodeDefinition}.
 *
 * <p>Layout, top-to-bottom:
 * <ol>
 *   <li>Title band ("Add Node")</li>
 *   <li>Search field — live-filters the grid as the user types</li>
 *   <li>Scrollable preview grid — one cell per registered definition,
 *       each cell showing a half-scale {@link NodeWidget} preview plus the
 *       node's display name underneath</li>
 *   <li>Submit / Cancel buttons</li>
 * </ol>
 *
 * <p>The previews are real {@code NodeWidget}s rendering real {@code Node}s
 * — same code path the canvas uses, scaled down via a matrix transform so
 * what the user sees in the picker matches what spawns on submit. They are
 * not registered as children of this screen, so mouse events never reach
 * them; the cell rectangle is the only clickable target.
 */
public class AddNodeDialog extends Screen {

    /**
     * Vertical band reserved for the title at the top of the screen.
     */
    private static final int TITLE_BAND_H = 20;
    /**
     * Vertical band reserved for the search field below the title.
     */
    private static final int SEARCH_BAND_H = 28;
    private static final int SEARCH_BAR_W = 240;
    private static final int SEARCH_BAR_H = 16;

    private static final int BACKDROP_COLOR = 0xC0000000;

    private final Screen parent;
    private final List<Holder.Reference<NodeDefinition>> options;
    private final Consumer<Holder.Reference<NodeDefinition>> onSubmit;

    /**
     * Per-option, immutable; built once in {@link #init()}.
     */
    private final List<PreviewTemplate> templates = new ArrayList<>();

    private Holder.@Nullable Reference<NodeDefinition> selectedRef;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private EditBox searchField;
    private Button submitButton;

    public AddNodeDialog(Screen parent,
                         List<Holder.Reference<NodeDefinition>> options,
                         Consumer<Holder.Reference<NodeDefinition>> onSubmit) {
        super(Component.literal("Add Node"));
        this.parent = parent;
        this.options = List.copyOf(options);
        this.onSubmit = onSubmit;

    }

    @Override
    protected void init() {
        super.init();

        Font font = Minecraft.getInstance().font;
        this.layout.addToHeader(new StringWidget(this.title, this.font), LayoutSettings::alignHorizontallyCenter);

        // Search field, centered horizontally below the title band.
        addSearchFilter(font);
        addNodeSelectionGrid();
        addActionsRow();

        // Default selection — first option, or null when there are none.
//        this.selectedRef = this.options.isEmpty() ? null : this.options.get(0);
        this.submitButton.active = false;

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    public void repositionElements() {
        this.layout.setHeaderHeight(40);
        this.layout.arrangeElements();
        this.layout.setFooterHeight(20);
    }

    private void addSearchFilter(Font font) {
        int searchX = (this.width - SEARCH_BAR_W) / 2;
        int searchY = TITLE_BAND_H + (SEARCH_BAND_H - SEARCH_BAR_H) / 2;
        this.searchField = new EditBox(font, searchX, searchY, SEARCH_BAR_W, SEARCH_BAR_H,
                Component.literal("Search"));
        this.searchField.setHint(Component.literal("Search…"));
        this.searchField.setMaxLength(64);
//        this.searchField.setResponder(s -> rebuildVisible());

        this.layout.addToHeader(this.searchField, LayoutSettings::alignHorizontallyCenter);
    }

    private void addNodeSelectionGrid() {

        final var grid = new GridLayout(0, 0);

        GridLayout.RowHelper helper = grid.columnSpacing(8)
                .rowSpacing(4)
                .createRowHelper(6);

        this.templates.clear();

        // Build a (Node, NodeWidget, name) triple per option. Nodes live at
        // logical (0, 0) — the matrix transform places them in their cell
        // when rendering.
//        int scaledMaxW = 0;
//        int scaledMaxH = 0;
//        int maxLabelW = 0;
//        for (Holder.Reference<NodeDefinition> ref : this.options) {
//            Component name = Component.translatable(ref.key().identifier().toLanguageKey("node"));
//            Node node = new Node(ref.value(), name, 0, 0);
//            this.templates.add(new PreviewTemplate(ref, node, new NodeWidget(node), name));
//            scaledMaxW = Math.max(scaledMaxW, scale(node.width()));
//            scaledMaxH = Math.max(scaledMaxH, scale(node.height()));
//            // Label is rendered at LABEL_SCALE, so its on-screen footprint is
//            // proportionally smaller — measure that, not the full-size width.
//            maxLabelW = Math.max(maxLabelW, Math.round(font.width(name) * LABEL_SCALE));
//        }
//        this.scaledNodeMaxH = scaledMaxH;
//
//        // Grid viewport sits between the search band and the button row.
//        this.gridX = CONTENT_PADDING;
//        this.gridY = TITLE_BAND_H + SEARCH_BAND_H;
//        this.gridW = this.width - 2 * CONTENT_PADDING;
//        this.gridH = (CONTENT_PADDING) - this.gridY;
//
//        int labelH = Math.round(font.lineHeight * LABEL_SCALE);
//        this.cellW = Math.max(scaledMaxW, maxLabelW) + 2 * CELL_PADDING;
//        this.cellH = scaledMaxH + LABEL_GAP + labelH + 2 * CELL_PADDING;

        this.layout.addToContents(grid);
    }

    private void addActionsRow() {
        int buttonWidth = 60;
        int buttonHeight = 16;

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));

        footer.addChild(Button.builder(Component.literal("Cancel"), b -> this.cancel())
                .size(buttonWidth, buttonHeight)
                .build());

        this.submitButton = footer.addChild(Button.builder(Component.literal("Submit"), b -> this.submit())
                .size(buttonWidth, buttonHeight)
                .build());

        this.layout.visitWidgets(widget -> {
            widget.setTabOrderGroup(1);
            addRenderableWidget(widget);
        });
    }

    private void submit() {
        if (this.selectedRef != null) {
            this.onSubmit.accept(this.selectedRef);
        }
        this.close();
    }

    private void cancel() {
        this.close();
    }

    private void close() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void onClose() {
        this.cancel();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;

        // Backdrop fades the parent screen.
        graphics.fill(0, 0, this.width, this.height, BACKDROP_COLOR);

        // Grid: clip to viewport, translate by -scrollOffset, render cells.
//        graphics.enableScissor(this.gridX, this.gridY, this.gridX + this.gridW, this.gridY + this.gridH);
//        graphics.pose().pushMatrix();
//        graphics.pose().translate(0f, (float) -this.scrollOffset);
//        for (Preview p : this.visible) {
//            renderCell(graphics, font, p, partialTick);
//        }
//        graphics.pose().popMatrix();
//        graphics.disableScissor();

        // Search field + Submit/Cancel.
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
//
//    private void renderCell(GuiGraphicsExtractor graphics, Font font, Preview p, float partialTick) {
//        boolean selected = (p.template.ref == this.selectedRef);
//        int bg = selected ? CELL_BG_SELECTED : CELL_BG_NORMAL;
//        int border = selected ? CELL_BORDER_SELECTED : CELL_BORDER_NORMAL;
//
//        if ((bg >>> 24) != 0) {
//            graphics.fill(p.cellX, p.cellY, p.cellX + this.cellW, p.cellY + this.cellH, bg);
//        }
//        graphics.outline(p.cellX, p.cellY, this.cellW, this.cellH, border);
//
//        // Scaled NodeWidget — translate to cell, scale, render. Each preview
//        // is centered vertically within the row's allocated preview slot
//        // (scaledNodeMaxH) so a short node like Tree Cutter Upgrade doesn't
//        // pin to the top of a row sized for the tallest node.
//        Node n = p.template.node;
//        int nodeRenderW = scale(n.width());
//        int nodeRenderH = scale(n.height());
//        int nodeRenderX = p.cellX + (this.cellW - nodeRenderW) / 2;
//        int nodeRenderY = p.cellY + CELL_PADDING + (this.scaledNodeMaxH - nodeRenderH) / 2;
//        graphics.pose().pushMatrix();
//        graphics.pose().translate((float) nodeRenderX, (float) nodeRenderY);
//        graphics.pose().scale(PREVIEW_SCALE, PREVIEW_SCALE);
//        // Pass mouse coordinates well outside the node's logical bounds so the
//        // preview never enters port-hover state — selection is a cell-level
//        // concern, not an internal-port one.
//        p.template.widget.extractRenderState(graphics, -10000, -10000, partialTick);
//        graphics.pose().popMatrix();
//
//        // Name label, half-scale font, centered below the preview slot.
//        int labelY = p.cellY + CELL_PADDING + this.scaledNodeMaxH + LABEL_GAP;
//        scaledCenteredText(graphics, font, p.template.name,
//                p.cellX + this.cellW / 2, labelY, LABEL_COLOR, LABEL_SCALE);
//    }

//    /**
//     * Renders {@code text} centered horizontally on {@code centerX} with its
//     * top at {@code y}, but at {@code scale}× the font's natural size. The
//     * translation accounts for the on-screen width shrinking with scale so the
//     * text stays centered on {@code centerX} after the matrix transform is
//     * applied.
//     */
//    private static void scaledCenteredText(GuiGraphicsExtractor graphics, Font font, Component text,
//                                           int centerX, int y, int color, float scale) {
//        int textWidth = font.width(text);
//        graphics.pose().pushMatrix();
//        graphics.pose().translate(centerX - textWidth * scale / 2f, (float) y);
//        graphics.pose().scale(scale, scale);
//        graphics.text(font, text, 0, 0, color, false);
//        graphics.pose().popMatrix();
//    }

    private record PreviewTemplate(Holder.Reference<NodeDefinition> ref,
                                   Node node,
                                   NodeWidget widget,
                                   Component name) {
    }

    private record Preview(PreviewTemplate template, int cellX, int cellY) {
    }
}
