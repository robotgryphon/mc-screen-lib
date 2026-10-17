package dev.robotgryphon.screenlib.core;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.List;

public final class ScreenLibCodecs {
    private ScreenLibCodecs() {}

    public static final Codec<Vector2ic> VECTOR2I = Codec.INT
            .listOf()
            .comapFlatMap(input -> Util.fixedSize(input, 2).map(d -> new Vector2i(d.get(0), d.get(1))),
                    vec -> List.of(vec.x(), vec.y()));

    public static final StreamCodec<ByteBuf, Vector2ic> VECTOR2I_STREAM = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Vector2ic::x,
            ByteBufCodecs.VAR_INT, Vector2ic::y,
            (x, y) -> new Vector2i(x, y)
    );
}
