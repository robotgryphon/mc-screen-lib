package dev.robotgryphon.screenlib.graph.canvas.client;

import dev.robotgryphon.screenlib.graph.canvas.CanvasGraph;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.properties.AllowedValues;
import dev.robotgryphon.screenlib.graph.state.CanvasState;
import dev.robotgryphon.screenlib.graph.state.NodeState;
import dev.robotgryphon.screenlib.graph.sync.CanvasStateManager;
import net.minecraft.core.RegistryAccess;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ClientCanvasState {
    private static final Vector2ic ORIGIN = new Vector2i();

    private final CanvasGraph graph;
    private final Map<UUID, Vector2i> positions = new HashMap<>();
    private final Map<UUID, List<String>> validity = new HashMap<>();
    private final Map<UUID, Map<String, Object>> clientData = new HashMap<>();
    private final Map<UUID, String> focusedProperties = new HashMap<>();
    private Set<UUID> required = Set.of();
    private UUID id = UUID.randomUUID();

    public ClientCanvasState(AllowedValues allowedValues) {
        this.graph = new CanvasGraph(allowedValues);
    }

    public CanvasGraph graph() {
        return this.graph;
    }

    public UUID id() {
        return this.id;
    }

    public void load(CanvasState state) {
        this.id = state.id();
        this.positions.clear();
        this.validity.clear();
        this.clientData.clear();
        this.focusedProperties.clear();
        CanvasStateManager.loadState(this.graph, state);
        for (NodeState node : state.nodes()) {
            this.positions.put(node.id(), new Vector2i(node.position()));
        }
    }

    public CanvasState toState(RegistryAccess registries) {
        return CanvasStateManager.toState(this.id, this.graph, this::position, registries);
    }

    public void removeNode(Node node) {
        if (this.isRequired(node)) {
            return;
        }
        this.graph.removeNode(node);
        this.positions.remove(node.id());
        this.validity.remove(node.id());
        this.clientData.remove(node.id());
        this.focusedProperties.remove(node.id());
    }

    public Vector2ic position(Node node) {
        return this.positions.getOrDefault(node.id(), new Vector2i(ORIGIN));
    }

    public int x(Node node) {
        return this.position(node).x();
    }

    public int y(Node node) {
        return this.position(node).y();
    }

    public void place(Node node, int x, int y) {
        this.positions.put(node.id(), new Vector2i(x, y));
    }

    public boolean isRequired(Node node) {
        return this.required.contains(node.id());
    }

    public void setRequired(Collection<UUID> required) {
        this.required = Set.copyOf(required);
    }

    public boolean isInvalid(Node node) {
        return this.validity.containsKey(node.id());
    }

    public void setValidity(Map<UUID, List<String>> results) {
        this.validity.clear();
        this.validity.putAll(results);
    }

    public @Nullable Object clientData(Node node, String property) {
        return this.clientData.getOrDefault(node.id(), Map.of()).get(property);
    }

    public void setClientData(Map<UUID, Map<String, Object>> data) {
        this.clientData.clear();
        this.clientData.putAll(data);
    }

    public @Nullable String focusedProperty(Node node) {
        return this.focusedProperties.get(node.id());
    }

    public void setFocusedProperty(Node node, @Nullable String property) {
        if (property == null) {
            this.focusedProperties.remove(node.id());
        } else {
            this.focusedProperties.put(node.id(), property);
        }
    }
}
