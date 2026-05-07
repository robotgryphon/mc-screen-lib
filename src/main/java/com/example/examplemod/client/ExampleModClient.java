package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.render.ExRenderPipelines;
import com.example.examplemod.client.render.pip.BezierCurvePiPRenderer;
import com.example.examplemod.client.render.pip.BezierCurveRenderState;
import com.example.examplemod.client.render.uniforms.RenderPipelineUniformsStorage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ExampleMod.MODID, dist = Dist.CLIENT)
public class ExampleModClient {
    public ExampleModClient(IEventBus eventBus) {

        eventBus.addListener(this::registerPips);
        eventBus.addListener(this::registerRenderPipelines);

        NeoForge.EVENT_BUS.addListener(this::onRenderFrameEnd);
    }

    public void registerPips(RegisterPictureInPictureRenderersEvent pips) {
        pips.register(BezierCurveRenderState.class, BezierCurvePiPRenderer::new);
    }

    public void registerRenderPipelines(RegisterRenderPipelinesEvent pipelines) {
        pipelines.registerPipeline(ExRenderPipelines.BEZIER_CURVED_LINES);
    }

    private void onRenderFrameEnd(RenderFrameEvent.Post event) {
        RenderPipelineUniformsStorage.endFrame();
    }
}
