package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.types.NodeDefinition;
import dev.robotgryphon.screenlib.types.PortDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The in-memory representation of a node placed on a {@link Canvas}.
 *
 * <p>{@code Node} mirrors {@code Canvas}: a plain data holder with no
 * direct UI dependency. A node owns its definition (the typed schema), the
 * runtime {@link Port}s materialized from that schema, the current values
 * of its declared properties, and its current layout state (position,
 * size, title). All port-positioning math lives here so that anything
 * reading the graph — connections, hit-testing — can compute geometry
 * without going through a widget.
 *
 * <p>Size is computed at construction time from the title, the port
 * labels, and the property labels using the active client font, so
 * callers don't have to guess at the right dimensions. The width is the
 * widest of the title, the port row (left+right with padding), and the
 * property rows (label+value); the body height grows with both the
 * number of property rows and the number of ports on the busier side.
 * {@code Node} therefore relies on {@code Minecraft.getInstance().font}
 * being available — fine in practice because nodes are only ever
 * instantiated on the client.
 */
public class Node {

    /** Vertical strip reserved for the title text at the top of the body. */
    public static final int TITLE_BAR_HEIGHT = 12;
    /** Half-extent of the rendered diamond (so a port spans 2*r+1 pixels per axis). */
    public static final int PORT_RADIUS = 3;
    /** Generous click target around the port center; bigger than the visible diamond. */
    public static final int PORT_HIT_RADIUS = 5;
    /** Pixels between the diamond's outer edge and the start of its label text. */
    public static final int PORT_LABEL_GAP = 4;

    /** Vertical pixels reserved per property row inside the node body. */
    public static final int PROPERTY_PITCH = 14;
    /**
     * Vertical pixels of empty space between consecutive property rows.
     * Used by the widget layer's {@code EqualSpacingLayout} as the
     * inter-child gap, and mirrored in {@link #propertyRegionHeight} and
     * {@link #propertyRowTop} so the data model agrees with the layout
     * the widget produces. Two pixels reads as a clear strip between
     * pills without bloating the node's height.
     */
    public static final int PROPERTY_ROW_GAP = 2;
    /** Pixels of horizontal padding on each side of a property row. */
    public static final int PROPERTY_PADDING_X = 6;
    /**
     * Pixels of empty space above the first property row and below the
     * last one. Set equal to {@link #PROPERTY_PADDING_X} so the property
     * region's vertical breathing room matches the horizontal — a row's
     * label sits {@code PROPERTY_PADDING_X} from the node's side edges,
     * and the first / last rows sit {@code PROPERTY_REGION_PADDING_Y}
     * from the region's top / bottom edges, all the same value.
     */
    public static final int PROPERTY_REGION_PADDING_Y = PROPERTY_PADDING_X;
    /** Minimum width reserved for a property's value area regardless of current content. */
    public static final int PROPERTY_VALUE_MIN_WIDTH = 60;
    /** Pixels between a property's label and its value column. */
    public static final int PROPERTY_LABEL_VALUE_GAP = 8;

    /** Horizontal padding on each side of the title text inside the title bar. */
    private static final int TITLE_PADDING = 8;
    /** Pixels between a port's anchor on the node edge and the start of its label. */
    private static final int PORT_LABEL_INSET = PORT_RADIUS + 1 + PORT_LABEL_GAP;
    /** Minimum gap between the longest left label and the longest right label. */
    private static final int LABEL_BETWEEN_GAP = 8;
    /** Vertical pixels reserved per port row; needs to comfortably fit the font. */
    private static final int MIN_PORT_PITCH = 12;
    /** Floor on the port-band region so a portless / single-port node still feels like a node. */
    private static final int MIN_PORT_BAND_HEIGHT = 24;
    /** Floor on width so empty-titled / portless nodes don't shrink to a sliver. */
    private static final int MIN_WIDTH = 60;

    /**
     * Registry holder for the node's schema. Kept alongside the resolved
     * {@link NodeDefinition} so callers can both read the schema directly
     * (the hot path — port lists, etc.) and serialize a stable reference
     * to it via {@link NodeDefinition#HOLDER_CODEC}.
     */
    private final Holder<NodeDefinition> definitionHolder;
    private final NodeDefinition definition;
    private final Component title;
    private final List<Port> ports;
    /** Ports grouped by side, in declaration order — used to compute layout positions. */
    private final Map<PortSide, List<Port>> portsBySide;

    /**
     * Current values of the declared properties, keyed by the
     * {@link PortDefinition#name()} the property entry is listed under
     * in {@link NodeDefinition#properties()}. Each entry's value is the
     * typed object decoded through the property's registered
     * {@link dev.robotgryphon.screenlib.types.PropertyDefinition} codec;
     * absent keys mean "no value set yet" and render as a placeholder.
     * The map is mutable so editor widgets can write through to it.
     */
    private final Map<String, Object> propertyValues = new HashMap<>();

    /**
     * Name of the property whose editor currently has focus on this node
     * (e.g., the one whose dropdown popup is open). Null when no property
     * editor is active. Owned by the node — not the canvas — because the
     * focused element belongs to a specific property on a specific node;
     * collecting it canvas-side would conflate "which node is being
     * interacted with" with "which sub-control inside that node is open".
     *
     * <p>The widget layer reads this each frame to decide whether to
     * render a floating popup, and writes it on click / dismissal. Set
     * to {@code null} when the popup closes (option picked, outside
     * click, etc.) so the next frame stops rendering the popup.
     */
    private @Nullable String focusedPropertyName;

    /** Total height of the property region (all rows stacked), cached at construction. */
    private final int propertyRegionHeight;

    /**
     * Total height of the port band, cached at construction. The band
     * sits directly under the title bar and above the property region;
     * its height is driven by the busier of the two port sides (the
     * one with the most ports) so a single-port-per-side node still
     * has the minimum visual breathing room.
     *
     * <p>Cached here so {@link #propertyRegionTop} can place the
     * property region directly below it without recomputing the
     * pitch / max-port math on every call.
     */
    private final int portBandHeight;

    private int x;
    private int y;
    private int width;
    private int height;

    public Node(Holder<NodeDefinition> definition, Component title, int x, int y) {
        this.definitionHolder = definition;
        this.definition = definition.value();
        this.title = title;
        this.x = x;
        this.y = y;
        this.ports = buildPorts(this.definition);
        this.portsBySide = groupBySide(this.ports);

        // Stacked rows plus a small empty gap between each consecutive
        // pair, framed by {@link #PROPERTY_REGION_PADDING_Y} of empty
        // space at the top and bottom of the region. Matches the
        // geometry an {@code EqualSpacingLayout} with
        // {@code PROPERTY_PITCH}-tall children and a
        // {@link #PROPERTY_ROW_GAP} inter-child gap produces, positioned
        // by the widget at {@code propertyRegionTop + PADDING_Y}, so
        // property ports still anchor exactly on the row's edge.
        int propRowCount = this.definition.properties().size();
        this.propertyRegionHeight = propRowCount == 0
                ? 0
                : 2 * PROPERTY_REGION_PADDING_Y
                        + propRowCount * PROPERTY_PITCH
                        + (propRowCount - 1) * PROPERTY_ROW_GAP;

        // Seed property values from the schema's declared defaults so a
        // freshly-spawned node renders the same numbers the datapack
        // author chose. Stored as already-decoded typed objects so the
        // renderer doesn't have to re-decode on every frame. Persistence
        // (CanvasState load) overwrites these afterwards if the user has
        // since modified a value.
        seedDefaultPropertyValues();

        // Auto-size from content. Done last so it has access to the populated
        // ports map (and indirectly, port titles) for label-width measurement.
        Font font = Minecraft.getInstance().font;
        this.width = computeWidth(font, title, this.definition);
        this.portBandHeight = computePortBandHeight(font, this.definition);
        this.height = TITLE_BAR_HEIGHT + this.portBandHeight + this.propertyRegionHeight;
    }

    /**
     * Seeds {@link #propertyValues} with the typed default declared by
     * each property's registered {@link
     * dev.robotgryphon.screenlib.types.PropertyDefinition}. The default
     * is already the typed value (the registry's {@code Optional<T>}
     * field), so no codec round-trip is needed at this point — that's
     * reserved for persistence ({@link Canvas#toState}). Properties whose
     * type carries no default are left unset.
     */
    private void seedDefaultPropertyValues() {
        for (PortDefinition prop : this.definition.properties()) {
            prop.type().value().defaultValueRaw()
                    .ifPresent(def -> this.propertyValues.put(prop.name(), def));
        }
    }

    private List<Port> buildPorts(NodeDefinition def) {
        List<Port> result = new ArrayList<>(
                def.inputs().size() + def.outputs().size() + def.properties().size());
        for (PortDefinition input : def.inputs()) {
            result.add(new Port(this, PortSide.LEFT, Component.literal(input.name()), input.type()));
        }
        for (PortDefinition output : def.outputs()) {
            // Output ports thread the optional {@code linkedProperty} into the
            // runtime port so a wire from the output knows which property's
            // value to relay — that's the only way data leaves a node now
            // that property right-side ports are gone.
            result.add(new Port(this, PortSide.RIGHT, Component.literal(output.name()),
                    output.type(), null, output.linkedProperty().orElse(null)));
        }
        // Properties get a LEFT (input) port only. A wire targeting it
        // overrides the property's local value with the upstream's. There's
        // no right-side property port — values flow OUT of a node only
        // through regular output ports, which can optionally name a
        // property to relay from via {@link PortDefinition#linkedProperty}.
        for (PortDefinition prop : def.properties()) {
            result.add(Port.property(this, PortSide.LEFT, prop.name(), prop.type()));
        }
        return List.copyOf(result);
    }

    /**
     * Groups <em>only the non-property ports</em> by side. Property ports
     * anchor to their property's row inside the body, not to the side's
     * port-band distribution, so they must be excluded here — otherwise
     * adding a property would shift every regular port's vertical position
     * (because the (i+1)/(N+1) distribution math counts them).
     */
    private static Map<PortSide, List<Port>> groupBySide(List<Port> ports) {
        Map<PortSide, List<Port>> map = new EnumMap<>(PortSide.class);
        for (Port p : ports) {
            if (p.isProperty()) continue;
            map.computeIfAbsent(p.side(), k -> new ArrayList<>()).add(p);
        }
        // Defensive copies so the per-side lists can't be mutated from outside.
        Map<PortSide, List<Port>> frozen = new EnumMap<>(PortSide.class);
        map.forEach((side, list) -> frozen.put(side, List.copyOf(list)));
        return Collections.unmodifiableMap(frozen);
    }

    // -- Auto-sizing --------------------------------------------------------

    private static int computeWidth(Font font, Component title, NodeDefinition def) {
        // Title needs to fit between the title bar's left and right edges with
        // a little breathing room on each side.
        int titleNeed = font.width(title) + 2 * TITLE_PADDING;

        // Ports: left labels and right labels have to coexist on the same row
        // without overlapping; each label sits PORT_LABEL_INSET pixels in from
        // its node edge, so the inner span is just left + gap + right.
        int maxLeft = maxPortLabelWidth(font, def.inputs());
        int maxRight = maxPortLabelWidth(font, def.outputs());
        int portsNeed = 0;
        if (maxLeft > 0 || maxRight > 0) {
            portsNeed = 2 * PORT_LABEL_INSET + maxLeft + maxRight + LABEL_BETWEEN_GAP;
        }

        // Properties: label on the left, a fixed-minimum value column on the
        // right. The row has uniform padding on both sides so the value column
        // sits flush with the node's right edge minus the padding.
        int propsNeed = 0;
        for (PortDefinition prop : def.properties()) {
            int labelWidth = font.width(propertyLabel(prop));
            int rowWidth = 2 * PROPERTY_PADDING_X
                    + labelWidth + PROPERTY_LABEL_VALUE_GAP + PROPERTY_VALUE_MIN_WIDTH;
            propsNeed = Math.max(propsNeed, rowWidth);
        }

        return Math.max(MIN_WIDTH, Math.max(Math.max(titleNeed, portsNeed), propsNeed));
    }

    /**
     * Height of the port-band region in pixels. The band has to host
     * the busier of the two sides (left vs. right ports), distributed
     * at {@code (i+1)/(N+1)} of its height, so it needs {@code N+1}
     * pitches to give every port a comfortable row. Floored at
     * {@link #MIN_PORT_BAND_HEIGHT} so a single-port-per-side node
     * still has room for its labels.
     *
     * <p>Special case: when a node has <em>no</em> ports on either side
     * (a properties-only node like a sampler, or any pure configuration
     * node), the band collapses to zero. Otherwise the band would
     * reserve {@link #MIN_PORT_BAND_HEIGHT} pixels of empty space
     * directly under the title bar — visible as a large gap above the
     * first property row.
     */
    private static int computePortBandHeight(Font font, NodeDefinition def) {
        int maxPortsPerSide = Math.max(def.inputs().size(), def.outputs().size());
        if (maxPortsPerSide == 0) {
            return 0;
        }
        int pitch = Math.max(MIN_PORT_PITCH, font.lineHeight + 3);
        return Math.max(MIN_PORT_BAND_HEIGHT, pitch * (maxPortsPerSide + 1));
    }

    private static int maxPortLabelWidth(Font font, List<PortDefinition> ports) {
        int max = 0;
        for (PortDefinition p : ports) {
            max = Math.max(max, font.width(Component.literal(p.name())));
        }
        return max;
    }

    // -- Schema / metadata -------------------------------------------------

    public NodeDefinition definition() {
        return this.definition;
    }

    /**
     * The registry holder backing {@link #definition()}. Use this when you
     * need a stable, serializable reference to the schema — e.g., for
     * {@link CanvasStateManager#toState}.
     */
    public Holder<NodeDefinition> definitionHolder() {
        return this.definitionHolder;
    }

    public Component title() {
        return this.title;
    }

    public List<Port> ports() {
        return this.ports;
    }

    // -- Property values ---------------------------------------------------

    /** Current value for the named property, or {@code null} if unset. */
    public @Nullable Object propertyValue(String name) {
        return this.propertyValues.get(name);
    }

    /**
     * Writes through to the property map. Caller is responsible for the
     * runtime type matching the property's declared
     * {@link dev.robotgryphon.screenlib.types.PropertyDefinition} — the
     * node itself stores values type-erased so it can host any property
     * kind without generic gymnastics.
     */
    public void setPropertyValue(String name, @Nullable Object value) {
        if (value == null) {
            this.propertyValues.remove(name);
        } else {
            this.propertyValues.put(name, value);
        }
    }

    /**
     * Label rendered next to a property row. Currently uses the
     * property's local name verbatim; once translatable labels land this
     * becomes a {@link Component#translatable(String)} keyed off the
     * property's owning node and the property name.
     */
    public static Component propertyLabel(PortDefinition prop) {
        return Component.literal(prop.name());
    }

    /**
     * Name of the property whose editor is currently focused — typically
     * the one with an open dropdown popup. Null when no property editor
     * has focus.
     */
    public @Nullable String focusedPropertyName() {
        return this.focusedPropertyName;
    }

    /**
     * Marks {@code name} as the focused property on this node, or
     * {@code null} to clear focus. The widget layer calls this when the
     * user opens a dropdown ({@code name}) and again when the popup
     * closes via selection or an outside click ({@code null}). The model
     * doesn't try to validate that {@code name} actually exists — that
     * stays a widget-layer concern, since the widget is the side that
     * knows how to find the property and render its editor.
     */
    public void setFocusedPropertyName(@Nullable String name) {
        this.focusedPropertyName = name;
    }

    // -- Layout state ------------------------------------------------------

    public int x() { return this.x; }
    public int y() { return this.y; }
    public int width() { return this.width; }
    public int height() { return this.height; }

    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    /** Total stacked height of the property region (zero when the node has no properties). */
    public int propertyRegionHeight() {
        return this.propertyRegionHeight;
    }

    /**
     * Top edge (inclusive) of the property region, in screen pixels.
     * The region sits below the title bar and the port band; this
     * means port labels never have to share vertical space with
     * property labels.
     */
    public int propertyRegionTop() {
        return this.y + TITLE_BAR_HEIGHT + this.portBandHeight;
    }

    /**
     * Top edge of the i-th property row, in screen pixels. Each row
     * occupies {@link #PROPERTY_PITCH} pixels followed by a
     * {@link #PROPERTY_ROW_GAP}-pixel strip of empty space before the
     * next row begins — matching the geometry the widget's
     * {@code EqualSpacingLayout} produces. The first row sits
     * {@link #PROPERTY_REGION_PADDING_Y} below the region's top edge,
     * so callers don't need to apply the padding themselves.
     */
    public int propertyRowTop(int index) {
        return this.propertyRegionTop() + PROPERTY_REGION_PADDING_Y
                + index * (PROPERTY_PITCH + PROPERTY_ROW_GAP);
    }

    // -- Port geometry -----------------------------------------------------

    /**
     * The on-screen center of the given port. Ports on the same side share
     * the port-band region equally: with N ports on a side, the i-th port
     * (0-indexed) sits at {@code (i+1)/(N+1)} along the band. The port
     * band starts directly under the title bar (and ends where the
     * property region begins) so port labels and rows never collide.
     *
     * <p>The returned point is the visual center of the port's center pixel:
     * the integer pixel anchor plus 0.5 on each axis. This keeps every port
     * aligned to whole-pixel rendering while ensuring the curved connector
     * passes through the actual middle of the port rather than the pixel
     * boundary above it (which would show a vertical offset at high zoom).
     */
    public Vector2fc portCenter(Port port) {
        // Property-bound port: anchor to the row that hosts its property,
        // not to the side's port-band distribution. The x-anchor is the
        // same edge logic as a regular port.
        if (port.isProperty()) {
            int rowIndex = propertyRowIndex(port.propertyName());
            if (rowIndex < 0) {
                throw new IllegalArgumentException("Property port references unknown property: " + port.propertyName());
            }
            int yAnchor = this.propertyRowTop(rowIndex) + PROPERTY_PITCH / 2;
            int xAnchor = switch (port.side()) {
                case LEFT -> this.x;
                case RIGHT -> this.x + this.width - 1;
                default -> throw new IllegalStateException("Unknown side: " + port.side());
            };
            return new Vector2f(xAnchor, yAnchor + 0.5f);
        }

        List<Port> sidePorts = this.portsBySide.get(port.side());
        if (sidePorts == null) {
            throw new IllegalArgumentException("Port not on this node: " + port);
        }
        // Reference equality so duplicate-equals records don't collide.
        int index = -1;
        for (int i = 0; i < sidePorts.size(); i++) {
            if (sidePorts.get(i) == port) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new IllegalArgumentException("Port not on this node: " + port);
        }
        int count = sidePorts.size();
        float t = (index + 1f) / (count + 1f);

        // Port band sits directly under the title bar and above the
        // property region — properties are pushed down by the same
        // {@link #portBandHeight} the band reports here, so the two
        // never overlap.
        int bandTop = this.y + TITLE_BAR_HEIGHT;
        int bandHeight = this.portBandHeight;

        // Snap layout to integer pixels so multi-port distribution doesn't
        // leave one port's line a fraction of a pixel above center and the
        // next port's line a fraction below.
        int yAnchor = bandTop + Math.round(bandHeight * t);
        int xAnchor = switch (port.side()) {
            case LEFT -> this.x;
            case RIGHT -> this.x + this.width - 1;
            default -> throw new IllegalStateException("Unknown side: " + port.side());
        };

        return new Vector2f(xAnchor + 0.5f, yAnchor + 0.5f);
    }

    /**
     * Index of the property with the given name in the definition's property
     * list, or {@code -1} if there's no such property. Lookup is O(N) since N
     * is small (typically &lt; 10) and the call site is only in {@link #portCenter}.
     */
    private int propertyRowIndex(String name) {
        List<PortDefinition> props = this.definition.properties();
        for (int i = 0; i < props.size(); i++) {
            if (props.get(i).name().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The point a connection line should attach to. The shader uses butt
     * caps (perpendicular cuts at the curve endpoints), so the attachment
     * lands at the diamond's outer tip — putting the cap flush with the
     * port at any zoom.
     */
    public Vector2fc portAttachment(Port port) {
        Vector2fc center = this.portCenter(port);
        float offset = PORT_RADIUS + 0.5f;
        switch (port.side()) {
            case LEFT -> {
                return new Vector2f(center.x() - offset, center.y());
            }
            case RIGHT -> {
                return new Vector2f(center.x() + offset, center.y());
            }
            default -> throw new IllegalStateException("Unknown side: " + port.side());
        }
    }

    /**
     * If ({@code mouseX}, {@code mouseY}) lands on one of this node's ports,
     * return that port; otherwise {@code null}. The hit radius is a few
     * pixels larger than the visible port so it's easy to grab.
     */
    public @Nullable Port portAt(double mouseX, double mouseY) {
        for (Port p : this.ports) {
            Vector2fc center = this.portCenter(p);
            double dx = center.x() - mouseX;
            double dy = center.y() - mouseY;
            if (dx * dx + dy * dy <= PORT_HIT_RADIUS * PORT_HIT_RADIUS) {
                return p;
            }
        }
        return null;
    }
}
