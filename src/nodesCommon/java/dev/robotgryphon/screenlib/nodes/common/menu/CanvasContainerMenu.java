package dev.robotgryphon.screenlib.nodes.common.menu;

import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasMenu;
import dev.robotgryphon.screenlib.nodes.api.state.CanvasState;
import dev.robotgryphon.screenlib.nodes.common.sync.AllowedValuesSnapshot;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class CanvasContainerMenu extends CanvasMenu {
    private final @Nullable CanvasMenuBackend backend;
    private final CanvasState state;
    private final AllowedValuesSnapshot allowedValues;
    private final List<UUID> requiredNodes;
    private final Predicate<Player> stillValid;

    public CanvasContainerMenu(MenuType<?> type, int containerId, CanvasMenuBackend backend, Predicate<Player> stillValid) {
        super(type, containerId);
        this.stillValid = stillValid;
        this.backend = backend;
        this.state = backend.state();
        this.allowedValues = backend.allowedValues();
        this.requiredNodes = backend.requiredNodes();
    }

    public CanvasContainerMenu(MenuType<?> type, int containerId, RegistryFriendlyByteBuf data) {
        super(type, containerId);
        this.stillValid = player -> true;
        this.backend = null;
        this.state = CanvasState.STREAM_CODEC.decode(data);
        this.allowedValues = AllowedValuesSnapshot.STREAM_CODEC.decode(data);
        this.requiredNodes = UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(data);
    }

    public static void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        CanvasContainerMenu canvasMenu = (CanvasContainerMenu) menu;
        CanvasState.STREAM_CODEC.encode(buffer, canvasMenu.state());
        AllowedValuesSnapshot.STREAM_CODEC.encode(buffer, canvasMenu.allowedValues());
        UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, canvasMenu.requiredNodes());
    }

    public Optional<CanvasMenuBackend> backend() {
        return Optional.ofNullable(this.backend);
    }

    public List<UUID> requiredNodes() {
        return this.requiredNodes;
    }

    public AllowedValuesSnapshot allowedValues() {
        return this.allowedValues;
    }

    public CanvasState state() {
        return this.backend != null ? this.backend.state() : this.state;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (this.backend != null) {
            this.backend.tick();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.stillValid.test(player);
    }
}
