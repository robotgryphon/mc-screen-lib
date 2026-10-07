package dev.robotgryphon.screenlib.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record ConnectionState(int sourceNodeIndex,
                              int sourcePortIndex,
                              Optional<String> sourcePortName,
                              int targetNodeIndex,
                              int targetPortIndex,
                              Optional<String> targetPortName) {
    public static final Codec<ConnectionState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("source_node").forGetter(ConnectionState::sourceNodeIndex),
            Codec.INT.fieldOf("source_port").forGetter(ConnectionState::sourcePortIndex),
            Codec.STRING.optionalFieldOf("source_port_name").forGetter(ConnectionState::sourcePortName),
            Codec.INT.fieldOf("target_node").forGetter(ConnectionState::targetNodeIndex),
            Codec.INT.fieldOf("target_port").forGetter(ConnectionState::targetPortIndex),
            Codec.STRING.optionalFieldOf("target_port_name").forGetter(ConnectionState::targetPortName)
    ).apply(i, ConnectionState::new));
}
