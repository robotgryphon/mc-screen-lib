package dev.robotgryphon.screenlib.client.ui.state;

import dev.robotgryphon.screenlib.client.PortLayout;
import dev.robotgryphon.screenlib.graph.nodes.ports.Port;

public final class PortState {
    private final Port port;
    private final PortLayout.Slot slot;
    private float hoverProgress;

    public PortState(Port port, PortLayout.Slot slot) {
        this.port = port;
        this.slot = slot;
    }

    public Port port() {
        return this.port;
    }

    public PortLayout.Slot slot() {
        return this.slot;
    }

    public float hoverProgress() {
        return this.hoverProgress;
    }

    public void advanceHover(boolean hovered, float lerpAmount) {
        float target = hovered ? 1f : 0f;
        float next = this.hoverProgress + (target - this.hoverProgress) * lerpAmount;
        if (Math.abs(next - target) < 0.005f) {
            next = target;
        }
        this.hoverProgress = next;
    }
}
