package dev.robotgryphon.screenlib.client.ui.render.uniforms;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.client.renderer.DynamicUniformStorage;
import org.joml.Vector4fc;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Supplier;

/**
 * Std140 uniform layout for the batched node-background shader.
 *
 * <p>One uniform write covers every node in a single PiP pass — the
 * shader iterates {@link #entries} per fragment to figure out which
 * node (if any) it belongs to. Per-node data is packed across five
 * parallel arrays (bounds + body / title / border colors + title-bar
 * extras) so the GLSL side can pull all attributes via one index.
 *
 * <p>Array storage is always {@link #MAX_NODES} slots wide regardless
 * of how many nodes are in the batch — std140 arrays have a fixed
 * stride, so partial arrays would land at the wrong offsets in the
 * UBO. Unused slots are zero-filled; the shader bails on
 * {@code i >= nodeCount} before touching them.
 *
 * <p>If the canvas ever needs more than {@link #MAX_NODES} simultaneous
 * nodes, the caller is responsible for splitting them into multiple
 * batches and submitting one PiP per batch. {@code 64} is comfortably
 * above what a typical edit session has on screen.
 */
public record NodeBackgroundUniform(int textureWidth, int textureHeight,
                                    float cornerRadius, float feather, float borderThickness,
                                    List<Entry> entries) implements RenderPipelineUniforms {

    /**
     * Per-node entry. {@code bounds} is in scaled (window) pixel space
     * relative to the PiP texture's top-left corner — the same space
     * {@code gl_FragCoord} reports for the fragment shader. Colors are
     * straight {@code rgba} 0..1.
     *
     * <p>{@code dropShadow} toggles the soft offset shadow under the
     * node. Used by the canvas widget to make a dragged node visually
     * lift off the static layer; static nodes pass {@code false}.
     */
    public record Entry(Vector4fc bounds,
                        Vector4fc bodyColor,
                        Vector4fc titleColor,
                        Vector4fc borderColor,
                        float titleHeight,
                        boolean dropShadow) {}

    public static final String NAME = "NodeBatch";

    /** Hard cap on nodes per batch — must match {@code MAX_NODES} in the shader. */
    public static final int MAX_NODES = 64;

    /** Five parallel arrays of vec4, plus two header vec4s ({@code size}, {@code params}). */
    private static final int VEC4_COUNT = 2 + 5 * MAX_NODES;

    public static final Supplier<DynamicUniformStorage<NodeBackgroundUniform>> STORAGE =
            RenderPipelineUniformsStorage.register(NAME + " UBO", 2, buildSizeCalculator());

    private static Std140SizeCalculator buildSizeCalculator() {
        Std140SizeCalculator calc = new Std140SizeCalculator();
        for (int i = 0; i < VEC4_COUNT; i++) {
            calc = calc.putVec4();
        }
        return calc;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void write(ByteBuffer buffer) {
        Std140Builder b = Std140Builder.intoBuffer(buffer);

        // size — only x/y populated; z/w padding.
        b.putVec4(textureWidth, textureHeight, 0f, 0f);
        // params — cornerRadius, feather, borderThickness, nodeCount.
        b.putVec4(cornerRadius, feather, borderThickness, entries.size());

        int n = Math.min(entries.size(), MAX_NODES);

        // bounds[]
        for (int i = 0; i < MAX_NODES; i++) {
            if (i < n) {
                b.putVec4(entries.get(i).bounds());
            } else {
                b.putVec4(0f, 0f, 0f, 0f);
            }
        }
        // bodyColors[]
        for (int i = 0; i < MAX_NODES; i++) {
            if (i < n) {
                b.putVec4(entries.get(i).bodyColor());
            } else {
                b.putVec4(0f, 0f, 0f, 0f);
            }
        }
        // titleColors[]
        for (int i = 0; i < MAX_NODES; i++) {
            if (i < n) {
                b.putVec4(entries.get(i).titleColor());
            } else {
                b.putVec4(0f, 0f, 0f, 0f);
            }
        }
        // borderColors[]
        for (int i = 0; i < MAX_NODES; i++) {
            if (i < n) {
                b.putVec4(entries.get(i).borderColor());
            } else {
                b.putVec4(0f, 0f, 0f, 0f);
            }
        }
        // extras[]:
        //   x = title-bar height in scaled pixels
        //   y = drop-shadow flag (0 = off, 1 = on)
        //   z, w = reserved for future per-node visual toggles
        for (int i = 0; i < MAX_NODES; i++) {
            if (i < n) {
                Entry e = entries.get(i);
                b.putVec4(e.titleHeight(), e.dropShadow() ? 1f : 0f, 0f, 0f);
            } else {
                b.putVec4(0f, 0f, 0f, 0f);
            }
        }

        b.get();
    }
}
