package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.client.ui.widget.CanvasWidget;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.Node;
import dev.robotgryphon.screenlib.graph.NodeState;
import dev.robotgryphon.screenlib.menu.TestScreenMenu;
import dev.robotgryphon.screenlib.network.UpdateCanvasStatePayload;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector2dc;

/**
 * Client view of the {@link TestScreenMenu}. The screen owns the editing
 * canvas; persistent state lives on the server's menu instance and rides
 * back and forth via packets:
 * <ul>
 *   <li>Open: the server seeds the menu with {@link Canvas} state read
 *       off its level attachment and ships that snapshot through the
 *       menu's extra-data buffer. {@link #initCanvas} hydrates the
 *       canvas from {@code menu.state()}.</li>
 *   <li>Close: {@link #removed} fires an {@link UpdateCanvasStatePayload}
 *       so the server can update its menu copy and persist it back onto
 *       the level attachment before the menu dies.</li>
 * </ul>
 *
 * <p>Note this class extends plain {@link Screen} — there are no item
 * slots to render — but it also implements {@link MenuAccess} so the
 * NeoForge {@code RegisterMenuScreensEvent} factory can produce it from
 * a {@link TestScreenMenu} the same way vanilla container screens are
 * produced from container menus.
 */
public class TestScreen extends Screen implements MenuAccess<TestScreenMenu> {

    private final TestScreenMenu menu;
    private final Canvas canvas = new Canvas();

    /**
     * Signature mandated by {@code MenuScreens.ScreenConstructor}: the
     * menu, the player's inventory, and the menu's display name. We
     * keep the menu and ignore the inventory and title — the test
     * screen has no inventory slots and renders its own chrome.
     */
    public TestScreen(TestScreenMenu menu, Inventory inv, Component title) {
        super(title);
        this.menu = menu;
        this.initCanvas();
    }

    @Override
    public TestScreenMenu getMenu() {
        return this.menu;
    }

    /**
     * Pulls the canvas snapshot off the menu (server-shipped at open
     * time) and rebuilds the editable canvas from it. Pan resets to a
     * centered offset on every open since view state is intentionally
     * out of the persisted snapshot.
     */
    private void initCanvas() {
        canvas.pan((float) -this.width / 2, 0);
        canvas.loadState(this.menu.state(), this::buildNodeWidget);
    }

    /**
     * Materializes a single {@link NodeWidget} from the persisted state.
     * The title is re-derived from the definition's registry id (the same
     * way every spawn path on this screen builds titles), so it isn't part
     * of the serialized data.
     */
    private NodeWidget buildNodeWidget(NodeState state) {
        Component title = Component.translatable(
                state.definition().unwrapKey().orElseThrow().identifier().toLanguageKey("node"));
        Node node = new Node(state.definition(), title, state.x(), state.y());
        return new NodeWidget(node);
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new CanvasWidget(this.canvas, 0, 0, this.width, this.height,
                this::openAddNodeDialog));
    }

    @Override
    public void onClose() {
        // Order matters here. The state update must reach the server while
        // the menu instance is still alive — once the close packet lands,
        // the server disposes the TestScreenMenu and the update would have
        // no menu to apply against. Sending the update first preserves the
        // edits without needing a separate "stash last state" path.
        //
        // We deliberately don't put this in removed(): removed() also
        // fires when the screen is *replaced* (e.g., opening the Add Node
        // dialog), which would spam the server with mid-edit state every
        // time the user navigates to a sub-screen.
        ClientPacketDistributor.sendToServer(new UpdateCanvasStatePayload(canvas.toState()));

        // The vanilla pattern for a menu-backed screen is to route close
        // through the local player so the ServerboundContainerClosePacket
        // is emitted and the server-side menu is properly disposed. Plain
        // Screen.onClose just clears the active screen and would leak the
        // server's TestScreenMenu instance.
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        } else {
            super.onClose();
        }
    }

    /**
     * Invoked when the user picks "Add Node" from the canvas context menu.
     * Opens the dialog as a separate Screen — we hand it back this instance
     * as its parent so the canvas state survives the round-trip.
     */
    private void openAddNodeDialog(Vector2dc canvasPos) {
        final var defs = minecraft.level.registryAccess()
                .lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                .listElements()
                .toList();
        if (defs.isEmpty()) {
            return;
        }
        Minecraft.getInstance().setScreen(new AddNodeDialog(this, defs, ref -> {
            // Submit handler: spawn a fresh Node + NodeWidget at the original
            // right-click location and add the pair to the canvas. Title is
            // resolved from the registry key the same way buildNodeWidget does.
            Component title = Component.translatable(ref.key().identifier().toLanguageKey("node"));
            Node node = new Node(ref, title, (int) canvasPos.x(), (int) canvasPos.y());
            this.canvas.addNode(new NodeWidget(node));
        }));
    }
}
