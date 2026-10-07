package dev.robotgryphon.screenlib.graph.computed;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.types.ItemHandlerAccess;
import dev.robotgryphon.screenlib.transfer.ItemHandlerContents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemHandlerAt implements ComputedProperty<ItemHandlerAccess>, ClientDataProvider<ItemHandlerAccess, ItemHandlerContents> {
    public static final DataKey<BlockPos> POSITION = DataKey.required("position", ScreenLib.BLOCK_POS_DATA_TYPE);
    public static final DataKey<ResourceKey<Level>> LEVEL = DataKey.required("level", ScreenLib.LEVEL_DATA_TYPE);
    public static final DataKey<Direction> SIDE = DataKey.optional("side", ScreenLib.DIRECTION_DATA_TYPE);

    @Override
    public List<DataKey<?>> requires() {
        return List.of(POSITION, LEVEL, SIDE);
    }

    @Override
    public Optional<ItemHandlerAccess> compute(ComputeContext context) {
        var server = context.server();
        Optional<BlockPos> position = context.get(POSITION);
        Optional<ResourceKey<Level>> levelKey = context.get(LEVEL);
        if (server == null || position.isEmpty() || levelKey.isEmpty()) {
            return Optional.empty();
        }
        Direction side = context.get(SIDE).orElse(null);
        var level = server.getLevel(levelKey.get());
        if (level == null || level.getCapability(Capabilities.Item.BLOCK, position.get(), side) == null) {
            return Optional.empty();
        }
        return Optional.of(new ItemHandlerAccess(levelKey.get(), position.get(), side));
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ItemHandlerContents> clientCodec() {
        return ItemHandlerContents.STREAM_CODEC;
    }

    @Override
    public Optional<ItemHandlerContents> clientData(ItemHandlerAccess value, ComputeContext context) {
        var server = context.server();
        var level = server == null ? null : server.getLevel(value.level());
        var handler = level == null ? null : level.getCapability(Capabilities.Item.BLOCK, value.pos(), value.side());
        if (handler == null) {
            return Optional.empty();
        }
        List<ItemStack> stacks = new ArrayList<>(handler.size());
        for (int slot = 0; slot < handler.size(); slot++) {
            var resource = handler.getResource(slot);
            stacks.add(resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(handler.getAmountAsInt(slot)));
        }
        return Optional.of(new ItemHandlerContents(stacks));
    }
}
