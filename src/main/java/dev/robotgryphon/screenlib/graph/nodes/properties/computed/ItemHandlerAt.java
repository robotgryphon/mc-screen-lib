package dev.robotgryphon.screenlib.graph.nodes.properties.computed;

import net.neoforged.neoforge.registries.DeferredHolder;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import dev.robotgryphon.screenlib.graph.nodes.properties.DataKey;
import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.types.ItemHandlerAccess;
import dev.robotgryphon.screenlib.transfer.ItemHandlerContents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
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
    public DeferredHolder<GraphDataType<?>, ? extends GraphDataType<ItemHandlerAccess>> type() {
        return ScreenLib.ITEM_HANDLER_DATA_TYPE;
    }

    @Override
    public Optional<ClientDataProvider<ItemHandlerAccess, ?>> clientDataProvider() {
        return Optional.of(this);
    }

    @Override
    public List<DataKey<?>> requires() {
        return List.of(POSITION, LEVEL, SIDE);
    }

    @Override
    public Optional<ItemHandlerAccess> compute(ComputeContext context) {
        var server = context.server();
        Optional<BlockPos> position = context.get(POSITION);
        Optional<ResourceKey<Level>> levelKey = context.get(LEVEL);
        if (position.isEmpty() || levelKey.isEmpty()) {
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
        var level = context.server().getLevel(value.level());
        var handler = level == null ? null : level.getCapability(Capabilities.Item.BLOCK, value.pos(), value.side());
        if (handler == null) {
            return Optional.empty();
        }
        List<ItemHandlerContents.Entry> entries = new ArrayList<>(handler.size());
        for (int slot = 0; slot < handler.size(); slot++) {
            var resource = handler.getResource(slot);
            entries.add(resource.isEmpty()
                    ? ItemHandlerContents.Entry.EMPTY
                    : new ItemHandlerContents.Entry(resource, handler.getAmountAsLong(slot)));
        }
        return Optional.of(new ItemHandlerContents(entries));
    }
}
