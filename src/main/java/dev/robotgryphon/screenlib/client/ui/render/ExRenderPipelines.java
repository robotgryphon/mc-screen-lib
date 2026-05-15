package dev.robotgryphon.screenlib.client.ui.render;

import dev.robotgryphon.screenlib.client.ui.render.uniforms.BezierCurveUniform;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

import static dev.robotgryphon.screenlib.ScreenLib.MOD_ID;

public class ExRenderPipelines {

    /// Shared between all line pipelines
    public static final RenderPipeline.Snippet LINE_SNIPPET = RenderPipeline.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .buildSnippet();

    public static final RenderPipeline BEZIER_CURVED_LINES = RenderPipeline.builder(LINE_SNIPPET)
            .withUniform(BezierCurveUniform.NAME, UniformType.UNIFORM_BUFFER)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/bezier_curve"))
            .withVertexShader(Identifier.fromNamespaceAndPath(MOD_ID, "bezier_curve"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(MOD_ID, "bezier_curve"))
            .build();

    /**
     * SDF-based rounded-rectangle pipeline used to paint node backgrounds
     * (body + title bar + outline) for an entire batch of nodes in a
     * single fragment-shader pass. Reuses {@link #LINE_SNIPPET}'s vertex
     * format and translucent blending since the shader writes premultiplied
     * RGBA into the PiP texture, same as the bezier curves do.
     */
    public static final RenderPipeline NODE_BACKGROUND = RenderPipeline.builder(LINE_SNIPPET)
            .withUniform(NodeBackgroundUniform.NAME, UniformType.UNIFORM_BUFFER)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/node_background"))
            .withVertexShader(Identifier.fromNamespaceAndPath(MOD_ID, "node_background"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(MOD_ID, "node_background"))
            .build();
}
