package dev.robotgryphon.testmod.menu;

import dev.robotgryphon.screenlib.graph.canvas.CanvasSupplier;
import dev.robotgryphon.screenlib.menu.CanvasMenu;
import dev.robotgryphon.testmod.TestModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

public class TestScreenMenu extends CanvasMenu {
    private final Predicate<Player> validFor;

    public TestScreenMenu(int containerId, Inventory inv, CanvasSupplier source, MenuProvider provider,
                          Predicate<Player> validFor) {
        super(TestModMenus.TEST_SCREEN.get(), containerId, inv, source, provider);
        this.validFor = validFor;
    }

    public TestScreenMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf data) {
        super(TestModMenus.TEST_SCREEN.get(), containerId, data);
        this.validFor = player -> true;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.validFor.test(player);
    }
}
