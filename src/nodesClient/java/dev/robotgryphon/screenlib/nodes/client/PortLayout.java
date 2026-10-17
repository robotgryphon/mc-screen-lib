package dev.robotgryphon.screenlib.nodes.client;

import dev.robotgryphon.screenlib.nodes.common.ports.Port;
import dev.robotgryphon.screenlib.nodes.common.ports.PropertyInputPort;
import dev.robotgryphon.screenlib.nodes.common.ports.PropertyOutputPort;
import net.minecraft.client.gui.Font;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public final class PortLayout {
    public record Slot(Port port, int anchorY) {
        public Vector2f offset(int nodeWidth) {
            return switch (this.port) {
                case PropertyInputPort ignored -> new Vector2f(0, this.anchorY + 0.5f);
                case PropertyOutputPort ignored -> new Vector2f(nodeWidth - 0.5f, this.anchorY + 0.5f);
            };
        }

        public Vector2f attachmentOffset(int nodeWidth) {
            float edgeX = switch (this.port) {
                case PropertyInputPort ignored -> 0;
                case PropertyOutputPort ignored -> nodeWidth;
            };
            return new Vector2f(edgeX, this.offset(nodeWidth).y());
        }
    }

    private final List<Slot> slots;
    private final int bandHeight;

    private PortLayout(List<Slot> slots, int bandHeight) {
        this.slots = slots;
        this.bandHeight = bandHeight;
    }

    public static PortLayout compile(List<Port> ports, Font font) {
        int outputs = (int) ports.stream().filter(port -> port instanceof PropertyOutputPort).count();
        int bandHeight = NodeRenderer.computePortBandHeight(font, outputs);
        int propertyRegionTop = NodeRenderer.TITLE_BAR_HEIGHT + bandHeight;

        List<Slot> slots = new ArrayList<>(ports.size());
        int outputIndex = 0;
        for (Port port : ports) {
            int anchorY = switch (port) {
                case PropertyInputPort input -> {
                    int propertyIndex = input.node().propertyManager().stored().indexOf(input.property());
                    int rowTop = propertyRegionTop + NodeRenderer.PROPERTY_REGION_PADDING_Y
                            + propertyIndex * (NodeRenderer.PROPERTY_PITCH + NodeRenderer.PROPERTY_ROW_GAP);
                    yield rowTop + NodeRenderer.PROPERTY_PITCH / 2;
                }
                case PropertyOutputPort ignored -> bandAnchor(outputIndex++, outputs, bandHeight);
            };
            slots.add(new Slot(port, anchorY));
        }
        return new PortLayout(List.copyOf(slots), bandHeight);
    }

    private static int bandAnchor(int index, int outputs, int bandHeight) {
        float t = (index + 1f) / (outputs + 1f);
        return NodeRenderer.TITLE_BAR_HEIGHT + Math.round(bandHeight * t);
    }

    public List<Slot> slots() {
        return this.slots;
    }

    public int bandHeight() {
        return this.bandHeight;
    }
}
