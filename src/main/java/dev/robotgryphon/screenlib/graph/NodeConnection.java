package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.client.math.ClientMath;
import dev.robotgryphon.screenlib.geometry.BezierCurve;
import dev.robotgryphon.screenlib.math.BezierCurveCalculator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Vector2f;
import org.joml.Vector2fc;

import static dev.robotgryphon.screenlib.math.BezierCurveCalculator.LINE_PADDING_PX;

public record NodeConnection(Vector2fc start, PortSide startSide, Vector2fc end, PortSide endSide, int color) {

    public static NodeConnection rightToLeft(Vector2fc start, Vector2fc end, int color) {
        return new NodeConnection(start, PortSide.RIGHT, end, PortSide.LEFT, color);
    }

    public BezierCurve asCurve(GuiGraphicsExtractor graphics) {
        return BezierCurve.from(graphics, start, end, this.color());
    }

    public ScreenRectangle bounds() {
        return ClientMath.calculateBoundsForConnection(start, end, LINE_PADDING_PX);
    }

}
