package dev.robotgryphon.testmod.common;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TestMod.MOD_ID)
public class TestMod {
    public static final String MOD_ID = "testmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TestMod(IEventBus modBus) {
        LOGGER.info("{} loaded", MOD_ID);
        TestModBlocks.register(modBus);
        TestModMenus.register(modBus);
    }
}
