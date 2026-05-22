package dev.robotgryphon.screenlib.client.ui.widget;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import dev.robotgryphon.screenlib.client.ui.widget.property.BooleanPropertyEditor;
import dev.robotgryphon.screenlib.client.ui.widget.property.DropdownEditor;
import dev.robotgryphon.screenlib.client.ui.widget.property.DropdownPopup;
import dev.robotgryphon.screenlib.client.ui.widget.property.NumericPropertyEditor;
import dev.robotgryphon.screenlib.client.ui.widget.property.PropertyEditor;
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
import net.minecraft.util.Util;
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
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Fraction of the way from a port's base color toward white that
     * its outline ring sits at. Tuned high enough that the outline
     * reads as a clearly distinct "halo" against the inner fill at any
     * type color, without turning the ring into a featureless white
     * smear.
     */
    public static final float PORT_OUTLINE_LIGHTEN = 0.30f;

    /**
     * Same idea as {@link #PORT_OUTLINE_LIGHTEN} but applied while the
     * cursor is hovering the port — the lighter shade signals "this is
     * the live click target" without changing the port's hue. Both
     * lighten strengths are intentionally relative to the base color
     * so every port type contrasts the same way against its outline.
     */
    public static final float PORT_HOVER_OUTLINE_LIGHTEN = 0.55f;

    /**
     * Multiplier on the port's visual radius while hovered. Picks a
     * "noticeable but not jumpy" growth — the user sees the port
     * expand a few pixels under their cursor, confirming the hit
     * target, without the surrounding label getting pushed visibly.
     */
    public static final float PORT_HOVER_RADIUS_FACTOR = 1.3f;

    /**
     * Exponential decay speed for the hover-progress animation, in
     * inverse seconds. {@code 18.0} means {@code 1 - e^(-18 * 0.16) ≈
     * 0.94} of the way from 0 to 1 after ~160ms — fast enough to feel
     * responsive (the user perceives the port expanding "right when"
     * they hover), slow enough that the growth and the halo brighten
     * both read as a smooth transition rather than a snap. Used for
     * both the radius lerp and the outline-lighten lerp so the two
     * stay visually coupled.
     */
    private static final float PORT_HOVER_ANIM_SPEED = 18f;

    /**
     * Corner radius for the node body / title bar / outline. Picked to
     * match the ComfyUI reference's pronounced rounding while staying
     * legible at the Minecraft GUI's effective pixel scale.
     */
    static final int NODE_CORNER_RADIUS = 4;

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
     * Per-port hover-animation progress, keyed by port reference (identity
     * — two structurally equal ports on different node rebuilds are still
     * distinct entries). {@code 0} means "fully at rest"; {@code 1} means
     * "fully hovered." Each frame the progress is nudged toward its
     * target via an exponential lerp keyed by wall-clock delta, so a port
     * smoothly grows / brightens when the cursor lands on it and smoothly
     * shrinks / dims when the cursor leaves. Lives on the widget (not the
     * model) because it's a pure view-layer affordance — undo / persistence
     * shouldn't care about it.
     */
    private final Map<Port, Float> portHoverProgress = new IdentityHashMap<>();

    /**
     * Wall-clock time of the most recent {@link #appendPortEntries} call,
     * used to compute the per-frame delta for the hover animation. Zero
     * means "no prior frame yet" — the next call seeds the timestamp
     * without advancing any progress (so a freshly-attached widget
     * doesn't jump on its first render frame).
     */
    private long lastPortAnimMillis;

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
            // The layout's height is the *inner content* span: rows plus
            // the inter-row gaps, no top / bottom padding. The padding
            // ({@link Node#PROPERTY_REGION_PADDING_Y}) is applied at
            // positioning time in {@link #renderProperties}, by offsetting
            // the layout's Y past the region top. This keeps the
            // equal-spacing distribution producing exactly
            // {@link Node#PROPERTY_ROW_GAP} between consecutive rows
            // (it would otherwise spread the padding into the gaps too).
            int n = properties.size();
            int contentHeight = n * Node.PROPERTY_PITCH
                    + Math.max(0, n - 1) * Node.PROPERTY_ROW_GAP;
            EqualSpacingLayout layout = new EqualSpacingLayout(
                    node.width(), contentHeight,
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
        int titleArgb = TITLE_BAR_COLOR;
        int borderArgb = this.dragging ? BORDER_DRAG_COLOR : BORDER_COLOR;

        // Optional per-node tint — blended into the body and title at
        // a fixed strength so the node still reads as a node (dark
        // base shade with hover lift), just a different family. The
        // border / drag-border are left untinted so the focus state
        // remains visually distinct across all tints.
        Integer tint = this.node.tintColor();
        if (tint != null) {
            bodyArgb = blendTint(bodyArgb, tint);
            titleArgb = blendTint(titleArgb, tint);
        }

        float titleHeight = Node.TITLE_BAR_HEIGHT * viewport.zoom() * guiScale;

        return new NodeBackgroundUniform.Entry(
                new Vector4f(relX, relY, w, h),
                argbToVec(bodyArgb),
                argbToVec(titleArgb),
                argbToVec(borderArgb),
                titleHeight,
                dropShadow);
    }

    /**
     * Fraction of the tint color mixed into the base body / title
     * shade. Tuned so a tinted node still reads as dark (you can see
     * which is body and which is title); higher values wash out the
     * underlying color and lose the depth the dark base gives.
     */
    private static final float TINT_STRENGTH = 0.35f;

    /**
     * Linearly mixes the tint's RGB channels into {@code base} at
     * {@link #TINT_STRENGTH}, preserving {@code base}'s alpha. The
     * tint's own alpha is ignored — callers express the tint
     * intensity through the constant, not the color value, so
     * "red 0xFFFF0000" and "red 0x80FF0000" produce the same result.
     */
    private static int blendTint(int base, int tint) {
        int br = (base >> 16) & 0xFF;
        int bg = (base >> 8) & 0xFF;
        int bb = base & 0xFF;
        int ba = (base >>> 24) & 0xFF;
        int tr = (tint >> 16) & 0xFF;
        int tg = (tint >> 8) & 0xFF;
        int tb = tint & 0xFF;
        float s = TINT_STRENGTH;
        int r = Math.round(br * (1f - s) + tr * s);
        int g = Math.round(bg * (1f - s) + tg * s);
        int b = Math.round(bb * (1f - s) + tb * s);
        return (ba << 24) | (r << 16) | (g << 8) | b;
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
            if (this.handlePropertyEditorClick(event, doubleClick)) {
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
    private boolean handlePropertyEditorClick(MouseButtonEvent event, boolean doubleClick) {
        List<PortDefinition> properties = this.node.definition().properties();
        if (properties.isEmpty()) {
            return false;
        }

        // Each editor is a proper {@link AbstractWidget} that owns its
        // own column hit-test, value step, and write-back callback —
        // the iteration here is just "ask every editor whether the
        // click is theirs," and the first {@code true} wins. Driven
        // rows are skipped: their value is the upstream's, so the
        // local editor wouldn't have anything to do anyway.
        for (int i = 0; i < properties.size(); i++) {
            PortDefinition prop = properties.get(i);
            if (this.findInputConnection(prop.name()) != null) continue;

            PropertyEditor editor = this.propertyRows.get(i).editor();
            if (editor == null) continue;

            // Refresh the editor's value snapshot right before the
            // click so the column step / commit operates on whatever
            // the model currently holds, even if some other path
            // wrote to the property since the last render.
            this.propertyRows.get(i).syncEditorValueFromNode();

            if (editor.mouseClicked(event, doubleClick)) {
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
     * set) can extend this check as they show up. Kept around because
     * {@link PropertyRow} consults it at construction time to pick the
     * right editor subclass for the row.
     */
    private static boolean isDropdownProperty(PortDefinition prop) {
        PropertyDefinition<?> def = prop.type().value();
        return def.codec() == Codec.STRING && def.allowedValues().isPresent();
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

    // -- Inline numeric editing -------------------------------------------

    /**
     * The numeric editor currently hosting an in-place edit on this
     * widget, or {@code null} when none is editing. At most one row
     * can be editing at a time — the click-dispatch path commits any
     * other row's in-flight edit before opening a new one — so the
     * first hit during the scan is the right one.
     */
    public @Nullable NumericPropertyEditor activeNumericEditor() {
        for (PropertyRow row : this.propertyRows) {
            if (row.editor() instanceof NumericPropertyEditor editor && editor.isEditing()) {
                return editor;
            }
        }
        return null;
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

        // Per-port labels — the colored circles themselves are batched
        // through the canvas-level port shader pass (see
        // {@link CanvasWidget#extractPortCircles}), so this loop only
        // submits the small piece of text next to each port. Property
        // ports skip the label step — their row already labels the
        // value, and a second copy on the port itself would clutter
        // the body.
        for (Port port : this.node.ports()) {
            if (port.isProperty()) continue;
            this.renderPortLabel(graphics, font, port, mouseX, mouseY);
        }

        // No separate EditBox pass — each numeric editor renders its
        // own in-place {@link EditBox} (when editing) from inside its
        // {@code extractRenderState}, called via the PropertyRow above.
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
        // Offset the layout past the region's top edge by the vertical
        // padding so the first row sits the same {@code PADDING_Y}
        // pixels below the region top that the last row sits above the
        // region bottom. The layout itself only spans the inner content
        // height (see the constructor), so the padding is purely a
        // positioning concern here — not folded into the equal-spacing
        // distribution.
        this.propertiesLayout.setX(this.node.x());
        this.propertiesLayout.setY(this.node.propertyRegionTop() + Node.PROPERTY_REGION_PADDING_Y);
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

        int labelColor = undriven
                ? PROPERTY_LABEL_UNDRIVEN_COLOR
                : (driven ? PROPERTY_LABEL_DRIVEN_COLOR : PROPERTY_LABEL_COLOR);
        int valueColor = driven ? PROPERTY_VALUE_DRIVEN_COLOR : PROPERTY_VALUE_COLOR;

        // The entire row reads as a single typed unit — label and value
        // dim together when the user is mid-drag from a port of a
        // different type, so a property can't accidentally look like a
        // valid drop target.
        float alpha = this.effectiveAlpha(prop.type());

        // No row-pill fill: the property row inherits the node body
        // color so the editor controls (numeric pill, dropdown trigger,
        // boolean switch) read as the active widgets on the row instead
        // of being undercut by another lighter rectangle. Driven /
        // undriven / normal state is now conveyed via the label and
        // value text colors alone.

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

        // Value area on the right edge of the row. Non-driven rows that
        // have a registered editor route through the editor; everything
        // else (driven rows, plus the rare property type with no editor
        // yet) falls back to plain text.
        PropertyRow row = this.propertyRows.get(rowIndex);
        PropertyEditor editor = row.editor();

        if (!driven && editor != null) {
            // Editors own their own +/- click dispatch, in-place edit,
            // popup-open hook, etc. — all the row needs to do is refresh
            // the editor's value snapshot from the node (so any external
            // writes show up next frame), push the current effective
            // alpha (which tracks the canvas's drag-time dimming), and
            // delegate the draw. Bounds are kept in sync by
            // {@link PropertyRow#setX}/{@link PropertyRow#setY} so we
            // don't have to reposition the editor here.
            row.syncEditorValueFromNode();
            editor.setAlpha(alpha);
            editor.extractRenderState(graphics, mouseX, mouseY, 0f);
        } else {
            // Plain text path — driven rows (showing upstream value or
            // the red "undriven" blank), and any property type without
            // a registered editor (the future-codec branch).
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

        /**
         * Editor for this row, built once based on the property's codec
         * — a {@link NumericPropertyEditor} for int/float/double, a
         * {@link DropdownEditor} for {@code allowedValues} strings, a
         * {@link BooleanPropertyEditor} for booleans, or {@code null}
         * for property types without a registered editor (falls back to
         * the plain-text render path). Each editor owns its own click
         * dispatch and write-back closure, so the row doesn't need to
         * fan out on codec at click / render time.
         */
        private final @Nullable PropertyEditor editor;

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

            // Build the right editor subclass for the row's property
            // codec. The closures captured here ({@code propName} and
            // {@code this.node}) outlive any single click, so the
            // editor can write back to the model without having to
            // know about the row, the node widget, or the canvas.
            // Editor bounds are passed at construction (X / Y get
            // refreshed each layout pass via {@link #setX} / {@link #setY};
            // width / height are constant for the row's lifetime since
            // nodes don't resize at runtime). Building with the final
            // size matters for editors like
            // {@link NumericPropertyEditor} that arrange their internal
            // child layout once in the constructor.
            this.editor = buildEditor(rowIndex);
        }

        /**
         * Picks the right editor subclass based on the property's codec.
         * Returns {@code null} for any codec without a registered
         * editor type (the row then falls back to the plain-text
         * value renderer).
         */
        private @Nullable PropertyEditor buildEditor(int rowIndex) {
            PortDefinition prop = NodeWidget.this.node.definition().properties().get(rowIndex);
            Codec<?> codec = prop.type().value().codec();
            final String propName = prop.name();
            int editorW = Node.PROPERTY_VALUE_MIN_WIDTH;
            int editorH = Node.PROPERTY_PITCH;
            if (isNumericCodec(codec)) {
                Number seed = NodeWidget.this.currentNumericValue(prop);
                return new NumericPropertyEditor(0, 0, editorW, editorH,
                        seed, codec,
                        next -> NodeWidget.this.node.setPropertyValue(propName, next));
            }
            if (isDropdownProperty(prop)) {
                Object current = NodeWidget.this.node.propertyValue(propName);
                String seed = current instanceof String s ? s : "";
                // The dropdown's click hook just marks the property as
                // focused on the node — the popup itself is built by
                // {@link NodeWidget#buildFocusedPropertyPopup} from the
                // node's focus state, so the canvas can position it in
                // screen-space above all other nodes.
                return new DropdownEditor(0, 0, editorW, editorH, seed,
                        () -> NodeWidget.this.node.setFocusedPropertyName(propName));
            }
            if (codec == Codec.BOOL) {
                Object current = NodeWidget.this.node.propertyValue(propName);
                boolean seed = current instanceof Boolean b && b;
                return new BooleanPropertyEditor(0, 0, editorW, editorH, seed,
                        next -> NodeWidget.this.node.setPropertyValue(propName, next));
            }
            return null;
        }

        /**
         * The editor for this row, or {@code null} when the property's
         * codec has no registered editor type. Exposed for
         * {@link NodeWidget}'s click iteration and render delegation;
         * the latter calls {@link #syncEditorValueFromNode} first so
         * the editor reflects the live property value.
         */
        @Nullable PropertyEditor editor() {
            return this.editor;
        }

        /**
         * Pushes the property's current model value into the editor.
         * Called both per-frame (during render) and per-click (before
         * mouse dispatch) so the editor never operates on a stale
         * snapshot. The codec dictates which {@code setValue} variant
         * runs; non-editor rows are a no-op here.
         */
        void syncEditorValueFromNode() {
            if (this.editor == null) return;
            PortDefinition prop = NodeWidget.this.node.definition().properties().get(this.rowIndex);
            Object current = NodeWidget.this.node.propertyValue(prop.name());
            if (this.editor instanceof NumericPropertyEditor n) {
                n.setValue(NodeWidget.this.currentNumericValue(prop));
            } else if (this.editor instanceof DropdownEditor d) {
                d.setValue(current instanceof String s ? s : "");
            } else if (this.editor instanceof BooleanPropertyEditor b) {
                b.setValue(current instanceof Boolean v && v);
            }
        }

        @Override public int getX() { return this.x; }
        @Override public int getY() { return this.y; }
        @Override public int getWidth() { return this.width; }
        @Override public int getHeight() { return this.height; }

        @Override
        public void setX(int x) {
            this.x = x;
            // Editor sits at the right edge of the row, padded inside
            // the value column. Keeping the editor's X in sync with
            // the row's X here means later click / render paths don't
            // have to recompute it.
            if (this.editor != null) {
                this.editor.setX(x + this.width
                        - Node.PROPERTY_PADDING_X - Node.PROPERTY_VALUE_MIN_WIDTH);
            }
        }

        @Override
        public void setY(int y) {
            this.y = y;
            if (this.editor != null) {
                this.editor.setY(y);
            }
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> visitor) {
            // The editor (if any) is the row's only widget child — but
            // we don't expose it here because {@code NodeWidget} routes
            // mouse / key events to the editor manually through
            // {@link #handlePropertyEditorClick} and
            // {@link CanvasWidget#findEditingEditor}; the implicit
            // {@code visitWidgets}-driven event dispatch isn't wired
            // into this widget tree yet.
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
     * Best-effort "what value is flowing into this input?" lookup.
     * Values exit a node only through regular output ports — properties
     * no longer have right-side handles of their own — so the resolution
     * looks for an output port that declares a {@code linkedProperty},
     * and reads through to that property's current value on the source
     * node. Outputs without a linked property carry no value (they're
     * pure type connectors) so a wire from one renders as
     * "connected but undriven" (the red-label state).
     *
     * <p>Resolution is intentionally non-recursive: if the upstream
     * property is itself driven by yet another wire, we still look at
     * its own local value rather than chasing the chain. Chained
     * evaluation is a follow-up once the graph has real semantics for
     * what an output "produces"; for now this is purely a visual
     * reflection.
     *
     * <p>When the source's property slot is null — typically because its
     * registered {@code PropertyDefinition} declared no default — the
     * lookup mirrors the editor's own null fallback (see
     * {@link #currentNumericValue}) so the driven row displays the same
     * zero value the source's editor would. Without this, the source
     * could show {@code "0"} on its own row while the target showed an
     * empty red-label "undriven" state, making it look like the wire was
     * broken when it was working fine.
     */
    private @Nullable Object resolveUpstreamValue(Connection connection) {
        Port source = connection.sourcePort();
        String linkedProp = source.linkedPropertyName();
        if (linkedProp == null) {
            // Source is a plain output port (or, defensively, something
            // else that has no property to relay) — no value to report.
            return null;
        }
        Object value = source.node().propertyValue(linkedProp);
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

    /**
     * Renders only the text label next to a port — the port's visual
     * circle is built into a batched shader entry in
     * {@link #appendPortEntries} instead. Splitting the two means every
     * port circle on the canvas can ride a single PiP submission with
     * sub-pixel-AA SDF edges, while labels stay CPU-rendered (they don't
     * benefit from the shader path).
     */
    private void renderPortLabel(GuiGraphicsExtractor graphics, Font font, Port port, int mouseX, int mouseY) {
        Component title = port.title();
        if (title == null || title.getString().isEmpty()) {
            return;
        }
        Vector2fc center = this.node.portCenter(port);
        int px = (int) center.x();
        int py = (int) center.y();
        float alpha = this.effectiveAlpha(port.type());
        int labelColor = ARGB.color((int) (255 * alpha), 0xCC, 0xCC, 0xD4);
        int textWidth = font.width(title);
        int textY = py - font.lineHeight / 2 + 1;
        switch (port.side()) {
            case LEFT -> graphics.text(font, title,
                    px + Node.PORT_RADIUS + 1 + Node.PORT_LABEL_GAP, textY, labelColor, false);
            case RIGHT -> graphics.text(font, title,
                    px - Node.PORT_RADIUS - Node.PORT_LABEL_GAP - textWidth, textY, labelColor, false);
        }
    }

    /**
     * Appends shader entries for every visible port on this node into
     * {@code outEntries}. Visibility rules:
     * <ul>
     *   <li>Regular ports (non-property): always visible.</li>
     *   <li>Property ports: visible when connected, port-hovered, or
     *       row-hovered. The row-hovered case uses a soft gray
     *       placeholder so the user gets a "you can wire here" hint
     *       without permanent clutter.</li>
     * </ul>
     *
     * <p>Each visible port carries its own animated hover progress
     * (advanced each frame by the wall-clock delta computed at the
     * top of this method via an exponential lerp at
     * {@link #PORT_HOVER_ANIM_SPEED}), so a port mid-transition can
     * have a half-grown circle and a half-brightened halo without
     * snapping. The progress is written into the entry as a per-entry
     * corner radius / border thickness override (packed into the
     * shader's {@code extras.z} / {@code extras.w} slots), which is
     * what lets a single PiP batch render dozens of ports at
     * arbitrary intermediate radii — the shader picks each entry's
     * size off its own extras instead of the batch's shared param.
     *
     * <p>Body color is the port's full type color (or transparent for
     * optional-input rings); the border color is the same type color
     * {@link #lighten lightened} toward white, interpolating between
     * {@link #PORT_OUTLINE_LIGHTEN} (rest) and
     * {@link #PORT_HOVER_OUTLINE_LIGHTEN} (hover) by the same progress,
     * so every port type contrasts the same way against its halo at
     * every point along the animation.
     *
     * @param canvasMouseX mouse position in canvas space, for hover detection
     * @param canvasMouseY same
     */
    public void appendPortEntries(CanvasViewport viewport,
                                  double textureOriginX, double textureOriginY,
                                  float guiScale,
                                  double canvasMouseX, double canvasMouseY,
                                  List<NodeBackgroundUniform.Entry> outEntries) {
        Port hoveredPort = this.portAt(canvasMouseX, canvasMouseY);

        // Advance the per-port hover progress before we read it for
        // entry building. Doing this in a single pass over all ports
        // (not just visible ones) keeps the animation playing even for
        // currently-invisible property ports — so they're already at
        // rest by the time row-hover makes them visible again.
        float lerpAmount = computeHoverLerpAmount();
        for (Port port : this.node.ports()) {
            advanceHoverProgress(port, port == hoveredPort, lerpAmount);
        }

        // Regular non-property ports — always drawn.
        for (Port port : this.node.ports()) {
            if (port.isProperty()) continue;
            // Optional inputs render as a hollow ring regardless of
            // hover: the ring just grows + brightens under the cursor.
            appendPortEntry(port, port.color(), port.optional(), viewport,
                    textureOriginX, textureOriginY, guiScale, outEntries);
        }

        // Property ports — conditional visibility.
        List<PortDefinition> properties = this.node.definition().properties();
        if (properties.isEmpty()) return;
        int nodeLeft = this.node.x();
        int nodeRight = nodeLeft + this.node.width();
        for (int i = 0; i < properties.size(); i++) {
            int rowTop = this.node.propertyRowTop(i);
            int rowBottom = rowTop + Node.PROPERTY_PITCH;
            boolean rowHovered = canvasMouseX >= nodeLeft && canvasMouseX < nodeRight
                    && canvasMouseY >= rowTop && canvasMouseY < rowBottom;
            String propertyName = properties.get(i).name();
            for (Port port : this.node.ports()) {
                if (!port.isProperty()) continue;
                if (!propertyName.equals(port.propertyName())) continue;
                boolean connected = isPortConnected(port);
                boolean portHovered = hoveredPort == port;
                if (!connected && !rowHovered && !portHovered) continue;

                int color;
                if (connected || portHovered) {
                    color = port.color();
                } else {
                    // Row-hover affordance — soft gray placeholder.
                    color = PROPERTY_PORT_PLACEHOLDER_COLOR;
                }
                // Property ports always render as solid (the placeholder
                // hint is a solid translucent gray, not a ring) so the
                // user can read the row-hover state at a glance.
                appendPortEntry(port, color, false, viewport,
                        textureOriginX, textureOriginY, guiScale, outEntries);
            }
        }
    }

    /**
     * Computes the per-frame lerp amount for the hover animation off
     * the wall-clock delta since the previous {@link #appendPortEntries}
     * call. Returns {@code 0} on the very first frame so a
     * freshly-attached widget doesn't snap its first animation step
     * by an enormous interval. The delta is capped at 100 ms so a
     * paused / backgrounded screen doesn't fast-forward the animation
     * the moment the user returns.
     */
    private float computeHoverLerpAmount() {
        long now = Util.getMillis();
        long previous = this.lastPortAnimMillis;
        this.lastPortAnimMillis = now;
        if (previous == 0L) {
            return 0f;
        }
        float dt = Math.min((now - previous) / 1000f, 0.1f);
        // Exponential approach: progress closes by {@code 1 - e^(-speed * dt)}
        // of the remaining gap each frame, giving an ease-out curve
        // that's frame-rate independent.
        return 1f - (float) Math.exp(-PORT_HOVER_ANIM_SPEED * dt);
    }

    /**
     * Nudges {@code port}'s stored hover progress one frame closer to
     * its current target ({@code 1} when hovered, {@code 0} when not).
     * Snaps to the target when within a small epsilon so the animation
     * settles cleanly instead of decaying forever (asymptotic exponential
     * lerps never quite hit their target).
     */
    private void advanceHoverProgress(Port port, boolean hovered, float lerpAmount) {
        float target = hovered ? 1f : 0f;
        Float stored = this.portHoverProgress.get(port);
        float current = stored == null ? target : stored;
        float next = current + (target - current) * lerpAmount;
        if (Math.abs(next - target) < 0.005f) {
            next = target;
        }
        this.portHoverProgress.put(port, next);
    }

    /**
     * Builds one shader entry for {@code port} and adds it to
     * {@code outEntries}. The bounds and outline color are interpolated
     * by this port's current hover-animation progress (read from
     * {@link #portHoverProgress}, advanced earlier in the frame by
     * {@link #advanceHoverProgress}). The per-entry corner radius and
     * border thickness ride along on the entry so the shader can
     * render this port at its current animated size — no batch split
     * required.
     *
     * <p>{@code ring} flips the entry into hollow-ring mode: body alpha
     * goes to zero so the lightened type-colored border (drawn by the
     * shader using this entry's own {@code borderThicknessOverride})
     * is the only visible mark. The title color rides with the body so
     * the shader's title-strip code path is a no-op either way.
     */
    private void appendPortEntry(Port port, int color, boolean ring,
                                 CanvasViewport viewport,
                                 double textureOriginX, double textureOriginY,
                                 float guiScale,
                                 List<NodeBackgroundUniform.Entry> outEntries) {
        Vector2fc center = this.node.portCenter(port);
        Vector2dc screen = viewport.canvasToScreen(center.x(), center.y());

        // Lerp radius factor and outline lightness off the same progress
        // so the visual change reads as one motion — the port doesn't
        // grow before its halo brightens, or vice versa.
        float progress = this.portHoverProgress.getOrDefault(port, 0f);
        float radiusFactor = 1f + (PORT_HOVER_RADIUS_FACTOR - 1f) * progress;
        float radiusScaled = Node.PORT_RADIUS * viewport.zoom() * guiScale * radiusFactor;
        float borderThicknessScaled = Math.max(1f, radiusScaled * 0.45f);

        float w = 2f * radiusScaled;
        float h = 2f * radiusScaled;
        float relX = (float) ((screen.x() - textureOriginX) * guiScale) - radiusScaled;
        float relY = (float) ((screen.y() - textureOriginY) * guiScale) - radiusScaled;

        float alpha = this.effectiveAlpha(port.type());
        int tinted = ARGB.multiply(color, ARGB.white(alpha));
        // Outline lightness is a linear blend between the at-rest and
        // hover values, driven by the same progress as the radius.
        float lightenStrength = PORT_OUTLINE_LIGHTEN
                + (PORT_HOVER_OUTLINE_LIGHTEN - PORT_OUTLINE_LIGHTEN) * progress;
        int outlined = ARGB.multiply(lighten(color, lightenStrength), ARGB.white(alpha));

        Vector4f borderVec = argbToFloatVec(outlined);
        Vector4f bodyVec = ring ? new Vector4f(0f, 0f, 0f, 0f) : argbToFloatVec(tinted);

        outEntries.add(new NodeBackgroundUniform.Entry(
                new Vector4f(relX, relY, w, h),
                bodyVec,
                bodyVec,
                borderVec,
                0f, false,
                radiusScaled, borderThicknessScaled));
    }

    /**
     * Linearly mixes the RGB channels of {@code argb} toward white
     * by {@code strength} ({@code 0} = unchanged, {@code 1} = full
     * white), preserving the alpha channel. Used to derive a
     * lightened outline / halo color directly from the port's base
     * type color — every type ends up with a consistent contrast
     * ratio between its body and ring without needing per-type
     * outline registrations.
     */
    private static int lighten(int argb, float strength) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int nr = Math.round(r + (255 - r) * strength);
        int ng = Math.round(g + (255 - g) * strength);
        int nb = Math.round(b + (255 - b) * strength);
        return (a << 24) | (nr << 16) | (ng << 8) | nb;
    }

    /** Normalize an ARGB int into a straight-alpha 0..1 rgba vector. */
    private static Vector4f argbToFloatVec(int argb) {
        return new Vector4f(
                ARGB.redFloat(argb),
                ARGB.greenFloat(argb),
                ARGB.blueFloat(argb),
                ARGB.alphaFloat(argb));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
