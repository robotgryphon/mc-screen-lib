package dev.robotgryphon.screenlib.ui.api.toolbar;

import dev.robotgryphon.screenlib.core.ScreenLib;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

public final class ToolbarButtons {
    private ToolbarButtons() {}

    public static final ResourceKey<Registry<ToolbarButton>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("toolbar_buttons"));

    public static final DeferredRegister<ToolbarButton> BUTTONS = DeferredRegister.create(REGISTRY_KEY, ScreenLib.MOD_ID);

    public static final Registry<ToolbarButton> REGISTRY = BUTTONS.makeRegistry(builder -> {});

    public static @Nullable ToolbarButton get(ResourceKey<ToolbarButton> key) {
        return REGISTRY.getValue(key);
    }
}
