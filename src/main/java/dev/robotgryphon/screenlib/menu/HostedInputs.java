package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.graph.computed.DataKey;

import java.util.Map;
import java.util.Optional;

public record HostedInputs(Map<String, Object> values) {
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(DataKey<T> key) {
        return Optional.ofNullable(this.values.get(key.name()))
                .filter(value -> key.type().get().dataType().isInstance(value))
                .map(value -> (T) value);
    }
}
