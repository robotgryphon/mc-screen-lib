package dev.robotgryphon.screenlib.nodes.api.render;

import dev.robotgryphon.screenlib.core.ScreenLib;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public final class PropertyValueRenderers {
    private PropertyValueRenderers() {}

    public static final ResourceKey<Registry<PropertyValueRenderer<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("property_renderers"));

    public static final ResourceKey<PropertyValueRenderer<?>> ITEM_HANDLER_BLOCK = key("item_handler_block");

    private static ResourceKey<PropertyValueRenderer<?>> key(String name) {
        return ResourceKey.create(REGISTRY_KEY, ScreenLib.id(name));
    }
}
