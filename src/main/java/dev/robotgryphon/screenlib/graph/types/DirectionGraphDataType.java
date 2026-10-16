package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;

import java.util.Collection;
import java.util.Set;

public class DirectionGraphDataType implements RestrictedDataType<Direction> {
    private static final Set<Direction> ALLOWED = Set.of(Direction.values());

    @Override
    public Class<Direction> dataType() {
        return Direction.class;
    }

    @Override
    public Codec<Direction> codec() {
        return Direction.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, Direction> streamCodec() {
        return Direction.STREAM_CODEC;
    }

    @Override
    public Collection<Direction> allowedValues(RegistryAccess registries) {
        return ALLOWED;
    }
}
