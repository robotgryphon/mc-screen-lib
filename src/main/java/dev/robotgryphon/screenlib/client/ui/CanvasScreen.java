package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.graph.state.CanvasState;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.sync.NodeComputed;
import dev.robotgryphon.screenlib.graph.sync.NodeClientData;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import dev.robotgryphon.screenlib.network.CanvasEvaluationPayload;
import dev.robotgryphon.screenlib.graph.sync.NodeValidity;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.client.ui.widget.CanvasWidget;
import dev.robotgryphon.screenlib.graph.canvas.client.ClientCanvasState;
import dev.robotgryphon.screenlib.graph.canvas.client.CanvasViewports;
import dev.robotgryphon.screenlib.graph.canvas.client.ViewportState;
import dev.robotgryphon.screenlib.menu.CanvasMenu;
import dev.robotgryphon.screenlib.network.RequestAddNodePayload;
import dev.robotgryphon.screenlib.network.SubscribeClientDataPayload;
import dev.robotgryphon.screenlib.client.appearance.NodeAppearances;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector2dc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CanvasScreen<T extends CanvasMenu> extends Screen implements MenuAccess<T> {
    private final T menu;
    private final ClientCanvasState canvas;
    private Optional<ViewportState> viewport;
    private @Nullable CanvasWidget canvasWidget;

    public CanvasScreen(T menu, Inventory unused, Component title) {
        super(title);
        this.menu = menu;
        this.canvas = new ClientCanvasState(menu.allowedValues().resolve());
        this.viewport = CanvasViewports.get(menu.state().id());
        ClientPacketDistributor.sendToServer(new SubscribeClientDataPayload(NodeAppearances.subscriptions()));
        this.initCanvas();
    }

    @Override
    public @NonNull T getMenu() {
        return this.menu;
    }

    private void initCanvas() {
        this.canvas.setRequired(this.menu.requiredNodes());
        this.canvas.load(this.menu.state());
        this.canvas.onEdit(ClientPacketDistributor::sendToServer);
    }

    public void applySync(CanvasState state) {
        this.canvas.load(state);
    }

    @Override
    protected void init() {
        super.init();
        this.captureViewport();
        this.canvasWidget = this.addRenderableWidget(new CanvasWidget(
                this.canvas, 0, 0, this.width, this.height,
                this::openAddNodeDialog));
        this.viewport.ifPresentOrElse(state -> this.canvasWidget.viewport().apply(state, this.width, this.height),
                () -> this.canvasWidget.viewport().pan((float) -this.width / 2, 0));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void captureViewport() {
        if (this.canvasWidget != null) {
            ViewportState captured = this.canvasWidget.viewport().state(
                    this.canvasWidget.getWidth(), this.canvasWidget.getHeight());
            this.viewport = Optional.of(captured);
            CanvasViewports.remember(this.canvas.id(), captured);
        }
    }

    @Override
    public void removed() {
        this.captureViewport();
        super.removed();
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

    @Override
    public void onClose() {
        if (this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        } else {
            super.onClose();
        }
    }

    private void openAddNodeDialog(Vector2dc canvasPos) {
        ClientPacketDistributor.sendToServer(new RequestAddNodePayload((int) canvasPos.x(), (int) canvasPos.y()));
    }
}
