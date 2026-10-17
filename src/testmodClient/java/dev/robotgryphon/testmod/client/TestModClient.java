package dev.robotgryphon.testmod.client;

import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasPanels;
import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasTools;
import dev.robotgryphon.screenlib.ui.api.UiScreens;
import dev.robotgryphon.screenlib.ui.api.layout.HorizontalAnchor;
import dev.robotgryphon.screenlib.ui.api.layout.Placement;
import dev.robotgryphon.screenlib.ui.api.layout.VerticalAnchor;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarStyle;

import dev.robotgryphon.testmod.common.TestMod;
import dev.robotgryphon.testmod.common.TestModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;

@Mod(value = TestMod.MOD_ID, dist = Dist.CLIENT)
public class TestModClient {
    private static final int TOOLBAR_SPACE = 5 + ToolbarStyle.THICKNESS + 4;

    public TestModClient(IEventBus modBus) {
        modBus.addListener(TestModClient::registerMenuScreens);
        TestToolbars.BUTTONS.register(modBus);
        TestToolbars.PANELS.register(modBus);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        UiScreens.register(event, TestModMenus.TREE_HARVESTER.get(), (menu, inventory, title) -> UiScreens.builder(title)
                .panel(Placement.inset(5, TOOLBAR_SPACE, 5, 5), CanvasPanels.forMenu(menu, title))
                .layout(TestToolbars::addTo)
                .tabs(Placement.anchored(HorizontalAnchor.LEFT, VerticalAnchor.TOP, 5, TOOLBAR_SPACE),
                        List.of(TestToolbars.PROVIDED_PANEL, TestToolbars.FAVORITES_PANEL, CanvasTools.ADD_NODE_PANEL)));
    }
}
