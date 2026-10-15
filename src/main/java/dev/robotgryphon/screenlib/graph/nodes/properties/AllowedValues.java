package dev.robotgryphon.screenlib.graph.nodes.properties;

import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;

import java.util.Collection;
import java.util.Optional;

public interface AllowedValues {
    Optional<Collection<Object>> of(Holder<GraphDataType<?>> type);

    static AllowedValues live(RegistryAccess registries) {
        return type -> unchecked(type.value().allowedValues(registries));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Optional<Collection<Object>> unchecked(Optional<? extends Collection<?>> values) {
        return (Optional) values;
    }
}
