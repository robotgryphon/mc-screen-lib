package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import dev.robotgryphon.screenlib.graph.nodes.NodeState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AddNodeMenu extends AbstractContainerMenu {
    private final @Nullable Level serverLevel;
    private final AddNodeData data;

    public AddNodeMenu(int containerId, Inventory inv, Level level, AddNodeData data) {
        super(ScreenLib.ADD_NODE_MENU.get(), containerId);
        this.serverLevel = level;
        this.data = data;
    }

    public AddNodeMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf buffer) {
        super(ScreenLib.ADD_NODE_MENU.get(), containerId);
        this.serverLevel = null;
        this.data = AddNodeData.STREAM_CODEC.decode(buffer);
    }

    public AddNodeData data() {
        return this.data;
    }

    public void submit(ServerPlayer player, ResourceKey<NodeDefinition> type) {
        if (this.serverLevel == null || !this.data.available().contains(type)) {
            return;
        }
        var definition = this.serverLevel.registryAccess()
                .lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                .getOrThrow(type);

        CanvasState state = this.serverLevel.getData(ScreenLib.TEST_SCREEN_ATTACHMENT);
        List<NodeState> nodes = new ArrayList<>(state.nodes());
        nodes.add(new NodeState(definition, this.data.x(), this.data.y(), Map.of(), Optional.empty()));
        this.serverLevel.setData(ScreenLib.TEST_SCREEN_ATTACHMENT, new CanvasState(nodes, state.connections()));

        this.returnToCanvas(player);
    }

    public void cancel(ServerPlayer player) {
        this.returnToCanvas(player);
    }

    private void returnToCanvas(ServerPlayer player) {
        if (this.serverLevel != null) {
            player.openMenu(new TestScreenMenuProvider(this.serverLevel));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
