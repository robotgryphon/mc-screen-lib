package dev.robotgryphon.screenlib.client.ui.widget.node;

import dev.robotgryphon.screenlib.client.GraphColors;
import dev.robotgryphon.screenlib.client.NodeRenderer;
import dev.robotgryphon.screenlib.client.PortLayout;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import dev.robotgryphon.screenlib.graph.canvas.client.CanvasViewport;
import dev.robotgryphon.screenlib.client.ui.state.PortState;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.ports.InputPort;
import dev.robotgryphon.screenlib.graph.nodes.ports.Port;
import dev.robotgryphon.screenlib.graph.nodes.ports.PropertyInputPort;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.joml.Vector2dc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NodePortsRenderer implements Renderable {
    public static final float HOVER_RADIUS_FACTOR = 1.3f;

    private static final float OUTLINE_LIGHTEN = 0.30f;

    private static final float HOVER_OUTLINE_LIGHTEN = 0.55f;

    private static final int PROPERTY_PLACEHOLDER_COLOR = 0xCCAEB1BC;

    private static final int HIT_RADIUS = 5;

    private static final float HOVER_ANIM_SPEED = 18f;

    private final NodeWidget owner;
    private final Node node;
    private final List<PortState> ports;

    private long lastAnimMillis;

    public NodePortsRenderer(NodeWidget owner, PortLayout layout) {
        this.owner = owner;
        this.node = owner.node();
        List<Port> ports = node.portManager().ports();
        List<PortState> states = new ArrayList<>(ports.size());
        for (int i = 0; i < ports.size(); i++) {
            states.add(new PortState(ports.get(i), layout.slots().get(i)));
        }
        this.ports = List.copyOf(states);
    }

    private PortState state(Port port) {
        for (PortState state : this.ports) {
            if (state.port() == port) {
                return state;
            }
        }
        throw new IllegalArgumentException("Port not on this node: " + port);
    }

    private Vector2fc center(PortState state) {
        return new Vector2f(this.owner.getX(), this.owner.getY())
                .add(state.slot().offset(this.owner.getWidth()));
    }

    public Vector2fc portCenter(Port port) {
        return this.center(this.state(port));
    }

    public Vector2fc portAttachment(Port port) {
        return new Vector2f(this.owner.getX(), this.owner.getY())
                .add(this.state(port).slot().attachmentOffset(this.owner.getWidth()));
    }

    private @Nullable PortState stateAt(double mouseX, double mouseY) {
        for (PortState state : this.ports) {
            Vector2fc center = this.center(state);
            double dx = center.x() - mouseX;
            double dy = center.y() - mouseY;
            if (dx * dx + dy * dy <= HIT_RADIUS * HIT_RADIUS) {
                return state;
            }
        }
        return null;
    }

    public @Nullable Port portAt(double mouseX, double mouseY) {
        PortState state = this.stateAt(mouseX, mouseY);
        return state == null ? null : state.port();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        for (PortState state : this.ports) {
            this.renderLabel(graphics, state);
        }
    }

    private void renderLabel(GuiGraphicsExtractor graphics, PortState state) {
        Port port = state.port();
        if (port instanceof PropertyInputPort) {
            return;
        }
        Component title = port.title();
        if (title == null || title.getString().isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        Vector2fc center = this.center(state);
        int px = (int) center.x();
        int py = (int) center.y();
        float alpha = this.owner.effectiveAlpha(port.type());
        int labelColor = ARGB.color((int) (255 * alpha), 0xCC, 0xCC, 0xD4);
        int textWidth = font.width(title);
        int textY = py - font.lineHeight / 2 + 1;
        switch (port.side()) {
            case LEFT -> graphics.text(font, title,
                    px + NodeRenderer.PORT_RADIUS + 1 + NodeRenderer.PORT_LABEL_GAP, textY, labelColor, false);
            case RIGHT -> graphics.text(font, title,
                    px - NodeRenderer.PORT_RADIUS - NodeRenderer.PORT_LABEL_GAP - textWidth, textY, labelColor, false);
        }
    }

    public void appendPortEntries(CanvasViewport viewport,
                                  double textureOriginX, double textureOriginY,
                                  float guiScale,
                                  double canvasMouseX, double canvasMouseY,
                                  List<NodeBackgroundUniform.Entry> outEntries) {
        PortState hovered = this.stateAt(canvasMouseX, canvasMouseY);

        float lerpAmount = this.computeHoverLerpAmount();
        for (PortState state : this.ports) {
            state.advanceHover(state == hovered, lerpAmount);
        }
        for (PortState state : this.ports) {
            this.appendMarkerEntry(state, viewport, textureOriginX, textureOriginY, guiScale,
                    state == hovered, canvasMouseX, canvasMouseY, outEntries);
        }
    }

    private void appendMarkerEntry(PortState state, CanvasViewport viewport,
                                   double textureOriginX, double textureOriginY,
                                   float guiScale,
                                   boolean hovered,
                                   double canvasMouseX, double canvasMouseY,
                                   List<NodeBackgroundUniform.Entry> outEntries) {
        Port port = state.port();
        int color = GraphColors.of(port);
        boolean ring = false;

        if (port instanceof PropertyInputPort propertyPort) {
            boolean connected = this.node.canvas.connections().isConnected(port);
            if (!connected && !hovered && !this.isPropertyRowHovered(propertyPort, canvasMouseX, canvasMouseY)) {
                return;
            }
            if (!connected && !hovered) {
                color = PROPERTY_PLACEHOLDER_COLOR;
            }
        } else {
            ring = port instanceof InputPort input && input.optional();
        }

        Vector2fc center = this.center(state);
        Vector2dc screen = viewport.canvasToScreen(center.x(), center.y());

        float hoverProgress = state.hoverProgress();
        float radiusFactor = 1f + (HOVER_RADIUS_FACTOR - 1f) * hoverProgress;
        float radiusScaled = NodeRenderer.PORT_RADIUS * viewport.zoom() * guiScale * radiusFactor;
        float borderThicknessScaled = Math.max(1f, radiusScaled * 0.45f);

        float w = 2f * radiusScaled;
        float h = 2f * radiusScaled;
        float relX = (float) ((screen.x() - textureOriginX) * guiScale) - radiusScaled;
        float relY = (float) ((screen.y() - textureOriginY) * guiScale) - radiusScaled;

        float alpha = this.owner.effectiveAlpha(port.type());
        int tinted = ARGB.multiply(color, ARGB.white(alpha));
        float lightenStrength = OUTLINE_LIGHTEN + (HOVER_OUTLINE_LIGHTEN - OUTLINE_LIGHTEN) * hoverProgress;
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

    private boolean isPropertyRowHovered(PropertyInputPort propertyPort, double canvasMouseX, double canvasMouseY) {
        List<StoredPropertyDefinition<?>> properties = this.node.propertyManager().stored();
        for (int i = 0; i < properties.size(); i++) {
            if (!properties.get(i).name().equals(propertyPort.propertyName())) continue;
            int rowTop = this.owner.properties().rowTop(i);
            int rowBottom = rowTop + NodeRenderer.PROPERTY_PITCH;
            return canvasMouseX >= this.owner.getX() && canvasMouseX < this.owner.getX() + this.owner.getWidth()
                    && canvasMouseY >= rowTop && canvasMouseY < rowBottom;
        }
        return false;
    }

    private float computeHoverLerpAmount() {
        long now = Util.getMillis();
        long previous = this.lastAnimMillis;
        this.lastAnimMillis = now;
        if (previous == 0L) {
            return 0f;
        }
        float dt = Math.min((now - previous) / 1000f, 0.1f);
        return 1f - (float) Math.exp(-HOVER_ANIM_SPEED * dt);
    }

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

    private static Vector4f argbToFloatVec(int argb) {
        return new Vector4f(
                ARGB.redFloat(argb),
                ARGB.greenFloat(argb),
                ARGB.blueFloat(argb),
                ARGB.alphaFloat(argb));
    }
}
