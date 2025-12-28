package com.example.examplemod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Player;

public class TestScreen extends Screen {
    private final Player player;

    public TestScreen(Player player) {
        super(Component.empty());
        this.player = player;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        graphics.drawString(minecraft.font, "Hello!", 10, 10, CommonColors.WHITE);

        // BIND SHADER
        // PASS TWO POINTS
        // DRAW
        // ????
        // PROFIT
    }
}
