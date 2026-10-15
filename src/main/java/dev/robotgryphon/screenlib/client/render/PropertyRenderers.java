package dev.robotgryphon.screenlib.client.render;

import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

public final class PropertyRenderers {
    private PropertyRenderers() {}

    public static final ResourceKey<Registry<PropertyValueRenderer<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("property_renderers"));

    public static final DeferredRegister<PropertyValueRenderer<?>> RENDERERS =
            DeferredRegister.create(REGISTRY_KEY, ScreenLib.MOD_ID);

    public static final Registry<PropertyValueRenderer<?>> REGISTRY = RENDERERS.makeRegistry(builder -> {});

    public static final DeferredHolder<PropertyValueRenderer<?>, ItemHandlerBlockRenderer> ITEM_HANDLER_BLOCK =
            RENDERERS.register("item_handler_block", ItemHandlerBlockRenderer::new);

    @SuppressWarnings("unchecked")
    public static @Nullable PropertyValueRenderer<Object> get(ResourceKey<PropertyValueRenderer<?>> type) {
        return (PropertyValueRenderer<Object>) REGISTRY.getValue(type);
    }
}
