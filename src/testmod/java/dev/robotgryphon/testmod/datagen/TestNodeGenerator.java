package dev.robotgryphon.testmod.datagen;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.nodes.NodeBuilder;
import dev.robotgryphon.screenlib.graph.nodes.NodeDefinition;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import dev.robotgryphon.testmod.TestMod;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class TestNodeGenerator {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(NodeDefinition.REGISTRY_KEY, TestNodeGenerator::addNodeTypes);

    private static ResourceKey<NodeDefinition> nodeType(String name) {
        return ResourceKey.create(NodeDefinition.REGISTRY_KEY,
                Identifier.fromNamespaceAndPath(TestMod.MOD_ID, name));
    }

    private static void addNodeTypes(BootstrapContext<NodeDefinition> ctx) {
        var blockPos = ScreenLib.BLOCK_POS_DATA_TYPE;
        var level = ScreenLib.LEVEL_DATA_TYPE;
        var blockState = ScreenLib.BLOCKSTATE_DATA_TYPE;

        ctx.register(nodeType("get_block"), new NodeBuilder()
                .addInput("Position", blockPos)
                .addInput("Level", level)
                .addProperty("state", blockState)
                .build());
    }
}
