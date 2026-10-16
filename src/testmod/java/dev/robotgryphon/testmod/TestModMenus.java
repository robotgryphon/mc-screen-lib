package dev.robotgryphon.testmod;

import dev.robotgryphon.testmod.menu.TreeHarvesterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TestModMenus {
    private TestModMenus() {}

    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, TestMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<TreeHarvesterMenu>> TREE_HARVESTER =
            MENU_TYPES.register("tree_harvester", () -> IMenuTypeExtension.create(TreeHarvesterMenu::new));

    public static void register(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}
