package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.graph.Node;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    /** Scale applied to each {@link NodeWidget} when rendered as a preview. */
    private static final float PREVIEW_SCALE = 0.5f;
    /** Scale applied to the dialog title and the per-cell name labels. */
    private static final float LABEL_SCALE = 0.5f;
    /** Hard cap on grid columns — anything wider would crowd the previews. */
    private static final int MAX_COLUMNS = 6;

    /** Pixels of breathing room between scaled node / label and the cell edge. */
    private static final int CELL_PADDING = 6;
    /** Vertical gap between the preview and its name label. */
    private static final int LABEL_GAP = 4;
    /** Gap between adjacent grid cells. */
    private static final int GRID_GAP = 6;
    /** Margin between the screen edge and the dialog chrome. */
    private static final int CONTENT_PADDING = 16;
    /** Vertical band reserved for the title at the top of the screen. */
    private static final int TITLE_BAND_H = 20;
    /** Vertical band reserved for the search field below the title. */
    private static final int SEARCH_BAND_H = 28;
    private static final int SEARCH_BAR_W = 240;
    private static final int SEARCH_BAR_H = 16;
    private static final int BUTTON_W = 80;
    private static final int BUTTON_H = 20;
    /** Pixels per scroll-wheel tick. */
    private static final int SCROLL_STEP = 16;

    private static final int BACKDROP_COLOR = 0xC0000000;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int CELL_BG_NORMAL = 0x00000000;
    private static final int CELL_BG_SELECTED = 0xCC000000;
    private static final int CELL_BORDER_NORMAL = 0x44FFFFFF;
    private static final int CELL_BORDER_SELECTED = 0xFFFFD24A;
    private static final int LABEL_COLOR = 0xFFE0E0E0;

    private final Screen parent;
    private final List<Holder.Reference<NodeDefinition>> options;
    private final Consumer<Holder.Reference<NodeDefinition>> onSubmit;

    /** Per-option, immutable; built once in {@link #init()}. */
    private final List<PreviewTemplate> templates = new ArrayList<>();
    /** Per-frame layout slots, derived from {@link #templates} after filtering. */
    private final List<Preview> visible = new ArrayList<>();

    private Holder.@Nullable Reference<NodeDefinition> selectedRef;

    private EditBox searchField;
    private Button submitButton;

    /** Vertical scroll offset into the grid; clamped to {@code [0, maxScroll]}. */
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // Grid clip region in screen coords (the visible "viewport" into the grid).
    private int gridX, gridY, gridW, gridH;
    // Uniform cell size — sized to fit the largest preview.
    private int cellW, cellH;
    /** Max scaled-node height across all templates — used to align name labels. */
    private int scaledNodeMaxH;

    public AddNodeDialog(Screen parent,
                         List<Holder.Reference<NodeDefinition>> options,
                         Consumer<Holder.Reference<NodeDefinition>> onSubmit) {
        super(Component.literal("Add Node"));
        this.parent = parent;
        this.options = List.copyOf(options);
        this.onSubmit = onSubmit;
    }

    private static int scale(int v) {
        return Math.round(v * PREVIEW_SCALE);
    }

    @Override
    protected void init() {
        super.init();

        Font font = Minecraft.getInstance().font;
        this.templates.clear();

        // Build a (Node, NodeWidget, name) triple per option. Nodes live at
        // logical (0, 0) — the matrix transform places them in their cell
        // when rendering.
        int scaledMaxW = 0;
        int scaledMaxH = 0;
        int maxLabelW = 0;
        for (Holder.Reference<NodeDefinition> ref : this.options) {
            Component name = Component.translatable(ref.key().identifier().toLanguageKey("node"));
            Node node = new Node(ref.value(), name, 0, 0);
            this.templates.add(new PreviewTemplate(ref, node, new NodeWidget(node), name));
            scaledMaxW = Math.max(scaledMaxW, scale(node.width()));
            scaledMaxH = Math.max(scaledMaxH, scale(node.height()));
            // Label is rendered at LABEL_SCALE, so its on-screen footprint is
            // proportionally smaller — measure that, not the full-size width.
            maxLabelW = Math.max(maxLabelW, Math.round(font.width(name) * LABEL_SCALE));
        }
        this.scaledNodeMaxH = scaledMaxH;

        int labelH = Math.round(font.lineHeight * LABEL_SCALE);
        this.cellW = Math.max(scaledMaxW, maxLabelW) + 2 * CELL_PADDING;
        this.cellH = scaledMaxH + LABEL_GAP + labelH + 2 * CELL_PADDING;

        // Search field, centered horizontally below the title band.
        int searchX = (this.width - SEARCH_BAR_W) / 2;
        int searchY = TITLE_BAND_H + (SEARCH_BAND_H - SEARCH_BAR_H) / 2;
        this.searchField = new EditBox(font, searchX, searchY, SEARCH_BAR_W, SEARCH_BAR_H,
                Component.literal("Search"));
        this.searchField.setHint(Component.literal("Search…"));
        this.searchField.setMaxLength(64);
        this.searchField.setResponder(s -> rebuildVisible());
        this.addRenderableWidget(this.searchField);

        // Submit / Cancel docked at the bottom-center.
        int buttonRowY = this.height - CONTENT_PADDING - BUTTON_H;
        int buttonsTotalW = 2 * BUTTON_W + CONTENT_PADDING;
        int cancelX = (this.width - buttonsTotalW) / 2;
        int submitX = cancelX + BUTTON_W + CONTENT_PADDING;
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.cancel())
                .bounds(cancelX, buttonRowY, BUTTON_W, BUTTON_H)
                .build());
        this.submitButton = this.addRenderableWidget(
                Button.builder(Component.literal("Submit"), b -> this.submit())
                        .bounds(submitX, buttonRowY, BUTTON_W, BUTTON_H)
                        .build());

        // Grid viewport sits between the search band and the button row.
        this.gridX = CONTENT_PADDING;
        this.gridY = TITLE_BAND_H + SEARCH_BAND_H;
        this.gridW = this.width - 2 * CONTENT_PADDING;
        this.gridH = (buttonRowY - CONTENT_PADDING) - this.gridY;

        // Default selection — first option, or null when there are none.
        this.selectedRef = this.options.isEmpty() ? null : this.options.get(0);

        rebuildVisible();
    }

    /**
     * Recomputes the visible cell list from {@link #templates}, applying the
     * search filter and grid layout. Called on init, on every search keystroke,
     * and any time the visible set might change.
     */
    private void rebuildVisible() {
        this.visible.clear();
        String filter = (this.searchField == null ? "" : this.searchField.getValue())
                .trim().toLowerCase(Locale.ROOT);

        // Filter against the displayed name; users will search by what they see.
        List<PreviewTemplate> filtered = new ArrayList<>();
        for (PreviewTemplate t : this.templates) {
            if (filter.isEmpty() || t.name.getString().toLowerCase(Locale.ROOT).contains(filter)) {
                filtered.add(t);
            }
        }

        // Pick the column count that fits the viewport without overflow,
        // capped at MAX_COLUMNS (so the row never gets denser than 6) and at
        // the filtered count (so we don't render empty trailing columns).
        int columns = Math.max(1, (this.gridW + GRID_GAP) / (this.cellW + GRID_GAP));
        columns = Math.min(columns, MAX_COLUMNS);
        columns = Math.min(columns, Math.max(1, filtered.size()));
        int rows = (filtered.size() + columns - 1) / columns;

        int gridContentW = columns * this.cellW + (columns - 1) * GRID_GAP;
        int startX = this.gridX + (this.gridW - gridContentW) / 2;
        int startY = this.gridY + CELL_PADDING;

        for (int i = 0; i < filtered.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int cx = startX + col * (this.cellW + GRID_GAP);
            int cy = startY + row * (this.cellH + GRID_GAP);
            this.visible.add(new Preview(filtered.get(i), cx, cy));
        }

        // Total content height includes the leading CELL_PADDING.
        int contentH = (rows == 0)
                ? 0
                : CELL_PADDING + rows * this.cellH + (rows - 1) * GRID_GAP + CELL_PADDING;
        this.maxScroll = Math.max(0, contentH - this.gridH);
        // Reset scroll on every rebuild — typing a query always starts at the
        // top of the (newly filtered) results, which avoids the "I'm typing
        // and nothing's appearing" trap when the previous scroll exceeded the
        // shrunk content's height.
        this.scrollOffset = 0;

        // If the previously selected option was filtered out, fall back to the
        // first visible one (or null if everything is filtered away).
        boolean stillVisible = false;
        for (Preview p : this.visible) {
            if (p.template.ref == this.selectedRef) {
                stillVisible = true;
                break;
            }
        }
        if (!stillVisible) {
            this.selectedRef = this.visible.isEmpty() ? null : this.visible.get(0).template.ref;
        }

        if (this.submitButton != null) {
            this.submitButton.active = (this.selectedRef != null);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Cell hit-test inside the scrollable grid area takes priority.
        if (event.x() >= this.gridX && event.x() < this.gridX + this.gridW
                && event.y() >= this.gridY && event.y() < this.gridY + this.gridH) {
            // Cell positions live in pre-scroll grid space; add scroll offset to
            // map the screen-space click into that space.
            double virtY = event.y() + this.scrollOffset;
            for (Preview p : this.visible) {
                if (event.x() >= p.cellX && event.x() < p.cellX + this.cellW
                        && virtY >= p.cellY && virtY < p.cellY + this.cellH) {
                    this.selectedRef = p.template.ref;
                    if (this.submitButton != null) {
                        this.submitButton.active = true;
                    }
                    return true;
                }
            }
            // Click in grid area but outside any cell — let the search field
            // and buttons get a shot at it via super.
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Scroll only affects the grid; outside, fall through to default.
        if (mouseX >= this.gridX && mouseX < this.gridX + this.gridW
                && mouseY >= this.gridY && mouseY < this.gridY + this.gridH) {
            int next = (int) Math.round(this.scrollOffset - scrollY * SCROLL_STEP);
            this.scrollOffset = Math.max(0, Math.min(this.maxScroll, next));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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

        // Title band — rendered at LABEL_SCALE so the chrome text reads at
        // the same weight as the per-cell labels below.
        graphics.centeredText(font, this.getTitle(), this.width / 2, CONTENT_PADDING / 2 + 2, TITLE_COLOR);

        // Grid: clip to viewport, translate by -scrollOffset, render cells.
        graphics.enableScissor(this.gridX, this.gridY, this.gridX + this.gridW, this.gridY + this.gridH);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0f, (float) -this.scrollOffset);
        for (Preview p : this.visible) {
            renderCell(graphics, font, p, partialTick);
        }
        graphics.pose().popMatrix();
        graphics.disableScissor();

        // Search field + Submit/Cancel.
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCell(GuiGraphicsExtractor graphics, Font font, Preview p, float partialTick) {
        boolean selected = (p.template.ref == this.selectedRef);
        int bg = selected ? CELL_BG_SELECTED : CELL_BG_NORMAL;
        int border = selected ? CELL_BORDER_SELECTED : CELL_BORDER_NORMAL;

        if ((bg >>> 24) != 0) {
            graphics.fill(p.cellX, p.cellY, p.cellX + this.cellW, p.cellY + this.cellH, bg);
        }
        graphics.outline(p.cellX, p.cellY, this.cellW, this.cellH, border);

        // Scaled NodeWidget — translate to cell, scale, render. Each preview
        // is centered vertically within the row's allocated preview slot
        // (scaledNodeMaxH) so a short node like Tree Cutter Upgrade doesn't
        // pin to the top of a row sized for the tallest node.
        Node n = p.template.node;
        int nodeRenderW = scale(n.width());
        int nodeRenderH = scale(n.height());
        int nodeRenderX = p.cellX + (this.cellW - nodeRenderW) / 2;
        int nodeRenderY = p.cellY + CELL_PADDING + (this.scaledNodeMaxH - nodeRenderH) / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) nodeRenderX, (float) nodeRenderY);
        graphics.pose().scale(PREVIEW_SCALE, PREVIEW_SCALE);
        // Pass mouse coordinates well outside the node's logical bounds so the
        // preview never enters port-hover state — selection is a cell-level
        // concern, not an internal-port one.
        p.template.widget.extractRenderState(graphics, -10000, -10000, partialTick);
        graphics.pose().popMatrix();

        // Name label, half-scale font, centered below the preview slot.
        int labelY = p.cellY + CELL_PADDING + this.scaledNodeMaxH + LABEL_GAP;
        scaledCenteredText(graphics, font, p.template.name,
                p.cellX + this.cellW / 2, labelY, LABEL_COLOR, LABEL_SCALE);
    }

    /**
     * Renders {@code text} centered horizontally on {@code centerX} with its
     * top at {@code y}, but at {@code scale}× the font's natural size. The
     * translation accounts for the on-screen width shrinking with scale so the
     * text stays centered on {@code centerX} after the matrix transform is
     * applied.
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

    private record PreviewTemplate(Holder.Reference<NodeDefinition> ref,
                                   Node node,
                                   NodeWidget widget,
                                   Component name) {}

    private record Preview(PreviewTemplate template, int cellX, int cellY) {}
}
