package dev.robotgryphon.screenlib.types;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public abstract class Property<T> {
    private final Class<T> propertyType;
    private final Component label;
    private final Optional<T> defaultValue;

    public Property(Component label, Class<T> propertyType) {
        this.propertyType = propertyType;
        this.label = label;
        this.defaultValue = Optional.empty();
    }

    public Component label() {
        return label;
    }

    public Optional<T> defaultValue() {
        return defaultValue;
    }

    public abstract Holder<PropertyType<T>> type();
}
