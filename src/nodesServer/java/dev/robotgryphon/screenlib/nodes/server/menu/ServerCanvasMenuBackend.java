package dev.robotgryphon.screenlib.nodes.server.menu;

import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasSupplier;
import dev.robotgryphon.screenlib.nodes.server.canvas.ServerCanvasSupplier;
import dev.robotgryphon.screenlib.nodes.server.evaluation.CanvasEvaluator;
import dev.robotgryphon.screenlib.nodes.api.ProvidedNodeReference;
import dev.robotgryphon.screenlib.graph.schema.context.NodeCreationContext;
import dev.robotgryphon.screenlib.graph.schema.properties.PropertyValues;
import dev.robotgryphon.screenlib.graph.schema.properties.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import dev.robotgryphon.screenlib.nodes.api.state.CanvasState;
import dev.robotgryphon.screenlib.nodes.api.state.NodeState;
import dev.robotgryphon.screenlib.nodes.common.sync.AllowedValuesSnapshot;
import dev.robotgryphon.screenlib.nodes.common.sync.CanvasEvaluation;
import dev.robotgryphon.screenlib.nodes.common.sync.ClientDataSubscription;
import dev.robotgryphon.screenlib.nodes.common.network.CanvasEditPayload;
import dev.robotgryphon.screenlib.nodes.common.network.CanvasEvaluationPayload;
import dev.robotgryphon.screenlib.nodes.common.network.CanvasSyncPayload;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import dev.robotgryphon.screenlib.nodes.common.menu.CanvasMenuBackend;

public final class ServerCanvasMenuBackend implements CanvasMenuBackend {
    private static final int EVALUATION_INTERVAL = 2;

    private final ServerCanvasSupplier source;
    private final ServerPlayer player;
    private final AllowedValuesSnapshot allowedValues;
    private final List<ResourceKey<NodeDefinition>> providedNodes;

    private CanvasState state;
    private CanvasEvaluator.Session session;
    private Set<ClientDataSubscription> subscriptions = Set.of();
    private @Nullable CanvasEvaluation lastSent;
    private int ticks;

    private ServerCanvasMenuBackend(ServerCanvasSupplier source, ServerPlayer player) {
        this.source = source;
        this.player = player;
        this.state = source.canvas().state();
        this.allowedValues = AllowedValuesSnapshot.capture(player.registryAccess());
        this.providedNodes = source.canvas().provided().stream().map(ProvidedNodeReference::type).toList();
        this.session = this.openSession();
    }

    public static ServerCanvasMenuBackend of(CanvasSupplier source, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            throw new IllegalArgumentException("Canvas menu backends only run on the server");
        }
        return new ServerCanvasMenuBackend(ServerCanvasSupplier.of(source), serverPlayer);
    }

    private MinecraftServer server() {
        return this.player.level().getServer();
    }

    private CanvasEvaluator.Session openSession() {
        MinecraftServer server = this.server();
        return CanvasEvaluator.open(this.state, server.registryAccess(), server);
    }

    @Override
    public CanvasState state() {
        return this.state;
    }

    @Override
    public AllowedValuesSnapshot allowedValues() {
        return this.allowedValues;
    }

    @Override
    public List<ResourceKey<NodeDefinition>> providedNodes() {
        return this.providedNodes;
    }

    @Override
    public void subscribe(Collection<ClientDataSubscription> subscriptions) {
        this.subscriptions = Set.copyOf(subscriptions);
        this.push(true);
    }

    @Override
    public void applyEdit(CanvasEditPayload edit) {
        MinecraftServer server = this.server();
        Optional<CanvasState> edited = edit.apply(this.source.canvas().state(), server.registryAccess());
        if (edited.isEmpty()) {
            PacketDistributor.sendToPlayer(this.player, new CanvasSyncPayload(this.source.canvas().state()));
            return;
        }
        this.commit(edited.get());
        this.push(true);
    }

    @Override
    public void addNode(ResourceKey<NodeDefinition> type, Vector2ic position) {
        Optional<Holder.Reference<NodeDefinition>> definition = this.player.registryAccess()
                .lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                .get(type)
                .filter(candidate -> !candidate.value().hidden());
        if (definition.isEmpty()) {
            return;
        }
        this.insert(new NodeState(UUID.randomUUID(), definition.get(), new Vector2i(position),
                initialProperties(definition.get(), new NodeCreationContext(this.player), Map.of())));
    }

    @Override
    public void addProvidedNode(ResourceKey<NodeDefinition> type, Vector2ic position) {
        Optional<ProvidedNodeReference> provided = this.source.canvas().provided(type);
        if (provided.isEmpty() || this.source.canvas().state().node(provided.get().id()).isPresent()) {
            PacketDistributor.sendToPlayer(this.player, new CanvasSyncPayload(this.source.canvas().state()));
            return;
        }
        Optional<Holder.Reference<NodeDefinition>> definition = this.player.registryAccess()
                .lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                .get(type);
        if (definition.isEmpty()) {
            return;
        }
        this.insert(new NodeState(provided.get().id(), definition.get(), new Vector2i(position),
                initialProperties(definition.get(), new NodeCreationContext(this.player), provided.get().values())));
    }

    private void insert(NodeState node) {
        CanvasState current = this.source.canvas().state();
        List<NodeState> nodes = new ArrayList<>(current.nodes());
        nodes.add(node);
        this.commit(current.withContents(nodes, current.connections()));
        PacketDistributor.sendToPlayer(this.player, new CanvasSyncPayload(this.state));
        this.push(true);
    }

    @Override
    public void tick() {
        if (++this.ticks % EVALUATION_INTERVAL == 0) {
            this.push(false);
        }
    }

    private void commit(CanvasState edited) {
        this.source.update(edited, this.server());
        this.state = this.source.canvas().state();
        this.session = this.openSession();
    }

    private void push(boolean force) {
        CanvasEvaluation evaluation = this.session.evaluate(this.subscriptions);
        this.source.dispatch(this.session);
        if (force || !evaluation.equals(this.lastSent)) {
            this.lastSent = evaluation;
            PacketDistributor.sendToPlayer(this.player, new CanvasEvaluationPayload(
                    evaluation.validity(), evaluation.computed(), evaluation.clientData()));
        }
    }

    private static PropertyValues initialProperties(Holder<NodeDefinition> definition, NodeCreationContext context,
                                                    Map<String, Object> preset) {
        PropertyValues values = PropertyValues.EMPTY;
        for (var supplier : definition.value().context()) {
            values = apply(values, definition, supplier.value().supply(context));
        }
        return apply(values, definition, preset);
    }

    private static PropertyValues apply(PropertyValues values, Holder<NodeDefinition> definition, Map<String, ?> entries) {
        for (var entry : entries.entrySet()) {
            for (StoredPropertyDefinition<?> property : definition.value().stored()) {
                if (property.name().equals(entry.getKey()) && entry.getValue() != null) {
                    values = values.withChecked(property, entry.getValue());
                }
            }
        }
        return values;
    }
}
