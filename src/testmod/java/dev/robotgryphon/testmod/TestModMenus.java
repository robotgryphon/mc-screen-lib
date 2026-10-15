package dev.robotgryphon.testmod;

import dev.robotgryphon.screenlib.graph.canvas.CanvasDefinition;
import dev.robotgryphon.screenlib.graph.canvas.CanvasSupplier;
import dev.robotgryphon.testmod.menu.TestScreenMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class TestModMenus {
    private TestModMenus() {}

    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, TestMod.MOD_ID);

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TestMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<TestScreenMenu>> TEST_SCREEN =
            MENU_TYPES.register("test_screen", () -> IMenuTypeExtension.create(TestScreenMenu::new));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CanvasDefinition>> CANVAS_ATTACHMENT =
            ATTACHMENT_TYPES.register("canvas", () -> AttachmentType
                    .builder(() -> new CanvasDefinition())
                    .serialize(CanvasDefinition.MAP_CODEC)
                    .build());

    public static CanvasSupplier levelCanvas(Level level) {
        CanvasDefinition canvas = level.getData(CANVAS_ATTACHMENT);
        return CanvasSupplier.of(canvas)
                .onChanged(() -> level.setData(CANVAS_ATTACHMENT, canvas));
    }

    public static void register(IEventBus modBus) {
        MENU_TYPES.register(modBus);
        ATTACHMENT_TYPES.register(modBus);
    }
}
