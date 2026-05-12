package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.CanvasState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Server-authoritative carrier for the test screen's canvas state. The
 * menu lives for the duration of an editing session — opened when a
 * player triggers {@link dev.robotgryphon.screenlib.ScreenOpenerItem},
 * destroyed when the player closes the screen. Two distinct construction
 * sites land here:
 *
 * <ul>
 *   <li><b>Server</b>: {@link #TestScreenMenu(int, Inventory, Level)} —
 *       reads the initial state straight off the level's attachment so
 *       the menu starts in sync with persisted server data.</li>
 *   <li><b>Client</b>: {@link #TestScreenMenu(int, Inventory, RegistryFriendlyByteBuf)}
 *       — invoked by the {@code IContainerFactory} on packet receipt;
 *       decodes whatever the server's {@code writeClientSideData} put on
 *       the wire.</li>
 * </ul>
 *
 * <p>There are no item slots — this menu only exists to be a typed
 * channel for {@link CanvasState}, not for inventory mediation. The
 * abstract members from {@link AbstractContainerMenu} get trivial
 * implementations: nothing to quick-move, no spatial constraint to
 * validate.
 */
public class TestScreenMenu extends AbstractContainerMenu {

    /**
     * Server-side reference to the level so we can persist state
     * updates straight onto the {@link ScreenLib#TEST_SCREEN_ATTACHMENT}
     * without a separate lookup. Null on the client copy of the menu.
     */
    private final @Nullable Level serverLevel;

    /**
     * Current snapshot of the canvas. Replaced wholesale rather than
     * mutated in place because {@link CanvasState} is an immutable
     * record — the codec round-trip already gives us new instances
     * for free.
     */
    private CanvasState state;

    /** Server-side constructor: seeded from the level's attachment. */
    public TestScreenMenu(int containerId, Inventory inv, Level level) {
        super(ScreenLib.TEST_SCREEN_MENU.get(), containerId);
        this.serverLevel = level;
        this.state = level.getData(ScreenLib.TEST_SCREEN_ATTACHMENT);
    }

    /**
     * Client-side constructor invoked by the registered
     * {@link net.neoforged.neoforge.network.IContainerFactory}. The
     * extra data buffer carries whatever the provider's
     * {@code writeClientSideData} wrote — in our case, the encoded
     * {@link CanvasState}.
     */
    public TestScreenMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf data) {
        super(ScreenLib.TEST_SCREEN_MENU.get(), containerId);
        this.serverLevel = null;
        this.state = ByteBufCodecs.fromCodecWithRegistries(CanvasState.CODEC).decode(data);
    }

    /** Current snapshot of the canvas. */
    public CanvasState state() {
        return state;
    }

    /**
     * Replaces the menu's snapshot. On the server this also writes
     * through to the level attachment so the canvas survives the menu
     * being closed. On the client this only updates the in-memory copy;
     * the persisted snapshot lives on the server.
     */
    public void updateState(CanvasState newState) {
        this.state = newState;
        if (this.serverLevel != null) {
            this.serverLevel.setData(ScreenLib.TEST_SCREEN_ATTACHMENT, newState);
        }
    }

    /**
     * No slots, so shift-clicking has nothing meaningful to do. Vanilla
     * still requires this method be present and non-throwing.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /**
     * The test screen has no positional anchor (no block-bound, no
     * item-bound proximity check), so the menu stays valid as long as
     * the player is alive. Returning {@code true} unconditionally is the
     * canonical pattern for "screen-only" menus.
     */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
