package dev.robotgryphon.screenlib.client;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.ports.Port;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
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
        register(ScreenLib.BLOCK_POS_DATA_TYPE, POSITION);
        register(ScreenLib.INTEGER_DATA_TYPE, NUMBER);
        register(ScreenLib.FLOAT_DATA_TYPE, NUMBER);
        register(ScreenLib.DOUBLE_DATA_TYPE, NUMBER);
        register(ScreenLib.BOOLEAN_DATA_TYPE, BOOLEAN);
        register(ScreenLib.STRING_DATA_TYPE, STRING);
        register(ScreenLib.DIRECTION_DATA_TYPE, DIRECTION);
        register(ScreenLib.ITEM_HANDLER_DATA_TYPE, RESOURCE);
        register(ScreenLib.BLOCKSTATE_DATA_TYPE, RESOURCE);
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
