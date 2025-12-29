package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.client.render.ExRenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = ExampleMod.MODID, dist = Dist.CLIENT)
public class ExampleModClient {
    public ExampleModClient(ModContainer container, IEventBus eventBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        eventBus.addListener(this::registerRenderPipelines);
    }

    public void registerRenderPipelines(RegisterRenderPipelinesEvent pipelines) {
        pipelines.registerPipeline(ExRenderPipelines.BEZIER_CURVED_LINES);
    }
}
