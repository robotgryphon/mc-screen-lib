package dev.robotgryphon.screenlib.nodes.common.properties;

import dev.robotgryphon.screenlib.graph.schema.types.RestrictedDataType;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public interface AllowedValues {
    Optional<Collection<Object>> of(Holder<GraphDataType<?>> type);

    static AllowedValues live(RegistryAccess registries) {
        return type -> type.value() instanceof RestrictedDataType<?> restricted
                ? Optional.of(Collections.unmodifiableCollection(restricted.allowedValues(registries)))
                : Optional.empty();
    }
}
