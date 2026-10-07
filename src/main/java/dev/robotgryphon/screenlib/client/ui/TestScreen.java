package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.client.ui.widget.CanvasWidget;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.CanvasStateManager;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.menu.TestScreenMenu;
import dev.robotgryphon.screenlib.network.UpdateCanvasStatePayload;
import dev.robotgryphon.screenlib.graph.nodes.NodeDefinition;
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
    private final Canvas canvas = new Canvas(Minecraft.getInstance().level.registryAccess());

    public TestScreen(TestScreenMenu menu, Inventory unused, Component title) {
        super(title);
        this.menu = menu;
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
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> this.save())
                .bounds(this.width - 60, 6, 54, 20)
                .build());
    }

    private void save() {
        assert minecraft.level != null;
        final var regAccess = minecraft.level.registryAccess();
        ClientPacketDistributor.sendToServer(new UpdateCanvasStatePayload(CanvasStateManager.toState(canvas, regAccess)));
    }

    @Override
    public void onClose() {
        this.save();

        if (this.minecraft.player != null) {
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
            this.canvas.addNode(new Node(this.canvas, ref, Node.defaultTitle(ref.key()),
                    (int) canvasPos.x(), (int) canvasPos.y()));
        }));
    }
}
