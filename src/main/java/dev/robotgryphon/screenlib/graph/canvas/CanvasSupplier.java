package dev.robotgryphon.screenlib.graph.canvas;

import dev.robotgryphon.screenlib.graph.state.CanvasState;

import dev.robotgryphon.screenlib.graph.nodes.properties.DataKey;
import dev.robotgryphon.screenlib.graph.evaluation.CanvasEvaluator;
import dev.robotgryphon.screenlib.graph.evaluation.NodeSnapshot;
import dev.robotgryphon.screenlib.graph.nodes.NodeReference;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class CanvasSupplier {
    private static final Object UNSET = new Object();

    private final CanvasDefinition canvas;
    private final List<Watch<?>> watches = new ArrayList<>();
    private Runnable onChanged = () -> {};

    private CanvasSupplier(CanvasDefinition canvas) {
        this.canvas = canvas;
    }

    public static CanvasSupplier of(CanvasDefinition canvas) {
        return new CanvasSupplier(canvas);
    }

    public CanvasDefinition canvas() {
        return this.canvas;
    }

    public <T> CanvasSupplier watch(NodeReference node, DataKey<T> key, Consumer<Optional<T>> listener) {
        this.watches.add(new Watch<>(node, state -> state
                .filter(NodeSnapshot::valid)
                .map(values -> values.values().get(key.name()))
                .filter(value -> key.type().get().dataType().isInstance(value))
                .map(value -> key.type().get().dataType().cast(value)), listener));
        return this;
    }

    public CanvasSupplier watchValidity(NodeReference node, Consumer<List<String>> listener) {
        this.watches.add(new Watch<>(node, state -> state.map(NodeSnapshot::errors).orElse(null), listener));
        return this;
    }

    public CanvasSupplier onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public CanvasState reconcile(RegistryAccess registries) {
        if (this.canvas.reconcile(registries)) {
            this.onChanged.run();
        }
        return this.canvas.state();
    }

    public void update(CanvasState state, MinecraftServer server) {
        this.canvas.setState(this.canvas.state().withContents(state.nodes(), state.connections()));
        this.canvas.reconcile(server.registryAccess());
        this.onChanged.run();
        this.refresh(server);
    }

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
