package dev.robotgryphon.testmod.block;

import dev.robotgryphon.screenlib.menu.CanvasSource;
import dev.robotgryphon.testmod.TestModBlocks;
import dev.robotgryphon.testmod.menu.BlockEntityCanvasSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TreeHarvesterBlockEntity extends BlockEntity {

    private final CanvasSource canvas = new BlockEntityCanvasSource(this);

    public TreeHarvesterBlockEntity(BlockPos pos, BlockState state) {
        super(TestModBlocks.TREE_HARVESTER_ENTITY.get(), pos, state);
    }

    public CanvasSource getCanvas() {
        return canvas;
    }
}
