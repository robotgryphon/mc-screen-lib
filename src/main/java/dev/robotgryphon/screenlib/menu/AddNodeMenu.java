package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.graph.nodes.properties.PropertyValues;
import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.state.CanvasState;
import dev.robotgryphon.screenlib.graph.nodes.context.NodeCreationContext;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import dev.robotgryphon.screenlib.graph.state.NodeState;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddNodeMenu extends AbstractContainerMenu {
    private final @Nullable CanvasMenu parent;
    private final AddNodeData data;

    public AddNodeMenu(int containerId, Inventory inv, CanvasMenu parent, AddNodeData data) {
        super(ScreenLib.ADD_NODE_MENU.get(), containerId);
        this.parent = parent;
        this.data = data;
    }

    public AddNodeMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf buffer) {
        super(ScreenLib.ADD_NODE_MENU.get(), containerId);
        this.parent = null;
        this.data = AddNodeData.STREAM_CODEC.decode(buffer);
    }

    public AddNodeData data() {
        return this.data;
    }

    public void submit(ServerPlayer player, ResourceKey<NodeDefinition> type) {
        var source = this.parent == null ? null : this.parent.source();
        if (source == null || !this.data.available().contains(type)) {
            return;
        }
        var definition = player.registryAccess()
                .lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                .getOrThrow(type);

        CanvasState state = source.canvas().state();
        List<NodeState> nodes = new ArrayList<>(state.nodes());
        nodes.add(new NodeState(UUID.randomUUID(), definition, new Vector2i(this.data.x(), this.data.y()),
                initialProperties(definition, new NodeCreationContext(player, source))));
        source.update(state.withContents(nodes, state.connections()), player.level().getServer());

        this.returnToCanvas(player);
    }

    private static PropertyValues initialProperties(Holder<NodeDefinition> definition, NodeCreationContext context) {
        PropertyValues values = PropertyValues.EMPTY;
        for (var supplier : definition.value().context()) {
            for (var entry : supplier.value().supply(context).entrySet()) {
                for (StoredPropertyDefinition<?> property : definition.value().stored()) {
                    if (property.name().equals(entry.getKey()) && entry.getValue() != null) {
                        values = values.withChecked(property, entry.getValue());
                    }
                }
            }
        }
        return values;
    }

    public void cancel(ServerPlayer player) {
        this.returnToCanvas(player);
    }

    private void returnToCanvas(ServerPlayer player) {
        if (this.parent != null && this.parent.provider() != null) {
            player.openMenu(this.parent.provider());
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.parent == null || this.parent.stillValid(player);
    }
}
