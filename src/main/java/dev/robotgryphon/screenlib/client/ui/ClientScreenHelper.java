package dev.robotgryphon.screenlib.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class ClientScreenHelper {
    public static void openTestScreen(Player player) {
        Minecraft.getInstance().setScreenAndShow(new TestScreen(player));
    }
}
