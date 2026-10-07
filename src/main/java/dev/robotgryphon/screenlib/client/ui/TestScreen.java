package dev.robotgryphon.screenlib.client.ui;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import dev.robotgryphon.screenlib.graph.schema.node.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.PropertyManager;
import dev.robotgryphon.screenlib.graph.evaluation.NodeComputed;
import dev.robotgryphon.screenlib.graph.evaluation.NodeRendered;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.List;
import java.util.IdentityHashMap;
import dev.robotgryphon.screenlib.network.CanvasEvaluationPayload;
import dev.robotgryphon.screenlib.graph.validation.NodeValidity;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.client.ui.widget.CanvasWidget;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.CanvasStateManager;
import dev.robotgryphon.screenlib.menu.TestScreenMenu;
import dev.robotgryphon.screenlib.network.RequestAddNodePayload;
import dev.robotgryphon.screenlib.network.UpdateCanvasStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector2dc;

public class TestScreen extends Screen implements MenuAccess<TestScreenMenu> {
    private final TestScreenMenu menu;
    private final Canvas canvas;
    private int sentRevision = -1;
    private int sentSequence;

    public TestScreen(TestScreenMenu menu, Inventory unused, Component title) {
        super(title);
        this.menu = menu;
        this.canvas = new Canvas(menu.allowedValues().resolve(Minecraft.getInstance().level.registryAccess()));
        this.initCanvas();
    }

    @Override
    public TestScreenMenu getMenu() {
        return this.menu;
    }

    private void initCanvas() {
        CanvasStateManager.loadState(canvas, this.menu.state());
    }

    @Override
    protected void init() {
        super.init();
        CanvasWidget canvasWidget = this.addRenderableWidget(new CanvasWidget(
                this.canvas, 0, 0, this.width, this.height,
                this::openAddNodeDialog));
        canvasWidget.viewport().pan((float) -this.width / 2, 0);
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> this.sendState())
                .bounds(this.width - 60, 6, 54, 20)
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.canvas.revision() != this.sentRevision) {
            this.sendState();
        }
    }

    private void sendState() {
        assert minecraft.level != null;
        this.sentRevision = this.canvas.revision();
        this.sentSequence++;
        final var regAccess = minecraft.level.registryAccess();
        ClientPacketDistributor.sendToServer(new UpdateCanvasStatePayload(
                CanvasStateManager.toState(canvas, regAccess), this.sentSequence));
    }

    @SuppressWarnings("unchecked")
    public void applyEvaluation(CanvasEvaluationPayload payload) {
        if (payload.sequence() != this.sentSequence) {
            return;
        }
        List<Node> nodes = this.canvas.nodes();

        Map<Node, List<String>> results = new IdentityHashMap<>();
        for (NodeValidity validity : payload.validity()) {
            if (validity.nodeIndex() >= 0 && validity.nodeIndex() < nodes.size()) {
                results.put(nodes.get(validity.nodeIndex()), validity.errors());
            }
        }
        this.canvas.setValidity(results);

        var ops = Minecraft.getInstance().level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (Node node : nodes) {
            node.propertyManager().clearComputed();
        }
        for (Node node : nodes) {
            node.renderManager().clear();
        }
        for (NodeRendered rendered : payload.rendered()) {
            if (rendered.nodeIndex() < 0 || rendered.nodeIndex() >= nodes.size()) {
                continue;
            }
            var manager = nodes.get(rendered.nodeIndex()).renderManager();
            if (rendered.providerIndex() < 0 || rendered.providerIndex() >= manager.providers().size()) {
                continue;
            }
            var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(rendered.data()),
                    Minecraft.getInstance().level.registryAccess());
            manager.setState(rendered.providerIndex(), manager.providers().get(rendered.providerIndex()).value().codec().decode(buffer));
        }
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
