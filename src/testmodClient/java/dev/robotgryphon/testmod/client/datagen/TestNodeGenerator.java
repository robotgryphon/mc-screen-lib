package dev.robotgryphon.testmod.client.datagen;

import dev.robotgryphon.screenlib.graph.schema.types.GraphDataTypes;
import dev.robotgryphon.screenlib.nodes.api.properties.computed.ComputedProperties;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinitionBuilder;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import dev.robotgryphon.testmod.common.TestMod;
import dev.robotgryphon.testmod.common.nodes.TreeCutterUpgradeNode;
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
        var blockPos = GraphDataTypes.BLOCK_POSITION;
        var level = GraphDataTypes.LEVEL;

        ctx.register(GET_BLOCK, new NodeDefinitionBuilder()
                .addProperty("position", prop -> prop.type(blockPos).input())
                .addProperty("level", prop -> prop.type(level).input())
                .addProperty("state", prop -> prop
                        .output()
                        .computed(ComputedProperties.BLOCK_STATE_AT)
                        .data(ComputedProperties.BlockStateAt.POSITION, "position")
                        .data(ComputedProperties.BlockStateAt.LEVEL, "level"))
                .build());

        ctx.register(TreeCutterUpgradeNode.KEY, TreeCutterUpgradeNode.definition());
    }
}
