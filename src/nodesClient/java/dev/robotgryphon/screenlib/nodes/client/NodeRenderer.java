package dev.robotgryphon.screenlib.nodes.client;

import dev.robotgryphon.screenlib.nodes.common.ports.PropertyOutputPort;
import dev.robotgryphon.screenlib.nodes.common.Node;
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

        int maxOutput = maxOutputLabelWidth(font, layout);
        int portsNeed = 0;
        if (maxOutput > 0) {
            portsNeed = 2 * PORT_LABEL_INSET + maxOutput + LABEL_BETWEEN_GAP;
        }

        int propsNeed = 0;
        for (int i = 0; i < rowNames.size(); i++) {
            String rowName = rowNames.get(i);
            int labelWidth = rowName.isEmpty() ? 0 : font.width(Component.literal(rowName)) + PROPERTY_LABEL_VALUE_GAP;
            int rowWidth = 2 * PROPERTY_PADDING_X + labelWidth + valueWidths.get(i);
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

    private static int maxOutputLabelWidth(Font font, PortLayout layout) {
        int max = 0;
        for (PortLayout.Slot slot : layout.slots()) {
            if (slot.port() instanceof PropertyOutputPort output) {
                max = Math.max(max, font.width(Component.literal(output.title().getString())));
            }
        }
        return max;
    }

    public static Component propertyLabel(Node node, String property) {
        return Node.propertyLabel(node.type(), property);
    }
}
