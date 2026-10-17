package dev.robotgryphon.screenlib.nodes.client.render;

import dev.robotgryphon.screenlib.nodes.api.render.NodeStateRenderer;
import dev.robotgryphon.screenlib.nodes.api.render.NodeStateRenderers;
import dev.robotgryphon.screenlib.core.ScreenLib;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

public final class NodeWidgets {
    private NodeWidgets() {}

    public static final ResourceKey<Registry<NodeStateRenderer<?>>> REGISTRY_KEY =
            NodeStateRenderers.REGISTRY_KEY;

    public static final DeferredRegister<NodeStateRenderer<?>> RENDERERS =
            DeferredRegister.create(REGISTRY_KEY, ScreenLib.MOD_ID);

    public static final Registry<NodeStateRenderer<?>> REGISTRY = RENDERERS.makeRegistry(builder -> {});

    public static final DeferredHolder<NodeStateRenderer<?>, BlockPreviewRenderer> BLOCK_PREVIEW =
            RENDERERS.register("block_preview", BlockPreviewRenderer::new);

    public static final DeferredHolder<NodeStateRenderer<?>, ItemHandlerPreviewRenderer> ITEM_HANDLER_PREVIEW =
            RENDERERS.register("item_handler_preview", ItemHandlerPreviewRenderer::new);

    public static @Nullable NodeStateRenderer<?> get(ResourceKey<NodeStateRenderer<?>> type) {
        return REGISTRY.getValue(type);
    }
}
