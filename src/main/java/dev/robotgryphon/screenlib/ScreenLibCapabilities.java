package dev.robotgryphon.screenlib;

import dev.robotgryphon.screenlib.menu.CanvasSource;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jspecify.annotations.Nullable;

public final class ScreenLibCapabilities {
    private ScreenLibCapabilities() {}

    public static final BlockCapability<CanvasSource, @Nullable Void> CANVAS_SOURCE =
            BlockCapability.createVoid(ScreenLib.id("canvas_source"), CanvasSource.class);

    static void register(RegisterCapabilitiesEvent event) {
        event.setNonProxyable(CANVAS_SOURCE);
    }
}
