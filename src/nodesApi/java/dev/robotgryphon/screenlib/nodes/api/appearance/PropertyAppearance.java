package dev.robotgryphon.screenlib.nodes.api.appearance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.nodes.api.render.PropertyValueRenderers;
import dev.robotgryphon.screenlib.nodes.api.render.PropertyValueRenderer;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;

public record PropertyAppearance(boolean hideLabel, Optional<ResourceKey<PropertyValueRenderer<?>>> renderer) {
    public static final PropertyAppearance DEFAULT = new PropertyAppearance(false, Optional.empty());

    public static final Codec<PropertyAppearance> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("hide_label", false).forGetter(PropertyAppearance::hideLabel),
            ResourceKey.codec(PropertyValueRenderers.REGISTRY_KEY).optionalFieldOf("renderer").forGetter(PropertyAppearance::renderer)
    ).apply(i, PropertyAppearance::new));

    public PropertyAppearance withHiddenLabel() {
        return new PropertyAppearance(true, this.renderer);
    }

    public PropertyAppearance withRenderer(ResourceKey<PropertyValueRenderer<?>> renderer) {
        return new PropertyAppearance(this.hideLabel, Optional.of(renderer));
    }
}
