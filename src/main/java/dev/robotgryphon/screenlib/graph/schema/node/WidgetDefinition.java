package dev.robotgryphon.screenlib.graph.schema.node;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.client.render.NodeStateRenderer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public record WidgetDefinition(ResourceKey<NodeStateRenderer<?>> type, String property) {
    public static final ResourceKey<Registry<NodeStateRenderer<?>>> RENDERER_REGISTRY =
            ResourceKey.createRegistryKey(ScreenLib.id("node_renderers"));

    public static final Codec<WidgetDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(RENDERER_REGISTRY).fieldOf("type").forGetter(WidgetDefinition::type),
            Codec.STRING.fieldOf("property").forGetter(WidgetDefinition::property)
    ).apply(i, WidgetDefinition::new));
}
