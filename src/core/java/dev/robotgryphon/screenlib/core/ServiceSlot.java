package dev.robotgryphon.screenlib.core;

import org.jspecify.annotations.Nullable;

public final class ServiceSlot<T> {
    private final String name;
    private @Nullable T service;

    public ServiceSlot(String name) {
        this.name = name;
    }

    public void install(T service) {
        if (this.service != null) {
            throw new IllegalStateException(this.name + " is already installed");
        }
        this.service = service;
    }

    public T get() {
        if (this.service == null) {
            throw new IllegalStateException(this.name + " has not been installed yet");
        }
        return this.service;
    }
}
