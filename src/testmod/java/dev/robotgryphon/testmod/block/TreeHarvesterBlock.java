package dev.robotgryphon.testmod.block;

import dev.robotgryphon.screenlib.ScreenLibCapabilities;
import dev.robotgryphon.testmod.TestModBlocks;
import dev.robotgryphon.testmod.menu.TestScreenMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != TestModBlocks.TREE_HARVESTER_ENTITY.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<TreeHarvesterBlockEntity>) (tickLevel, pos, tickState, harvester) ->
                harvester.serverTick((ServerLevel) tickLevel);
    }

    @NonNull
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        var canvas = level.getCapability(ScreenLibCapabilities.CANVAS, pos);
        var blockEntity = level.getBlockEntity(pos);
        if (player instanceof ServerPlayer serverPlayer && canvas != null && blockEntity != null) {
            serverPlayer.openMenu(new TestScreenMenuProvider(canvas,
                    viewer -> Container.stillValidBlockEntity(blockEntity, viewer)));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
