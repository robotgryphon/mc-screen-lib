package dev.robotgryphon.testmod.block;

import dev.robotgryphon.screenlib.ScreenLibCapabilities;
import dev.robotgryphon.testmod.TestModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TreeHarvesterBlock extends Block implements EntityBlock {
    public TreeHarvesterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TestModBlocks.TREE_HARVESTER_ENTITY.get().create(pos, state);
    }

    @NonNull
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        var source = level.getCapability(ScreenLibCapabilities.CANVAS_SOURCE, pos);
        if (player instanceof ServerPlayer serverPlayer && source != null) {
            serverPlayer.openMenu(source.menu());
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
