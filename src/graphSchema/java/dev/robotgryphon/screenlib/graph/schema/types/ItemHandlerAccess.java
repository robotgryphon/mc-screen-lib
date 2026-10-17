package dev.robotgryphon.screenlib.graph.schema.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public record ItemHandlerAccess(ResourceKey<Level> level, BlockPos pos, @Nullable Direction side) {
    public static final Codec<ItemHandlerAccess> CODEC = RecordCodecBuilder.create(i -> i.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("level").forGetter(ItemHandlerAccess::level),
            BlockPos.CODEC.fieldOf("pos").forGetter(ItemHandlerAccess::pos),
            Direction.CODEC.optionalFieldOf("side").forGetter(access -> Optional.ofNullable(access.side()))
    ).apply(i, (level, pos, side) -> new ItemHandlerAccess(level, pos, side.orElse(null))));

    public static final StreamCodec<ByteBuf, ItemHandlerAccess> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), ItemHandlerAccess::level,
            BlockPos.STREAM_CODEC, ItemHandlerAccess::pos,
            ByteBufCodecs.optional(Direction.STREAM_CODEC), access -> Optional.ofNullable(access.side()),
            (level, pos, side) -> new ItemHandlerAccess(level, pos, side.orElse(null))
    );
}
