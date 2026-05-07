package com.example.examplemod.client;

import com.example.examplemod.client.render.BezierCurveRenderer;
import com.example.examplemod.graph.NodeConnection;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector2f;

public class TestScreen extends Screen {
    private final Player player;

    public TestScreen(Player player) {
        super(Component.empty());
        this.player = player;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        renderCurvedLine(graphics, new Vector2f(100, 100), new Vector2f(240, 220), CommonColors.GREEN);
        renderCurvedLine(graphics, new Vector2f(100, 200), new Vector2f(240, 20), CommonColors.RED);
        renderCurvedLine(graphics, new Vector2f(120, 80), new Vector2f(160, 160), CommonColors.YELLOW);
    }

    private static void renderCurvedLine(GuiGraphicsExtractor graphics, Vector2f start, Vector2f end, int color) {
        var curve = NodeConnection.rightToLeft(start, end, color);
        BezierCurveRenderer.render(graphics, curve, curve.bounds());

        graphics.nextStratum();
        graphics.fakeItem(new ItemStack(Items.ENDER_PEARL), (int) curve.start().x() - 8, (int) curve.start().y() - 8);
        graphics.fakeItem(new ItemStack(Items.ENDER_PEARL), (int) curve.end().x() - 8, (int) curve.end().y() - 8);
    }
}
