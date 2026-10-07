package dev.robotgryphon.screenlib.graph.render;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.computed.ComputeContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class BlockPreviewProvider implements NodeRenderProvider<BlockPreviewState> {
    private static final StreamCodec<RegistryFriendlyByteBuf, BlockPreviewState> CODEC =
            ByteBufCodecs.fromCodecWithRegistries(BlockState.CODEC)
                    .map(BlockPreviewState::new, BlockPreviewState::state);

    @Override
    public int width() {
        return 40;
    }

    @Override
    public int height() {
        return 40;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, BlockPreviewState> codec() {
        return CODEC;
    }

    @Override
    public Optional<BlockPreviewState> extract(ComputeContext context) {
        return context.get("state", ScreenLib.BLOCKSTATE_DATA_TYPE.value()).map(BlockPreviewState::new);
    }
}
