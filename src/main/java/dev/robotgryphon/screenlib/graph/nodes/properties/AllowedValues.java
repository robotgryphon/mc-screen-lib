package dev.robotgryphon.screenlib.graph.nodes.properties;

import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public interface AllowedValues {
    Optional<Collection<Object>> of(Holder<GraphDataType<?>> type);

    static AllowedValues live(RegistryAccess registries) {
        return type -> type.value().allowedValues(registries).map(Collections::unmodifiableCollection);
    }
}
