package dev.robotgryphon.screenlib;

import com.mojang.logging.LogUtils;
import dev.robotgryphon.screenlib.graph.nodes.properties.computed.BlockPositionFromCoordinates;
import dev.robotgryphon.screenlib.graph.nodes.properties.computed.BlockStateAt;
import dev.robotgryphon.screenlib.graph.nodes.properties.computed.ComputedProperty;
import dev.robotgryphon.screenlib.graph.nodes.properties.computed.ItemHandlerAt;
import dev.robotgryphon.screenlib.graph.nodes.context.NodeContextSupplier;
import dev.robotgryphon.screenlib.graph.nodes.context.PlayerPosition;
import dev.robotgryphon.screenlib.network.NetworkRegistration;
import dev.robotgryphon.screenlib.graph.types.*;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;
import org.slf4j.Logger;

@Mod(ScreenLib.MOD_ID)
public class ScreenLib {
    public static final String MOD_ID = "screenlib";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<GraphDataType<?>> CORE_DATA_TYPES =
            DeferredRegister.create(GraphDataType.REGISTRY_KEY, "c");

    public static final DeferredRegister<GraphDataType<?>> LIB_DATA_TYPES =
            DeferredRegister.create(GraphDataType.REGISTRY_KEY, MOD_ID);

    public static final DeferredRegister<ComputedProperty<?>> COMPUTED_PROPERTIES =
            DeferredRegister.create(ComputedProperty.REGISTRY_KEY, MOD_ID);

    public static final DeferredRegister<NodeContextSupplier> CONTEXT_SUPPLIERS =
            DeferredRegister.create(NodeContextSupplier.REGISTRY_KEY, MOD_ID);

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

    public ScreenLib(IEventBus modBus) {
        modBus.addListener(ScreenLib::datapackRegistries);
        modBus.addListener(NetworkRegistration::register);
        modBus.addListener(EventPriority.HIGH, ScreenLibCapabilities::register);

        CORE_DATA_TYPES.register(modBus);
        LIB_DATA_TYPES.register(modBus);
        COMPUTED_PROPERTIES.register(modBus);
        CONTEXT_SUPPLIERS.register(modBus);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void datapackRegistries(NewDatapackRegistryEvent newRegistries) {
        newRegistries.worldRegistry(NodeDefinition.REGISTRY_KEY, NodeDefinition.CODEC, NodeDefinition.CODEC);
    }
}
