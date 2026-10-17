package dev.robotgryphon.screenlib.nodes.client.datagen;

import dev.robotgryphon.screenlib.core.ScreenLib;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;

@EventBusSubscriber(modid = ScreenLib.MOD_ID)
public class DataGeneration {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.createWorldRegistryObjects(NodeGenerator.BUILDER, Set.of(ScreenLib.MOD_ID));
        event.createProvider(NodeAppearanceGenerator::new);
    }
}
