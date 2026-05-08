package dev.robotgryphon.screenlib.client;

import com.llamalad7.mixinextras.sugar.Local;
import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.client.ui.render.ExRenderPipelines;
import dev.robotgryphon.screenlib.client.ui.render.pip.BezierCurvePiPRenderer;
import dev.robotgryphon.screenlib.client.ui.render.pip.BezierCurveRenderState;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.RenderPipelineUniformsStorage;
import dev.robotgryphon.screenlib.client.ui.ClientScreenHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import static dev.robotgryphon.screenlib.ScreenLib.MOD_ID;

@Mod(value = ScreenLib.MOD_ID, dist = Dist.CLIENT)
public class ScreenLibClient {

    public ScreenLibClient(IEventBus modEventBus) {
        modEventBus.addListener(ScreenLibClient::registerPips);
        modEventBus.addListener(ScreenLibClient::registerRenderPipelines);

        NeoForge.EVENT_BUS.addListener(ScreenLibClient::regClientCommands);
        NeoForge.EVENT_BUS.addListener(ScreenLibClient::onRenderFrameEnd);
    }

    private static void registerPips(RegisterPictureInPictureRenderersEvent pips) {
        pips.register(BezierCurveRenderState.class, BezierCurvePiPRenderer::new);
    }

    private static void registerRenderPipelines(RegisterRenderPipelinesEvent pipelines) {
        pipelines.registerPipeline(ExRenderPipelines.BEZIER_CURVED_LINES);
    }

    private static void onRenderFrameEnd(RenderFrameEvent.Post event) {
        RenderPipelineUniformsStorage.endFrame();
    }

    private static void regClientCommands(final RegisterClientCommandsEvent e) {
        final var dispatcher = e.getDispatcher();
        final var root = Commands.literal(MOD_ID);

        final var test = Commands.literal("test")
                .requires(s -> s.getEntity() instanceof LocalPlayer)
                .executes(ctx -> {
                    ClientScreenHelper.openTestScreen((LocalPlayer) ctx.getSource().getEntityOrException());
                    return 0;
                });

        root.then(test);
        dispatcher.register(root);
    }

}
