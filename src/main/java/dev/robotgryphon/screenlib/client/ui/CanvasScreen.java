package dev.robotgryphon.screenlib.client.ui;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.properties.PropertyManager;
import dev.robotgryphon.screenlib.graph.sync.NodeComputed;
import dev.robotgryphon.screenlib.graph.sync.NodeClientData;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;
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
import dev.robotgryphon.screenlib.network.UpdateCanvasStatePayload;
import dev.robotgryphon.screenlib.client.appearance.NodeAppearances;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector2dc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class CanvasScreen<T extends CanvasMenu> extends Screen implements MenuAccess<T> {
    private final T menu;
    private final ClientCanvasState canvas;
    private int sentRevision = -1;
    private Optional<ViewportState> viewport;
    private @Nullable CanvasWidget canvasWidget;
    private int sentSequence;

    public CanvasScreen(T menu, Inventory unused, Component title) {
        super(title);
        this.menu = menu;
        this.canvas = new ClientCanvasState(menu.allowedValues().resolve(Minecraft.getInstance().level.registryAccess()));
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
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> this.sendState())
                .bounds(this.width - 60, 6, 54, 20)
                .build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.canvas.graph().revision() != this.sentRevision) {
            this.sendState();
        }
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

    private void sendState() {
        assert minecraft.level != null;
        this.captureViewport();
        this.sentRevision = this.canvas.graph().revision();
        this.sentSequence++;
        final var regAccess = minecraft.level.registryAccess();
        ClientPacketDistributor.sendToServer(new UpdateCanvasStatePayload(
                this.canvas.toState(regAccess), this.sentSequence));
    }

    @SuppressWarnings("unchecked")
    public void applyEvaluation(CanvasEvaluationPayload payload) {
        if (payload.sequence() != this.sentSequence) {
            return;
        }
        List<Node> nodes = this.canvas.graph().nodes();

        Map<UUID, List<String>> results = new HashMap<>();
        for (NodeValidity validity : payload.validity()) {
            if (validity.nodeIndex() >= 0 && validity.nodeIndex() < nodes.size()) {
                results.put(nodes.get(validity.nodeIndex()).id(), validity.errors());
            }
        }
        this.canvas.setValidity(results);

        var ops = Minecraft.getInstance().level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (Node node : nodes) {
            node.propertyManager().clearComputed();
        }
        Map<UUID, Map<String, Object>> clientData = new HashMap<>();
        for (NodeClientData data : payload.clientData()) {
            if (data.nodeIndex() < 0 || data.nodeIndex() >= nodes.size()) {
                continue;
            }
            Node node = nodes.get(data.nodeIndex());
            for (ComputedPropertyDefinition definition : node.propertyManager().computedProperties()) {
                if (!definition.name().equals(data.property())) {
                    continue;
                }
                definition.clientData().ifPresent(provider -> {
                    var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data.data()),
                            Minecraft.getInstance().level.registryAccess());
                    clientData.computeIfAbsent(node.id(), ignored -> new HashMap<>())
                            .put(definition.name(), provider.clientCodec().decode(buffer));
                });
            }
        }
        this.canvas.setClientData(clientData);
        for (NodeComputed computed : payload.computed()) {
            if (computed.nodeIndex() < 0 || computed.nodeIndex() >= nodes.size()) {
                continue;
            }
            PropertyManager properties = nodes.get(computed.nodeIndex()).propertyManager();
            for (ComputedPropertyDefinition definition : properties.computedProperties()) {
                Tag tag = computed.values().get(definition.name());
                if (tag == null) {
                    continue;
                }
                Codec<Object> codec = (Codec<Object>) definition.type().value().codec();
                codec.parse(ops, tag).result().ifPresent(value -> properties.setComputedValue(definition.name(), value));
            }
        }
    }

    @Override
    public void onClose() {
        this.sendState();

        if (this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        } else {
            super.onClose();
        }
    }

    private void openAddNodeDialog(Vector2dc canvasPos) {
        this.sendState();
        ClientPacketDistributor.sendToServer(new RequestAddNodePayload((int) canvasPos.x(), (int) canvasPos.y()));
    }
}
