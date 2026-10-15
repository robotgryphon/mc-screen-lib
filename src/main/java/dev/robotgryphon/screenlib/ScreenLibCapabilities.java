package dev.robotgryphon.screenlib;

import dev.robotgryphon.screenlib.graph.canvas.CanvasSupplier;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jspecify.annotations.Nullable;

public final class ScreenLibCapabilities {
    private ScreenLibCapabilities() {}

    public static final BlockCapability<CanvasSupplier, @Nullable Void> CANVAS =
            BlockCapability.createVoid(ScreenLib.id("canvas"), CanvasSupplier.class);

    static void register(RegisterCapabilitiesEvent event) {
        event.setNonProxyable(CANVAS);
    }
}
