package dev.robotgryphon.screenlib.graph.nodes.ports;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record InputPortDefinition(String property) {
    public static final Codec<InputPortDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("property").forGetter(InputPortDefinition::property)
    ).apply(i, InputPortDefinition::new));
}
