package dev.robotgryphon.screenlib.datagen;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.schema.node.NodeBuilder;
import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

public class NodeGenerator {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(NodeDefinition.REGISTRY_KEY, NodeGenerator::addCoreNodeTypes);

    private static ResourceKey<NodeDefinition> nodeType(String name) {
        return ResourceKey.create(NodeDefinition.REGISTRY_KEY, ScreenLib.id(name));
    }

    private static void addCoreNodeTypes(BootstrapContext<NodeDefinition> ctx) {
        var defs = ctx.lookup(GraphDataType.REGISTRY_KEY);

        var boolType = ScreenLib.BOOLEAN_DATA_TYPE;
        var intType = ScreenLib.INTEGER_DATA_TYPE;
        var floatType = ScreenLib.FLOAT_DATA_TYPE;

        var blockPos = ScreenLib.BLOCK_POS_DATA_TYPE;
        var direction = ScreenLib.DIRECTION_DATA_TYPE;

        var itemHandler = ScreenLib.ITEM_HANDLER_DATA_TYPE;
        var level = ScreenLib.LEVEL_DATA_TYPE;

        ctx.register(nodeType("integer"), new NodeBuilder()
                .addOutput("INTEGER", intType, "value")
                .addProperty("value", intType)
                .build());

        ctx.register(nodeType("float"), new NodeBuilder()
                .addOutput("FLOAT", floatType, "value")
                .addProperty("value", floatType)
                .build());

        ctx.register(nodeType("boolean"), new NodeBuilder()
                .addOutput("BOOLEAN", boolType, "value")
                .addProperty("value", boolType)
                .build());

        ctx.register(nodeType("block_position"), new NodeBuilder()
                .addOutput("Position", blockPos, "position")
                .addProperty("x", intType)
                .addProperty("y", intType)
                .addProperty("z", intType)
                .addComputedProperty("position", blockPos, ScreenLib.BLOCK_POSITION_FROM_COORDINATES)
                .build());

        ctx.register(nodeType("direction"), new NodeBuilder()
                .addOutput("direction", direction, "value")
                .addProperty("value", direction)
                .build());

        ctx.register(nodeType("level"), new NodeBuilder()
                .addOutput("Level", level, "value")
                .addProperty("value", level)
                .build());

        ctx.register(nodeType("resource_access_items"), new NodeBuilder()
                .addInput("Position", blockPos)
                .addInput("Level", level)
                .addProperty("Side", direction, true)
                .addComputedProperty("storage", itemHandler, ScreenLib.ITEM_HANDLER_AT, true)
                .addOutput("Storage", itemHandler, "storage")
                .addRender(ScreenLib.ITEM_HANDLER_PREVIEW)
                .build());

        ctx.register(nodeType("tree_cutter_upgrade"), new NodeBuilder()
                .addInput("Tools", itemHandler)
                .addInput("Drops", itemHandler)
                .build());
    }
}
