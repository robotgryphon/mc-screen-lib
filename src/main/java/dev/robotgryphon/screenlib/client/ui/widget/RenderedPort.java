package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.client.GraphColors;
import dev.robotgryphon.screenlib.client.NodeRenderer;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.ports.InputPort;
import dev.robotgryphon.screenlib.graph.ports.Port;
import dev.robotgryphon.screenlib.client.PortLayout;
import dev.robotgryphon.screenlib.graph.ports.PropertyInputPort;
import dev.robotgryphon.screenlib.graph.schema.node.StoredPropertyDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.joml.Vector2dc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector4f;

import java.util.List;

public class RenderedPort implements Renderable {
    public static final float HOVER_RADIUS_FACTOR = 1.3f;

    public static final float OUTLINE_LIGHTEN = 0.30f;

    public static final float HOVER_OUTLINE_LIGHTEN = 0.55f;

    private static final int PROPERTY_PLACEHOLDER_COLOR = 0xCCAEB1BC;

    private static final int HIT_RADIUS = 5;

    static final float HOVER_ANIM_SPEED = 18f;

    private final NodePortsWidget parent;
    private final Node node;
    private final Port port;
    private final PortLayout.Slot slot;

    private float hoverProgress;

    public RenderedPort(NodePortsWidget parent, Node node, Port port, PortLayout.Slot slot) {
        this.parent = parent;
        this.node = node;
        this.port = port;
        this.slot = slot;
    }

    public Node node() {
        return this.node;
    }

    public Port port() {
        return this.port;
    }

    public Vector2fc center() {
        return new Vector2f(this.parent.owner().getX(), this.parent.owner().getY())
                .add(this.slot().offset(this.parent.owner().getWidth()));
    }

    public PortLayout.Slot slot() {
        return this.slot;
    }

    public boolean contains(double mouseX, double mouseY) {
        Vector2fc center = this.center();
        double dx = center.x() - mouseX;
        double dy = center.y() - mouseY;
        return dx * dx + dy * dy <= HIT_RADIUS * HIT_RADIUS;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.port instanceof PropertyInputPort) {
            return;
        }
        Component title = this.port.title();
        if (title == null || title.getString().isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        Vector2fc center = this.center();
        int px = (int) center.x();
        int py = (int) center.y();
        float alpha = this.parent.effectiveAlpha(this.port.type());
        int labelColor = ARGB.color((int) (255 * alpha), 0xCC, 0xCC, 0xD4);
        int textWidth = font.width(title);
        int textY = py - font.lineHeight / 2 + 1;
        switch (this.port.side()) {
            case LEFT -> graphics.text(font, title,
                    px + NodeRenderer.PORT_RADIUS + 1 + NodeRenderer.PORT_LABEL_GAP, textY, labelColor, false);
            case RIGHT -> graphics.text(font, title,
                    px - NodeRenderer.PORT_RADIUS - NodeRenderer.PORT_LABEL_GAP - textWidth, textY, labelColor, false);
        }
    }

    void advanceHover(boolean hovered, float lerpAmount) {
        float target = hovered ? 1f : 0f;
        float next = this.hoverProgress + (target - this.hoverProgress) * lerpAmount;
        if (Math.abs(next - target) < 0.005f) {
            next = target;
        }
        this.hoverProgress = next;
    }

    void appendMarkerEntry(CanvasViewport viewport,
                           double textureOriginX, double textureOriginY,
                           float guiScale,
                           boolean hovered,
                           double canvasMouseX, double canvasMouseY,
                           List<NodeBackgroundUniform.Entry> outEntries) {
        int color = GraphColors.of(this.port);
        boolean ring = false;

        if (this.port instanceof PropertyInputPort propertyPort) {
            boolean connected = this.parent.isConnected(this.port);
            if (!connected && !hovered && !this.isPropertyRowHovered(propertyPort, canvasMouseX, canvasMouseY)) {
                return;
            }
            if (!connected && !hovered) {
                color = PROPERTY_PLACEHOLDER_COLOR;
            }
        } else {
            ring = this.port instanceof InputPort input && input.optional();
        }

        Vector2fc center = this.center();
        Vector2dc screen = viewport.canvasToScreen(center.x(), center.y());

        float radiusFactor = 1f + (HOVER_RADIUS_FACTOR - 1f) * this.hoverProgress;
        float radiusScaled = NodeRenderer.PORT_RADIUS * viewport.zoom() * guiScale * radiusFactor;
        float borderThicknessScaled = Math.max(1f, radiusScaled * 0.45f);

        float w = 2f * radiusScaled;
        float h = 2f * radiusScaled;
        float relX = (float) ((screen.x() - textureOriginX) * guiScale) - radiusScaled;
        float relY = (float) ((screen.y() - textureOriginY) * guiScale) - radiusScaled;

        float alpha = this.parent.effectiveAlpha(this.port.type());
        int tinted = ARGB.multiply(color, ARGB.white(alpha));
        float lightenStrength = OUTLINE_LIGHTEN + (HOVER_OUTLINE_LIGHTEN - OUTLINE_LIGHTEN) * this.hoverProgress;
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
            int rowTop = this.parent.propertyRowTop(i);
            int rowBottom = rowTop + NodeRenderer.PROPERTY_PITCH;
            return canvasMouseX >= this.parent.owner().getX() && canvasMouseX < this.parent.owner().getX() + this.parent.owner().getWidth()
                    && canvasMouseY >= rowTop && canvasMouseY < rowBottom;
        }
        return false;
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
