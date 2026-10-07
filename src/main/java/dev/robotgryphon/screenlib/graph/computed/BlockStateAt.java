package dev.robotgryphon.screenlib.graph.computed;

import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

public class BlockStateAt implements ComputedProperty<BlockState>, ClientDataProvider<BlockState, BlockState> {
    public static final DataKey<BlockPos> POSITION = DataKey.required("position", ScreenLib.BLOCK_POS_DATA_TYPE);
    public static final DataKey<ResourceKey<Level>> LEVEL = DataKey.required("level", ScreenLib.LEVEL_DATA_TYPE);

    private static final StreamCodec<RegistryFriendlyByteBuf, BlockState> CLIENT_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(BlockState.CODEC);

    @Override
    public List<DataKey<?>> requires() {
        return List.of(POSITION, LEVEL);
    }

    @Override
    public Optional<BlockState> compute(ComputeContext context) {
        var server = context.server();
        var position = context.get(POSITION);
        var levelKey = context.get(LEVEL);

        if (server == null || position.isEmpty() || levelKey.isEmpty())
            return Optional.empty();

        var level = server.getLevel(levelKey.get());
        return level == null ? Optional.empty() : Optional.of(level.getBlockState(position.get()));
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, BlockState> clientCodec() {
        return CLIENT_CODEC;
    }

    @Override
    public Optional<BlockState> clientData(BlockState value, ComputeContext context) {
        return Optional.of(value);
    }
}
