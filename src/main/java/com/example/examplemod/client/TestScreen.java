package com.example.examplemod.client;

import com.example.examplemod.client.render.BezierCurveRenderer;
import com.example.examplemod.graph.NodeConnection;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
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

        graphics.text(minecraft.font, "Hello!", 10, 10, CommonColors.WHITE);

        var curve = NodeConnection.rightToLeft(
                new Vector2f(100, 100),
                new Vector2f(240, 220f),
                CommonColors.GREEN
        );

//        graphics.fill(renderBounds.left(), renderBounds.top(), renderBounds.right(), renderBounds.bottom(),
//                ARGB.color(0.5f, CommonColors.GRAY));

        BezierCurveRenderer.render(graphics, curve, curve.bounds());

        graphics.nextStratum();

        graphics.fakeItem(new ItemStack(Items.ENDER_PEARL), (int) curve.start().x() - 8, (int) curve.start().y() - 8);
        graphics.fakeItem(new ItemStack(Items.ENDER_PEARL), (int) curve.end().x() - 8, (int) curve.end().y() - 8);
    }
}
