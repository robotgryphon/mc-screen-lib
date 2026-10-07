package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.ports.Port;
import dev.robotgryphon.screenlib.client.PortLayout;
import dev.robotgryphon.screenlib.graph.ports.PortManager;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.core.Holder;
import net.minecraft.util.Util;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NodePortsWidget implements Renderable {
    private final NodeWidget owner;
    private final Node node;
    private final PortManager portManager;
    private final List<RenderedPort> renderedPorts;

    private long lastAnimMillis;

    public NodePortsWidget(NodeWidget owner, PortManager portManager, PortLayout layout) {
        this.owner = owner;
        this.node = owner.node();
        this.portManager = portManager;
        List<Port> ports = portManager.ports();
        List<RenderedPort> rendered = new ArrayList<>(ports.size());
        for (int i = 0; i < ports.size(); i++) {
            rendered.add(new RenderedPort(this, this.node, ports.get(i), layout.slots().get(i)));
        }
        this.renderedPorts = List.copyOf(rendered);
    }

    NodeWidget owner() {
        return this.owner;
    }

    public RenderedPort renderedPort(Port port) {
        for (RenderedPort rendered : this.renderedPorts) {
            if (rendered.port() == port) {
                return rendered;
            }
        }
        throw new IllegalArgumentException("Port not on this node: " + port);
    }

    public Vector2fc portAttachment(Port port) {
        PortLayout.Slot slot = this.renderedPort(port).slot();
        return new Vector2f(this.owner.getX(), this.owner.getY())
                .add(slot.attachmentOffset(this.owner.getWidth()));
    }

    public List<RenderedPort> ports() {
        return this.renderedPorts;
    }

    public @Nullable RenderedPort renderedPortAt(double mouseX, double mouseY) {
        for (RenderedPort rendered : this.renderedPorts) {
            if (rendered.contains(mouseX, mouseY)) {
                return rendered;
            }
        }
        return null;
    }

    public @Nullable Port portAt(double mouseX, double mouseY) {
        RenderedPort rendered = this.renderedPortAt(mouseX, mouseY);
        return rendered == null ? null : rendered.port();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        for (RenderedPort rendered : this.renderedPorts) {
            rendered.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    public void appendPortEntries(CanvasViewport viewport,
                                  double textureOriginX, double textureOriginY,
                                  float guiScale,
                                  double canvasMouseX, double canvasMouseY,
                                  List<NodeBackgroundUniform.Entry> outEntries) {
        RenderedPort hovered = this.renderedPortAt(canvasMouseX, canvasMouseY);

        float lerpAmount = this.computeHoverLerpAmount();
        for (RenderedPort rendered : this.renderedPorts) {
            rendered.advanceHover(rendered == hovered, lerpAmount);
        }
        for (RenderedPort rendered : this.renderedPorts) {
            rendered.appendMarkerEntry(viewport, textureOriginX, textureOriginY, guiScale,
                    rendered == hovered, canvasMouseX, canvasMouseY, outEntries);
        }
    }

    private float computeHoverLerpAmount() {
        long now = Util.getMillis();
        long previous = this.lastAnimMillis;
        this.lastAnimMillis = now;
        if (previous == 0L) {
            return 0f;
        }
        float dt = Math.min((now - previous) / 1000f, 0.1f);
        return 1f - (float) Math.exp(-RenderedPort.HOVER_ANIM_SPEED * dt);
    }

    int propertyRowTop(int index) {
        return this.owner.properties().rowTop(index);
    }

    boolean isConnected(Port port) {
        return this.node.portManager().isConnected(port);
    }

    float effectiveAlpha(Holder<GraphDataType<?>> type) {
        return this.owner.effectiveAlpha(type);
    }
}
