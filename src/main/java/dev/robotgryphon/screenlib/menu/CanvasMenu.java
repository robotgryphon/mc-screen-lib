package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.graph.canvas.CanvasSupplier;
import dev.robotgryphon.screenlib.graph.nodes.NodeReference;
import dev.robotgryphon.screenlib.graph.state.CanvasState;
import dev.robotgryphon.screenlib.graph.state.NodeState;
import dev.robotgryphon.screenlib.graph.nodes.context.NodeCreationContext;
import dev.robotgryphon.screenlib.graph.nodes.properties.PropertyValues;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import dev.robotgryphon.screenlib.graph.sync.CanvasEvaluation;
import dev.robotgryphon.screenlib.graph.evaluation.CanvasEvaluator;
import dev.robotgryphon.screenlib.graph.sync.ClientDataSubscription;
import dev.robotgryphon.screenlib.graph.sync.AllowedValuesSnapshot;
import dev.robotgryphon.screenlib.network.CanvasEditPayload;
import dev.robotgryphon.screenlib.network.CanvasEvaluationPayload;
import dev.robotgryphon.screenlib.network.CanvasSyncPayload;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public abstract class CanvasMenu extends AbstractContainerMenu {
    private static final int EVALUATION_INTERVAL = 2;

    private final @Nullable CanvasSupplier source;
    private final @Nullable MenuProvider provider;
    private final @Nullable ServerPlayer player;
    private final AllowedValuesSnapshot allowedValues;
    private final List<UUID> requiredNodes;

    private CanvasState state;
    private Set<ClientDataSubscription> subscriptions = Set.of();
    private CanvasEvaluator.@Nullable Session session;
    private @Nullable CanvasEvaluation lastSent;
    private int ticks;

    protected CanvasMenu(MenuType<?> type, int containerId, Inventory inv, CanvasSupplier source,
                         MenuProvider provider) {
        super(type, containerId);
        this.source = source;
        this.provider = provider;
        this.player = inv.player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        this.state = source.reconcile(inv.player.registryAccess());
        this.allowedValues = AllowedValuesSnapshot.capture(inv.player.registryAccess());
        this.requiredNodes = source.canvas().required().stream().map(NodeReference::id).toList();
        if (this.player != null) {
            var server = this.player.level().getServer();
            this.session = CanvasEvaluator.open(this.state, server.registryAccess(), server);
        }
    }

    protected CanvasMenu(MenuType<?> type, int containerId, RegistryFriendlyByteBuf data) {
        super(type, containerId);
        this.source = null;
        this.provider = null;
        this.player = null;
        this.state = CanvasState.STREAM_CODEC.decode(data);
        this.allowedValues = AllowedValuesSnapshot.STREAM_CODEC.decode(data);
        this.requiredNodes = UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(data);
    }

    public static void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        CanvasMenu canvasMenu = (CanvasMenu) menu;
        CanvasState.STREAM_CODEC.encode(buffer, canvasMenu.state());
        AllowedValuesSnapshot.STREAM_CODEC.encode(buffer, canvasMenu.allowedValues());
        UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, canvasMenu.requiredNodes());
    }

    public List<UUID> requiredNodes() {
        return this.requiredNodes;
    }

    public @Nullable CanvasSupplier source() {
        return this.source;
    }

    public @Nullable MenuProvider provider() {
        return this.provider;
    }

    public AllowedValuesSnapshot allowedValues() {
        return this.allowedValues;
    }

    public CanvasState state() {
        return this.state;
    }

    public void subscribe(Collection<ClientDataSubscription> subscriptions) {
        this.subscriptions = Set.copyOf(subscriptions);
        this.push(true);
    }

    public void applyEdit(CanvasEditPayload edit) {
        if (this.source == null || this.player == null) {
            return;
        }
        var server = this.player.level().getServer();
        Optional<CanvasState> edited = edit.apply(this.source.canvas().state(), server.registryAccess(),
                Set.copyOf(this.requiredNodes));
        if (edited.isEmpty()) {
            PacketDistributor.sendToPlayer(this.player, new CanvasSyncPayload(this.source.canvas().state()));
            return;
        }
        this.source.update(edited.get(), server);
        this.state = this.source.canvas().state();
        this.session = CanvasEvaluator.open(this.state, server.registryAccess(), server);
        this.push(true);
    }

    public void addNode(ServerPlayer player, ResourceKey<NodeDefinition> type, Vector2ic position) {
        if (this.source == null) {
            return;
        }
        Optional<Holder.Reference<NodeDefinition>> definition = player.registryAccess()
                .lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                .get(type)
                .filter(candidate -> !candidate.value().hidden());
        if (definition.isEmpty()) {
            return;
        }
        var server = player.level().getServer();
        CanvasState current = this.source.canvas().state();
        List<NodeState> nodes = new ArrayList<>(current.nodes());
        nodes.add(new NodeState(UUID.randomUUID(), definition.get(), new Vector2i(position),
                initialProperties(definition.get(), new NodeCreationContext(player, this.source))));
        this.source.update(current.withContents(nodes, current.connections()), server);
        this.state = this.source.canvas().state();
        this.session = CanvasEvaluator.open(this.state, server.registryAccess(), server);
        PacketDistributor.sendToPlayer(player, new CanvasSyncPayload(this.state));
        this.push(true);
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
        CanvasEvaluation evaluation = this.session.evaluate(this.subscriptions);
        if (this.source != null) {
            this.source.dispatch(this.session);
        }
        if (force || !evaluation.equals(this.lastSent)) {
            this.lastSent = evaluation;
            PacketDistributor.sendToPlayer(this.player, new CanvasEvaluationPayload(
                    evaluation.validity(), evaluation.computed(), evaluation.clientData()));
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
