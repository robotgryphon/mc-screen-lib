package dev.robotgryphon.screenlib.client.ui.widget.property;

import net.minecraft.resources.ResourceKey;

public final class PropertyValues {
    public static final Object CLEAR = new Object();

    private PropertyValues() {}

    public static String display(Object value) {
        if (value == CLEAR) {
            return "— (clear)";
        }
        if (value instanceof net.minecraft.world.level.block.state.BlockState state) {
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        }
        if (value instanceof net.minecraft.core.BlockPos pos) {
            return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        }
        if (value instanceof dev.robotgryphon.screenlib.graph.types.ItemHandlerAccess access) {
            return display(access.pos());
        }
        if (value instanceof ResourceKey<?> key) {
            return key.identifier().toString();
        }
        return value.toString();
    }
}
