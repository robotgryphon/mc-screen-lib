package dev.robotgryphon.screenlib.ui.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.jspecify.annotations.NonNull;

public class RenderHelper {
    public static PipelineRenderer.@NonNull UploadedMesh createMesh(int width, int height, int color) {
        try (var byteBuffer = new ByteBufferBuilder(256)) {
            var buffer = new BufferBuilder(byteBuffer, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);

            PipelineRenderer.UploadedMesh mesh;
            buffer.addVertex(0f, 0f, 0f).setColor(color);
            buffer.addVertex(0f, height, 0f).setColor(color);
            buffer.addVertex(width, height, 0f).setColor(color);
            buffer.addVertex(width, 0f, 0f).setColor(color);
            mesh = PipelineRenderer.UploadedMesh.upload(buffer.buildOrThrow());
            return mesh;
        }
    }
}
