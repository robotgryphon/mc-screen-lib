package dev.robotgryphon.screenlib.client;

import dev.robotgryphon.screenlib.graph.nodes.ports.Port;
import dev.robotgryphon.screenlib.graph.nodes.ports.PortSide;
import dev.robotgryphon.screenlib.graph.nodes.ports.PropertyInputPort;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.client.gui.Font;
import net.minecraft.core.Holder;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public final class PortLayout {
    public enum Kind { INPUT, OUTPUT, PROPERTY }

    public record Slot(Kind kind, PortSide side, String name,
                       Holder<GraphDataType<?>> type, int anchorY, boolean rowAnchored) {
        public Vector2f offset(int nodeWidth) {
            int x = switch (this.side) {
                case LEFT -> 0;
                case RIGHT -> nodeWidth - 1;
            };
            return this.rowAnchored
                    ? new Vector2f(x, this.anchorY + 0.5f)
                    : new Vector2f(x + 0.5f, this.anchorY + 0.5f);
        }

        public Vector2f attachmentOffset(int nodeWidth) {
            float edgeX = switch (this.side) {
                case LEFT -> 0;
                case RIGHT -> nodeWidth;
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
        int inputs = 0;
        int outputs = 0;
        for (Port port : ports) {
            if (port instanceof PropertyInputPort) continue;
            if (port.side() == PortSide.LEFT) inputs++;
            else outputs++;
        }
        int busiestSide = Math.max(inputs, outputs);
        int bandHeight = NodeRenderer.computePortBandHeight(font, busiestSide);
        int propertyRegionTop = NodeRenderer.TITLE_BAR_HEIGHT + bandHeight;

        List<Slot> slots = new ArrayList<>(ports.size());
        int inputIndex = 0;
        int outputIndex = 0;
        for (Port port : ports) {
            if (port instanceof PropertyInputPort property) {
                int propertyIndex = property.node().propertyManager().stored().indexOf(property.property());
                int rowTop = propertyRegionTop + NodeRenderer.PROPERTY_REGION_PADDING_Y
                        + propertyIndex * (NodeRenderer.PROPERTY_PITCH + NodeRenderer.PROPERTY_ROW_GAP);
                slots.add(new Slot(Kind.PROPERTY, PortSide.LEFT, property.propertyName(), property.type(),
                        rowTop + NodeRenderer.PROPERTY_PITCH / 2, true));
            } else if (port.side() == PortSide.LEFT) {
                slots.add(new Slot(Kind.INPUT, PortSide.LEFT, port.title().getString(), port.type(),
                        bandAnchor(inputIndex++, busiestSide, bandHeight), false));
            } else {
                slots.add(new Slot(Kind.OUTPUT, PortSide.RIGHT, port.title().getString(), port.type(),
                        bandAnchor(outputIndex++, busiestSide, bandHeight), false));
            }
        }
        return new PortLayout(List.copyOf(slots), bandHeight);
    }

    private static int bandAnchor(int index, int busiestSide, int bandHeight) {
        float t = (index + 1f) / (busiestSide + 1f);
        return NodeRenderer.TITLE_BAR_HEIGHT + Math.round(bandHeight * t);
    }

    public List<Slot> slots() {
        return this.slots;
    }

    public int bandHeight() {
        return this.bandHeight;
    }
}
