package dev.robotgryphon.screenlib.client.appearance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.client.render.NodeStateRenderer;
import dev.robotgryphon.screenlib.client.render.NodeWidgets;
import net.minecraft.resources.ResourceKey;

public record WidgetAppearance(ResourceKey<NodeStateRenderer<?>> type, String property) {
    public static final Codec<WidgetAppearance> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(NodeWidgets.REGISTRY_KEY).fieldOf("type").forGetter(WidgetAppearance::type),
            Codec.STRING.fieldOf("property").forGetter(WidgetAppearance::property)
    ).apply(i, WidgetAppearance::new));
}
