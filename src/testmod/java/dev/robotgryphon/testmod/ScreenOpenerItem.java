package dev.robotgryphon.testmod;

import dev.robotgryphon.testmod.menu.TestScreenMenuProvider;
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
        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new TestScreenMenuProvider(TestModMenus.levelCanvas(level)));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
