package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.graph.CanvasState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

/**
 * Bridges {@code player.openMenu(...)} to a fresh {@link TestScreenMenu}.
 * Holds the {@link Level} the menu should bind to so the menu can read
 * its initial {@link CanvasState} off the level's attachment and write
 * future updates back to the same place.
 *
 * <p>NeoForge's {@link #writeClientSideData} hook ferries the live menu's
 * state across to the client constructor — that way the client doesn't
 * have to make a separate request for the initial snapshot.
 */
public record TestScreenMenuProvider(Level level) implements MenuProvider {

    @Override
    public Component getDisplayName() {
        // The test screen uses its own header — vanilla never reads this
        // unless the screen falls back on the default title rendering.
        return Component.empty();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new TestScreenMenu(containerId, inv, this.level);
    }

    /**
     * Called by NeoForge after {@link #createMenu} on the server side, with
     * the constructed menu and the extra-data buffer that will be sent to
     * the client. We serialize the menu's current canvas state here so the
     * client's {@code IContainerFactory} can decode it in one shot.
     */
    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        // The cast is safe by construction: createMenu only ever returns
        // a TestScreenMenu, and NeoForge feeds that very instance back to us.
        TestScreenMenu testMenu = (TestScreenMenu) menu;
        ByteBufCodecs.fromCodecWithRegistries(CanvasState.CODEC)
                .encode(buffer, testMenu.state());
    }
}
