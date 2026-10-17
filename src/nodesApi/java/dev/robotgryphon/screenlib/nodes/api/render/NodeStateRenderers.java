package dev.robotgryphon.screenlib.nodes.api.render;

import dev.robotgryphon.screenlib.core.ScreenLib;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public final class NodeStateRenderers {
    private NodeStateRenderers() {}

    public static final ResourceKey<Registry<NodeStateRenderer<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("node_renderers"));

    public static final ResourceKey<NodeStateRenderer<?>> BLOCK_PREVIEW = key("block_preview");
    public static final ResourceKey<NodeStateRenderer<?>> ITEM_HANDLER_PREVIEW = key("item_handler_preview");

    private static ResourceKey<NodeStateRenderer<?>> key(String name) {
        return ResourceKey.create(REGISTRY_KEY, ScreenLib.id(name));
    }
}
