package dev.robotgryphon.screenlib.datagen;

import dev.robotgryphon.screenlib.ScreenLib;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = ScreenLib.MOD_ID)
public class DataGeneration {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.createProvider(NodeGenerator::new);
    }
}
