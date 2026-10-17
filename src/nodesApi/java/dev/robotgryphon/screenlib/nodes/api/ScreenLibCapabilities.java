package dev.robotgryphon.screenlib.nodes.api;

import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasSupplier;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jspecify.annotations.Nullable;

public final class ScreenLibCapabilities {
    private ScreenLibCapabilities() {}

    public static final BlockCapability<CanvasSupplier, @Nullable Void> CANVAS =
            BlockCapability.createVoid(ScreenLib.id("canvas"), CanvasSupplier.class);
}
