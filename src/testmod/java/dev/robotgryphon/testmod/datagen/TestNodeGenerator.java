package dev.robotgryphon.testmod.datagen;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.nodes.properties.computed.BlockStateAt;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinitionBuilder;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import dev.robotgryphon.testmod.TestMod;
import dev.robotgryphon.testmod.nodes.TreeCutterUpgradeNode;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class TestNodeGenerator {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(NodeDefinition.REGISTRY_KEY, TestNodeGenerator::addNodeTypes);

    public static final ResourceKey<NodeDefinition> GET_BLOCK = nodeType("get_block");

    private static ResourceKey<NodeDefinition> nodeType(String name) {
        return ResourceKey.create(NodeDefinition.REGISTRY_KEY,
                Identifier.fromNamespaceAndPath(TestMod.MOD_ID, name));
    }

    private static void addNodeTypes(BootstrapContext<NodeDefinition> ctx) {
        var blockPos = ScreenLib.BLOCK_POS_DATA_TYPE;
        var level = ScreenLib.LEVEL_DATA_TYPE;
        var blockState = ScreenLib.BLOCKSTATE_DATA_TYPE;

        ctx.register(GET_BLOCK, new NodeDefinitionBuilder()
                .addProperty("position", prop -> prop.type(blockPos).input())
                .addProperty("level", prop -> prop.type(level).input())
                .addProperty("state", prop -> prop.type(blockState)
                        .output()
                        .computed(ScreenLib.BLOCK_STATE_AT)
                        .data(BlockStateAt.POSITION, "position")
                        .data(BlockStateAt.LEVEL, "level"))
                .build());

        ctx.register(TreeCutterUpgradeNode.KEY, TreeCutterUpgradeNode.definition());
    }
}
