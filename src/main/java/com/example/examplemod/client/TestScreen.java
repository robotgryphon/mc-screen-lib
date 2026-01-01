package com.example.examplemod.client;

import com.example.examplemod.client.render.BezierCurveRenderer;
import com.example.examplemod.graph.NodeConnection;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;

public class TestScreen extends Screen {
    private final Player player;

    public TestScreen(Player player) {
        super(Component.empty());
        this.player = player;
    }

    @Override
    public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        graphics.drawString(minecraft.font, "Hello!", 10, 10, CommonColors.WHITE);

        var curve = NodeConnection.rightToLeft(
                new Vector2f(100, 100),
                new Vector2f(240, 420f),
                CommonColors.WHITE
        );

//        BezierCurveRenderer.renderControlLines(graphics, controlPoints, this.width, this.height);

        BezierCurveRenderer.render(graphics, curve,
                new ScreenRectangle(new ScreenPosition(100, 100), 140, 320));

        // BIND SHADER
        // PASS TWO POINTS
        // DRAW
        // ????
        // PROFIT
    }
}
