package dev.robotgryphon.screenlib.graph.canvas.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CanvasViewports {
    private static final Map<UUID, ViewportState> VIEWPORTS = new HashMap<>();

    private CanvasViewports() {}

    public static Optional<ViewportState> get(UUID canvas) {
        return Optional.ofNullable(VIEWPORTS.get(canvas));
    }

    public static void remember(UUID canvas, ViewportState viewport) {
        VIEWPORTS.put(canvas, viewport);
    }

    public static void clear() {
        VIEWPORTS.clear();
    }
}
