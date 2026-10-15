package dev.robotgryphon.screenlib.datagen;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.nodes.properties.computed.BlockPositionFromCoordinates;
import dev.robotgryphon.screenlib.graph.nodes.properties.computed.ItemHandlerAt;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinitionBuilder;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

public class NodeGenerator {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(NodeDefinition.REGISTRY_KEY, NodeGenerator::addCoreNodeTypes);

    public static final ResourceKey<NodeDefinition> RESOURCE_ACCESS_ITEMS = nodeType("resource_access_items");

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

        ctx.register(nodeType("integer"), new NodeDefinitionBuilder()
                .addProperty("value", prop -> prop.type(intType).input().output())
                .build());

        ctx.register(nodeType("float"), new NodeDefinitionBuilder()
                .addProperty("value", prop -> prop.type(floatType).input().output())
                .build());

        ctx.register(nodeType("boolean"), new NodeDefinitionBuilder()
                .addProperty("value", prop -> prop.type(boolType).input().output())
                .build());

        ctx.register(nodeType("block_position"), new NodeDefinitionBuilder()
                .addProperty("x", prop -> prop.type(intType).input())
                .addProperty("y", prop -> prop.type(intType).input())
                .addProperty("z", prop -> prop.type(intType).input())
                .addProperty("position", prop -> prop.type(blockPos)
                        .output()
                        .computed(ScreenLib.BLOCK_POSITION_FROM_COORDINATES)
                        .data(BlockPositionFromCoordinates.X, "x")
                        .data(BlockPositionFromCoordinates.Y, "y")
                        .data(BlockPositionFromCoordinates.Z, "z"))
                .addContextSupplier(ScreenLib.PLAYER_POSITION)
                .build());

        ctx.register(nodeType("direction"), new NodeDefinitionBuilder()
                .addProperty("value", prop -> prop.type(direction).input().output())
                .build());

        ctx.register(nodeType("level"), new NodeDefinitionBuilder()
                .addProperty("value", prop -> prop.type(level).input().output())
                .build());

        ctx.register(RESOURCE_ACCESS_ITEMS, new NodeDefinitionBuilder()
                .addProperty("position", prop -> prop.type(blockPos).input())
                .addProperty("level", prop -> prop.type(level).input())
                .addProperty("side", prop -> prop.type(direction).optional().input().output())
                .addProperty("storage", prop -> prop.type(itemHandler)
                        .output()
                        .computed(ScreenLib.ITEM_HANDLER_AT)
                        .data(ItemHandlerAt.POSITION, "position")
                        .data(ItemHandlerAt.LEVEL, "level")
                        .data(ItemHandlerAt.SIDE, "side"))
                .build());
    }
}
