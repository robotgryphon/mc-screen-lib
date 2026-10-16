package dev.robotgryphon.screenlib.client;

import dev.robotgryphon.screenlib.client.render.block.BlockPreviewPiPRenderer;
import dev.robotgryphon.screenlib.client.render.block.BlockPreviewRenderState;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.client.appearance.NodeAppearances;
import dev.robotgryphon.screenlib.client.render.NodeWidgets;
import dev.robotgryphon.screenlib.client.ui.widget.canvas.CanvasTools;
import dev.robotgryphon.screenlib.client.ui.widget.panel.PanelTypes;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.ToolbarButtons;
import dev.robotgryphon.screenlib.client.render.PropertyRenderers;
import dev.robotgryphon.screenlib.client.ui.render.ExRenderPipelines;
import dev.robotgryphon.screenlib.client.ui.render.pip.BezierCurvePiPRenderer;
import dev.robotgryphon.screenlib.client.ui.render.pip.BezierCurveRenderState;
import dev.robotgryphon.screenlib.client.ui.render.pip.NodeBackgroundPiPRenderer;
import dev.robotgryphon.screenlib.client.ui.render.pip.NodeBackgroundRenderState;
import net.neoforged.api.distmarker.Dist;
import dev.robotgryphon.screenlib.graph.canvas.client.CanvasViewports;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = ScreenLib.MOD_ID, dist = Dist.CLIENT)
public class ScreenLibClient {

    public ScreenLibClient(IEventBus modEventBus) {
        modEventBus.addListener(ScreenLibClient::registerPips);
        modEventBus.addListener(ScreenLibClient::registerRenderPipelines);
        modEventBus.addListener(ScreenLibClient::registerReloadListeners);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> CanvasViewports.clear());
        NodeWidgets.RENDERERS.register(modEventBus);
        ToolbarButtons.BUTTONS.register(modEventBus);
        PanelTypes.PANELS.register(modEventBus);
        CanvasTools.register(modEventBus);
        PropertyRenderers.RENDERERS.register(modEventBus);
    }

    private static void registerPips(RegisterPictureInPictureRenderersEvent pips) {
        pips.register(BezierCurveRenderState.class, BezierCurvePiPRenderer::new);
        pips.register(NodeBackgroundRenderState.class, NodeBackgroundPiPRenderer::new);
        pips.register(BlockPreviewRenderState.class, BlockPreviewPiPRenderer::new);
    }

    private static void registerRenderPipelines(RegisterRenderPipelinesEvent pipelines) {
        pipelines.registerPipeline(ExRenderPipelines.BEZIER_CURVED_LINES);
        pipelines.registerPipeline(ExRenderPipelines.NODE_BACKGROUND);
    }

    private static void registerReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(NodeAppearances.ID, NodeAppearances.INSTANCE);
    }
}
