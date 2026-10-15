package dev.robotgryphon.testmod.datagen;

import dev.robotgryphon.testmod.TestMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;

@EventBusSubscriber(modid = TestMod.MOD_ID)
public class TestModDataGeneration {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.createWorldRegistryObjects(TestNodeGenerator.BUILDER, Set.of(TestMod.MOD_ID));
        event.createProvider(TestNodeAppearanceGenerator::new);
    }
}
