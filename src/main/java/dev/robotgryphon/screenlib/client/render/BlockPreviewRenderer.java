package dev.robotgryphon.screenlib.client.render;

import dev.robotgryphon.screenlib.client.render.block.BlockPreviews;
import dev.robotgryphon.screenlib.graph.nodes.properties.computed.PlacedBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.List;

public class BlockPreviewRenderer implements NodeStateRenderer<PlacedBlock> {
    private static final int HOVER_FILL = 0x30FFFFFF;
    private static final int SIZE = 40;

    @Override
    public Class<PlacedBlock> stateType() {
        return PlacedBlock.class;
    }

    @Override
    public int width() {
        return SIZE;
    }

    @Override
    public int height() {
        return SIZE;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, PlacedBlock block,
                       ScreenRectangle area, int mouseX, int mouseY, float alpha) {
        if (block.state().isAir()) {
            return;
        }
        if (area.containsPoint(mouseX, mouseY)) {
            graphics.fill(area.left(), area.top(), area.left() + area.width(), area.top() + area.height(), HOVER_FILL);
        }
        BlockPreviews.submit(graphics, area, block.level(), block.pos(), block.state());
    }

    @Override
    public void tooltip(GuiGraphicsExtractor graphics, PlacedBlock placed, ScreenRectangle area,
                        int mouseX, int mouseY, int screenX, int screenY) {
        var block = placed.state().getBlock();
        graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, List.of(
                block.getName(),
                Component.literal(BuiltInRegistries.BLOCK.getKey(block).toString())
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY)), screenX, screenY);
    }
}
