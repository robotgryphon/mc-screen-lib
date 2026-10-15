package dev.robotgryphon.screenlib.client.render;

import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

public final class NodeWidgets {
    private NodeWidgets() {}

    public static final ResourceKey<Registry<NodeStateRenderer<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("node_renderers"));

    public static final DeferredRegister<NodeStateRenderer<?>> RENDERERS =
            DeferredRegister.create(REGISTRY_KEY, ScreenLib.MOD_ID);

    public static final Registry<NodeStateRenderer<?>> REGISTRY = RENDERERS.makeRegistry(builder -> {});

    public static final DeferredHolder<NodeStateRenderer<?>, BlockPreviewRenderer> BLOCK_PREVIEW =
            RENDERERS.register("block_preview", BlockPreviewRenderer::new);

    public static final DeferredHolder<NodeStateRenderer<?>, ItemHandlerPreviewRenderer> ITEM_HANDLER_PREVIEW =
            RENDERERS.register("item_handler_preview", ItemHandlerPreviewRenderer::new);

    @SuppressWarnings("unchecked")
    public static <S> @Nullable NodeStateRenderer<S> get(ResourceKey<NodeStateRenderer<?>> type) {
        return (NodeStateRenderer<S>) REGISTRY.getValue(type);
    }
}
