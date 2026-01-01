package com.example.examplemod.client.render.uniforms;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.DynamicUniformStorage;
import org.joml.Vector2fc;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public record BezierCurveUniform(Vector2fc[] controlPoints, ScreenRectangle area) implements RenderPipelineUniforms {
    public static final String NAME = "BezierCurve";

    public static final Supplier<DynamicUniformStorage<BezierCurveUniform>> STORAGE = RenderPipelineUniformsStorage.register(
            NAME + " UBO",
            2,
            new Std140SizeCalculator().putVec2().putVec2().putVec2().putVec2().putVec2()
    );


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void write(ByteBuffer buffer) {
        Std140Builder.intoBuffer(buffer)
                .putVec2(controlPoints[0])
                .putVec2(controlPoints[1])
                .putVec2(controlPoints[2])
                .putVec2(controlPoints[3])
                .putVec2(area.width(), area.height())
                .get();
    }
}
