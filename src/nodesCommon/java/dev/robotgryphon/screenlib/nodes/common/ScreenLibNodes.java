package dev.robotgryphon.screenlib.nodes.common;

import dev.robotgryphon.screenlib.nodes.common.properties.computed.BlockPositionFromCoordinates;
import dev.robotgryphon.screenlib.nodes.common.properties.computed.BlockStateAt;
import dev.robotgryphon.screenlib.graph.schema.properties.computed.ComputedProperty;
import dev.robotgryphon.screenlib.nodes.common.properties.computed.ItemHandlerAt;
import dev.robotgryphon.screenlib.graph.schema.context.NodeContextSupplier;
import dev.robotgryphon.screenlib.nodes.common.context.PlayerPosition;
import dev.robotgryphon.screenlib.nodes.common.network.NetworkRegistration;
import dev.robotgryphon.screenlib.graph.schema.types.IntGraphDataType;
import dev.robotgryphon.screenlib.graph.schema.types.LevelGraphDataType;
import dev.robotgryphon.screenlib.graph.schema.types.DoubleGraphDataType;
import dev.robotgryphon.screenlib.graph.schema.types.BlockPositionGraphDataType;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import dev.robotgryphon.screenlib.graph.schema.types.ItemHandlerGraphDataType;
import dev.robotgryphon.screenlib.graph.schema.types.FloatGraphDataType;
import dev.robotgryphon.screenlib.graph.schema.types.BooleanGraphDataType;
import dev.robotgryphon.screenlib.graph.schema.types.BlockStateGraphDataType;
import dev.robotgryphon.screenlib.graph.schema.types.DirectionGraphDataType;
import dev.robotgryphon.screenlib.graph.schema.types.StringGraphDataType;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinitionCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;
import dev.robotgryphon.screenlib.core.ScreenLib;

@Mod(ScreenLib.MOD_ID)
public class ScreenLibNodes {
    private static final DeferredRegister<GraphDataType<?>> CORE_DATA_TYPES =
            DeferredRegister.create(GraphDataType.REGISTRY_KEY, "c");

    public static final DeferredRegister<GraphDataType<?>> LIB_DATA_TYPES =
            DeferredRegister.create(GraphDataType.REGISTRY_KEY, ScreenLib.MOD_ID);

    public static final DeferredRegister<ComputedProperty<?>> COMPUTED_PROPERTIES =
            DeferredRegister.create(ComputedProperty.REGISTRY_KEY, ScreenLib.MOD_ID);

    public static final DeferredRegister<NodeContextSupplier> CONTEXT_SUPPLIERS =
            DeferredRegister.create(NodeContextSupplier.REGISTRY_KEY, ScreenLib.MOD_ID);

    static {
        CONTEXT_SUPPLIERS.makeRegistry(builder -> builder.sync(true));
        COMPUTED_PROPERTIES.makeRegistry(builder -> builder.sync(true));
        LIB_DATA_TYPES.makeRegistry(builder -> builder.sync(true));
    }

    public static final DeferredHolder<GraphDataType<?>, BlockPositionGraphDataType> BLOCK_POS_DATA_TYPE = CORE_DATA_TYPES.register("block_position", BlockPositionGraphDataType::new);
    public static final DeferredHolder<GraphDataType<?>, DirectionGraphDataType> DIRECTION_DATA_TYPE = CORE_DATA_TYPES.register("direction", DirectionGraphDataType::new);
    public static final DeferredHolder<GraphDataType<?>, IntGraphDataType> INTEGER_DATA_TYPE = CORE_DATA_TYPES.register("integer", IntGraphDataType::new);
    public static final DeferredHolder<GraphDataType<?>, FloatGraphDataType> FLOAT_DATA_TYPE = CORE_DATA_TYPES.register("float", FloatGraphDataType::new);
    public static final DeferredHolder<GraphDataType<?>, DoubleGraphDataType> DOUBLE_DATA_TYPE = CORE_DATA_TYPES.register("double", DoubleGraphDataType::new);
    public static final DeferredHolder<GraphDataType<?>, StringGraphDataType> STRING_DATA_TYPE = CORE_DATA_TYPES.register("string", StringGraphDataType::new);
    public static final DeferredHolder<GraphDataType<?>, BooleanGraphDataType> BOOLEAN_DATA_TYPE = CORE_DATA_TYPES.register("boolean", BooleanGraphDataType::new);

    public static final DeferredHolder<GraphDataType<?>, ItemHandlerGraphDataType> ITEM_HANDLER_DATA_TYPE =
            LIB_DATA_TYPES.register("item_handler", ItemHandlerGraphDataType::new);

    public static final DeferredHolder<GraphDataType<?>, LevelGraphDataType> LEVEL_DATA_TYPE = LIB_DATA_TYPES.register("level", LevelGraphDataType::new);

    public static final DeferredHolder<GraphDataType<?>, BlockStateGraphDataType> BLOCKSTATE_DATA_TYPE =
            LIB_DATA_TYPES.register("blockstate", BlockStateGraphDataType::new);

    public static final DeferredHolder<ComputedProperty<?>, BlockStateAt> BLOCK_STATE_AT =
            COMPUTED_PROPERTIES.register("block_state_at", BlockStateAt::new);

    public static final DeferredHolder<ComputedProperty<?>, BlockPositionFromCoordinates> BLOCK_POSITION_FROM_COORDINATES =
            COMPUTED_PROPERTIES.register("block_position_from_coordinates", BlockPositionFromCoordinates::new);

    public static final DeferredHolder<ComputedProperty<?>, ItemHandlerAt> ITEM_HANDLER_AT =
            COMPUTED_PROPERTIES.register("item_handler_at", ItemHandlerAt::new);

    public static final DeferredHolder<NodeContextSupplier, PlayerPosition> PLAYER_POSITION =
            CONTEXT_SUPPLIERS.register("player_position", PlayerPosition::new);

    public ScreenLibNodes(IEventBus modBus) {
        modBus.addListener(ScreenLibNodes::datapackRegistries);
        modBus.addListener(NetworkRegistration::register);

        CORE_DATA_TYPES.register(modBus);
        LIB_DATA_TYPES.register(modBus);
        COMPUTED_PROPERTIES.register(modBus);
        CONTEXT_SUPPLIERS.register(modBus);
    }

    private static void datapackRegistries(NewDatapackRegistryEvent newRegistries) {
        newRegistries.worldRegistry(NodeDefinition.REGISTRY_KEY, NodeDefinitionCodecs.NODE, NodeDefinitionCodecs.NODE);
    }
}
