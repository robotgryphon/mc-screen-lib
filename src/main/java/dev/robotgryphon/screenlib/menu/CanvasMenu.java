package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.graph.evaluation.CanvasEvaluation;
import dev.robotgryphon.screenlib.graph.evaluation.CanvasEvaluator;
import dev.robotgryphon.screenlib.graph.schema.AllowedValuesSnapshot;
import dev.robotgryphon.screenlib.network.CanvasEvaluationPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

public abstract class CanvasMenu extends AbstractContainerMenu {
    private static final int EVALUATION_INTERVAL = 2;

    private final @Nullable CanvasSource source;
    private final @Nullable ServerPlayer player;
    private final AllowedValuesSnapshot allowedValues;

    private CanvasState state;
    private CanvasEvaluator.@Nullable Session session;
    private @Nullable CanvasEvaluation lastSent;
    private int sequence;
    private int ticks;

    protected CanvasMenu(MenuType<?> type, int containerId, Inventory inv, CanvasSource source) {
        super(type, containerId);
        this.source = source;
        this.player = inv.player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        CanvasState loaded = source.load();
        this.state = HostedNodes.reconcile(loaded, source.hostedNodes(), source.level().registryAccess());
        if (!this.state.equals(loaded)) {
            source.save(this.state);
        }
        this.allowedValues = AllowedValuesSnapshot.capture(source.level().registryAccess());
    }

    protected CanvasMenu(MenuType<?> type, int containerId, RegistryFriendlyByteBuf data) {
        super(type, containerId);
        this.source = null;
        this.player = null;
        this.state = ByteBufCodecs.fromCodecWithRegistries(CanvasState.CODEC).decode(data);
        this.allowedValues = AllowedValuesSnapshot.STREAM_CODEC.decode(data);
    }

    public static void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        CanvasMenu canvasMenu = (CanvasMenu) menu;
        ByteBufCodecs.fromCodecWithRegistries(CanvasState.CODEC).encode(buffer, canvasMenu.state());
        AllowedValuesSnapshot.STREAM_CODEC.encode(buffer, canvasMenu.allowedValues());
    }

    public @Nullable CanvasSource source() {
        return this.source;
    }

    public AllowedValuesSnapshot allowedValues() {
        return this.allowedValues;
    }

    public CanvasState state() {
        return this.state;
    }

    public void updateState(CanvasState newState, int sequence) {
        this.sequence = sequence;
        if (this.source == null) {
            this.state = newState;
            return;
        }
        var level = this.source.level();
        this.state = HostedNodes.reconcile(newState, this.source.hostedNodes(), level.registryAccess());
        this.source.save(this.state);
        HostedNodes.publish(this.source);
        this.session = CanvasEvaluator.open(this.state, level.registryAccess(), level.getServer());
        this.push(true);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (this.session != null && ++this.ticks % EVALUATION_INTERVAL == 0) {
            this.push(false);
        }
    }

    private void push(boolean force) {
        if (this.session == null || this.player == null) {
            return;
        }
        CanvasEvaluation evaluation = this.session.evaluate();
        if (force || !evaluation.equals(this.lastSent)) {
            this.lastSent = evaluation;
            PacketDistributor.sendToPlayer(this.player, new CanvasEvaluationPayload(this.sequence,
                    evaluation.validity(), evaluation.computed(), evaluation.clientData()));
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
