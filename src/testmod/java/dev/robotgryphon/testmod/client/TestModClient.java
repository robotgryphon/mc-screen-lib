package dev.robotgryphon.testmod.client;

import dev.robotgryphon.testmod.TestMod;
import dev.robotgryphon.testmod.TestModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = TestMod.MOD_ID, dist = Dist.CLIENT)
public class TestModClient {
    public TestModClient(IEventBus modBus) {
        modBus.addListener(TestModClient::registerMenuScreens);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TestModMenus.TEST_SCREEN.get(), TestScreen::new);
    }
}
