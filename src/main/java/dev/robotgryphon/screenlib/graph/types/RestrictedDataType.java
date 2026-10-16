package dev.robotgryphon.screenlib.graph.types;

import net.minecraft.core.RegistryAccess;

import java.util.Collection;

public interface RestrictedDataType<T> extends GraphDataType<T> {
    Collection<T> allowedValues(RegistryAccess registries);
}
