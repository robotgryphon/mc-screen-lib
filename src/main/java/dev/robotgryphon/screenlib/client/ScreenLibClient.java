package dev.robotgryphon.screenlib.client;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.client.render.BlockPreviewRenderer;
import dev.robotgryphon.screenlib.client.render.ItemHandlerPreviewRenderer;
import dev.robotgryphon.screenlib.client.render.NodeRenderers;
import dev.robotgryphon.screenlib.client.ui.AddNodeScreen;
import dev.robotgryphon.screenlib.client.ui.TestScreen;
import dev.robotgryphon.screenlib.client.ui.render.ExRenderPipelines;
import dev.robotgryphon.screenlib.client.ui.render.pip.BezierCurvePiPRenderer;
import dev.robotgryphon.screenlib.client.ui.render.pip.BezierCurveRenderState;
import dev.robotgryphon.screenlib.client.ui.render.pip.NodeBackgroundPiPRenderer;
import dev.robotgryphon.screenlib.client.ui.render.pip.NodeBackgroundRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = ScreenLib.MOD_ID, dist = Dist.CLIENT)
public class ScreenLibClient {
    public ScreenLibClient(IEventBus modEventBus) {
        modEventBus.addListener(ScreenLibClient::registerPips);
        modEventBus.addListener(ScreenLibClient::registerRenderPipelines);
        modEventBus.addListener(ScreenLibClient::registerMenuScreens);
        NodeRenderers.register(ScreenLib.BLOCK_PREVIEW, new BlockPreviewRenderer());
        NodeRenderers.register(ScreenLib.ITEM_HANDLER_PREVIEW, new ItemHandlerPreviewRenderer());
    }

    private static void registerPips(RegisterPictureInPictureRenderersEvent pips) {
        pips.register(BezierCurveRenderState.class, BezierCurvePiPRenderer::new);
        pips.register(NodeBackgroundRenderState.class, NodeBackgroundPiPRenderer::new);
    }

    private static void registerRenderPipelines(RegisterRenderPipelinesEvent pipelines) {
        pipelines.registerPipeline(ExRenderPipelines.BEZIER_CURVED_LINES);
        pipelines.registerPipeline(ExRenderPipelines.NODE_BACKGROUND);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ScreenLib.TEST_SCREEN_MENU.get(), TestScreen::new);
        event.register(ScreenLib.ADD_NODE_MENU.get(), AddNodeScreen::new);
    }
}
