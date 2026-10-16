package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Optional;

public class LevelGraphDataType implements GraphDataType<ResourceKey<Level>> {
    @Override
    public Class<ResourceKey<Level>> dataType() {
        //noinspection unchecked
        return (Class<ResourceKey<Level>>) (Object) ResourceKey.class;
    }

    @Override
    public Codec<ResourceKey<Level>> codec() {
        return Level.RESOURCE_KEY_CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ResourceKey<Level>> streamCodec() {
        return ResourceKey.streamCodec(Registries.DIMENSION);
    }

    @Override
    public Optional<Collection<ResourceKey<Level>>> allowedValues(RegistryAccess registries) {
        var reg = registries.lookupOrThrow(Registries.DIMENSION);
        return Optional.of(reg.listElementIds().toList());
    }
}
