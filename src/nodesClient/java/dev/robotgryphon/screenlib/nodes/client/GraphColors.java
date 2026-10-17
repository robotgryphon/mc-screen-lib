package dev.robotgryphon.screenlib.nodes.client;

import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.nodes.common.ScreenLibNodes;
import dev.robotgryphon.screenlib.nodes.common.ports.Port;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class GraphColors {
    public static final int DEFAULT = 0xFF8FA0FF;
    public static final int POSITION = 0xFF55D784;
    public static final int NUMBER = 0xFFE89E4A;
    public static final int BOOLEAN = 0xFFE8C84A;
    public static final int STRING = 0xFF6FD0E8;
    public static final int DIRECTION = 0xFF7AA8FF;
    public static final int RESOURCE = 0xFFE07ADC;

    private static final Map<Identifier, Integer> COLORS = new HashMap<>();

    static {
        register(ScreenLibNodes.BLOCK_POS_DATA_TYPE, POSITION);
        register(ScreenLibNodes.INTEGER_DATA_TYPE, NUMBER);
        register(ScreenLibNodes.FLOAT_DATA_TYPE, NUMBER);
        register(ScreenLibNodes.DOUBLE_DATA_TYPE, NUMBER);
        register(ScreenLibNodes.BOOLEAN_DATA_TYPE, BOOLEAN);
        register(ScreenLibNodes.STRING_DATA_TYPE, STRING);
        register(ScreenLibNodes.DIRECTION_DATA_TYPE, DIRECTION);
        register(ScreenLibNodes.ITEM_HANDLER_DATA_TYPE, RESOURCE);
        register(ScreenLibNodes.BLOCKSTATE_DATA_TYPE, RESOURCE);
    }

    private GraphColors() {}

    public static void register(Holder<GraphDataType<?>> type, int color) {
        COLORS.put(type.unwrapKey().orElseThrow().identifier(), color);
    }

    public static int of(Holder<GraphDataType<?>> type) {
        return type.unwrapKey()
                .map(key -> COLORS.getOrDefault(key.identifier(), DEFAULT))
                .orElse(DEFAULT);
    }

    public static int of(Port port) {
        return of(port.type());
    }
}
