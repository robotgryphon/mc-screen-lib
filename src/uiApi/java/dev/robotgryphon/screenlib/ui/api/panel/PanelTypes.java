package dev.robotgryphon.screenlib.ui.api.panel;

import dev.robotgryphon.screenlib.core.ScreenLib;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

public final class PanelTypes {
    private PanelTypes() {}

    public static final ResourceKey<Registry<PanelType<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("panels"));

    public static final DeferredRegister<PanelType<?>> PANELS = DeferredRegister.create(REGISTRY_KEY, ScreenLib.MOD_ID);

    public static final Registry<PanelType<?>> REGISTRY = PANELS.makeRegistry(builder -> {});

    public static @Nullable PanelType<?> get(ResourceKey<PanelType<?>> key) {
        return REGISTRY.getValue(key);
    }
}
