package dev.robotgryphon.screenlib.client.ui.render.uniforms;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Vector2fc;
import org.joml.Vector4fc;

import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public record BezierCurveUniform(Vector2fc[] controlPoints, ScreenRectangle area,
                                 float halfWidth, float feather,
                                 Vector4fc indicator) {
    public static final String NAME = "BezierCurve";

    public static final int SIZE = new Std140SizeCalculator()
            .putVec2()
            .putVec2()
            .putVec2()
            .putVec2()
            .putVec2()
            .putVec2()
            .putVec4()
            .get();

    public static class Storage implements AutoCloseable {
        private final GpuBuffer buffer = RenderSystem.getDevice()
                .createBuffer(() -> ScreenLib.MOD_ID + ": Bezier Curve UBO",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, SIZE);

        public GpuBufferSlice write(BezierCurveUniform uniform) {
            try (var stack = MemoryStack.stackPush()) {
                ByteBuffer data = stack.malloc(SIZE);
                uniform.write(data);
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), data);
            }
            return buffer.slice();
        }

        @Override
        public void close() {
            buffer.close();
        }
    }

    public void write(ByteBuffer buffer) {
        Std140Builder.intoBuffer(buffer)
                .putVec2(controlPoints[0])
                .putVec2(controlPoints[1])
                .putVec2(controlPoints[2])
                .putVec2(controlPoints[3])
                .putVec2(area.width(), area.height())
                .putVec2(halfWidth, feather)
                .putVec4(indicator)
                .get();
    }
}
