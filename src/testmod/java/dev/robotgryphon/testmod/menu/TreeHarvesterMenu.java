package dev.robotgryphon.testmod.menu;

import dev.robotgryphon.screenlib.graph.canvas.CanvasSupplier;
import dev.robotgryphon.screenlib.menu.CanvasMenu;
import dev.robotgryphon.testmod.TestModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public class TreeHarvesterMenu extends CanvasMenu {
    private final @Nullable BlockEntity blockEntity;

    public TreeHarvesterMenu(int containerId, Inventory inventory, CanvasSupplier source, MenuProvider provider,
                             BlockEntity blockEntity) {
        super(TestModMenus.TREE_HARVESTER.get(), containerId, inventory, source, provider);
        this.blockEntity = blockEntity;
    }

    public TreeHarvesterMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        super(TestModMenus.TREE_HARVESTER.get(), containerId, data);
        this.blockEntity = null;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity == null || Container.stillValidBlockEntity(this.blockEntity, player);
    }
}
