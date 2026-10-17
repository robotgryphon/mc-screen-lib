package dev.robotgryphon.screenlib.nodes.client.datagen;

import dev.robotgryphon.screenlib.nodes.api.properties.computed.ComputedProperties;
import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.nodes.common.ScreenLibNodes;
import dev.robotgryphon.screenlib.nodes.common.properties.computed.BlockPositionFromCoordinates;
import dev.robotgryphon.screenlib.nodes.common.properties.computed.ItemHandlerAt;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinitionBuilder;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
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

        var boolType = ScreenLibNodes.BOOLEAN_DATA_TYPE;
        var intType = ScreenLibNodes.INTEGER_DATA_TYPE;
        var floatType = ScreenLibNodes.FLOAT_DATA_TYPE;

        var blockPos = ScreenLibNodes.BLOCK_POS_DATA_TYPE;
        var direction = ScreenLibNodes.DIRECTION_DATA_TYPE;
        var level = ScreenLibNodes.LEVEL_DATA_TYPE;

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
                .addProperty("position", prop -> prop
                        .output()
                        .computed(ScreenLibNodes.BLOCK_POSITION_FROM_COORDINATES)
                        .data(ComputedProperties.BlockPositionFromCoordinates.X, "x")
                        .data(ComputedProperties.BlockPositionFromCoordinates.Y, "y")
                        .data(ComputedProperties.BlockPositionFromCoordinates.Z, "z"))
                .addContextSupplier(ScreenLibNodes.PLAYER_POSITION)
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
                .addProperty("storage", prop -> prop
                        .output()
                        .computed(ScreenLibNodes.ITEM_HANDLER_AT)
                        .data(ComputedProperties.ItemHandlerAt.POSITION, "position")
                        .data(ComputedProperties.ItemHandlerAt.LEVEL, "level")
                        .data(ComputedProperties.ItemHandlerAt.SIDE, "side"))
                .build());
    }
}
