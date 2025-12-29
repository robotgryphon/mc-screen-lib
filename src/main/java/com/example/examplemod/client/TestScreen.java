package com.example.examplemod.client;

import com.example.examplemod.client.render.BezierCurveRenderState;
import com.example.examplemod.client.render.BezierCurveRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector2f;
import org.jspecify.annotations.NonNull;

import java.util.List;

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

        var controlPoints = List.of(
                new Vector2f(0.1f, 0.1f),
                new Vector2f(0.5f, 0.1f),
                new Vector2f(0.5f, 0.9f),
                new Vector2f(0.9f, 0.9f)
        );

//        BezierCurveRenderer.renderControlLines(graphics, controlPoints, this.width, this.height);

        BezierCurveRenderer.render(graphics, controlPoints, this.width, this.height);

        // BIND SHADER
        // PASS TWO POINTS
        // DRAW
        // ????
        // PROFIT
    }
}
