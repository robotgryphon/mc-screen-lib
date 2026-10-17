package dev.robotgryphon.screenlib.graph.schema.properties.computed;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public interface ClientDataProvider<T, D> {
    StreamCodec<RegistryFriendlyByteBuf, D> clientCodec();

    Optional<D> clientData(T value, ComputeContext context);
}
