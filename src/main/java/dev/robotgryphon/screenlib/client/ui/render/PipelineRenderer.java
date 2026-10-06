package dev.robotgryphon.screenlib.client.ui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class PipelineRenderer {

    /**
     * A mesh whose vertex data is already on the GPU. Uploading is a buffer
     * command, which the backend forbids while a render pass is open, so
     * {@link #upload} must run before the pass and {@link #draw} inside it.
     */
    public static final class UploadedMesh implements AutoCloseable {
        private final GpuBuffer vertices;
        private final int indexCount;
        private final RenderSystem.AutoStorageIndexBuffer indices;

        private UploadedMesh(GpuBuffer vertices, int indexCount, RenderSystem.AutoStorageIndexBuffer indices) {
            this.vertices = vertices;
            this.indexCount = indexCount;
            this.indices = indices;
        }

        /** Uploads and closes {@code mesh}. Quads expand via the shared sequential index buffer. */
        public static UploadedMesh upload(MeshData mesh) {
            try (mesh) {
                GpuBuffer vertices = RenderSystem.getDevice()
                        .createBuffer(() -> "screenlib vertex buffer", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                var indices = RenderSystem.getSequentialBuffer(mesh.drawState().primitiveTopology());
                int indexCount = mesh.drawState().indexCount();
                indices.getBuffer(indexCount);
                return new UploadedMesh(vertices, indexCount, indices);
            }
        }

        /** The pipeline and uniforms must already be bound. */
        public void draw(RenderPass pass) {
            pass.setVertexBuffer(0, vertices.slice());
            pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type());
            pass.drawIndexed(indexCount, 1, 0, 0, 0);
        }

        @Override
        public void close() {
            vertices.close();
        }
    }

    public static GpuBufferSlice getDynamicTransforms(int color) {
        return RenderSystem.getDynamicUniforms()
                .writeTransform(
                        RenderSystem.getModelViewMatrixCopy(),
                        new Vector4f(ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color), ARGB.alphaFloat(color)),
                        new Vector3f(),
                        new Matrix4f()
                );
    }
}