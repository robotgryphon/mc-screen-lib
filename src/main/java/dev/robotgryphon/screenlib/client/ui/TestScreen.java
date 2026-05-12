package dev.robotgryphon.screenlib.client.ui;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.client.ui.widget.CanvasWidget;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.graph.Node;
import dev.robotgryphon.screenlib.graph.NodeState;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector2dc;
import org.jspecify.annotations.Nullable;

public class TestScreen extends Screen {
    private final Player player;
    private final Canvas canvas = new Canvas();

    public TestScreen(Player player) {
        super(Component.empty());
        this.player = player;
        this.initCanvas();
    }

    /**
     * Pulls the previously-saved canvas snapshot off the level's attachment
     * (or {@link CanvasState#EMPTY} if nothing's been saved yet) and rebuilds
     * the canvas from it. Pan resets to a centered offset on every open since
     * view state is intentionally out of the persisted snapshot.
     */
    private void initCanvas() {
        canvas.pan((float) -this.width / 2, 0);

        Level level = currentLevel();
        if (level == null) {
            // No level yet — nothing to load. The Add Node dialog also
            // requires a level for its registry lookup, so the screen is
            // effectively non-functional in this branch anyway.
            return;
        }

        CanvasState saved = level.getData(ScreenLib.TEST_SCREEN_ATTACHMENT);
        canvas.loadState(saved, this::buildNodeWidget);
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
    public void removed() {
        super.removed();
        // Snapshot the current canvas back to the level attachment so the
        // next time this screen opens it picks up exactly what the user
        // left behind. Triggers on Esc, on a screen replacement, and on
        // programmatic dismissal — every "this screen is going away" path.
        Level level = currentLevel();
        if (level != null) {
            level.setData(ScreenLib.TEST_SCREEN_ATTACHMENT, canvas.toState());
        }
    }

    private static @Nullable Level currentLevel() {
        return Minecraft.getInstance().level;
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
