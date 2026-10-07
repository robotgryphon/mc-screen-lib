package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.graph.evaluation.CanvasEvaluation;
import dev.robotgryphon.screenlib.graph.evaluation.CanvasEvaluator;
import dev.robotgryphon.screenlib.network.CanvasEvaluationPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import dev.robotgryphon.screenlib.graph.schema.AllowedValuesSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class TestScreenMenu extends AbstractContainerMenu {
    private final @Nullable Level serverLevel;

    private static final int EVALUATION_INTERVAL = 2;

    private CanvasState state;
    private final @Nullable ServerPlayer player;
    private CanvasEvaluator.@Nullable Session session;
    private @Nullable CanvasEvaluation lastSent;
    private int sequence;
    private int ticks;
    private final AllowedValuesSnapshot allowedValues;

    public TestScreenMenu(int containerId, Inventory inv, Level level) {
        super(ScreenLib.TEST_SCREEN_MENU.get(), containerId);
        this.serverLevel = level;
        this.player = inv.player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        this.state = level.getData(ScreenLib.TEST_SCREEN_ATTACHMENT);
        this.allowedValues = AllowedValuesSnapshot.capture(level.registryAccess());
    }

    public TestScreenMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf data) {
        super(ScreenLib.TEST_SCREEN_MENU.get(), containerId);
        this.serverLevel = null;
        this.player = null;
        this.state = ByteBufCodecs.fromCodecWithRegistries(CanvasState.CODEC).decode(data);
        this.allowedValues = AllowedValuesSnapshot.STREAM_CODEC.decode(data);
    }

    public AllowedValuesSnapshot allowedValues() {
        return this.allowedValues;
    }

    public CanvasState state() {
        return state;
    }

    public void updateState(CanvasState newState, int sequence) {
        this.state = newState;
        this.sequence = sequence;
        if (this.serverLevel != null) {
            this.serverLevel.setData(ScreenLib.TEST_SCREEN_ATTACHMENT, newState);
            this.session = CanvasEvaluator.open(newState, this.serverLevel.registryAccess(), this.serverLevel.getServer());
            this.push(true);
        }
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
                    evaluation.validity(), evaluation.computed(), evaluation.rendered()));
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
