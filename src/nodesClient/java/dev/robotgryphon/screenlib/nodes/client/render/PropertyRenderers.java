package dev.robotgryphon.screenlib.nodes.client.render;

import dev.robotgryphon.screenlib.nodes.api.render.PropertyValueRenderer;
import dev.robotgryphon.screenlib.nodes.api.render.PropertyValueRenderers;
import dev.robotgryphon.screenlib.core.ScreenLib;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

public final class PropertyRenderers {
    private PropertyRenderers() {}

    public static final ResourceKey<Registry<PropertyValueRenderer<?>>> REGISTRY_KEY =
            PropertyValueRenderers.REGISTRY_KEY;

    public static final DeferredRegister<PropertyValueRenderer<?>> RENDERERS =
            DeferredRegister.create(REGISTRY_KEY, ScreenLib.MOD_ID);

    public static final Registry<PropertyValueRenderer<?>> REGISTRY = RENDERERS.makeRegistry(builder -> {});

    public static final DeferredHolder<PropertyValueRenderer<?>, ItemHandlerBlockRenderer> ITEM_HANDLER_BLOCK =
            RENDERERS.register("item_handler_block", ItemHandlerBlockRenderer::new);

    public static @Nullable PropertyValueRenderer<?> get(ResourceKey<PropertyValueRenderer<?>> type) {
        return REGISTRY.getValue(type);
    }
}
