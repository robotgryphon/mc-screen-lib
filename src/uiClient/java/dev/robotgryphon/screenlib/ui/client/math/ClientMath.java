package dev.robotgryphon.screenlib.ui.client.math;

import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Vector2fc;

public final class ClientMath {
    public static ScreenRectangle calculateBoundsForConnection(Vector2fc start, Vector2fc end, int padding) {
        int minX = (int) Math.min(start.x(), end.x()) - padding;
        int minY = (int) Math.min(start.y(), end.y()) - padding;
        int maxX = (int) Math.max(start.x(), end.x()) + padding;
        int maxY = (int) Math.max(start.y(), end.y()) + padding;
        return new ScreenRectangle(new ScreenPosition(minX, minY), maxX - minX, maxY - minY);
    }
}
