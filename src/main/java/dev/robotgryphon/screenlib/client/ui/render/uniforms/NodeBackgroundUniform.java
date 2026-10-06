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

/// Std140 uniform layout for the batched node-background shader.
///
/// One uniform write covers every node in a single PiP pass — the
/// shader iterates [#entries] per fragment to figure out which
/// node (if any) it belongs to. Per-node data is packed across six
/// parallel arrays (bounds + body / title / border colors + extras +
/// glow color) so the GLSL side can pull all attributes via one index.
public record NodeBackgroundUniform(int textureWidth, int textureHeight,
                                    float cornerRadius, float feather, float borderThickness,
                                    List<Entry> entries)  {

    public static class Storage implements AutoCloseable {


        private final GpuBuffer buffer = RenderSystem.getDevice()
                .createBuffer(() -> ScreenLib.MOD_ID + ": Node Background UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, SIZE);

        public static final String NAME = "NodeBatch";

        /**
         * Hard cap on nodes per batch — must match {@code MAX_NODES} in the shader.
         */
        public static final int MAX_NODES = 64;

        /**
         * Six parallel arrays of vec4, plus two header vec4s ({@code size}, {@code params}).
         */
        private static final int VEC4_COUNT = 2 + 6 * MAX_NODES;

        public static final int SIZE = buildSizeCalculator().get();

        private static Std140SizeCalculator buildSizeCalculator() {
            Std140SizeCalculator calc = new Std140SizeCalculator()
                    .putVec4()  // Header
                    .putVec4(); // Header

            for (int i = 0; i < MAX_NODES; i++) {
                // 6 sides per
                for (int s = 0; s < 6; s++)
                    calc = calc.putVec4();
            }
            return calc;
        }

        public GpuBufferSlice write(NodeBackgroundUniform uniform) {
            try (var stack = MemoryStack.stackPush()) {
                Std140Builder b = Std140Builder.onStack(stack, SIZE);

                // size — only x/y populated; z/w padding.
                b.putVec4(uniform.textureWidth, uniform.textureHeight, 0f, 0f);
                // params — cornerRadius, feather, borderThickness, nodeCount.
                b.putVec4(uniform.cornerRadius, uniform.feather, uniform.borderThickness, uniform.entries.size());

                int n = Math.min(uniform.entries.size(), MAX_NODES);

                // bounds[]
                for (int i = 0; i < MAX_NODES; i++) {
                    if (i < n) {
                        b.putVec4(uniform.entries.get(i).bounds());
                    } else {
                        b.putVec4(0f, 0f, 0f, 0f);
                    }
                }
                // bodyColors[]
                for (int i = 0; i < MAX_NODES; i++) {
                    if (i < n) {
                        b.putVec4(uniform.entries.get(i).bodyColor());
                    } else {
                        b.putVec4(0f, 0f, 0f, 0f);
                    }
                }
                // titleColors[]
                for (int i = 0; i < MAX_NODES; i++) {
                    if (i < n) {
                        b.putVec4(uniform.entries.get(i).titleColor());
                    } else {
                        b.putVec4(0f, 0f, 0f, 0f);
                    }
                }
                // borderColors[]
                for (int i = 0; i < MAX_NODES; i++) {
                    if (i < n) {
                        b.putVec4(uniform.entries.get(i).borderColor());
                    } else {
                        b.putVec4(0f, 0f, 0f, 0f);
                    }
                }
                // extras[]:
                //   x = title-bar height in scaled pixels
                //   y = drop-shadow flag (0 = off, 1 = on)
                //   z = per-entry corner radius override; 0 = use params.x
                //   w = per-entry border thickness override; 0 = use params.z
                for (int i = 0; i < MAX_NODES; i++) {
                    if (i < n) {
                        Entry e = uniform.entries.get(i);
                        b.putVec4(e.titleHeight(), e.dropShadow() ? 1f : 0f,
                                e.cornerRadiusOverride(), e.borderThicknessOverride());
                    } else {
                        b.putVec4(0f, 0f, 0f, 0f);
                    }
                }
                // glowColors[]: rgba 0..1. alpha <= 0 → no glow (the shader's
                // bail-out condition). Carried in its own parallel array so the
                // existing four color arrays stay untouched and call sites that
                // don't want a glow don't pay any per-entry packing cost.
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
        public void close() throws Exception {
            buffer.close();
        }

    }

    /**
     * Per-node entry. {@code bounds} is in scaled (window) pixel space
     * relative to the PiP texture's top-left corner — the same space
     * {@code gl_FragCoord} reports for the fragment shader. Colors are
     * straight {@code rgba} 0..1.
     *
     * <p>{@code dropShadow} toggles the soft offset shadow under the
     * node. Used by the canvas widget to make a dragged node visually
     * lift off the static layer; static nodes pass {@code false}.
     *
     * <p>{@code cornerRadiusOverride} / {@code borderThicknessOverride}
     * — when positive, replace the batch-shared values from
     * {@link NodeBackgroundUniform#cornerRadius} /
     * {@link NodeBackgroundUniform#borderThickness} for this one
     * entry. {@code 0} (the default from the 6-arg convenience
     * constructor) means "use the shared params," which is what node
     * backgrounds and editor pills want. The port batch sets per-entry
     * values so each hovered port can animate its size and halo
     * thickness independently of the rest of the batch.
     *
     * <p>{@code glowColor} carries an optional soft outer glow drawn
     * around the entry, copied from the drop-shadow approach but with
     * no offset and a shorter blur — used for visual states like
     * "this node failed validation" where the consumer wants to call
     * attention to a node without altering its body. An alpha of zero
     * (the default from the convenience constructors) disables the
     * glow entirely so existing call sites pay no cost.
     */
    public record Entry(Vector4fc bounds,
                        Vector4fc bodyColor,
                        Vector4fc titleColor,
                        Vector4fc borderColor,
                        float titleHeight,
                        boolean dropShadow,
                        float cornerRadiusOverride,
                        float borderThicknessOverride,
                        Vector4fc glowColor) {

        /**
         * Transparent — the shader treats {@code glowColor.a <= 0} as "no glow."
         */
        private static final Vector4fc NO_GLOW = new org.joml.Vector4f(0f, 0f, 0f, 0f);

        /**
         * Six-arg convenience — keeps existing callers source-compatible.
         * Defaults the per-entry overrides to {@code 0} (use shared
         * params) and the glow to transparent (off).
         */
        public Entry(Vector4fc bounds,
                     Vector4fc bodyColor,
                     Vector4fc titleColor,
                     Vector4fc borderColor,
                     float titleHeight,
                     boolean dropShadow) {
            this(bounds, bodyColor, titleColor, borderColor, titleHeight, dropShadow, 0f, 0f, NO_GLOW);
        }

        /**
         * Eight-arg convenience — keeps the port-batch call sites that
         * already pass corner / border overrides source-compatible.
         * Defaults the glow to transparent.
         */
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
