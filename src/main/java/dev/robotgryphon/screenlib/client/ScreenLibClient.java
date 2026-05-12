package dev.robotgryphon.screenlib.client;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.client.ui.TestScreen;
import dev.robotgryphon.screenlib.client.ui.render.ExRenderPipelines;
import dev.robotgryphon.screenlib.client.ui.render.pip.BezierCurvePiPRenderer;
import dev.robotgryphon.screenlib.client.ui.render.pip.BezierCurveRenderState;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.RenderPipelineUniformsStorage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = ScreenLib.MOD_ID, dist = Dist.CLIENT)
public class ScreenLibClient {

    public ScreenLibClient(IEventBus modEventBus) {
        modEventBus.addListener(ScreenLibClient::registerPips);
        modEventBus.addListener(ScreenLibClient::registerRenderPipelines);
        modEventBus.addListener(ScreenLibClient::registerMenuScreens);

        NeoForge.EVENT_BUS.addListener(ScreenLibClient::onRenderFrameEnd);
    }

    private static void registerPips(RegisterPictureInPictureRenderersEvent pips) {
        pips.register(BezierCurveRenderState.class, BezierCurvePiPRenderer::new);
    }

    private static void registerRenderPipelines(RegisterRenderPipelinesEvent pipelines) {
        pipelines.registerPipeline(ExRenderPipelines.BEZIER_CURVED_LINES);
    }

    /**
     * Binds the test screen menu type to its client-side screen
     * constructor. Without this, vanilla's open-container flow has no
     * way to know which Screen to instantiate when the server tells the
     * client "open menu of type {@code screenlib:test_screen}".
     */
    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ScreenLib.TEST_SCREEN_MENU.get(), TestScreen::new);
    }

    private static void onRenderFrameEnd(RenderFrameEvent.Post event) {
        RenderPipelineUniformsStorage.endFrame();
    }
}
