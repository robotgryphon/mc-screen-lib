package dev.robotgryphon.testmod.client;

import dev.robotgryphon.screenlib.client.ui.CanvasSession;
import dev.robotgryphon.screenlib.client.ui.core.UiScreen;
import dev.robotgryphon.screenlib.client.ui.core.UiScreens;
import dev.robotgryphon.screenlib.client.ui.core.layout.HorizontalAnchor;
import dev.robotgryphon.screenlib.client.ui.core.layout.Placement;
import dev.robotgryphon.screenlib.client.ui.core.layout.VerticalAnchor;
import dev.robotgryphon.screenlib.client.ui.widget.canvas.CanvasPanel;
import dev.robotgryphon.screenlib.client.ui.widget.canvas.CanvasTools;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.Toolbar;

import dev.robotgryphon.testmod.TestMod;
import dev.robotgryphon.testmod.TestModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = TestMod.MOD_ID, dist = Dist.CLIENT)
public class TestModClient {
    private static final int TOOLBAR_SPACE = 5 + Toolbar.THICKNESS + 4;

    public TestModClient(IEventBus modBus) {
        modBus.addListener(TestModClient::registerMenuScreens);
        TestToolbars.BUTTONS.register(modBus);
        TestToolbars.PANELS.register(modBus);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        UiScreens.register(event, TestModMenus.TREE_HARVESTER.get(), (menu, inventory, title) -> {
            CanvasSession session = new CanvasSession(menu);
            return UiScreen.builder(title)
                    .panel(Placement.inset(5, TOOLBAR_SPACE, 5, 5), host -> new CanvasPanel(host, session, title))
                    .layout(TestToolbars::addTo)
                    .tabs(Placement.anchored(HorizontalAnchor.LEFT, VerticalAnchor.TOP, 5, TOOLBAR_SPACE),
                            TestToolbars.PROVIDED_PANEL, TestToolbars.FAVORITES_PANEL, CanvasTools.ADD_NODE_PANEL);
        });
    }
}
