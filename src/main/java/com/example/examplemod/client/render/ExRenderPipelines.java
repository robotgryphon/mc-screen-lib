package com.example.examplemod.client.render;

import com.example.examplemod.client.render.uniforms.BezierCurveUniform;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

import static com.example.examplemod.ExampleMod.MODID;

public class ExRenderPipelines {

    public static final RenderPipeline BEZIER_CURVED_LINES = RenderPipeline.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform(BezierCurveUniform.NAME, UniformType.UNIFORM_BUFFER)
            .withLocation(Identifier.fromNamespaceAndPath(MODID, "pipeline/bezier_curve"))
            .withVertexShader(Identifier.fromNamespaceAndPath(MODID, "bezier_curve"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(MODID, "bezier_curve"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build();
}
