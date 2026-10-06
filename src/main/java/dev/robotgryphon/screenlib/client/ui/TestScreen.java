package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.client.ui.widget.CanvasWidget;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.CanvasStateManager;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.NodeState;
import dev.robotgryphon.screenlib.menu.TestScreenMenu;
import dev.robotgryphon.screenlib.network.UpdateCanvasStatePayload;
import dev.robotgryphon.screenlib.graph.nodes.NodeDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector2dc;

public class TestScreen extends Screen implements MenuAccess<TestScreenMenu> {
    private final TestScreenMenu menu;
    private final Canvas canvas = new Canvas();

    public TestScreen(TestScreenMenu menu, Inventory inv, Component title) {
        super(title);
        this.menu = menu;
        this.initCanvas();
    }

    @Override
    public TestScreenMenu getMenu() {
        return this.menu;
    }

    private void initCanvas() {
        CanvasStateManager.loadState(canvas, this.menu.state(), this::buildNodeWidget);
    }

    private NodeWidget buildNodeWidget(Canvas canvas, NodeState state) {
        Component title = Component.translatable(
                state.definition().unwrapKey().orElseThrow().identifier().toLanguageKey("node"));
        Node node = new Node(canvas, state.definition(), title, state.x(), state.y());
        return new NodeWidget(node);
    }

    @Override
    protected void init() {
        super.init();
        CanvasWidget canvasWidget = this.addRenderableWidget(new CanvasWidget(
                this.canvas, 0, 0, this.width, this.height,
                this::openAddNodeDialog));
        canvasWidget.viewport().pan((float) -this.width / 2, 0);
    }

    @Override
    public void onClose() {
        ClientPacketDistributor.sendToServer(new UpdateCanvasStatePayload(CanvasStateManager.toState(canvas)));

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        } else {
            super.onClose();
        }
    }

    private void openAddNodeDialog(Vector2dc canvasPos) {
        final var defs = minecraft.level.registryAccess()
                .lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                .listElements()
                .toList();
        if (defs.isEmpty()) {
            return;
        }
        Minecraft.getInstance().setScreenAndShow(new AddNodeDialog(this, defs, ref -> {
            Component title = Component.translatable(ref.key().identifier().toLanguageKey("node"));
            Node node = new Node(this.canvas, ref, title, (int) canvasPos.x(), (int) canvasPos.y());
            this.canvas.addNode(new NodeWidget(node));
        }));
    }
}
