package dev.robotgryphon.screenlib.client.render;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.schema.node.WidgetDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

public final class NodeWidgets {
    private NodeWidgets() {}

    public static final DeferredRegister<NodeStateRenderer<?>> RENDERERS =
            DeferredRegister.create(WidgetDefinition.RENDERER_REGISTRY, ScreenLib.MOD_ID);

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
