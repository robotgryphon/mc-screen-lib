package dev.robotgryphon.testmod.common;

import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasMenu;
import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasMenus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TestModMenus {
    private TestModMenus() {}

    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, TestMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CanvasMenu>> TREE_HARVESTER =
            MENU_TYPES.register("tree_harvester", CanvasMenus::createType);

    public static void register(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}
