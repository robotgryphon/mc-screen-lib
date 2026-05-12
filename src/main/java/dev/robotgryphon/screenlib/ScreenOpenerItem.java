package dev.robotgryphon.screenlib;

import dev.robotgryphon.screenlib.menu.TestScreenMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ScreenOpenerItem extends Item {
    public ScreenOpenerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Server-only path. openMenu spans the protocol round-trip: it
        // builds a TestScreenMenu against the level's attachment, fires
        // ClientboundOpenScreenPacket with the menu type id + window id +
        // the extra-data buffer that TestScreenMenuProvider.writeClientSideData
        // populates, and the client materializes the menu + screen from the
        // registered factories. We don't need a custom open packet because
        // openMenu already does all of that.
        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new TestScreenMenuProvider(level));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
