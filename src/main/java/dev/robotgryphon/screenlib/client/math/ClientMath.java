package dev.robotgryphon.screenlib.client.math;

import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Vector2fc;

public final class ClientMath {

    public static ScreenRectangle calculateBoundsForConnection(Vector2fc start, Vector2fc end, int padding) {
        int minX = (int) Math.min(start.x(), end.x()) - padding;
        int minY = (int) Math.min(start.y(), end.y()) - padding;
        int maxX = (int) Math.max(start.x(), end.x()) + padding;
        int maxY = (int) Math.max(start.y(), end.y()) + padding;
        // The padding gives the fixed-pixel-width line headroom around the
        // curve's bbox, so axis-aligned curves no longer clip down to a sliver
        // (or zero-dimension texture). Padding must match BezierCurveCalculator.
        return new ScreenRectangle(new ScreenPosition(minX, minY), maxX - minX, maxY - minY);
    }

}
