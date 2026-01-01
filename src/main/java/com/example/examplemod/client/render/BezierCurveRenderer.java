package com.example.examplemod.client.render;

import com.example.examplemod.client.render.pip.BezierCurveRenderState;
import com.example.examplemod.graph.NodeConnection;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

import java.util.Collection;

/**
 * Renders bezier curves using the Minecraft GUI rendering pipeline with custom shaders.
 */
public class BezierCurveRenderer {

    /**
     * Renders a cubic bezier curve with a red-to-green gradient using the custom shader pipeline.
     *
     * @param graphics The GUI graphics context
     */
    public static void render(GuiGraphics graphics, NodeConnection connection, ScreenRectangle bounds) {
        graphics.submitPictureInPictureRenderState(BezierCurveRenderState.from(
                new Matrix3x2f(graphics.pose()),
                connection.realControlPoints(),
                connection.relativeControlPoints(bounds),
                connection.color()
        ));
    }

    /**
     * Renders control lines connecting P0-P1 and P2-P3.
     */
    public static void renderControlLines(GuiGraphics graphics, Collection<Vector2f> controlPoints,
                                          int canvasWidth, int canvasHeight) {
        if (controlPoints == null || controlPoints.size() != 4) {
            return;
        }

        int lineColor = 0x80FFFFFF;

        // P0 to P1
//        drawDashedLine(graphics,
//                (int) (controlPoints[0].x * canvasWidth),
//                (int) (controlPoints[0].y * canvasHeight),
//                (int) (controlPoints[1].x * canvasWidth),
//                (int) (controlPoints[1].y * canvasHeight),
//                lineColor);
//
//        // P2 to P3
//        drawDashedLine(graphics,
//                (int) (controlPoints[2].x * canvasWidth),
//                (int) (controlPoints[2].y * canvasHeight),
//                (int) (controlPoints[3].x * canvasWidth),
//                (int) (controlPoints[3].y * canvasHeight),
//                lineColor);
    }

    private static void drawDashedLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        int segments = Math.max(1, (int) (length / 8));

        for (int i = 0; i < segments; i += 2) {
            float t1 = (float) i / segments;
            float t2 = (float) Math.min(i + 1, segments) / segments;

            int sx = (int) (x1 + dx * t1);
            int sy = (int) (y1 + dy * t1);
            int ex = (int) (x1 + dx * t2);
            int ey = (int) (y1 + dy * t2);

            if (Math.abs(ex - sx) > Math.abs(ey - sy)) {
                graphics.fill(Math.min(sx, ex), sy, Math.max(sx, ex) + 1, sy + 1, color);
            } else {
                graphics.fill(sx, Math.min(sy, ey), sx + 1, Math.max(sy, ey) + 1, color);
            }
        }
    }

    /**
     * Linearly interpolates between two ARGB colors.
     */
    private static int lerpColor(int color1, int color2, float t) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
