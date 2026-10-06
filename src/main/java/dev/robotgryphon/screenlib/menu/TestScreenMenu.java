package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.CanvasState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class TestScreenMenu extends AbstractContainerMenu {
    private final @Nullable Level serverLevel;

    private CanvasState state;

    public TestScreenMenu(int containerId, Inventory inv, Level level) {
        super(ScreenLib.TEST_SCREEN_MENU.get(), containerId);
        this.serverLevel = level;
        this.state = level.getData(ScreenLib.TEST_SCREEN_ATTACHMENT);
    }

    public TestScreenMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf data) {
        super(ScreenLib.TEST_SCREEN_MENU.get(), containerId);
        this.serverLevel = null;
        this.state = ByteBufCodecs.fromCodecWithRegistries(CanvasState.CODEC).decode(data);
    }

    public CanvasState state() {
        return state;
    }

    public void updateState(CanvasState newState) {
        this.state = newState;
        if (this.serverLevel != null) {
            this.serverLevel.setData(ScreenLib.TEST_SCREEN_ATTACHMENT, newState);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
