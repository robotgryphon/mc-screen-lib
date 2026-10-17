package dev.robotgryphon.screenlib.nodes.server.canvas;

import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasSupplier;
import dev.robotgryphon.screenlib.nodes.api.state.CanvasState;

import dev.robotgryphon.screenlib.graph.schema.properties.DataKey;
import dev.robotgryphon.screenlib.nodes.server.evaluation.CanvasEvaluator;
import dev.robotgryphon.screenlib.nodes.server.evaluation.NodeSnapshot;
import dev.robotgryphon.screenlib.nodes.api.NodeReference;
import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.nodes.api.RequiredNodeReference;
import dev.robotgryphon.screenlib.graph.schema.properties.PropertyValues;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import dev.robotgryphon.screenlib.nodes.api.state.NodeState;
import org.joml.Vector2i;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasDefinition;

public final class ServerCanvasSupplier implements CanvasSupplier {
    private static final Object UNSET = new Object();
    private static final int NEW_NODE_OFFSET = 24;

    private final CanvasDefinition canvas;
    private final List<Watch<?>> watches = new ArrayList<>();
    private Runnable onChanged = () -> {};

    public ServerCanvasSupplier(CanvasDefinition canvas) {
        this.canvas = canvas;
    }

    public static ServerCanvasSupplier of(CanvasSupplier supplier) {
        if (supplier instanceof ServerCanvasSupplier server) {
            return server;
        }
        throw new IllegalArgumentException("Unsupported canvas supplier: " + supplier);
    }

    @Override
    public CanvasDefinition canvas() {
        return this.canvas;
    }

    @Override
    public <T> ServerCanvasSupplier watch(NodeReference node, DataKey<T> key, Consumer<Optional<T>> listener) {
        this.watches.add(new Watch<>(node, state -> state
                .filter(NodeSnapshot::valid)
                .map(values -> values.values().get(key.name()))
                .filter(value -> key.type().get().dataType().isInstance(value))
                .map(value -> key.type().get().dataType().cast(value)), listener));
        return this;
    }

    @Override
    public ServerCanvasSupplier watchValidity(NodeReference node, Consumer<List<String>> listener) {
        this.watches.add(new Watch<>(node, state -> state.map(NodeSnapshot::errors).orElse(null), listener));
        return this;
    }

    @Override
    public ServerCanvasSupplier onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public CanvasState reconcile(RegistryAccess registries) {
        if (this.reconcileRequired(registries)) {
            this.onChanged.run();
        }
        return this.canvas.state();
    }

    private boolean reconcileRequired(RegistryAccess registries) {
        CanvasState state = this.canvas.state();
        var definitions = registries.lookupOrThrow(NodeDefinition.REGISTRY_KEY);
        Set<UUID> present = state.nodes().stream().map(NodeState::id).collect(Collectors.toSet());
        List<NodeState> nodes = new ArrayList<>(state.nodes());
        for (RequiredNodeReference required : this.canvas.required()) {
            if (present.contains(required.id())) {
                continue;
            }

            var definition = definitions.get(required.type());
            if (definition.isEmpty()) {
                ScreenLib.LOGGER.error("Required node uses unknown node type {}", required.type().identifier());
                continue;
            }

            int offset = (nodes.size() - state.nodes().size()) * NEW_NODE_OFFSET;
            nodes.add(new NodeState(required.id(), definition.get(), new Vector2i(offset, offset), PropertyValues.EMPTY));
        }

        if (nodes.size() == state.nodes().size()) {
            return false;
        }

        this.canvas.setState(state.withContents(nodes, state.connections()));
        return true;
    }

    public void update(CanvasState state, MinecraftServer server) {
        this.canvas.setState(this.canvas.state().withContents(state.nodes(), state.connections()));
        this.reconcileRequired(server.registryAccess());
        this.onChanged.run();
        this.refresh(server);
    }

    @Override
    public void refresh(MinecraftServer server) {
        if (this.watches.isEmpty()) {
            return;
        }
        var session = CanvasEvaluator.open(this.reconcile(server.registryAccess()), server.registryAccess(), server);
        session.compute();
        this.dispatch(session);
    }

    public void dispatch(CanvasEvaluator.Session session) {
        for (Watch<?> watch : this.watches) {
            watch.dispatch(session);
        }
    }

    private static final class Watch<T> {
        private final NodeReference node;
        private final Function<Optional<NodeSnapshot>, @Nullable T> extract;
        private final Consumer<T> listener;
        private @Nullable Object last = UNSET;

        private Watch(NodeReference node, Function<Optional<NodeSnapshot>, @Nullable T> extract, Consumer<T> listener) {
            this.node = node;
            this.extract = extract;
            this.listener = listener;
        }

        private void dispatch(CanvasEvaluator.Session session) {
            T value = this.extract.apply(session.snapshot(this.node));
            if (value == null || Objects.equals(value, this.last)) {
                return;
            }
            this.last = value;
            this.listener.accept(value);
        }
    }
}
