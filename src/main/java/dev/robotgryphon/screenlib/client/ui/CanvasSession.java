package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.client.appearance.NodeAppearances;
import dev.robotgryphon.screenlib.graph.canvas.client.CanvasViewports;
import dev.robotgryphon.screenlib.graph.canvas.client.ClientCanvasState;
import dev.robotgryphon.screenlib.graph.canvas.client.ViewportState;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.state.CanvasState;
import dev.robotgryphon.screenlib.graph.sync.NodeClientData;
import dev.robotgryphon.screenlib.graph.sync.NodeComputed;
import dev.robotgryphon.screenlib.graph.sync.NodeValidity;
import dev.robotgryphon.screenlib.menu.CanvasMenu;
import dev.robotgryphon.screenlib.network.CanvasEvaluationPayload;
import dev.robotgryphon.screenlib.network.AddNodePayload;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import org.joml.Vector2i;
import dev.robotgryphon.screenlib.network.SubscribeClientDataPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector2dc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CanvasSession {
    private final CanvasMenu menu;
    private final ClientCanvasState canvas;

    public CanvasSession(CanvasMenu menu) {
        this.menu = menu;
        this.canvas = new ClientCanvasState(menu.allowedValues().resolve());
        this.canvas.setRequired(menu.requiredNodes());
        this.canvas.load(menu.state());
        this.canvas.onEdit(ClientPacketDistributor::sendToServer);
        ClientPacketDistributor.sendToServer(new SubscribeClientDataPayload(NodeAppearances.subscriptions()));
    }

    public CanvasMenu menu() {
        return this.menu;
    }

    public ClientCanvasState canvas() {
        return this.canvas;
    }

    public Optional<ViewportState> savedViewport() {
        return CanvasViewports.get(this.canvas.id());
    }

    public void rememberViewport(ViewportState viewport) {
        CanvasViewports.remember(this.canvas.id(), viewport);
    }

    public void addNode(ResourceKey<NodeDefinition> type, Vector2dc canvasPos) {
        ClientPacketDistributor.sendToServer(new AddNodePayload(type,
                new Vector2i(Mth.floor(canvasPos.x()), Mth.floor(canvasPos.y()))));
    }

    public void applySync(CanvasState state) {
        this.canvas.load(state);
    }

    public void applyEvaluation(CanvasEvaluationPayload payload) {
        List<Node> nodes = this.canvas.graph().nodes();
        Map<UUID, Node> byId = new HashMap<>();
        for (Node node : nodes) {
            byId.put(node.id(), node);
        }

        Map<UUID, List<String>> results = new HashMap<>();
        for (NodeValidity validity : payload.validity()) {
            if (byId.containsKey(validity.node())) {
                results.put(validity.node(), validity.errors());
            }
        }
        this.canvas.setValidity(results);

        for (Node node : nodes) {
            node.propertyManager().clearComputed();
        }
        Map<UUID, Map<String, Object>> clientData = new HashMap<>();
        for (NodeClientData data : payload.clientData()) {
            Node node = byId.get(data.node());
            if (node == null) {
                continue;
            }
            for (ComputedPropertyDefinition<?> definition : node.propertyManager().computedProperties()) {
                if (!definition.name().equals(data.property())) {
                    continue;
                }
                definition.function().clientDataProvider().ifPresent(provider -> {
                    var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data.data()),
                            Minecraft.getInstance().level.registryAccess());
                    clientData.computeIfAbsent(node.id(), ignored -> new HashMap<>())
                            .put(definition.name(), provider.clientCodec().decode(buffer));
                });
            }
        }
        this.canvas.setClientData(clientData);
        for (NodeComputed computed : payload.computed()) {
            Node node = byId.get(computed.node());
            if (node != null && node.definition().value() == computed.definition().value()) {
                node.propertyManager().loadComputed(computed.values());
            }
        }
    }
}
