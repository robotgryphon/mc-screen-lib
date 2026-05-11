package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.client.ui.widget.ChipFilterInput;
import dev.robotgryphon.screenlib.client.ui.widget.ChipSuggestion;
import dev.robotgryphon.screenlib.client.ui.widget.NodePreviewWidget;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.graph.Node;
import dev.robotgryphon.screenlib.graph.Port;
import dev.robotgryphon.screenlib.graph.PortSide;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import dev.robotgryphon.screenlib.types.PortDefinition;
import dev.robotgryphon.screenlib.types.PropertyType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Picker screen for choosing a {@link NodeDefinition}.
 *
 * <p>Header is a single {@link ChipFilterInput} that combines the filter-type
 * picker, search field, and active-chip strip into one widget. The chips it
 * collects carry typed filter clauses via their {@code data} slot, recovered
 * here through {@link ChipFilterInput#filters()}.
 *
 * <p>The grid below the header is the same scrollable preview area as
 * before — each tile is a {@link NodePreviewWidget} backed by a real
 * {@link Node}/{@link NodeWidget} pair, scaled to half-size for the preview.
 */
public class AddNodeDialog extends Screen {

    /** Total width of the ChipFilterInput in the header. */
    private static final int FILTER_INPUT_W = 300;
    /** Gap between stacked rows (title, filter input) inside the header. */
    private static final int HEADER_ROW_SPACING = 4;
    /**
     * Reserved height for the header band. Must accommodate the title row
     * (~font.lineHeight) and the {@link ChipFilterInput}'s static height
     * (search row + chip row + gap) with a little breathing room since the
     * underlying {@code FrameLayout} centers the stack inside this band.
     */
    private static final int HEADER_HEIGHT = 60;

    /** Grid layout shape — kept in sync with the spacing/columns used in {@link #addNodeSelectionGrid()}. */
    private static final int GRID_COLUMNS = 6;
    private static final int GRID_SPACING_X = 8;
    private static final int GRID_SPACING_Y = 4;

    private final Screen parent;
    private final List<Holder.Reference<NodeDefinition>> options;
    private final Consumer<Holder.Reference<NodeDefinition>> onSubmit;

    /** Per-option, immutable; built once in {@link #init()}. */
    private final List<PreviewTemplate> templates = new ArrayList<>();
    /**
     * Tiles parallel to {@link #templates}: {@code previewTiles.get(i)} is
     * the rendered widget for {@code templates.get(i)}. Kept alongside the
     * templates so {@link #applyFilter()} can hide and re-pack tiles by
     * index without walking the layout tree.
     */
    private final List<NodePreviewWidget> previewTiles = new ArrayList<>();

    /** Uniform cell size, set by {@link #addNodeSelectionGrid()} and reused when re-packing tiles in {@link #applyFilter()}. */
    private int gridCellW;
    private int gridCellH;

    private Holder.@Nullable Reference<NodeDefinition> selectedRef;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private ChipFilterInput<FilterMode> filterInput;
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

        // HeaderAndFooterLayout's header is a FrameLayout — calling
        // addToHeader multiple times overlays the children at the centered
        // anchor instead of stacking them. Build our header as a single
        // vertical LinearLayout containing the title and the filter input,
        // then hand THAT to the frame.
        LinearLayout headerStack = LinearLayout.vertical().spacing(HEADER_ROW_SPACING);
        headerStack.defaultCellSetting().alignHorizontallyCenter();
        headerStack.addChild(new StringWidget(this.title, this.font));

        // Templates need to be built first so suggestionsFor has data to draw
        // from. Pre-populate them here, then let addNodeSelectionGrid lay
        // them out into the grid below.
        buildTemplates();

        this.filterInput = new ChipFilterInput<>(
                0, 0, FILTER_INPUT_W,
                FilterMode.NAME,
                List.of(FilterMode.values()),
                FilterMode::label,
                FilterMode::chipColor,
                this::suggestionsFor);
        this.filterInput.setOnChange(this::applyFilter);
        headerStack.addChild(this.filterInput);

        this.layout.addToHeader(headerStack);

        addNodeSelectionGrid();
        addActionsRow();

        // No default selection — submit stays disabled until the user clicks
        // a tile. selectRef flips submitButton.active when that happens.
        this.submitButton.active = false;

        // Single walk over everything in the layout: sets the shared tab-order
        // group and registers each piece (header text, filter input, every
        // preview tile in the grid, footer buttons) as a renderable widget.
        this.layout.visitWidgets(widget -> {
            widget.setTabOrderGroup(1);
            addRenderableWidget(widget);
        });
        this.repositionElements();
    }

    @Override
    public void repositionElements() {
        this.layout.setHeaderHeight(HEADER_HEIGHT);
        this.layout.setFooterHeight(20);
        this.layout.arrangeElements();
        this.applyFilter();
    }

    /**
     * Builds the {@link PreviewTemplate} list once per init, ahead of the
     * grid construction. Pulled out so both the grid and the suggestions
     * provider can read from a shared list — suggestions need it at filter
     * input construction time, which happens before the grid runs.
     */
    private void buildTemplates() {
        this.templates.clear();
        for (Holder.Reference<NodeDefinition> ref : this.options) {
            Component name = Component.translatable(ref.key().identifier().toLanguageKey("node"));
            Node node = new Node(ref.value(), name, 0, 0);
            this.templates.add(new PreviewTemplate(ref, node, new NodeWidget(node), name));
        }
    }

    private void addNodeSelectionGrid() {
        final var grid = new GridLayout(0, 0);
        GridLayout.RowHelper helper = grid.columnSpacing(GRID_SPACING_X)
                .rowSpacing(GRID_SPACING_Y)
                .createRowHelper(GRID_COLUMNS);

        this.previewTiles.clear();
        if (this.templates.isEmpty()) {
            this.layout.addToContents(grid);
            return;
        }

        // Measure footprints across the pre-built templates to pick a uniform
        // cell size. Nodes live at logical (0, 0); the preview widget places
        // them at render time via a matrix transform.
        int scaledMaxW = 0;
        int scaledMaxH = 0;
        int maxLabelW = 0;
        for (PreviewTemplate template : this.templates) {
            scaledMaxW = Math.max(scaledMaxW, NodePreviewWidget.scale(template.node.width()));
            scaledMaxH = Math.max(scaledMaxH, NodePreviewWidget.scale(template.node.height()));
            // Label is rendered at LABEL_SCALE, so its on-screen footprint is
            // proportionally smaller — measure that, not the full-size width.
            maxLabelW = Math.max(maxLabelW, Math.round(this.font.width(template.name) * NodePreviewWidget.LABEL_SCALE));
        }

        // Cell size has to be uniform across the row, sized to fit the tallest
        // preview + the longest label. Stored on the screen so applyFilter
        // can re-pack tiles into the same grid pitch.
        int labelH = Math.round(this.font.lineHeight * NodePreviewWidget.LABEL_SCALE);
        this.gridCellW = Math.max(scaledMaxW, maxLabelW) + 2 * NodePreviewWidget.CELL_PADDING;
        this.gridCellH = scaledMaxH + NodePreviewWidget.LABEL_GAP + labelH + 2 * NodePreviewWidget.CELL_PADDING;
        final int sharedScaledNodeMaxH = scaledMaxH;

        // Build a renderable preview widget per template at the uniform cell
        // size and drop it into the grid in declaration order.
        for (PreviewTemplate template : this.templates) {
            final Holder.Reference<NodeDefinition> ref = template.ref;
            NodePreviewWidget preview = new NodePreviewWidget(
                    this.gridCellW, this.gridCellH, sharedScaledNodeMaxH,
                    template.node, template.widget, template.name,
                    () -> this.selectedRef == ref,
                    () -> this.selectRef(ref));
            helper.addChild(preview);
            this.previewTiles.add(preview);
        }

        this.layout.addToContents(grid);
    }

    /**
     * Suggestions provider handed to the {@link ChipFilterInput}. Switches
     * on the active filter mode:
     * <ul>
     *   <li>{@link FilterMode#NAME} → matching node display names.</li>
     *   <li>{@link FilterMode#INPUT} → distinct input port types whose
     *       display name matches the input. The chip carries the type's
     *       display name as a string so {@link FilterMode#matches} can do
     *       its substring check unchanged.</li>
     * </ul>
     */
    private List<ChipSuggestion> suggestionsFor(FilterMode mode, String input) {
        String needle = input.toLowerCase(Locale.ROOT);
        return switch (mode) {
            case NAME -> this.templates.stream()
                    .map(PreviewTemplate::name)
                    .filter(name -> name.getString().toLowerCase(Locale.ROOT).contains(needle))
                    .map(name -> new ChipSuggestion(name, FilterMode.NAME.chipColor(), name.getString()))
                    .toList();
            case INPUT -> distinctInputTypes()
                    .filter(holder -> holder.value().displayName().getString()
                            .toLowerCase(Locale.ROOT).contains(needle))
                    .map(holder -> new ChipSuggestion(
                            holder.value().displayName(),
                            holder.value().color(),
                            holder.value().displayName().getString()))
                    .toList();
        };
    }

    /**
     * Distinct input types across every template, in first-seen order. Used
     * by the INPUT filter's suggestions provider. {@code LinkedHashSet}
     * preserves the order so two consecutive identical typings yield the
     * same suggestion list rather than a shuffled one.
     */
    private Stream<Holder<PropertyType<?>>> distinctInputTypes() {
        Set<Holder<PropertyType<?>>> seen = new LinkedHashSet<>();
        for (PreviewTemplate t : this.templates) {
            for (PortDefinition input : t.node.definition().inputs()) {
                seen.add(input.type());
            }
        }
        return seen.stream();
    }

    /**
     * Hide tiles whose nodes don't satisfy every active filter clause and
     * re-pack the survivors into the grid origin so matches always fill
     * from the top-left without leaving gaps.
     *
     * <p>Active clauses come from two sources, both via the
     * {@link ChipFilterInput}: every committed chip (via {@link
     * ChipFilterInput#filters()}), and the currently-staged search text
     * (via {@link ChipFilterInput#currentMode()} / {@link
     * ChipFilterInput#stagedText()}). They AND together — a tile must
     * match every clause to remain visible.
     */
    private void applyFilter() {
        if (this.previewTiles.isEmpty() || this.filterInput == null) {
            return;
        }

        List<FilterClause> clauses = collectActiveClauses();

        // Origin is the first tile's position as arranged by the layout. All
        // tiles share the same row Y until we re-pack them.
        int originX = this.previewTiles.get(0).getX();
        int originY = this.previewTiles.get(0).getY();

        boolean selectedStillVisible = false;
        int visibleIndex = 0;
        for (int i = 0; i < this.previewTiles.size(); i++) {
            NodePreviewWidget tile = this.previewTiles.get(i);
            PreviewTemplate template = this.templates.get(i);

            boolean matches = matchesAll(template, clauses);
            tile.visible = matches;
            tile.active = matches;

            // Highlight ports contributed to the match. Union across clauses
            // so multiple INPUT chips light up every relevant input.
            tile.setHighlightedPorts(matches ? matchingPortsAcross(template, clauses) : Set.of());

            if (matches) {
                int row = visibleIndex / GRID_COLUMNS;
                int col = visibleIndex % GRID_COLUMNS;
                tile.setX(originX + col * (this.gridCellW + GRID_SPACING_X));
                tile.setY(originY + row * (this.gridCellH + GRID_SPACING_Y));
                visibleIndex++;

                if (template.ref == this.selectedRef) {
                    selectedStillVisible = true;
                }
            }
        }

        // If the previously selected tile got filtered out, clear the
        // selection so Submit can't commit a definition the user can't see.
        if (this.selectedRef != null && !selectedStillVisible) {
            this.selectedRef = null;
            if (this.submitButton != null) {
                this.submitButton.active = false;
            }
        }
    }

    /**
     * Gathers the active clause list — every chip's stored value (paired
     * with its filter mode) plus the staged text (if any) under the current
     * mode. Chip values are strings stored by the suggestions provider; the
     * raw-text Enter fallback in {@link ChipFilterInput} also stores
     * strings, so a single {@code instanceof String} extraction handles
     * both paths.
     */
    private List<FilterClause> collectActiveClauses() {
        List<FilterClause> clauses = new ArrayList<>();
        this.filterInput.filters().forEach(filter -> {
            if (filter.value() instanceof String needle && !needle.isEmpty()) {
                clauses.add(new FilterClause(filter.mode(), needle.toLowerCase(Locale.ROOT)));
            }
        });
        String staged = this.filterInput.stagedText().trim();
        if (!staged.isEmpty()) {
            clauses.add(new FilterClause(this.filterInput.currentMode(), staged.toLowerCase(Locale.ROOT)));
        }
        return clauses;
    }

    private static boolean matchesAll(PreviewTemplate template, List<FilterClause> clauses) {
        for (FilterClause c : clauses) {
            if (!c.mode().matches(template, c.needle())) {
                return false;
            }
        }
        return true;
    }

    private static Set<Port> matchingPortsAcross(PreviewTemplate template, List<FilterClause> clauses) {
        if (clauses.isEmpty()) {
            return Set.of();
        }
        Set<Port> hits = new HashSet<>();
        for (FilterClause c : clauses) {
            hits.addAll(c.mode().matchingPorts(template, c.needle()));
        }
        return hits;
    }

    /**
     * Selection callback handed to each {@link NodePreviewWidget}. Updates
     * {@link #selectedRef} so every tile re-evaluates {@code isSelected} on
     * its next render, and re-enables the Submit button now that there's a
     * concrete choice to commit.
     */
    private void selectRef(Holder.Reference<NodeDefinition> ref) {
        this.selectedRef = ref;
        if (this.submitButton != null) {
            this.submitButton.active = true;
        }
    }

    private void addActionsRow() {
        int buttonWidth = 60;
        int buttonHeight = 16;

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));

        footer.addChild(Button.builder(Component.literal("Cancel"), b -> this.close())
                .size(buttonWidth, buttonHeight)
                .build());

        this.submitButton = footer.addChild(Button.builder(Component.literal("Submit"), b -> this.submit())
                .size(buttonWidth, buttonHeight)
                .build());
    }

    private void submit() {
        if (this.selectedRef != null) {
            this.onSubmit.accept(this.selectedRef);
        }
        this.close();
    }

    private void close() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void onClose() {
        this.close();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // The screen only auto-unfocuses a widget when another widget claims
        // the click. Title (StringWidget) and empty regions don't claim, so
        // without this nudge the filter input keeps focus and the suggestion
        // popup stays up after the user clicks "anywhere else". Drop screen
        // focus before dispatching when the click is outside the filter
        // input's interactive area (its static bounds + the popup overlay,
        // both included in filterInput.isMouseOver).
        if (this.filterInput != null
                && !this.filterInput.isMouseOver(event.x(), event.y())) {
            this.setFocused(null);
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        // Suggestion popup is drawn last so it lands on top of the preview
        // grid below us. Stratum-bumping from inside filterInput's normal
        // render pass wouldn't help — stratum increments are global, so the
        // grid (rendered after filterInput in the renderable list) would end
        // up in the same elevated stratum and paint over the popup. Drawing
        // the popup here, after the screen has finished iterating widgets,
        // is the cleanest way to guarantee z-order.
        if (this.filterInput != null) {
            this.filterInput.renderSuggestionOverlay(graphics, mouseX, mouseY);
        }
    }

    private record PreviewTemplate(Holder.Reference<NodeDefinition> ref,
                                   Node node,
                                   NodeWidget widget,
                                   Component name) {
    }

    /**
     * A single filter constraint — a {@link FilterMode} plus the lowercased
     * needle it should be matched against. Recovered from
     * {@link ChipFilterInput#filters()} (whose value is the suggestion's
     * stored string) and from the staged text in
     * {@link ChipFilterInput#stagedText()}.
     */
    private record FilterClause(FilterMode mode, String needle) {
    }

    /**
     * The axis a search query is matched against. Each value owns its own
     * label, chip dot color, and matching logic; new filter axes are added
     * by extending the enum.
     *
     * <p>An empty needle is short-circuited at the call site — the per-mode
     * logic only handles real queries.
     */
    private enum FilterMode {
        /** Match the node's displayed name (translated, case-insensitive substring). */
        NAME(Component.literal("Name"), 0xFFB0B0B0) {
            @Override
            boolean matches(PreviewTemplate t, String needle) {
                return t.name().getString().toLowerCase(Locale.ROOT).contains(needle);
            }
            // No port-level highlights — the match is on the title text, not
            // on any one of the node's diamonds.
        },
        /**
         * Match any of the node's input port types. A node matches if at
         * least one input's {@link dev.robotgryphon.screenlib.types.PropertyType}
         * display name contains the needle. Mirrors the "Input: IMAGE" chips
         * in graph-style picker UIs.
         */
        INPUT(Component.literal("Input"), 0xFF6FA8FF) {
            @Override
            boolean matches(PreviewTemplate t, String needle) {
                for (PortDefinition input : t.node().definition().inputs()) {
                    if (typeNameMatches(input.type().value().displayName(), needle)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            Set<Port> matchingPorts(PreviewTemplate t, String needle) {
                // Iterate the runtime ports (which the preview's NodeWidget
                // actually draws) rather than PortDefinitions, so the set we
                // hand to the tile keys against the same Port instances it
                // would highlight.
                Set<Port> hits = new HashSet<>();
                for (Port port : t.node().ports()) {
                    if (port.side() != PortSide.LEFT) {
                        continue;
                    }
                    if (typeNameMatches(port.type().value().displayName(), needle)) {
                        hits.add(port);
                    }
                }
                return hits;
            }
        };

        private final Component label;
        private final int chipColor;

        FilterMode(Component label, int chipColor) {
            this.label = label;
            this.chipColor = chipColor;
        }

        Component label() {
            return this.label;
        }

        /** Fallback dot color for chips created without a suggestion (e.g. raw-text Enter). */
        int chipColor() {
            return this.chipColor;
        }

        /**
         * Returns {@code true} if {@code template} satisfies a non-empty
         * {@code needle} under this mode. Needle is already trimmed and
         * lowercased by the caller.
         */
        abstract boolean matches(PreviewTemplate t, String needle);

        /**
         * Returns the specific ports that contributed to the match — used by
         * the preview tile to paint highlight halos. The default is an empty
         * set; modes whose matching logic is port-scoped (like {@link #INPUT})
         * override this to expose the per-port hits.
         */
        Set<Port> matchingPorts(PreviewTemplate t, String needle) {
            return Set.of();
        }

        private static boolean typeNameMatches(Component displayName, String needle) {
            return displayName.getString().toLowerCase(Locale.ROOT).contains(needle);
        }
    }
}
