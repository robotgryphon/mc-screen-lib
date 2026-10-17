package dev.robotgryphon.screenlib.nodes.common.properties.computed;

import dev.robotgryphon.screenlib.nodes.api.properties.computed.ComputedProperties;
import net.neoforged.neoforge.registries.DeferredHolder;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import dev.robotgryphon.screenlib.graph.schema.properties.DataKey;
import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.nodes.common.ScreenLibNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import dev.robotgryphon.screenlib.graph.schema.properties.computed.ClientDataProvider;
import dev.robotgryphon.screenlib.graph.schema.properties.computed.ComputeContext;
import dev.robotgryphon.screenlib.graph.schema.properties.computed.ComputedProperty;

public class BlockStateAt implements ComputedProperty<BlockState>, ClientDataProvider<BlockState, PlacedBlock> {
    private static final DataKey<BlockPos> POSITION = ComputedProperties.BlockStateAt.POSITION;
    private static final DataKey<ResourceKey<Level>> LEVEL = ComputedProperties.BlockStateAt.LEVEL;

    @Override
    public DeferredHolder<GraphDataType<?>, ? extends GraphDataType<BlockState>> type() {
        return ScreenLibNodes.BLOCKSTATE_DATA_TYPE;
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
