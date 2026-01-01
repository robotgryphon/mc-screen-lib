package com.example.examplemod.client.render.uniforms;

import net.minecraft.client.renderer.DynamicUniformStorage;

import java.nio.ByteBuffer;

public interface RenderPipelineUniforms extends DynamicUniformStorage.DynamicUniform {

    String name();

    @Override
    void write(ByteBuffer buffer);
}


