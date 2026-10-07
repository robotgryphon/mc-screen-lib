package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.client.GraphColors;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.client.ui.widget.chips.ChipFilterInput;
import dev.robotgryphon.screenlib.client.ui.widget.chips.ChipSuggestion;
import dev.robotgryphon.screenlib.client.ui.widget.NodePreviewWidget;
import dev.robotgryphon.screenlib.client.ui.widget.DragState;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.ports.Port;
import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import dev.robotgryphon.screenlib.graph.ports.StandardInputPort;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import dev.robotgryphon.screenlib.menu.AddNodeMenu;
import dev.robotgryphon.screenlib.network.AddNodeResultPayload;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public class AddNodeScreen extends Screen implements MenuAccess<AddNodeMenu> {
    private static final int FILTER_INPUT_W = 300;
    private static final int HEADER_ROW_SPACING = 4;
    private static final int HEADER_HEIGHT = 60;

    private static final int GRID_COLUMNS = 6;
    private static final int GRID_SPACING_X = 8;
    private static final int GRID_SPACING_Y = 4;

    private final AddNodeMenu menu;
    private final Canvas previewCanvas;
    private final List<Holder.Reference<NodeDefinition>> options;
    private boolean answered;

    private final List<PreviewTemplate> templates = new ArrayList<>();
    private final List<NodePreviewWidget> previewTiles = new ArrayList<>();

    private int gridCellW;
    private int gridCellH;

    private Holder.@Nullable Reference<NodeDefinition> selectedRef;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private ChipFilterInput<FilterMode> filterInput;
    private Button submitButton;

    public AddNodeScreen(AddNodeMenu menu, Inventory inventory, Component title) {
        super(Component.literal("Add Node"));
        this.menu = menu;
        var registries = Minecraft.getInstance().level.registryAccess();
        var definitions = registries.lookupOrThrow(NodeDefinition.REGISTRY_KEY);
        this.options = menu.data().available().stream()
                .flatMap(key -> definitions.get(key).stream())
                .toList();
        this.previewCanvas = new Canvas(menu.data().allowedValues().resolve(registries));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public AddNodeMenu getMenu() {
        return this.menu;
    }

    @Override
    protected void init() {
        super.init();

        Font font = Minecraft.getInstance().font;

        LinearLayout headerStack = LinearLayout.vertical().spacing(HEADER_ROW_SPACING);
        headerStack.defaultCellSetting().alignHorizontallyCenter();
        headerStack.addChild(new StringWidget(this.title, this.font));

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

        this.submitButton.active = false;

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

    private void buildTemplates() {
        this.templates.clear();
        for (Holder.Reference<NodeDefinition> ref : this.options) {
            Component name = Component.translatable(ref.key().identifier().toLanguageKey("node"));
            Node node = new Node(this.previewCanvas, ref, name, 0, 0);
            this.templates.add(new PreviewTemplate(ref, node, new NodeWidget(node, new DragState()), name));
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

        int scaledMaxW = 0;
        int scaledMaxH = 0;
        int maxLabelW = 0;
        for (PreviewTemplate template : this.templates) {
            scaledMaxW = Math.max(scaledMaxW, NodePreviewWidget.scale(template.widget().getWidth()));
            scaledMaxH = Math.max(scaledMaxH, NodePreviewWidget.scale(template.widget().getHeight()));
            maxLabelW = Math.max(maxLabelW, Math.round(this.font.width(template.name) * NodePreviewWidget.LABEL_SCALE));
        }

        int labelH = Math.round(this.font.lineHeight * NodePreviewWidget.LABEL_SCALE);
        this.gridCellW = Math.max(scaledMaxW, maxLabelW) + 2 * NodePreviewWidget.CELL_PADDING;
        this.gridCellH = scaledMaxH + NodePreviewWidget.LABEL_GAP + labelH + 2 * NodePreviewWidget.CELL_PADDING;
        final int sharedScaledNodeMaxH = scaledMaxH;

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

    private List<ChipSuggestion> suggestionsFor(FilterMode mode, String input) {
        String needle = input.toLowerCase(Locale.ROOT);
        return switch (mode) {
            case NAME -> this.templates.stream()
                    .map(PreviewTemplate::name)
                    .filter(name -> name.getString().toLowerCase(Locale.ROOT).contains(needle))
                    .map(name -> new ChipSuggestion(name, FilterMode.NAME.chipColor(), name.getString()))
                    .toList();
            case INPUT -> distinctInputTypes()
                    .filter(holder -> GraphDataType.displayName(holder).getString()
                            .toLowerCase(Locale.ROOT).contains(needle))
                    .map(holder -> new ChipSuggestion(
                            GraphDataType.displayName(holder),
                            GraphColors.of(holder),
                            GraphDataType.displayName(holder).getString()))
                    .toList();
        };
    }

    private Stream<Holder<GraphDataType<?>>> distinctInputTypes() {
        Set<Holder<GraphDataType<?>>> seen = new LinkedHashSet<>();
        for (PreviewTemplate t : this.templates) {
            for (Port port : t.node.portManager().ports()) {
                if (port instanceof StandardInputPort) {
                    seen.add(port.type());
                }
            }
        }
        return seen.stream();
    }

    private void applyFilter() {
        if (this.previewTiles.isEmpty() || this.filterInput == null) {
            return;
        }

        List<FilterClause> clauses = collectActiveClauses();

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

        if (this.selectedRef != null && !selectedStillVisible) {
            this.selectedRef = null;
            if (this.submitButton != null) {
                this.submitButton.active = false;
            }
        }
    }

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
            this.answer(Optional.of(this.selectedRef.key()));
        }
    }

    private void close() {
        this.answer(Optional.empty());
    }

    private void answer(Optional<ResourceKey<NodeDefinition>> selected) {
        if (this.answered) {
            return;
        }
        this.answered = true;
        ClientPacketDistributor.sendToServer(new AddNodeResultPayload(selected));
    }

    @Override
    public void onClose() {
        this.close();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.filterInput != null
                && !this.filterInput.isMouseOver(event.x(), event.y())) {
            this.setFocused(null);
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.filterInput != null) {
            this.filterInput.renderSuggestionOverlay(graphics, mouseX, mouseY);
        }
    }

    private record PreviewTemplate(Holder.Reference<NodeDefinition> ref,
                                   Node node,
                                   NodeWidget widget,
                                   Component name) {
    }

    private record FilterClause(FilterMode mode, String needle) {
    }

    private enum FilterMode {
        NAME(Component.literal("Name"), 0xFFB0B0B0) {
            @Override
            boolean matches(PreviewTemplate t, String needle) {
                return t.name().getString().toLowerCase(Locale.ROOT).contains(needle);
            }
        },
        INPUT(Component.literal("Input"), 0xFF6FA8FF) {
            @Override
            boolean matches(PreviewTemplate t, String needle) {
                for (Port port : t.node().portManager().ports()) {
                    if (port instanceof StandardInputPort
                            && typeNameMatches(GraphDataType.displayName(port.type()), needle)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            Set<Port> matchingPorts(PreviewTemplate t, String needle) {
                Set<Port> hits = new HashSet<>();
                for (Port port : t.node().portManager().ports()) {
                    if (!(port instanceof StandardInputPort)) {
                        continue;
                    }
                    if (typeNameMatches(GraphDataType.displayName(port.type()), needle)) {
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

        int chipColor() {
            return this.chipColor;
        }

        abstract boolean matches(PreviewTemplate t, String needle);

        Set<Port> matchingPorts(PreviewTemplate t, String needle) {
            return Set.of();
        }

        private static boolean typeNameMatches(Component displayName, String needle) {
            return displayName.getString().toLowerCase(Locale.ROOT).contains(needle);
        }
    }
}
