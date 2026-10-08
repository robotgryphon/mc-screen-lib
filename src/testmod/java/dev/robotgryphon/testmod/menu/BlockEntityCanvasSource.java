package dev.robotgryphon.testmod.menu;

import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.menu.CanvasSource;
import dev.robotgryphon.screenlib.menu.HostedNode;
import dev.robotgryphon.testmod.TestModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public record BlockEntityCanvasSource(BlockEntity blockEntity, List<HostedNode> hostedNodes) implements CanvasSource {
    @Override
    public Level level() {
        return this.blockEntity.getLevel();
    }

    @Override
    public CanvasState load() {
        return this.blockEntity.getData(TestModMenus.CANVAS_ATTACHMENT);
    }

    @Override
    public void save(CanvasState state) {
        this.blockEntity.setData(TestModMenus.CANVAS_ATTACHMENT, state);
    }

    @Override
    public MenuProvider menu() {
        return new TestScreenMenuProvider(this);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this.blockEntity, player);
    }
}
