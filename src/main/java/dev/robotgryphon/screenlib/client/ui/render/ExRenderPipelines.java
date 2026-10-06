package dev.robotgryphon.screenlib.client.ui.render;

import com.mojang.renderpearl.api.pipeline.*;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.BezierCurveUniform;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

import static dev.robotgryphon.screenlib.ScreenLib.MOD_ID;

public class ExRenderPipelines {

    public static final BindGroupLayout BEZIER_CURVE = BindGroupLayout.builder()
            .withUniform(BezierCurveUniform.NAME, UniformType.UNIFORM_BUFFER)
            .build();

    public static final BindGroupLayout NODE_BATCH = BindGroupLayout.builder()
            .withUniform(NodeBackgroundUniform.Storage.NAME, UniformType.UNIFORM_BUFFER)
            .build();

    /// Shared between all pipelines (everything except the per-shader uniform layout)
    public static final RenderPipeline.Snippet BASE_SNIPPET = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withCull(false)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .buildSnippet();

    public static final RenderPipeline BEZIER_CURVED_LINES = RenderPipeline.builder(BASE_SNIPPET)
            .withBindGroupLayout(BEZIER_CURVE)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/bezier_curve"))
            .withVertexShader(Identifier.fromNamespaceAndPath(MOD_ID, "bezier_curve"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(MOD_ID, "bezier_curve"))
            .build();

    /**
     * SDF-based rounded-rectangle pipeline used to paint node backgrounds
     * (body + title bar + outline) for an entire batch of nodes in a
     * single fragment-shader pass. Reuses {@link #BASE_SNIPPET}'s vertex
     * format and translucent blending since the shader writes premultiplied
     * RGBA into the PiP texture, same as the bezier curves do.
     */
    public static final RenderPipeline NODE_BACKGROUND = RenderPipeline.builder(BASE_SNIPPET)
            .withBindGroupLayout(NODE_BATCH)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/node_background"))
            .withVertexShader(Identifier.fromNamespaceAndPath(MOD_ID, "node_background"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(MOD_ID, "node_background"))
            .build();
}
