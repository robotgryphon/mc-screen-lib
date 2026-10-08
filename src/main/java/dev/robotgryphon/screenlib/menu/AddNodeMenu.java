package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.ScreenLib;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.graph.context.NodeCreationContext;
import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import dev.robotgryphon.screenlib.graph.nodes.NodeState;
import dev.robotgryphon.screenlib.graph.schema.node.StoredPropertyDefinition;
import net.minecraft.core.Holder;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AddNodeMenu extends AbstractContainerMenu {
    private final @Nullable CanvasSource source;
    private final AddNodeData data;

    public AddNodeMenu(int containerId, Inventory inv, CanvasSource source, AddNodeData data) {
        super(ScreenLib.ADD_NODE_MENU.get(), containerId);
        this.source = source;
        this.data = data;
    }

    public AddNodeMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf buffer) {
        super(ScreenLib.ADD_NODE_MENU.get(), containerId);
        this.source = null;
        this.data = AddNodeData.STREAM_CODEC.decode(buffer);
    }

    public AddNodeData data() {
        return this.data;
    }

    public void submit(ServerPlayer player, ResourceKey<NodeDefinition> type) {
        if (this.source == null || !this.data.available().contains(type)) {
            return;
        }
        var definition = this.source.level().registryAccess()
                .lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                .getOrThrow(type);

        CanvasState state = this.source.load();
        List<NodeState> nodes = new ArrayList<>(state.nodes());
        nodes.add(new NodeState(definition, this.data.x(), this.data.y(),
                initialProperties(definition, new NodeCreationContext(player, this.source)), Optional.empty(), Optional.empty()));
        this.source.save(new CanvasState(nodes, state.connections(), state.viewport()));
        HostedNodes.publish(this.source);

        this.returnToCanvas(player);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, Dynamic<?>> initialProperties(Holder<NodeDefinition> definition, NodeCreationContext context) {
        Map<String, Dynamic<?>> values = new HashMap<>();
        for (var supplier : definition.value().context()) {
            supplier.value().supply(context).forEach((name, value) -> {
                for (StoredPropertyDefinition<?> property : definition.value().stored()) {
                    var type = property.type().value();
                    if (property.name().equals(name) && type.dataType().isInstance(value)) {
                        DataResult<Tag> result = ((Codec) type.codec()).encodeStart(NbtOps.INSTANCE, value);
                        result.result().ifPresent(tag -> values.put(name, new Dynamic<>(NbtOps.INSTANCE, tag)));
                    }
                }
            });
        }
        return values;
    }

    public void cancel(ServerPlayer player) {
        this.returnToCanvas(player);
    }

    private void returnToCanvas(ServerPlayer player) {
        if (this.source != null) {
            player.openMenu(this.source.menu());
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.source == null || this.source.stillValid(player);
    }
}
