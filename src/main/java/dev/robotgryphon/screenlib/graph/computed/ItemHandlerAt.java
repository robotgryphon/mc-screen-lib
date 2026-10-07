package dev.robotgryphon.screenlib.graph.computed;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.types.ItemHandlerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.Optional;

public class ItemHandlerAt implements ComputedProperty<ItemHandlerAccess> {
    @Override
    public Optional<ItemHandlerAccess> compute(ComputeContext context) {
        var server = context.server();
        Optional<BlockPos> position = context.get("Position", ScreenLib.BLOCK_POS_DATA_TYPE.value());
        Optional<ResourceKey<Level>> levelKey = context.get("Level", ScreenLib.LEVEL_DATA_TYPE.value());
        if (server == null || position.isEmpty() || levelKey.isEmpty()) {
            return Optional.empty();
        }
        Direction side = context.get("Side", ScreenLib.DIRECTION_DATA_TYPE.value()).orElse(null);
        var level = server.getLevel(levelKey.get());
        if (level == null || level.getCapability(Capabilities.Item.BLOCK, position.get(), side) == null) {
            return Optional.empty();
        }
        return Optional.of(new ItemHandlerAccess(levelKey.get(), position.get(), side));
    }
}
