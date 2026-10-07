package dev.robotgryphon.testmod;

import com.mojang.logging.LogUtils;
import dev.robotgryphon.screenlib.ScreenLib;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TestMod.MOD_ID)
public class TestMod {
    public static final String MOD_ID = "testmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TestMod() {
        LOGGER.info("{} loaded against {}", MOD_ID, ScreenLib.MOD_ID);
    }
}
