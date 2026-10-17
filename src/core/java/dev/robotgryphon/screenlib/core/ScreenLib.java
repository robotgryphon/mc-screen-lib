package dev.robotgryphon.screenlib.core;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public final class ScreenLib {
    public static final String MOD_ID = "screenlib";
    public static final Logger LOGGER = LogUtils.getLogger();

    private ScreenLib() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
