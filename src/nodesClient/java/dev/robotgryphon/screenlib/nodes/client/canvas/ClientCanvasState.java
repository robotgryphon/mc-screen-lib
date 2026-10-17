package dev.robotgryphon.screenlib.nodes.client.canvas;

import dev.robotgryphon.screenlib.graph.schema.properties.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.nodes.common.Connection;
import dev.robotgryphon.screenlib.nodes.common.ports.Port;
import dev.robotgryphon.screenlib.nodes.api.state.ConnectionState;
import dev.robotgryphon.screenlib.nodes.common.network.CanvasEditPayload;
import dev.robotgryphon.screenlib.nodes.common.network.ConnectNodesPayload;
import dev.robotgryphon.screenlib.nodes.common.network.DisconnectNodesPayload;
import dev.robotgryphon.screenlib.nodes.common.network.MoveNodePayload;
import dev.robotgryphon.screenlib.nodes.common.network.RemoveNodePayload;
import dev.robotgryphon.screenlib.nodes.common.network.SetNodePropertyPayload;
import dev.robotgryphon.screenlib.nodes.common.canvas.CanvasGraph;
import dev.robotgryphon.screenlib.nodes.common.Node;
import dev.robotgryphon.screenlib.nodes.common.properties.AllowedValues;
import dev.robotgryphon.screenlib.nodes.api.state.CanvasState;
import dev.robotgryphon.screenlib.nodes.api.state.NodeState;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class ClientCanvasState {
    private static final Vector2ic ORIGIN = new Vector2i();

    private final CanvasGraph graph;
    private final Map<UUID, Vector2i> positions = new HashMap<>();
    private final Map<UUID, List<String>> validity = new HashMap<>();
    private final Map<UUID, Map<String, Object>> clientData = new HashMap<>();
    private final Map<UUID, String> focusedProperties = new HashMap<>();
    private Set<UUID> required = Set.of();
    private UUID id = UUID.randomUUID();
    private Consumer<CanvasEditPayload> edits = edit -> {};

    public ClientCanvasState(AllowedValues allowedValues) {
        this.graph = new CanvasGraph(allowedValues);
        this.graph.onPropertyChanged(this::propertyChanged);
    }

    public void onEdit(Consumer<CanvasEditPayload> edits) {
        this.edits = edits;
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
        this.graph.load(state);
        for (NodeState node : state.nodes()) {
            this.positions.put(node.id(), new Vector2i(node.position()));
        }
    }

    public void connect(Port from, Port to) {
        if (this.graph.connections().connect(from, to)) {
            this.edits.accept(new ConnectNodesPayload(new ConnectionState(
                    from.node().id(), from.propertyName(), to.node().id(), to.propertyName())));
        }
    }

    public void disconnect(Connection connection) {
        this.graph.connections().remove(connection);
        this.edits.accept(new DisconnectNodesPayload(new ConnectionState(
                connection.source().id(), connection.sourcePort().propertyName(),
                connection.target().id(), connection.targetPort().propertyName())));
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
        this.edits.accept(new RemoveNodePayload(node.id()));
    }

    public void commitPosition(Node node) {
        this.edits.accept(new MoveNodePayload(node.id(), new Vector2i(this.position(node))));
    }

    private <T> void propertyChanged(Node node, StoredPropertyDefinition<T> property, Optional<T> value) {
        this.edits.accept(new SetNodePropertyPayload<>(node.id(), node.definition(), property, value));
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
