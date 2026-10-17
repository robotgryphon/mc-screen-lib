package dev.robotgryphon.screenlib.ui.client.render;

import com.mojang.renderpearl.api.pipeline.*;
import dev.robotgryphon.screenlib.ui.client.render.uniforms.BezierCurveUniform;
import dev.robotgryphon.screenlib.ui.client.render.uniforms.RoundedRectUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

import static dev.robotgryphon.screenlib.core.ScreenLib.MOD_ID;

public class ExRenderPipelines {
    public static final BindGroupLayout BEZIER_CURVE = BindGroupLayout.builder()
            .withUniform(BezierCurveUniform.NAME, UniformType.UNIFORM_BUFFER)
            .build();

    public static final BindGroupLayout NODE_BATCH = BindGroupLayout.builder()
            .withUniform(RoundedRectUniform.Storage.NAME, UniformType.UNIFORM_BUFFER)
            .build();

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

    public static final RenderPipeline NODE_BACKGROUND = RenderPipeline.builder(BASE_SNIPPET)
            .withBindGroupLayout(NODE_BATCH)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/node_background"))
            .withVertexShader(Identifier.fromNamespaceAndPath(MOD_ID, "node_background"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(MOD_ID, "node_background"))
            .build();
}
