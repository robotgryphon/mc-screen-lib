package dev.robotgryphon.screenlib.client.ui.widget.canvas;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.robotgryphon.screenlib.client.GraphColors;
import dev.robotgryphon.screenlib.client.ui.core.UiHost;
import dev.robotgryphon.screenlib.client.ui.state.DragState;
import dev.robotgryphon.screenlib.client.ui.widget.chips.ChipFilterInput;
import dev.robotgryphon.screenlib.client.ui.widget.chips.ChipSuggestion;
import dev.robotgryphon.screenlib.client.ui.widget.node.NodePreviewWidget;
import dev.robotgryphon.screenlib.client.ui.widget.node.NodeWidget;
import dev.robotgryphon.screenlib.client.ui.widget.panel.Panel;
import dev.robotgryphon.screenlib.graph.canvas.client.ClientCanvasState;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.ports.InputPort;
import dev.robotgryphon.screenlib.graph.nodes.ports.Port;
import dev.robotgryphon.screenlib.graph.nodes.properties.AllowedValues;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import dev.robotgryphon.screenlib.graph.sync.AllowedValuesSnapshot;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Vector2d;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AddNodePanel extends Panel {
    private static final int WIDTH = 220;
    private static final int HEIGHT = 220;
    private static final int COLUMNS = 2;
    private static final int GRID_GAP = 4;
    private static final int LIST_GAP = 4;
    private static final int DRAG_THRESHOLD = 4;
    private static final int GHOST_OFFSET = 6;
    private static final int OFFSCREEN = -10_000;

    private record Entry(Holder.Reference<NodeDefinition> definition, Node node, NodePreviewWidget preview) {
        Component name() {
            return this.node.title;
        }

        List<Port> inputs() {
            return this.node.portManager().ports().stream().filter(port -> port instanceof InputPort).toList();
        }
    }

    private record Clause(FilterMode mode, String needle) {}

    private record Press(Entry entry, double x, double y) {}

    private final UiHost host;
    private final List<Entry> entries;
    private final ChipFilterInput<FilterMode> filter;
    private final int cellHeight;
    private List<Entry> visible;
    private int scroll;
    private @Nullable Press press;
    private boolean dragging;

    public AddNodePanel(UiHost host) {
        super(WIDTH, HEIGHT, Component.literal("Add node"));
        this.host = host;

        var registries = Minecraft.getInstance().level.registryAccess();
        AllowedValues allowed = host.panel(CanvasPanel.class)
                .map(panel -> panel.session().menu().allowedValues().resolve())
                .orElseGet(() -> AllowedValuesSnapshot.capture(registries).resolve());
        ClientCanvasState previews = new ClientCanvasState(allowed);
        List<Holder.Reference<NodeDefinition>> definitions = registries.lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                .listElements()
                .filter(definition -> !definition.value().hidden())
                .toList();

        List<NodeWidget> widgets = new ArrayList<>();
        List<Node> nodes = new ArrayList<>();
        int maxHeight = 0;
        for (Holder.Reference<NodeDefinition> definition : definitions) {
            Node node = new Node(previews.graph(), UUID.randomUUID(), definition, Node.defaultTitle(definition.key()));
            NodeWidget widget = new NodeWidget(node, previews, new DragState());
            nodes.add(node);
            widgets.add(widget);
            maxHeight = Math.max(maxHeight, NodePreviewWidget.scale(widget.getHeight()));
        }
        int labelHeight = Math.round(Minecraft.getInstance().font.lineHeight * NodePreviewWidget.LABEL_SCALE);
        this.cellHeight = maxHeight + NodePreviewWidget.LABEL_GAP + labelHeight + 2 * NodePreviewWidget.CELL_PADDING;

        List<Entry> built = new ArrayList<>();
        for (int i = 0; i < definitions.size(); i++) {
            Node node = nodes.get(i);
            NodePreviewWidget preview = new NodePreviewWidget(this.cellWidth(), this.cellHeight, maxHeight,
                    node, widgets.get(i), node.title,
                    () -> this.press != null && this.press.entry().node() == node);
            built.add(new Entry(definitions.get(i), node, preview));
        }
        built.sort((a, b) -> a.name().getString().compareToIgnoreCase(b.name().getString()));
        this.entries = List.copyOf(built);
        this.visible = this.entries;

        this.filter = new ChipFilterInput<>(0, 0, WIDTH - 2 * PADDING, FilterMode.NAME, List.of(FilterMode.values()),
                FilterMode::label, FilterMode::chipColor, this::suggestions);
        this.filter.setOnChange(this::applyFilter);
    }

    private int cellWidth() {
        return (WIDTH - 2 * PADDING - (COLUMNS - 1) * GRID_GAP) / COLUMNS;
    }

    private ScreenRectangle gridArea() {
        ScreenRectangle content = this.contentArea();
        if (this.filter.getX() != content.left() || this.filter.getY() != content.top()) {
            this.filter.setPosition(content.left(), content.top());
        }
        int top = this.filter.getBottom() + LIST_GAP;
        return new ScreenRectangle(content.left(), top, content.width(), Math.max(0, content.bottom() - top));
    }

    private int rows() {
        return (this.visible.size() + COLUMNS - 1) / COLUMNS;
    }

    private int maxScroll() {
        int height = this.rows() * (this.cellHeight + GRID_GAP) - GRID_GAP;
        return Math.max(0, height - this.gridArea().height());
    }

    private ScreenRectangle layoutGrid() {
        ScreenRectangle grid = this.gridArea();
        for (int i = 0; i < this.visible.size(); i++) {
            int column = i % COLUMNS;
            int row = i / COLUMNS;
            this.visible.get(i).preview().setPosition(grid.left() + column * (this.cellWidth() + GRID_GAP),
                    grid.top() + row * (this.cellHeight + GRID_GAP) - this.scroll);
        }
        return grid;
    }

    private @Nullable Entry entryAt(double mouseX, double mouseY) {
        ScreenRectangle grid = this.layoutGrid();
        if (!grid.containsPoint(Mth.floor(mouseX), Mth.floor(mouseY)) || this.filter.isMouseOver(mouseX, mouseY)) {
            return null;
        }
        for (Entry entry : this.visible) {
            if (entry.preview().getRectangle().containsPoint(Mth.floor(mouseX), Mth.floor(mouseY))) {
                return entry;
            }
        }
        return null;
    }

    private List<Clause> clauses() {
        List<Clause> clauses = new ArrayList<>();
        this.filter.filters().forEach(chip -> {
            if (chip.value() instanceof String needle && !needle.isEmpty()) {
                clauses.add(new Clause(chip.mode(), needle.toLowerCase(Locale.ROOT)));
            }
        });
        String staged = this.filter.stagedText().trim();
        if (!staged.isEmpty()) {
            clauses.add(new Clause(this.filter.currentMode(), staged.toLowerCase(Locale.ROOT)));
        }
        return clauses;
    }

    private void applyFilter() {
        List<Clause> clauses = this.clauses();
        List<Entry> matching = new ArrayList<>();
        for (Entry entry : this.entries) {
            boolean matches = clauses.stream().allMatch(clause -> clause.mode().matches(entry, clause.needle()));
            Set<Port> highlighted = new HashSet<>();
            if (matches) {
                matching.add(entry);
                clauses.forEach(clause -> highlighted.addAll(clause.mode().matchingPorts(entry, clause.needle())));
            }
            entry.preview().setHighlightedPorts(highlighted);
        }
        this.visible = List.copyOf(matching);
        this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
    }

    private List<ChipSuggestion> suggestions(FilterMode mode, String input) {
        String needle = input.toLowerCase(Locale.ROOT);
        return switch (mode) {
            case NAME -> this.entries.stream()
                    .map(Entry::name)
                    .filter(name -> name.getString().toLowerCase(Locale.ROOT).contains(needle))
                    .map(name -> new ChipSuggestion(name, FilterMode.NAME.chipColor(), name.getString()))
                    .toList();
            case INPUT -> this.entries.stream()
                    .flatMap(entry -> entry.inputs().stream())
                    .map(Port::type)
                    .distinct()
                    .filter(type -> GraphDataType.displayName(type).getString().toLowerCase(Locale.ROOT).contains(needle))
                    .map(type -> new ChipSuggestion(GraphDataType.displayName(type), GraphColors.of(type),
                            GraphDataType.displayName(type).getString()))
                    .toList();
        };
    }

    private Optional<CanvasPanel> canvasAt(double mouseX, double mouseY) {
        if (this.isMouseOver(mouseX, mouseY)) {
            return Optional.empty();
        }
        List<CanvasPanel> panels = this.host.panels(CanvasPanel.class);
        for (int i = panels.size() - 1; i >= 0; i--) {
            CanvasPanel panel = panels.get(i);
            if (panel.isOpen() && panel.canvas().isMouseOver(mouseX, mouseY)) {
                return Optional.of(panel);
            }
        }
        return Optional.empty();
    }

    private void addToCenter(Entry entry) {
        this.host.panels(CanvasPanel.class).stream().filter(Panel::isOpen).findFirst().ifPresent(panel -> {
            CanvasBounds view = panel.canvas().viewBounds();
            panel.session().addNode(entry.definition().key(), new Vector2d(view.centerX(), view.centerY()));
        });
    }

    private void drop(Entry entry, double mouseX, double mouseY) {
        this.canvasAt(mouseX, mouseY).ifPresent(panel -> panel.session().addNode(entry.definition().key(),
                panel.canvas().viewport().screenToCanvas(mouseX, mouseY)));
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, ScreenRectangle content, int mouseX, int mouseY) {
        ScreenRectangle grid = this.layoutGrid();
        this.filter.extractRenderState(graphics, mouseX, mouseY, 0);

        boolean pointing = !this.dragging && !this.filter.isMouseOver(mouseX, mouseY);
        int previewX = pointing ? mouseX : OFFSCREEN;
        int previewY = pointing ? mouseY : OFFSCREEN;
        graphics.enableScissor(grid.left(), grid.top(), grid.right(), grid.bottom());
        for (Entry entry : this.visible) {
            ScreenRectangle cell = entry.preview().getRectangle();
            if (cell.bottom() > grid.top() && cell.top() < grid.bottom()) {
                entry.preview().extractRenderState(graphics, previewX, previewY, 0);
            }
        }
        graphics.disableScissor();

        graphics.nextStratum();
        this.filter.renderSuggestionOverlay(graphics, mouseX, mouseY);

        if (this.dragging && this.press != null) {
            graphics.nextStratum();
            NodePreviewWidget ghost = this.press.entry().preview();
            ghost.setPosition(mouseX + GHOST_OFFSET, mouseY + GHOST_OFFSET);
            ghost.extractRenderState(graphics, OFFSCREEN, OFFSCREEN, 0);
        }
    }

    @Override
    public @Nullable CursorType cursor(double mouseX, double mouseY) {
        if (this.dragging) {
            return this.canvasAt(mouseX, mouseY).isPresent() ? null : CursorTypes.NOT_ALLOWED;
        }
        return super.cursor(mouseX, mouseY);
    }

    @Override
    protected boolean contentClicked(MouseButtonEvent event, boolean doubleClick) {
        this.gridArea();
        if (this.filter.isMouseOver(event.x(), event.y())) {
            this.filter.setFocused(true);
            return this.filter.mouseClicked(event, doubleClick);
        }
        this.filter.setFocused(false);
        Entry entry = this.entryAt(event.x(), event.y());
        if (entry != null) {
            this.press = new Press(entry, event.x(), event.y());
            this.dragging = false;
        }
        return true;
    }

    @Override
    protected boolean contentDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.press == null) {
            return false;
        }
        if (!this.dragging && Math.hypot(event.x() - this.press.x(), event.y() - this.press.y()) >= DRAG_THRESHOLD) {
            this.dragging = true;
        }
        return true;
    }

    @Override
    protected boolean contentReleased(MouseButtonEvent event) {
        Press press = this.press;
        boolean dragged = this.dragging;
        this.press = null;
        this.dragging = false;
        if (press == null) {
            return false;
        }
        if (dragged) {
            this.drop(press.entry(), event.x(), event.y());
        } else if (this.entryAt(event.x(), event.y()) == press.entry()) {
            this.addToCenter(press.entry());
        }
        return true;
    }

    @Override
    protected boolean contentScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = (this.cellHeight + GRID_GAP) / 2;
        this.scroll = Mth.clamp(this.scroll - (int) Math.round(scrollY * step), 0, this.maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.filter.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return this.filter.charTyped(event);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.filter.setFocused(false);
        }
    }

    private enum FilterMode {
        NAME(Component.literal("Name"), 0xFFB0B0B0) {
            @Override
            boolean matches(Entry entry, String needle) {
                return entry.name().getString().toLowerCase(Locale.ROOT).contains(needle);
            }
        },
        INPUT(Component.literal("Input"), 0xFF6FA8FF) {
            @Override
            boolean matches(Entry entry, String needle) {
                return !this.matchingPorts(entry, needle).isEmpty();
            }

            @Override
            Set<Port> matchingPorts(Entry entry, String needle) {
                Set<Port> hits = new HashSet<>();
                for (Port port : entry.inputs()) {
                    if (GraphDataType.displayName(port.type()).getString().toLowerCase(Locale.ROOT).contains(needle)) {
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

        abstract boolean matches(Entry entry, String needle);

        Set<Port> matchingPorts(Entry entry, String needle) {
            return Set.of();
        }
    }
}
