package dev.robotgryphon.screenlib.graph.nodes.properties.computed;

import net.neoforged.neoforge.registries.DeferredHolder;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import dev.robotgryphon.screenlib.graph.nodes.properties.DataKey;
import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.codec.RegistryCodecs;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Optional;

public interface ComputedProperty<T> {
    ResourceKey<Registry<ComputedProperty<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("computed_properties"));

    Codec<Holder<ComputedProperty<?>>> HOLDER_CODEC = RegistryCodecs.holder(REGISTRY_KEY);

    DeferredHolder<GraphDataType<?>, ? extends GraphDataType<T>> type();

    List<DataKey<?>> requires();

    Optional<T> compute(ComputeContext context);

    default Optional<ClientDataProvider<T, ?>> clientDataProvider() {
        return Optional.empty();
    }
}
