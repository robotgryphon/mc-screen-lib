package com.example.examplemod.client.render;

import com.example.examplemod.client.render.pip.BezierCurveRenderState;
import com.example.examplemod.graph.NodeConnection;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;

/**
 * Renders Bézier curves using the Minecraft GUI rendering pipeline with custom shaders.
 */
public class BezierCurveRenderer {

    /**
     * Renders a cubic Bézier curve with a red-to-green gradient using the custom shader pipeline.
     *
     * @param graphics The GUI graphics context
     */
    public static void render(GuiGraphicsExtractor graphics, NodeConnection connection, ScreenRectangle bounds) {
        graphics.submitPictureInPictureRenderState(BezierCurveRenderState.from(
                new Matrix3x2f(graphics.pose()),
                connection.realControlPoints(),
                connection.relativeControlPoints(bounds),
                connection.color()
        ));
    }

}
