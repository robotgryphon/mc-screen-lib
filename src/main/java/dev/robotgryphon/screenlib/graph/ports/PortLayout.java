package dev.robotgryphon.screenlib.graph.ports;

import dev.robotgryphon.screenlib.client.NodeRenderer;
import dev.robotgryphon.screenlib.graph.nodes.NodeDefinition;
import dev.robotgryphon.screenlib.graph.properties.NodeProperty;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.Holder;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class PortLayout {
    private static final Map<NodeDefinition, PortLayout> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    public enum Kind { INPUT, OUTPUT, PROPERTY }

    public record Slot(Kind kind, int index, PortSide side, String name,
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

    public static PortLayout from(NodeDefinition definition) {
        return CACHE.computeIfAbsent(definition,
                def -> compile(def, Minecraft.getInstance().font));
    }

    public static PortLayout compile(NodeDefinition def, Font font) {
        int inputs = def.inputs().size();
        int outputs = def.outputs().size();
        int busiestSide = Math.max(inputs, outputs);
        int bandHeight = NodeRenderer.computePortBandHeight(font, busiestSide);

        List<Slot> slots = new ArrayList<>(inputs + outputs + def.properties().size());
        for (int i = 0; i < inputs; i++) {
            var input = def.inputs().get(i);
            slots.add(new Slot(Kind.INPUT, i, PortSide.LEFT, input.name(), input.type(),
                    bandAnchor(i, busiestSide, bandHeight), false));
        }
        for (int i = 0; i < outputs; i++) {
            var output = def.outputs().get(i);
            slots.add(new Slot(Kind.OUTPUT, i, PortSide.RIGHT, output.name(), output.type(),
                    bandAnchor(i, busiestSide, bandHeight), false));
        }
        int propertyRegionTop = NodeRenderer.TITLE_BAR_HEIGHT + bandHeight;
        for (int i = 0; i < def.properties().size(); i++) {
            NodeProperty<?> prop = def.properties().get(i);
            int rowTop = propertyRegionTop + NodeRenderer.PROPERTY_REGION_PADDING_Y
                    + i * (NodeRenderer.PROPERTY_PITCH + NodeRenderer.PROPERTY_ROW_GAP);
            slots.add(new Slot(Kind.PROPERTY, i, PortSide.LEFT, prop.name(), prop.anyType(),
                    rowTop + NodeRenderer.PROPERTY_PITCH / 2, true));
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
