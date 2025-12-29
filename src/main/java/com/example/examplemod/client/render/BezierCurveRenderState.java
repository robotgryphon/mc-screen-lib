package com.example.examplemod.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.util.CommonColors;
import org.joml.Matrix3x2fc;
import org.joml.Vector2fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record BezierCurveRenderState(Matrix3x2fc pose,
                                     Vector2fc[] points,
                                     ScreenRectangle bounds) implements GuiElementRenderState {

    public static BezierCurveRenderState from(Matrix3x2fc pose, Vector2fc[] points) {
        var bounds = getBounds(points, pose);
        return new BezierCurveRenderState(pose, points, bounds);
    }

    @Override
    public void buildVertices(@NonNull VertexConsumer consumer) {
        for (var point : points)
            consumer.addVertexWith2DPose(pose, point.x(), point.y())
                    .setColor(CommonColors.RED)
                    .setLineWidth(1);
    }

    @Override
    public @NonNull RenderPipeline pipeline() {
        return ExRenderPipelines.BEZIER_CURVED_LINES;
    }

    @Override
    public @NonNull TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return null;
    }

    private static @NonNull ScreenRectangle getBounds(
            Vector2fc[] points,
            Matrix3x2fc pose
    ) {
        int minX = (int) Math.floor(Math.min(Math.min(points[0].x(), points[1].x()), Math.min(points[2].x(), points[3].x())));
        int minY = (int) Math.floor(Math.min(Math.min(points[0].y(), points[1].y()), Math.min(points[2].y(), points[3].y())));
        int maxX = (int) Math.ceil(Math.max(Math.max(points[0].x(), points[1].x()), Math.max(points[2].x(), points[3].x())));
        int maxY = (int) Math.ceil(Math.max(Math.max(points[0].y(), points[1].y()), Math.max(points[2].y(), points[3].y())));

        return new ScreenRectangle(minX, minY, maxX - minX, maxY - minY).transformMaxBounds(pose);
    }
}
