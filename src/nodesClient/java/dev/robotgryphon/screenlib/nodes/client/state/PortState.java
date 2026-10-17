package dev.robotgryphon.screenlib.nodes.client.state;

import dev.robotgryphon.screenlib.nodes.client.PortLayout;
import dev.robotgryphon.screenlib.nodes.common.ports.Port;

public final class PortState {
    private final PortLayout.Slot slot;
    private float hoverProgress;

    public PortState(PortLayout.Slot slot) {
        this.slot = slot;
    }

    public Port port() {
        return this.slot.port();
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
