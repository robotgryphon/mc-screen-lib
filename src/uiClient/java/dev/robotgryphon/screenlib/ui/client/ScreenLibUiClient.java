package dev.robotgryphon.screenlib.ui.client;

import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.ui.api.panel.PanelTypes;
import dev.robotgryphon.screenlib.ui.api.render.RoundedRectRenderState;
import dev.robotgryphon.screenlib.ui.api.spi.UiProvider;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarButtons;
import dev.robotgryphon.screenlib.ui.client.render.ExRenderPipelines;
import dev.robotgryphon.screenlib.ui.client.render.pip.BezierCurvePiPRenderer;
import dev.robotgryphon.screenlib.ui.common.render.BezierCurveRenderState;
import dev.robotgryphon.screenlib.ui.client.render.pip.RoundedRectPiPRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@Mod(value = ScreenLib.MOD_ID, dist = Dist.CLIENT)
public class ScreenLibUiClient {
    public ScreenLibUiClient(IEventBus modEventBus) {
        modEventBus.addListener(ScreenLibUiClient::registerPips);
        modEventBus.addListener(ScreenLibUiClient::registerRenderPipelines);
        UiProvider.SLOT.install(new ScreenLibUiProvider());
        ToolbarButtons.BUTTONS.register(modEventBus);
        PanelTypes.PANELS.register(modEventBus);
    }

    private static void registerPips(RegisterPictureInPictureRenderersEvent pips) {
        pips.register(BezierCurveRenderState.class, BezierCurvePiPRenderer::new);
        pips.register(RoundedRectRenderState.class, RoundedRectPiPRenderer::new);
    }

    private static void registerRenderPipelines(RegisterRenderPipelinesEvent pipelines) {
        pipelines.registerPipeline(ExRenderPipelines.BEZIER_CURVED_LINES);
        pipelines.registerPipeline(ExRenderPipelines.NODE_BACKGROUND);
    }
}
