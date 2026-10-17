package dev.robotgryphon.screenlib.graph.schema.ports;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record OutputPortDefinition(String property) {
    public static final Codec<OutputPortDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("property").forGetter(OutputPortDefinition::property)
    ).apply(i, OutputPortDefinition::new));
}
