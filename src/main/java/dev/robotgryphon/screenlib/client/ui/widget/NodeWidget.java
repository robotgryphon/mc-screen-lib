package dev.robotgryphon.screenlib.client.ui.widget;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.CanvasViewport;
import dev.robotgryphon.screenlib.graph.Node;
import dev.robotgryphon.screenlib.graph.Port;
import dev.robotgryphon.screenlib.graph.PortSide;
import dev.robotgryphon.screenlib.types.PortDefinition;
import dev.robotgryphon.screenlib.types.PropertyDefinition;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.Holder;
import org.joml.Vector2dc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.joml.Vector2dc;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
 *   <li>Looking up connection state for property ports (which only become
 *       visible when wired) — done through a back-reference to the
 *       {@link Canvas} that {@code Canvas.addNode} installs</li>
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

    /**
     * Corner radius for the node body / title bar / outline. Picked to
     * match the ComfyUI reference's pronounced rounding while staying
     * legible at the Minecraft GUI's effective pixel scale.
     */
    static final int NODE_CORNER_RADIUS = 4;

    /** Pill background for property rows, slightly lighter than the body. */
    private static final int PROPERTY_ROW_COLOR = 0xCC2F303A;
    /** Pill background for a row that's being driven by an incoming wire — sunk a shade so it reads as "locked". */
    private static final int PROPERTY_ROW_DRIVEN_COLOR = 0xCC23242C;
    /** Property label color — softer than the value so the eye lands on the value first. */
    private static final int PROPERTY_LABEL_COLOR = 0xFFA0A4B3;
    /** Dimmer label for input-driven rows — signals "you can't edit this anymore" without disappearing. */
    private static final int PROPERTY_LABEL_DRIVEN_COLOR = 0xFF6E7180;
    /** Label color when an input is wired but the upstream hasn't supplied a value — warning red. */
    private static final int PROPERTY_LABEL_UNDRIVEN_COLOR = 0xFFE26060;
    /** Property value color — bright, matching the title bar text. */
    private static final int PROPERTY_VALUE_COLOR = 0xFFFFFFFF;
    /** Value color for a driven row — same dim grey family as the driven label so the row reads as a unit. */
    private static final int PROPERTY_VALUE_DRIVEN_COLOR = 0xFFC2C5CE;
    /** Placeholder rendered for a property whose value hasn't been set yet. */
    private static final String PROPERTY_UNSET_PLACEHOLDER = "—"; // em dash

    /**
     * Color for the "you can connect here" placeholder dot drawn on a
     * property row hover. Translucent so it reads as a soft hint rather
     * than a hard graphic element competing with the row text.
     */
    private static final int PROPERTY_PORT_PLACEHOLDER_COLOR = 0xCCAEB1BC;

    /**
     * Alpha multiplier applied to anything whose type doesn't match the
     * canvas's active drag type. Kept in sync with the value the canvas
     * widget uses for wires so the two reads as a single "this is faded"
     * visual treatment.
     */
    private static final float MISMATCHED_TYPE_ALPHA = 0.3f;

    private final Node node;

    /**
     * The canvas this widget lives on, installed by {@link Canvas#addNode}.
     * Null while the widget is detached (e.g., while it's being built as
     * part of {@code AddNodeDialog}'s preview grid). Used at render time
     * to query connection presence for property ports, which only become
     * visible once something is wired to them.
     */
    private @Nullable Canvas canvas;

    private boolean dragging;
    /** Offset from the widget's top-left to the mouse when the body drag started. */
    private double grabOffsetX;
    private double grabOffsetY;

    /**
     * Vertical layout frame that owns the property region's rows. Each
     * child is a {@link PropertyRow} sized to one row pitch tall, and
     * the layout's height matches {@link Node#propertyRegionHeight()}
     * exactly — so the equal-spacing distribution produces gaps of
     * precisely {@link Node#PROPERTY_ROW_GAP} pixels between consecutive
     * rows. The layout is the source of truth for where each row sits
     * during rendering and hit testing; the data-model math in
     * {@link Node#propertyRowTop(int)} mirrors it so port anchoring
     * stays consistent.
     *
     * <p>Null when the node has no properties — there's nothing to lay
     * out, and skipping the allocation keeps preview / portless nodes
     * lightweight.
     */
    private final @Nullable EqualSpacingLayout propertiesLayout;

    /**
     * Flat list of the {@code PropertyRow}s contained in
     * {@link #propertiesLayout}, kept in declaration order so callers
     * can map a property index to its row without round-tripping
     * through the layout's iteration API. Same lifetime as the layout —
     * built once in the constructor since the node's definition (and
     * therefore its property list) doesn't change at runtime.
     */
    private final List<PropertyRow> propertyRows;

    public NodeWidget(Node node) {
        super(node.x(), node.y(), node.width(), node.height(), node.title());
        this.node = node;

        // Build the property-row layout once. The layout's content
        // dimensions match Node's declared property region; arrange-time
        // positioning later (each render frame) places the rows at the
        // node's current screen-space coordinates so the layout follows
        // the node as it pans / zooms / drags.
        List<PortDefinition> properties = node.definition().properties();
        if (properties.isEmpty()) {
            this.propertiesLayout = null;
            this.propertyRows = List.of();
        } else {
            EqualSpacingLayout layout = new EqualSpacingLayout(
                    node.width(), node.propertyRegionHeight(),
                    EqualSpacingLayout.Orientation.VERTICAL);
            List<PropertyRow> rows = new ArrayList<>(properties.size());
            for (int i = 0; i < properties.size(); i++) {
                PropertyRow row = new PropertyRow(i);
                rows.add(row);
                layout.addChild(row);
            }
            this.propertiesLayout = layout;
            this.propertyRows = List.copyOf(rows);
        }
    }

    public Node node() {
        return this.node;
    }

    /**
     * Installs the canvas back-reference. Called by {@link Canvas#addNode}
     * (and the loadState path that goes through it). Idempotent — wiring
     * the same canvas twice is a no-op; wiring a different one swaps it in.
     */
    public void setCanvas(@Nullable Canvas canvas) {
        this.canvas = canvas;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    /**
     * Screen-space bounds for this node under {@code viewport}'s current
     * pan / zoom. Used by {@code CanvasWidget} when computing the
     * bounding box for the batched node-background PiP texture.
     */
    public ScreenRectangle screenBounds(CanvasViewport viewport) {
        Vector2dc tl = viewport.canvasToScreen(this.node.x(), this.node.y());
        Vector2dc br = viewport.canvasToScreen(
                this.node.x() + this.node.width(),
                this.node.y() + this.node.height());
        int sx0 = (int) Math.floor(tl.x());
        int sy0 = (int) Math.floor(tl.y());
        int sx1 = (int) Math.ceil(br.x());
        int sy1 = (int) Math.ceil(br.y());
        return new ScreenRectangle(sx0, sy0, sx1 - sx0, sy1 - sy0);
    }

    /**
     * Builds the per-node uniform entry for the batched background PiP.
     * Bounds are expressed in scaled (window) pixels relative to the
     * texture's top-left ({@code textureOriginX} / {@code textureOriginY},
     * in screen pixels) so the shader can use {@code gl_FragCoord}
     * directly. The title-height parameter is the strip from the top of
     * the node that should be painted in the title color, also in scaled
     * pixels.
     *
     * <p>{@code dropShadow} is forwarded to the shader as the
     * "render a soft offset shadow underneath this node" flag. Callers
     * (typically {@link dev.robotgryphon.screenlib.client.ui.widget.CanvasWidget})
     * pass {@code true} for the dragged batch so the node visually
     * lifts off the static layer, and {@code false} for the static
     * batch where every node sits flat on the canvas.
     */
    public NodeBackgroundUniform.Entry buildBackgroundEntry(CanvasViewport viewport,
                                                            double textureOriginX,
                                                            double textureOriginY,
                                                            float guiScale,
                                                            boolean dropShadow) {
        Vector2dc tl = viewport.canvasToScreen(this.node.x(), this.node.y());
        Vector2dc br = viewport.canvasToScreen(
                this.node.x() + this.node.width(),
                this.node.y() + this.node.height());

        float relX = (float) ((tl.x() - textureOriginX) * guiScale);
        float relY = (float) ((tl.y() - textureOriginY) * guiScale);
        float w = (float) ((br.x() - tl.x()) * guiScale);
        float h = (float) ((br.y() - tl.y()) * guiScale);

        boolean hovered = this.isHovered() || this.dragging;
        int bodyArgb = hovered ? BACKGROUND_HOVER_COLOR : BACKGROUND_COLOR;
        int borderArgb = this.dragging ? BORDER_DRAG_COLOR : BORDER_COLOR;

        float titleHeight = Node.TITLE_BAR_HEIGHT * viewport.zoom() * guiScale;

        return new NodeBackgroundUniform.Entry(
                new Vector4f(relX, relY, w, h),
                argbToVec(bodyArgb),
                argbToVec(TITLE_BAR_COLOR),
                argbToVec(borderArgb),
                titleHeight,
                dropShadow);
    }

    /** Decompose an ARGB int into normalized 0..1 RGBA for the shader uniforms. */
    private static Vector4fc argbToVec(int argb) {
        return new Vector4f(
                ARGB.redFloat(argb),
                ARGB.greenFloat(argb),
                ARGB.blueFloat(argb),
                ARGB.alphaFloat(argb));
    }

    /** Delegates to {@link Node#portAt} — kept here so callers iterating widgets still find ports. */
    public @Nullable Port portAt(double mouseX, double mouseY) {
        return this.node.portAt(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.isValidClickButton(event.buttonInfo())) {
            // Don't claim clicks that landed on a port — those are for the
            // screen to interpret as the start of a connection drag. By
            // returning false here we prevent ContainerEventHandler from
            // setting this widget as focused/dragging, which would
            // otherwise hijack the drag.
            if (this.portAt(event.x(), event.y()) != null) {
                return false;
            }
            // A click that lands on an inline property editor — the
            // +/- buttons on an integer row, etc. — consumes the click
            // entirely. Returning true keeps the node from entering
            // drag state (super.mouseClicked → onClick would set
            // dragging=true), which would otherwise have the node
            // sliding around as the user tried to nudge a value.
            if (this.handlePropertyEditorClick(event)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * Dispatches a click to whichever property row's editor it landed in,
     * applying any value change before returning. Returns {@code true}
     * when the click was inside an editor (whether or not the value
     * actually moved — e.g., the value column between the buttons), so
     * the caller knows to skip the node-drag pathway.
     *
     * <p>Driven rows are skipped: their value is the upstream's, not
     * locally editable, so a click on what would be the editor area
     * falls through to the normal drag behavior.
     */
    private boolean handlePropertyEditorClick(MouseButtonEvent event) {
        List<PortDefinition> properties = this.node.definition().properties();
        if (properties.isEmpty()) {
            return false;
        }
        int left = this.node.x();
        int width = this.node.width();
        int editorX = left + width - Node.PROPERTY_PADDING_X - Node.PROPERTY_VALUE_MIN_WIDTH;
        int editorWidth = Node.PROPERTY_VALUE_MIN_WIDTH;
        int editorHeight = Node.PROPERTY_PITCH;

        for (int i = 0; i < properties.size(); i++) {
            PortDefinition prop = properties.get(i);
            // Driven rows show the upstream's value — no local editing.
            if (this.findInputConnection(prop.name()) != null) continue;

            int editorY = this.node.propertyRowTop(i);
            Codec<?> codec = prop.type().value().codec();

            if (isNumericCodec(codec)) {
                if (!NumericPropertyEditor.isInside(event.x(), event.y(),
                        editorX, editorY, editorWidth, editorHeight)) {
                    continue;
                }
                this.applyNumericClick(prop, codec, event.x(), editorX, editorWidth);
                return true;
            }

            if (isDropdownProperty(prop)) {
                if (!DropdownEditor.isInside(event.x(), event.y(),
                        editorX, editorY, editorWidth, editorHeight)) {
                    continue;
                }
                this.openDropdownPopup(prop, editorX, editorY, editorWidth, editorHeight);
                return true;
            }
        }
        return false;
    }

    /**
     * True if a property's registered {@link PropertyDefinition} declares
     * a fixed value set and the property's value type is something the
     * dropdown editor knows how to render. Today that's just String —
     * other dropdown-able types (enums, integers picked from a known
     * set) can extend this check as they show up.
     */
    private static boolean isDropdownProperty(PortDefinition prop) {
        PropertyDefinition<?> def = prop.type().value();
        return def.codec() == Codec.STRING && def.allowedValues().isPresent();
    }

    /**
     * Marks {@code prop} as the focused property on this widget's node.
     * The popup itself is built lazily from the node's state each frame
     * (via {@link #buildFocusedPropertyPopup}) — storing only the name
     * here means a node-position change or value edit flows through the
     * popup naturally on the next frame without needing to rebuild and
     * reassign anything.
     *
     * <p>The editor bounds are unused at this point but kept on the
     * signature so the caller doesn't need to recompute them: when the
     * popup is rebuilt for render, it derives its position from the
     * node's live row geometry, not from whatever the editor bounds
     * were at click time. That keeps the popup pinned to its row even
     * if the node moves while the popup is open.
     */
    private void openDropdownPopup(PortDefinition prop,
                                   int editorX, int editorY, int editorWidth, int editorHeight) {
        this.node.setFocusedPropertyName(prop.name());
    }

    /**
     * True when this node has an open property popup. Used by the canvas
     * widget to find the one popup-bearing node out of all the nodes on
     * the canvas — popup state lives per-node, so the canvas iterates
     * rather than tracking a separate pointer. Doesn't need a viewport
     * because it's a state check, not a geometry build.
     */
    public boolean hasFocusedPropertyPopup() {
        return resolveFocusedProperty() != null;
    }

    /** Clears the focused-property marker on the underlying node. */
    public void clearFocusedProperty() {
        this.node.setFocusedPropertyName(null);
    }

    /**
     * Renders the current focused-property popup, if any. Caller is
     * responsible for invoking this <em>after</em> popping the canvas
     * pose — the popup is screen-anchored, not canvas-anchored, so
     * pan/zoom doesn't apply to it. The {@code viewport} is passed in
     * (rather than reached through {@code canvas}) because viewport
     * state now lives on the canvas widget, not the canvas itself.
     */
    public void renderFocusedPropertyPopup(GuiGraphicsExtractor graphics, CanvasViewport viewport,
                                           int mouseX, int mouseY) {
        DropdownPopup popup = buildFocusedPropertyPopup(viewport);
        if (popup != null) {
            popup.render(graphics, mouseX, mouseY);
        }
    }

    /**
     * Dispatches a screen-space click to the focused popup, if any.
     * Returns {@code true} when the click landed on the popup (an option
     * may have been selected — the popup's {@code onSelect} callback
     * handles writing the value and clearing focus). Returns
     * {@code false} when the click missed; the canvas widget interprets
     * that as "the popup was open but the user clicked outside it" and
     * clears focus modally.
     */
    public boolean handleFocusedPropertyPopupClick(CanvasViewport viewport,
                                                   double screenMouseX, double screenMouseY) {
        DropdownPopup popup = buildFocusedPropertyPopup(viewport);
        if (popup == null) return false;
        return popup.mouseClicked(screenMouseX, screenMouseY);
    }

    /**
     * Locates the currently focused property and verifies it's a
     * dropdown — without touching the viewport. Used by
     * {@link #hasFocusedPropertyPopup()} (a pure state check) and as the
     * first step inside {@link #buildFocusedPropertyPopup(CanvasViewport)}
     * (which then computes the popup's screen-space position).
     */
    private @Nullable FocusedRow resolveFocusedProperty() {
        String name = this.node.focusedPropertyName();
        if (name == null) {
            return null;
        }
        List<PortDefinition> properties = this.node.definition().properties();
        for (int i = 0; i < properties.size(); i++) {
            PortDefinition prop = properties.get(i);
            if (prop.name().equals(name) && isDropdownProperty(prop)) {
                return new FocusedRow(prop, i);
            }
        }
        return null;
    }

    /** Tuple of (property, row index) returned by {@link #resolveFocusedProperty}. */
    private record FocusedRow(PortDefinition prop, int rowIndex) {}

    /**
     * Lazily builds the popup object from the node's current focus
     * state and live row geometry. Returns {@code null} when nothing is
     * focused, when the focused property no longer exists, or when the
     * focused property isn't actually a dropdown (e.g., its allowed
     * value set was removed). Built fresh each call so the popup's
     * position tracks the node's current screen-space location instead
     * of getting stuck at wherever it was when the dropdown opened.
     */
    private @Nullable DropdownPopup buildFocusedPropertyPopup(CanvasViewport viewport) {
        FocusedRow focused = resolveFocusedProperty();
        if (focused == null) {
            return null;
        }
        PortDefinition prop = focused.prop();

        PropertyDefinition<?> def = prop.type().value();
        // The dropdown is currently String-only, gated by isDropdownProperty.
        // The unchecked cast is the standard shape — Codec<?> erases to a
        // String at runtime when the codec is Codec.STRING.
        @SuppressWarnings("unchecked")
        List<String> options = (List<String>) def.allowedValues().orElseThrow();

        String name = prop.name();
        Object current = this.node.propertyValue(name);
        String currentValue = current instanceof String s ? s : "";

        int editorWidth = Node.PROPERTY_VALUE_MIN_WIDTH;
        int editorHeight = Node.PROPERTY_PITCH;
        int editorX = this.node.x() + this.node.width()
                - Node.PROPERTY_PADDING_X - editorWidth;
        int editorY = this.node.propertyRowTop(focused.rowIndex());

        // Popup anchors at the editor's bottom-left in screen space.
        Vector2dc screenAnchor = viewport.canvasToScreen(editorX, editorY + editorHeight);
        int popupX = (int) screenAnchor.x();
        int popupY = (int) screenAnchor.y();
        int popupWidth = Math.max(editorWidth, (int) (editorWidth * viewport.zoom()));

        final String propName = name;
        return new DropdownPopup(popupX, popupY, popupWidth,
                options, currentValue, picked -> {
                    this.node.setPropertyValue(propName, picked);
                    this.node.setFocusedPropertyName(null);
                });
    }

    /**
     * Routes the click to whichever {@link NumericPropertyEditor} step
     * variant matches the property's codec, and writes the result back
     * through {@link Node#setPropertyValue} if it actually moved. The
     * dispatch is reference-equality on the codec singleton — {@code
     * Codec.INT}, {@code Codec.FLOAT}, {@code Codec.DOUBLE} are all
     * unique instances, so this is both correct and faster than a class
     * check on the boxed value.
     */
    private void applyNumericClick(PortDefinition prop, Codec<?> codec,
                                   double mouseX, int editorX, int editorWidth) {
        Object current = this.node.propertyValue(prop.name());
        if (codec == Codec.INT) {
            int v = current instanceof Integer iv ? iv : 0;
            int next = NumericPropertyEditor.applyIntClick(mouseX, editorX, editorWidth, v);
            if (next != v) this.node.setPropertyValue(prop.name(), next);
        } else if (codec == Codec.FLOAT) {
            float v = current instanceof Float fv ? fv : 0f;
            float next = NumericPropertyEditor.applyFloatClick(mouseX, editorX, editorWidth, v);
            if (next != v) this.node.setPropertyValue(prop.name(), next);
        } else if (codec == Codec.DOUBLE) {
            double v = current instanceof Double dv ? dv : 0.0;
            double next = NumericPropertyEditor.applyDoubleClick(mouseX, editorX, editorWidth, v);
            if (next != v) this.node.setPropertyValue(prop.name(), next);
        }
    }

    /**
     * True if the property's value type is one the {@link NumericPropertyEditor}
     * knows how to render and step. Comparing to the codec singletons
     * (rather than {@code instanceof}-checking the boxed value) keeps
     * the check correct for properties that haven't had their value set
     * yet — the codec is always present on the registered
     * {@link dev.robotgryphon.screenlib.types.PropertyDefinition}.
     */
    private static boolean isNumericCodec(Codec<?> codec) {
        return codec == Codec.INT || codec == Codec.FLOAT || codec == Codec.DOUBLE;
    }

    /**
     * Decodes a property's current value into a {@link Number} suitable
     * for the numeric editor's display. Falls back to a typed zero
     * ({@code 0}, {@code 0f}, {@code 0.0}) when the slot is unset so the
     * editor still renders something sensible — without this, the editor
     * would either crash or show "null" the moment a property without a
     * default got rendered.
     */
    private Number currentNumericValue(PortDefinition prop) {
        Object current = this.node.propertyValue(prop.name());
        Codec<?> codec = prop.type().value().codec();
        if (codec == Codec.INT) return current instanceof Integer iv ? iv : 0;
        if (codec == Codec.FLOAT) return current instanceof Float fv ? fv : 0f;
        if (codec == Codec.DOUBLE) return current instanceof Double dv ? dv : 0.0;
        return 0;
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

        // Body / title bar / outline are painted by the canvas-level
        // batched PiP shader (see CanvasWidget.extractNodeBackgrounds).
        // Only the on-top decorations — title text, property rows, ports —
        // are rendered CPU-side here.

        // Title text — centered in the title bar.
        Font font = Minecraft.getInstance().font;
        int titleColor = ARGB.color((int) (255 * this.getAlpha()), 0xFF, 0xFF, 0xFF);
        graphics.centeredText(font, this.node.title(), left + this.node.width() / 2, top + 2, titleColor);

        // Property rows in the body, between the title bar and the port band.
        // Rendered first so the port labels can overlay them at any z-order
        // (they don't, by virtue of the layout, but the ordering is the safe
        // default if a property ever extends into the port band's edge).
        // Pass the mouse position so embedded editors can show their hover
        // states (e.g., the numeric editor's button highlights).
        this.renderProperties(graphics, font, mouseX, mouseY);

        // Regular connection ports — diamond + per-port label. Property
        // ports anchor to the body and follow their own visibility rules
        // (hidden unless connected or row-hovered), so they're handled in
        // a separate pass after this loop.
        for (Port port : this.node.ports()) {
            if (port.isProperty()) continue;
            this.renderPort(graphics, font, port, mouseX, mouseY);
        }

        // Property ports last so any decoration they draw lands on top of
        // the row's pill fill, not underneath it.
        this.renderPropertyPorts(graphics, mouseX, mouseY);
    }

    /**
     * Renders the property region by arranging and drawing the
     * {@link #propertiesLayout}. The layout's position is updated each
     * frame so it tracks the node's current screen-space top-left;
     * {@code arrangeElements} then distributes the {@link PropertyRow}s
     * with their declared {@link Node#PROPERTY_ROW_GAP} between them,
     * and a final pass renders each row in turn.
     *
     * <p>Mouse coordinates are forwarded into each row so the embedded
     * editors can show their hover states (the numeric pill's button
     * highlights, the dropdown's elevated background, etc.).
     */
    private void renderProperties(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        if (this.propertiesLayout == null) {
            return;
        }
        this.propertiesLayout.setX(this.node.x());
        this.propertiesLayout.setY(this.node.propertyRegionTop());
        this.propertiesLayout.arrangeElements();
        for (PropertyRow row : this.propertyRows) {
            row.extractRenderState(graphics, mouseX, mouseY, 0f);
        }
    }

    /**
     * Draws a single property row at its current layout-assigned bounds.
     * The full row-mode resolution (driven / undriven / locally editable)
     * lives here so {@link PropertyRow} can stay a thin
     * {@link LayoutElement} wrapper — the row's behavior is too tied to
     * {@link NodeWidget}'s connection / canvas state to live in its own
     * top-level class.
     */
    private void renderPropertyRow(GuiGraphicsExtractor graphics, Font font,
                                   int rowIndex, int rowLeft, int rowTop,
                                   int rowWidth, int rowHeight,
                                   int mouseX, int mouseY) {
        PortDefinition prop = this.node.definition().properties().get(rowIndex);
        int rowBottom = rowTop + rowHeight;

        // Resolve the row's render mode. A row is "driven" when its
        // input port has at least one incoming connection; once driven,
        // the property's local value is no longer authoritative and the
        // upstream value is shown instead. Driven-but-empty (the wire
        // exists but the upstream has nothing to give) is the error
        // case — visible as a red label so the user knows the wire
        // isn't actually delivering anything yet.
        Connection inputConn = this.findInputConnection(prop.name());
        boolean driven = inputConn != null;
        Object upstreamValue = driven ? this.resolveUpstreamValue(inputConn) : null;
        boolean undriven = driven && upstreamValue == null;

        int pillColor = driven ? PROPERTY_ROW_DRIVEN_COLOR : PROPERTY_ROW_COLOR;
        int labelColor = undriven
                ? PROPERTY_LABEL_UNDRIVEN_COLOR
                : (driven ? PROPERTY_LABEL_DRIVEN_COLOR : PROPERTY_LABEL_COLOR);
        int valueColor = driven ? PROPERTY_VALUE_DRIVEN_COLOR : PROPERTY_VALUE_COLOR;

        // The entire row reads as a single typed unit — pill, label,
        // and value all dim together when the user is mid-drag from a
        // port of a different type, so a property can't accidentally
        // look like a valid drop target.
        float alpha = this.effectiveAlpha(prop.type());

        // Row pill — full-width darker fill so the property region reads
        // as distinct strips, like the screenshot's KSampler-style node.
        // The vertical strip between rows is owned by the surrounding
        // {@code EqualSpacingLayout}, so the pill fills its full
        // assigned height here without an inset.
        graphics.fill(rowLeft + 1, rowTop, rowLeft + rowWidth - 1, rowBottom,
                ARGB.multiply(pillColor, ARGB.white(alpha)));

        // Label (left), value (right). Vertically centered using the
        // font line height — the +1 nudges the text optical center down
        // to the pill's geometric center on the 14px row.
        int textY = rowTop + (rowHeight - font.lineHeight) / 2 + 1;

        Component label = Node.propertyLabel(prop);
        graphics.text(font, label,
                rowLeft + Node.PROPERTY_PADDING_X,
                textY,
                ARGB.multiply(labelColor, ARGB.white(alpha)),
                false);

        // Value area on the right edge of the row. Used either by the
        // typed inline editor (for editable, non-driven properties whose
        // codec we know how to render a control for) or as a plain
        // read-only text slot otherwise.
        int editorX = rowLeft + rowWidth - Node.PROPERTY_PADDING_X - Node.PROPERTY_VALUE_MIN_WIDTH;
        int editorY = rowTop;
        int editorWidth = Node.PROPERTY_VALUE_MIN_WIDTH;
        int editorHeight = rowHeight;

        if (!driven && isNumericCodec(prop.type().value().codec())) {
            // Numeric editor — handles int, float, and double via the
            // same +/- pill. The value falls back to a zero of the
            // matching type when nothing is set yet so the buttons
            // still operate from a sane starting point rather than
            // reading null.
            Number current = currentNumericValue(prop);
            NumericPropertyEditor.render(graphics, font,
                    editorX, editorY, editorWidth, editorHeight,
                    mouseX, mouseY,
                    current, alpha);
        } else if (!driven && isDropdownProperty(prop)) {
            // Dropdown editor — the property is a pick from a fixed
            // value set, so the row trigger reads the current value
            // with a chevron and the actual list opens as a popup
            // when clicked.
            Object current = this.node.propertyValue(prop.name());
            String display = current instanceof String s ? s : "";
            DropdownEditor.render(graphics, font,
                    editorX, editorY, editorWidth, editorHeight,
                    mouseX, mouseY,
                    display, alpha);
        } else {
            // Plain text path — driven rows (showing upstream value or
            // the red "undriven" blank), and any non-int property type
            // until those get their own editors.
            String valueText;
            if (undriven) {
                valueText = "";
            } else if (driven) {
                valueText = formatPropertyValue(upstreamValue);
            } else {
                valueText = formatPropertyValue(this.node.propertyValue(prop.name()));
            }

            if (!valueText.isEmpty()) {
                int valueWidth = font.width(valueText);
                graphics.text(font, Component.literal(valueText),
                        rowLeft + rowWidth - Node.PROPERTY_PADDING_X - valueWidth,
                        textY,
                        ARGB.multiply(valueColor, ARGB.white(alpha)),
                        false);
            }
        }
    }

    /**
     * A single property row living inside {@link #propertiesLayout}.
     * Holds nothing but the row's index in the node's property list and
     * the bounds the layout assigned during {@code arrangeElements};
     * the actual drawing is delegated back to
     * {@link NodeWidget#renderPropertyRow} so the row's mode-resolution
     * logic (driven / undriven / editable / dropdown) stays alongside
     * the rest of {@code NodeWidget}'s state-aware rendering.
     *
     * <p>Constant-sized: every row is exactly one property-pitch tall
     * and as wide as the node. The width matches the node's content
     * width snapshot taken at construction time, which is also the
     * width baked into the parent {@link EqualSpacingLayout}; nodes
     * don't resize at runtime so this is safe.
     *
     * <p>Implements {@link LayoutElement} so the layout can position
     * it, and {@link Renderable} so the render path can call
     * {@code extractRenderState} symmetrically with any other Mojang
     * widget. {@link #visitWidgets} is empty — the row contains no
     * {@link AbstractWidget} children of its own.
     */
    private final class PropertyRow implements LayoutElement, Renderable {

        private final int rowIndex;
        private int x;
        private int y;
        private int width;
        private int height;

        PropertyRow(int rowIndex) {
            this.rowIndex = rowIndex;
            this.x = 0;
            this.y = 0;
            // Each row is exactly one property pitch tall and as wide as
            // the host node. The layout reads these on add and uses them
            // when distributing children.
            this.width = NodeWidget.this.node.width();
            this.height = Node.PROPERTY_PITCH;
        }

        @Override public int getX() { return this.x; }
        @Override public int getY() { return this.y; }
        @Override public int getWidth() { return this.width; }
        @Override public int getHeight() { return this.height; }

        @Override public void setX(int x) { this.x = x; }
        @Override public void setY(int y) { this.y = y; }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> visitor) {
            // No inner AbstractWidget children — the row draws itself
            // directly using {@code GuiGraphicsExtractor} primitives.
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics,
                                       int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            NodeWidget.this.renderPropertyRow(graphics, font,
                    this.rowIndex, this.x, this.y, this.width, this.height,
                    mouseX, mouseY);
        }
    }

    /**
     * Returns the alpha to render typed visuals at. When the canvas isn't
     * mid-drag (or this widget is detached, so it can't tell), returns the
     * widget's normal alpha unchanged. Mid-drag, anything whose type
     * doesn't match the source port's type is faded by
     * {@link #MISMATCHED_TYPE_ALPHA} so legal targets visually pop.
     *
     * <p>Type identity goes through {@code Holder.value()} because
     * {@link dev.robotgryphon.screenlib.types.PropertyDefinition} instances are
     * registry singletons — reference equality on the resolved value is
     * the same notion of "same type" {@link Canvas#connect} uses to
     * gate connections.
     */
    private float effectiveAlpha(Holder<PropertyDefinition<?>> type) {
        float base = this.getAlpha();
        if (this.canvas == null) {
            return base;
        }
        Holder<PropertyDefinition<?>> active = this.canvas.activeDragType();
        if (active == null) {
            return base;
        }
        return type.value() == active.value() ? base : base * MISMATCHED_TYPE_ALPHA;
    }

    /**
     * Finds an active connection whose target is the named property's
     * input port on this node. Returns {@code null} when nothing is wired
     * to that input, or when the widget is detached from a canvas (no
     * connection list to consult).
     */
    private @Nullable Connection findInputConnection(String propertyName) {
        if (this.canvas == null) {
            return null;
        }
        for (Connection connection : this.canvas.connections()) {
            Port target = connection.targetPort();
            if (target.node() != this.node) continue;
            if (!target.isProperty()) continue;
            if (target.side() != PortSide.LEFT) continue;
            if (propertyName.equals(target.propertyName())) {
                return connection;
            }
        }
        return null;
    }

    /**
     * Best-effort "what value is flowing into this input?" lookup. Only
     * property outputs carry a value today (their owning node's local
     * property value); regular output ports don't have a value pipeline
     * behind them, so a wire from one of those is treated as "connected
     * but undriven" and triggers the red-label state.
     *
     * <p>Resolution is intentionally non-recursive: if the upstream
     * property is itself driven by yet another wire, we still look at its
     * own local value rather than chasing the chain. Chained evaluation
     * is a follow-up once the graph has real semantics for what an output
     * "produces"; for now this is purely a visual reflection.
     *
     * <p>When the source's slot is null — typically because its registered
     * {@code PropertyDefinition} declared no default — the lookup mirrors
     * the editor's own null fallback (see {@link #currentNumericValue})
     * so the driven row displays the same zero value the source's editor
     * would. Without this, the source could show {@code "0"} on its own
     * row while the target showed an empty red-label "undriven" state,
     * making it look like the wire was broken when it was working fine.
     */
    private @Nullable Object resolveUpstreamValue(Connection connection) {
        Port source = connection.sourcePort();
        if (!source.isProperty()) {
            return null;
        }
        Object value = source.node().propertyValue(source.propertyName());
        if (value != null) {
            return value;
        }
        Codec<?> codec = source.type().value().codec();
        if (codec == Codec.INT) return 0;
        if (codec == Codec.FLOAT) return 0f;
        if (codec == Codec.DOUBLE) return 0.0;
        // String / bool / non-numeric types intentionally fall through to
        // null: there's no obvious "zero" string or bool to show, and a
        // missing source value on those should keep reading as a wiring
        // issue (red label) rather than silently substituting "".
        return null;
    }

    /**
     * Renders the implicit input/output ports for each property. A property
     * port draws in one of three modes:
     *
     * <ol>
     *   <li><b>Connected</b> — there's at least one {@link Connection}
     *       touching this port. Drawn as a colored diamond matching the
     *       property type, exactly like a regular port.</li>
     *   <li><b>Port-hovered</b> — the mouse is on the port itself (within
     *       the hit radius). Drawn in the hover-yellow so the user knows
     *       the click target is live.</li>
     *   <li><b>Row-hovered</b> — the mouse is somewhere over the property's
     *       row, but not exactly on the port. Drawn as a translucent gray
     *       circle: a soft "you can connect here" affordance that goes
     *       away as soon as the cursor leaves the row.</li>
     * </ol>
     *
     * Otherwise the port renders nothing — keeping the node's body uncluttered
     * for properties that are purely local-value configuration.
     */
    private void renderPropertyPorts(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<PortDefinition> properties = this.node.definition().properties();
        if (properties.isEmpty()) {
            return;
        }
        int nodeLeft = this.node.x();
        int nodeRight = nodeLeft + this.node.width();
        Port hoveredPort = this.portAt(mouseX, mouseY);

        for (int i = 0; i < properties.size(); i++) {
            int rowTop = this.node.propertyRowTop(i);
            int rowBottom = rowTop + Node.PROPERTY_PITCH;
            boolean rowHovered = mouseX >= nodeLeft && mouseX < nodeRight
                    && mouseY >= rowTop && mouseY < rowBottom;
            String propertyName = properties.get(i).name();

            for (Port port : this.node.ports()) {
                if (!port.isProperty()) continue;
                if (!propertyName.equals(port.propertyName())) continue;

                boolean connected = isPortConnected(port);
                boolean portHovered = hoveredPort == port;
                if (!connected && !rowHovered && !portHovered) {
                    continue;
                }

                Vector2fc center = this.node.portCenter(port);
                int px = (int) center.x();
                int py = (int) center.y();
                float alpha = this.effectiveAlpha(port.type());

                if (connected || portHovered) {
                    // Match the regular port look so the user reads "this
                    // is a live connection point" the same way they do for
                    // explicit ports.
                    int fill = portHovered ? PORT_HOVER_COLOR : port.color();
                    fillDiamond(graphics, px, py, Node.PORT_RADIUS + 1,
                            ARGB.multiply(PORT_OUTLINE_COLOR, ARGB.white(alpha)));
                    fillDiamond(graphics, px, py, Node.PORT_RADIUS,
                            ARGB.multiply(fill, ARGB.white(alpha)));
                } else {
                    // Row-hover affordance — soft circle, no outline.
                    fillCircle(graphics, px, py, Node.PORT_RADIUS,
                            ARGB.multiply(PROPERTY_PORT_PLACEHOLDER_COLOR, ARGB.white(alpha)));
                }
            }
        }
    }

    /**
     * Looks up whether any active connection touches {@code port}. Returns
     * false when the widget is detached (no canvas back-reference) so
     * preview / standalone renders don't accidentally light up property
     * ports based on stale state.
     */
    private boolean isPortConnected(Port port) {
        if (this.canvas == null) {
            return false;
        }
        for (Connection connection : this.canvas.connections()) {
            if (connection.sourcePort() == port || connection.targetPort() == port) {
                return true;
            }
        }
        return false;
    }

    /**
     * String form of a property value for display. Until typed editor
     * widgets land, this is a best-effort {@code toString()} that handles
     * the null case explicitly so an unset property doesn't render as
     * the literal word "null".
     */
    private static String formatPropertyValue(@Nullable Object value) {
        if (value == null) {
            return PROPERTY_UNSET_PLACEHOLDER;
        }
        return value.toString();
    }

    private void renderPort(GuiGraphicsExtractor graphics, Font font, Port port, int mouseX, int mouseY) {
        Vector2fc center = this.node.portCenter(port);
        // Truncate to the integer pixel anchor used by the diamond geometry;
        // the +0.5 in portCenter() is for the bezier endpoint, not for raster.
        int px = (int) center.x();
        int py = (int) center.y();
        boolean hovered = this.portAt(mouseX, mouseY) == port;
        int fill = hovered ? PORT_HOVER_COLOR : port.color();
        float alpha = this.effectiveAlpha(port.type());

        // Outline first, fill on top — the outline diamond is one pixel larger
        // on every side, so the fill leaves a 1px dark border that reads cleanly
        // against the panel background regardless of port color. Both diamonds
        // ride the same effective alpha so the port either dims or doesn't as a
        // single graphic, never an outline-without-fill ghost.
        fillDiamond(graphics, px, py, Node.PORT_RADIUS + 1,
                ARGB.multiply(PORT_OUTLINE_COLOR, ARGB.white(alpha)));
        fillDiamond(graphics, px, py, Node.PORT_RADIUS,
                ARGB.multiply(fill, ARGB.white(alpha)));

        Component title = port.title();
        if (title == null || title.getString().isEmpty()) {
            return;
        }
        int labelColor = ARGB.color((int) (255 * alpha), 0xCC, 0xCC, 0xD4);
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

    /**
     * Scanline rasterizer for a filled disc of radius {@code r}. Each row's
     * half-width is the integer floor of {@code sqrt(r² - dy²) + 0.5}, which
     * gives the same rounded silhouette vanilla uses for small UI badges
     * without leaning on antialiasing.
     */
    private static void fillCircle(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
        int r2 = r * r;
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.floor(Math.sqrt(r2 - dy * dy) + 0.5);
            graphics.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
