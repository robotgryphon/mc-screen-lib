package dev.robotgryphon.screenlib.client.ui.render.uniforms;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.robotgryphon.screenlib.ScreenLib;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;

import java.util.List;

public record NodeBackgroundUniform(int textureWidth, int textureHeight,
                                    float cornerRadius, float feather, float borderThickness,
                                    List<Entry> entries)  {
    public static class Storage implements AutoCloseable {
        private final GpuBuffer buffer = RenderSystem.getDevice()
                .createBuffer(() -> ScreenLib.MOD_ID + ": Node Background UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, SIZE);

        public static final String NAME = "NodeBatch";

        public static final int MAX_NODES = 64;

        private static final int VEC4_COUNT = 2 + 6 * MAX_NODES;

        public static final int SIZE = buildSizeCalculator().get();

        private static Std140SizeCalculator buildSizeCalculator() {
            Std140SizeCalculator calc = new Std140SizeCalculator()
                    .putVec4()
                    .putVec4();

            for (int i = 0; i < MAX_NODES; i++) {
                for (int s = 0; s < 6; s++)
                    calc = calc.putVec4();
            }
            return calc;
        }

        public GpuBufferSlice write(NodeBackgroundUniform uniform) {
            try (var stack = MemoryStack.stackPush()) {
                Std140Builder b = Std140Builder.onStack(stack, SIZE);

                b.putVec4(uniform.textureWidth, uniform.textureHeight, 0f, 0f);
                b.putVec4(uniform.cornerRadius, uniform.feather, uniform.borderThickness, uniform.entries.size());

                int n = Math.min(uniform.entries.size(), MAX_NODES);

                for (int i = 0; i < MAX_NODES; i++) {
                    if (i < n) {
                        b.putVec4(uniform.entries.get(i).bounds());
                    } else {
                        b.putVec4(0f, 0f, 0f, 0f);
                    }
                }
                for (int i = 0; i < MAX_NODES; i++) {
                    if (i < n) {
                        b.putVec4(uniform.entries.get(i).bodyColor());
                    } else {
                        b.putVec4(0f, 0f, 0f, 0f);
                    }
                }
                for (int i = 0; i < MAX_NODES; i++) {
                    if (i < n) {
                        b.putVec4(uniform.entries.get(i).titleColor());
                    } else {
                        b.putVec4(0f, 0f, 0f, 0f);
                    }
                }
                for (int i = 0; i < MAX_NODES; i++) {
                    if (i < n) {
                        b.putVec4(uniform.entries.get(i).borderColor());
                    } else {
                        b.putVec4(0f, 0f, 0f, 0f);
                    }
                }
                for (int i = 0; i < MAX_NODES; i++) {
                    if (i < n) {
                        Entry e = uniform.entries.get(i);
                        b.putVec4(e.titleHeight(), e.dropShadow() ? 1f : 0f,
                                e.cornerRadiusOverride(), e.borderThicknessOverride());
                    } else {
                        b.putVec4(0f, 0f, 0f, 0f);
                    }
                }
                for (int i = 0; i < MAX_NODES; i++) {
                    if (i < n) {
                        b.putVec4(uniform.entries.get(i).glowColor());
                    } else {
                        b.putVec4(0f, 0f, 0f, 0f);
                    }
                }

                RenderSystem.getDevice()
                        .createCommandEncoder()
                        .writeToBuffer(buffer.slice(), b.get());
            }

            return buffer.slice();
        }

        @Override
        public void close() {
            buffer.close();
        }
    }

    public record Entry(Vector4fc bounds,
                        Vector4fc bodyColor,
                        Vector4fc titleColor,
                        Vector4fc borderColor,
                        float titleHeight,
                        boolean dropShadow,
                        float cornerRadiusOverride,
                        float borderThicknessOverride,
                        Vector4fc glowColor) {
        private static final Vector4fc NO_GLOW = new org.joml.Vector4f(0f, 0f, 0f, 0f);

        public Entry(Vector4fc bounds,
                     Vector4fc bodyColor,
                     Vector4fc titleColor,
                     Vector4fc borderColor,
                     float titleHeight,
                     boolean dropShadow) {
            this(bounds, bodyColor, titleColor, borderColor, titleHeight, dropShadow, 0f, 0f, NO_GLOW);
        }

        public Entry(Vector4fc bounds,
                     Vector4fc bodyColor,
                     Vector4fc titleColor,
                     Vector4fc borderColor,
                     float titleHeight,
                     boolean dropShadow,
                     float cornerRadiusOverride,
                     float borderThicknessOverride) {
            this(bounds, bodyColor, titleColor, borderColor, titleHeight, dropShadow,
                    cornerRadiusOverride, borderThicknessOverride, NO_GLOW);
        }
    }
}
