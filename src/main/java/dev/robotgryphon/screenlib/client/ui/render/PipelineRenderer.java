package dev.robotgryphon.screenlib.client.ui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class PipelineRenderer {

    // Reference from 26.1 code
//    public record Buffers(
//            GpuBuffer vertex,
//            GpuBuffer index,
//            VertexFormat type
//    ){
//        public static Buffers of(MeshData mesh, RenderPipeline pipeline) {
//            GpuBuffer vertex = pipeline.getVertexFormat().uploadImmediateVertexBuffer(mesh.vertexBuffer());
//            if (mesh.indexBuffer() == null) {
//                var storage = RenderSystem.getSequentialBuffer(mesh.drawState().mode());
//                return new Buffers(
//                        vertex,
//                        storage.getBuffer(mesh.drawState().indexCount()),
//                        storage.type()
//                );
//            }
//            return new Buffers(
//                    vertex,
//                    pipeline.getVertexFormat().uploadImmediateIndexBuffer(mesh.indexBuffer()),
//                    mesh.drawState().indexType()
//            );
//        }
//    }

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