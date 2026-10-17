package dev.robotgryphon.screenlib.graph.schema.types;

import dev.robotgryphon.screenlib.core.ScreenLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class GraphDataTypes {
    private GraphDataTypes() {}

    public static final DeferredHolder<GraphDataType<?>, GraphDataType<BlockPos>> BLOCK_POSITION = common("block_position");
    public static final DeferredHolder<GraphDataType<?>, GraphDataType<Direction>> DIRECTION = common("direction");
    public static final DeferredHolder<GraphDataType<?>, GraphDataType<Integer>> INTEGER = common("integer");
    public static final DeferredHolder<GraphDataType<?>, GraphDataType<Float>> FLOAT = common("float");
    public static final DeferredHolder<GraphDataType<?>, GraphDataType<Double>> DOUBLE = common("double");
    public static final DeferredHolder<GraphDataType<?>, GraphDataType<String>> STRING = common("string");
    public static final DeferredHolder<GraphDataType<?>, GraphDataType<Boolean>> BOOLEAN = common("boolean");

    public static final DeferredHolder<GraphDataType<?>, GraphDataType<ItemHandlerAccess>> ITEM_HANDLER = library("item_handler");
    public static final DeferredHolder<GraphDataType<?>, GraphDataType<ResourceKey<Level>>> LEVEL = library("level");
    public static final DeferredHolder<GraphDataType<?>, GraphDataType<BlockState>> BLOCK_STATE = library("blockstate");

    private static <T> DeferredHolder<GraphDataType<?>, GraphDataType<T>> common(String path) {
        return DeferredHolder.create(GraphDataType.REGISTRY_KEY, Identifier.fromNamespaceAndPath("c", path));
    }

    private static <T> DeferredHolder<GraphDataType<?>, GraphDataType<T>> library(String path) {
        return DeferredHolder.create(GraphDataType.REGISTRY_KEY, ScreenLib.id(path));
    }
}
