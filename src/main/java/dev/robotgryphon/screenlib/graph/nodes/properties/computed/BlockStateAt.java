package dev.robotgryphon.screenlib.graph.nodes.properties.computed;

import net.neoforged.neoforge.registries.DeferredHolder;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import dev.robotgryphon.screenlib.graph.nodes.properties.DataKey;
import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

public class BlockStateAt implements ComputedProperty<BlockState>, ClientDataProvider<BlockState, PlacedBlock> {
    public static final DataKey<BlockPos> POSITION = DataKey.required("position", ScreenLib.BLOCK_POS_DATA_TYPE);
    public static final DataKey<ResourceKey<Level>> LEVEL = DataKey.required("level", ScreenLib.LEVEL_DATA_TYPE);

    @Override
    public DeferredHolder<GraphDataType<?>, ? extends GraphDataType<BlockState>> type() {
        return ScreenLib.BLOCKSTATE_DATA_TYPE;
    }

    @Override
    public Optional<ClientDataProvider<BlockState, ?>> clientDataProvider() {
        return Optional.of(this);
    }

    @Override
    public List<DataKey<?>> requires() {
        return List.of(POSITION, LEVEL);
    }

    @Override
    public Optional<BlockState> compute(ComputeContext context) {
        var server = context.server();
        var position = context.get(POSITION);
        var levelKey = context.get(LEVEL);

        if (position.isEmpty() || levelKey.isEmpty())
            return Optional.empty();

        var level = server.getLevel(levelKey.get());
        return level == null ? Optional.empty() : Optional.of(level.getBlockState(position.get()));
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, PlacedBlock> clientCodec() {
        return PlacedBlock.STREAM_CODEC.cast();
    }

    @Override
    public Optional<PlacedBlock> clientData(BlockState value, ComputeContext context) {
        return context.get(LEVEL).flatMap(level -> context.get(POSITION).map(pos -> new PlacedBlock(level, pos, value)));
    }
}
