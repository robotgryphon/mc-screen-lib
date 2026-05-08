package dev.robotgryphon.screenlib.client.ui.render;

import dev.robotgryphon.screenlib.client.ui.render.uniforms.BezierCurveUniform;
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
}
