package dev.robotgryphon.screenlib.nodes.client;

import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.nodes.api.spi.NodesClientProvider;
import dev.robotgryphon.screenlib.nodes.client.appearance.NodeAppearances;
import dev.robotgryphon.screenlib.nodes.client.canvas.CanvasViewports;
import dev.robotgryphon.screenlib.nodes.client.network.ClientPayloadHandler;
import dev.robotgryphon.screenlib.nodes.client.render.NodeWidgets;
import dev.robotgryphon.screenlib.nodes.client.render.PropertyRenderers;
import dev.robotgryphon.screenlib.nodes.client.render.block.BlockPreviewPiPRenderer;
import dev.robotgryphon.screenlib.nodes.client.render.block.BlockPreviewRenderState;
import dev.robotgryphon.screenlib.nodes.client.widget.canvas.CanvasToolRegistration;
import dev.robotgryphon.screenlib.nodes.common.network.CanvasEvaluationPayload;
import dev.robotgryphon.screenlib.nodes.common.network.CanvasSyncPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ScreenLib.MOD_ID, dist = Dist.CLIENT)
public class ScreenLibNodesClient {
    public ScreenLibNodesClient(IEventBus modEventBus) {
        NodesClientProvider.SLOT.install(new ClientNodesProvider());
        modEventBus.addListener(ScreenLibNodesClient::registerPips);
        modEventBus.addListener(ScreenLibNodesClient::registerReloadListeners);
        modEventBus.addListener(ScreenLibNodesClient::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> CanvasViewports.clear());
        NodeWidgets.RENDERERS.register(modEventBus);
        PropertyRenderers.RENDERERS.register(modEventBus);
        CanvasToolRegistration.register(modEventBus);
    }

    private static void registerPips(RegisterPictureInPictureRenderersEvent pips) {
        pips.register(BlockPreviewRenderState.class, BlockPreviewPiPRenderer::new);
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(CanvasSyncPayload.TYPE, ClientPayloadHandler::handleSync);
        event.register(CanvasEvaluationPayload.TYPE, ClientPayloadHandler::handleEvaluation);
    }

    private static void registerReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(NodeAppearances.ID, NodeAppearances.INSTANCE);
    }
}
