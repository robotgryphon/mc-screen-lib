package dev.robotgryphon.screenlib.client;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class NodeRenderer {
    private NodeRenderer() {}

    public static final int TITLE_BAR_HEIGHT = 12;
    public static final int PORT_RADIUS = 3;
    public static final int PORT_LABEL_GAP = 4;

    public static final int PROPERTY_PITCH = 14;
    public static final int PROPERTY_ROW_GAP = 2;
    public static final int PROPERTY_PADDING_X = 8;
    public static final int PROPERTY_REGION_PADDING_Y = PROPERTY_PADDING_X;
    public static final int PROPERTY_VALUE_MIN_WIDTH = 60;
    public static final int COMPUTED_VALUE_WIDTH = 110;
    public static final int RENDER_REGION_PADDING = 4;

    public static int renderRegionHeight(List<Integer> heights) {
        if (heights.isEmpty()) {
            return 0;
        }
        int total = RENDER_REGION_PADDING;
        for (int height : heights) {
            total += height + RENDER_REGION_PADDING;
        }
        return total;
    }
    public static final int PROPERTY_LABEL_VALUE_GAP = 16;

    private static final int TITLE_PADDING = 8;
    private static final int PORT_LABEL_INSET = PORT_RADIUS + 1 + PORT_LABEL_GAP;
    private static final int LABEL_BETWEEN_GAP = 8;
    private static final int MIN_PORT_PITCH = 12;
    private static final int MIN_PORT_BAND_HEIGHT = 24;
    private static final int MIN_WIDTH = 60;

    public static int propertyRegionHeight(int rowCount) {
        return rowCount == 0
                ? 0
                : 2 * PROPERTY_REGION_PADDING_Y
                        + rowCount * PROPERTY_PITCH
                        + (rowCount - 1) * PROPERTY_ROW_GAP;
    }

    public static int computeWidth(Font font, Component title, PortLayout layout,
                                   List<String> rowNames, List<Integer> valueWidths) {
        int titleNeed = font.width(title) + 2 * TITLE_PADDING;

        int maxLeft = maxPortLabelWidth(font, layout, PortLayout.Kind.INPUT);
        int maxRight = maxPortLabelWidth(font, layout, PortLayout.Kind.OUTPUT);
        int portsNeed = 0;
        if (maxLeft > 0 || maxRight > 0) {
            portsNeed = 2 * PORT_LABEL_INSET + maxLeft + maxRight + LABEL_BETWEEN_GAP;
        }

        int propsNeed = 0;
        for (int i = 0; i < rowNames.size(); i++) {
            int labelWidth = font.width(Component.literal(rowNames.get(i)));
            int rowWidth = 2 * PROPERTY_PADDING_X
                    + labelWidth + PROPERTY_LABEL_VALUE_GAP + valueWidths.get(i);
            propsNeed = Math.max(propsNeed, rowWidth);
        }

        return Math.max(MIN_WIDTH, Math.max(Math.max(titleNeed, portsNeed), propsNeed));
    }

    public static int computePortBandHeight(Font font, int maxPortsPerSide) {
        if (maxPortsPerSide == 0) {
            return 0;
        }
        int pitch = Math.max(MIN_PORT_PITCH, font.lineHeight + 3);
        return Math.max(MIN_PORT_BAND_HEIGHT, pitch * (maxPortsPerSide + 1));
    }

    private static int maxPortLabelWidth(Font font, PortLayout layout, PortLayout.Kind kind) {
        int max = 0;
        for (PortLayout.Slot slot : layout.slots()) {
            if (slot.kind() == kind) {
                max = Math.max(max, font.width(Component.literal(slot.name())));
            }
        }
        return max;
    }

    public static Component propertyLabel(Node node, String property) {
        return Node.propertyLabel(node.type(), property);
    }
}
