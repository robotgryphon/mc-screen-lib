package dev.robotgryphon.screenlib.nodes.api.properties.computed;

import dev.robotgryphon.screenlib.graph.schema.properties.computed.ComputedProperty;
import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.graph.schema.properties.DataKey;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataTypes;
import dev.robotgryphon.screenlib.graph.schema.types.ItemHandlerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ComputedProperties {
    private ComputedProperties() {}

    public static final DeferredHolder<ComputedProperty<?>, ComputedProperty<BlockState>> BLOCK_STATE_AT =
            create("block_state_at");

    public static final DeferredHolder<ComputedProperty<?>, ComputedProperty<BlockPos>> BLOCK_POSITION_FROM_COORDINATES =
            create("block_position_from_coordinates");

    public static final DeferredHolder<ComputedProperty<?>, ComputedProperty<ItemHandlerAccess>> ITEM_HANDLER_AT =
            create("item_handler_at");

    public static final class BlockStateAt {
        private BlockStateAt() {}

        public static final DataKey<BlockPos> POSITION = DataKey.required("position", GraphDataTypes.BLOCK_POSITION);
        public static final DataKey<ResourceKey<Level>> LEVEL = DataKey.required("level", GraphDataTypes.LEVEL);
    }

    public static final class BlockPositionFromCoordinates {
        private BlockPositionFromCoordinates() {}

        public static final DataKey<Integer> X = DataKey.required("x", GraphDataTypes.INTEGER);
        public static final DataKey<Integer> Y = DataKey.required("y", GraphDataTypes.INTEGER);
        public static final DataKey<Integer> Z = DataKey.required("z", GraphDataTypes.INTEGER);
    }

    public static final class ItemHandlerAt {
        private ItemHandlerAt() {}

        public static final DataKey<BlockPos> POSITION = DataKey.required("position", GraphDataTypes.BLOCK_POSITION);
        public static final DataKey<ResourceKey<Level>> LEVEL = DataKey.required("level", GraphDataTypes.LEVEL);
        public static final DataKey<Direction> SIDE = DataKey.optional("side", GraphDataTypes.DIRECTION);
    }

    private static <T> DeferredHolder<ComputedProperty<?>, ComputedProperty<T>> create(String path) {
        return DeferredHolder.create(ComputedProperty.REGISTRY_KEY, ScreenLib.id(path));
    }
}
