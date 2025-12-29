package com.example.examplemod.client.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.joml.Vector2fc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Renders bezier curves using the Minecraft GUI rendering pipeline with custom shaders.
 */
public class BezierCurveRenderer {

    private static final int SAMPLES = 100;
    private static final float THICKNESS = 3.0f;
    private static final int START_COLOR = 0xFFFF0000; // Red
    private static final int END_COLOR = 0xFF00FF00;   // Green

    /**
     * Renders a cubic bezier curve with a red-to-green gradient using the custom shader pipeline.
     *
     * @param graphics      The GUI graphics context
     * @param controlPoints Array of 4 control points in normalized coordinates (0-1)
     * @param canvasWidth   Width of the canvas area in pixels
     * @param canvasHeight  Height of the canvas area in pixels
     */
    public static void render(GuiGraphics graphics, List<Vector2f> controlPoints,
                              int canvasWidth, int canvasHeight) {
        if (controlPoints == null || controlPoints.size() != 4) {
            return;
        }

        float halfThickness = THICKNESS / 2.0f;
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());

        for (int i = 0; i < SAMPLES - 1; i++) {
            float t1 = (float) i / (SAMPLES - 1);
            float t2 = (float) (i + 1) / (SAMPLES - 1);

            Vector2f p1 = evaluateBezier(controlPoints, t1);
            Vector2f p2 = evaluateBezier(controlPoints, t2);

            float x1 = p1.x * canvasWidth;
            float y1 = p1.y * canvasHeight;
            float x2 = p2.x * canvasWidth;
            float y2 = p2.y * canvasHeight;

            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float) Math.sqrt(dx * dx + dy * dy);

            if (len < 0.001f) {
                continue;
            }

            float px = -dy / len * halfThickness;
            float py = dx / len * halfThickness;

            int color1 = lerpColor(START_COLOR, END_COLOR, t1);
            int color2 = lerpColor(START_COLOR, END_COLOR, t2);

            Vector2f[] list = new Vector2f[]{
                    new Vector2f(x1 - px, y1 - py),
                    new Vector2f(x1 + px, y1 + py),
                    new Vector2f(x2 + px, y2 + py),
                    new Vector2f(x2 - px, y2 - py)
            };

            graphics.submitGuiElementRenderState(BezierCurveRenderState.from(
                    pose,
                    list
            ));
        }
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
     * Evaluates a cubic bezier curve at parameter t.
     * B(t) = (1-t)^3 * P0 + 3*(1-t)^2*t * P1 + 3*(1-t)*t^2 * P2 + t^3 * P3
     */
    private static Vector2f evaluateBezier(List<Vector2f> points, float t) {
        float u = 1 - t;
        float tt = t * t;
        float uu = u * u;
        float uuu = uu * u;
        float ttt = tt * t;

        float x = uuu * points.get(0).x
                + 3 * uu * t * points.get(1).x
                + 3 * u * tt * points.get(2).x
                + ttt * points.get(3).x;

        float y = uuu * points.get(0).y
                + 3 * uu * t * points.get(1).y
                + 3 * u * tt * points.get(2).y
                + ttt * points.get(3).y;

        return new Vector2f(x, y);
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
