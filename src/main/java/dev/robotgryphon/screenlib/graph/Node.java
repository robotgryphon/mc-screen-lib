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
import java.util.List;
import java.util.Map;

/**
 * The in-memory representation of a node placed on a {@link Canvas}.
 *
 * <p>{@code Node} mirrors {@code Canvas}: a plain data holder with no
 * direct UI dependency. A node owns its definition (the typed schema), the
 * runtime {@link Port}s materialized from that schema, and its current
 * layout state (position, size, title). All port-positioning math lives
 * here so that anything reading the graph — connections, hit-testing — can
 * compute geometry without going through a widget.
 *
 * <p>Size is computed at construction time from the title and port labels
 * using the active client font, so callers don't have to guess at the right
 * dimensions. The width is the larger of the title's width and the row of
 * port labels (left max + right max, plus padding); the height grows with
 * the number of ports on the busier side. {@code Node} therefore relies on
 * {@code Minecraft.getInstance().font} being available — fine in practice
 * because nodes are only ever instantiated on the client.
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

    /** Horizontal padding on each side of the title text inside the title bar. */
    private static final int TITLE_PADDING = 8;
    /** Pixels between a port's anchor on the node edge and the start of its label. */
    private static final int PORT_LABEL_INSET = PORT_RADIUS + 1 + PORT_LABEL_GAP;
    /** Minimum gap between the longest left label and the longest right label. */
    private static final int LABEL_BETWEEN_GAP = 8;
    /** Vertical pixels reserved per port row; needs to comfortably fit the font. */
    private static final int MIN_PORT_PITCH = 12;
    /** Floor on the body region so a portless / single-port node still feels like a node. */
    private static final int MIN_BODY_HEIGHT = 24;
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

        // Auto-size from content. Done last so it has access to the populated
        // ports map (and indirectly, port titles) for label-width measurement.
        Font font = Minecraft.getInstance().font;
        this.width = computeWidth(font, title, this.definition);
        this.height = computeHeight(font, this.definition);
    }

    private List<Port> buildPorts(NodeDefinition def) {
        List<Port> result = new ArrayList<>(def.inputs().size() + def.outputs().size());
        for (PortDefinition input : def.inputs()) {
            result.add(new Port(this, PortSide.LEFT, Component.literal(input.name()), input.type()));
        }
        for (PortDefinition output : def.outputs()) {
            result.add(new Port(this, PortSide.RIGHT, Component.literal(output.name()), output.type()));
        }
        return List.copyOf(result);
    }

    private static Map<PortSide, List<Port>> groupBySide(List<Port> ports) {
        Map<PortSide, List<Port>> map = new EnumMap<>(PortSide.class);
        for (Port p : ports) {
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
        int maxLeft = maxLabelWidth(font, def.inputs());
        int maxRight = maxLabelWidth(font, def.outputs());
        int portsNeed = 0;
        if (maxLeft > 0 || maxRight > 0) {
            portsNeed = 2 * PORT_LABEL_INSET + maxLeft + maxRight + LABEL_BETWEEN_GAP;
        }

        return Math.max(MIN_WIDTH, Math.max(titleNeed, portsNeed));
    }

    private static int computeHeight(Font font, NodeDefinition def) {
        int maxPortsPerSide = Math.max(def.inputs().size(), def.outputs().size());
        // Ports are evenly distributed at (i+1)/(N+1) of the body; each "slot"
        // is bodyHeight / (N+1), so the body needs (N+1) pitches to give every
        // port a comfortable row.
        int pitch = Math.max(MIN_PORT_PITCH, font.lineHeight + 3);
        int bodyHeight = Math.max(MIN_BODY_HEIGHT, pitch * (maxPortsPerSide + 1));
        return TITLE_BAR_HEIGHT + bodyHeight;
    }

    private static int maxLabelWidth(Font font, List<PortDefinition> ports) {
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
     * {@code CanvasState.toState()}.
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

    // -- Port geometry -----------------------------------------------------

    /**
     * The on-screen center of the given port. Ports on the same side share
     * the body extent equally: with N ports on a side, the i-th port
     * (0-indexed) sits at {@code (i+1)/(N+1)} along the body.
     *
     * <p>The returned point is the visual center of the port's center pixel:
     * the integer pixel anchor plus 0.5 on each axis. This keeps every port
     * aligned to whole-pixel rendering while ensuring the curved connector
     * passes through the actual middle of the port rather than the pixel
     * boundary above it (which would show a vertical offset at high zoom).
     */
    public Vector2fc portCenter(Port port) {
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

        // Reserve the title-bar slice; ports lay out within the body region.
        int bodyTop = this.y + TITLE_BAR_HEIGHT;
        int bodyHeight = this.height - TITLE_BAR_HEIGHT;

        // Snap layout to integer pixels so multi-port distribution doesn't
        // leave one port's line a fraction of a pixel above center and the
        // next port's line a fraction below.
        int yAnchor = bodyTop + Math.round(bodyHeight * t);
        int xAnchor = switch (port.side()) {
            case LEFT -> this.x;
            case RIGHT -> this.x + this.width;
            default -> throw new IllegalStateException("Unknown side: " + port.side());
        };

        return new Vector2f(xAnchor + 0.5f, yAnchor + 0.5f);
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
