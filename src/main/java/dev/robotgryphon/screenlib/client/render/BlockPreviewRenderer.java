package dev.robotgryphon.screenlib.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class BlockPreviewRenderer implements NodeStateRenderer<BlockState> {
    private static final int ITEM_SIZE = 16;
    private static final int HOVER_FILL = 0x30FFFFFF;
    private static final int SIZE = 40;

    @Override
    public Class<BlockState> stateType() {
        return BlockState.class;
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
    public void render(GuiGraphicsExtractor graphics, BlockState state,
                       ScreenRectangle area, int mouseX, int mouseY, float alpha) {
        ItemStack stack = new ItemStack(state.getBlock());
        if (stack.isEmpty()) {
            return;
        }
        if (area.containsPoint(mouseX, mouseY)) {
            graphics.fill(area.left(), area.top(), area.left() + area.width(), area.top() + area.height(), HOVER_FILL);
        }
        float scale = Math.min(area.width(), area.height()) / (float) ITEM_SIZE;
        graphics.pose().pushMatrix();
        graphics.pose().translate(area.left() + (area.width() - ITEM_SIZE * scale) / 2f,
                area.top() + (area.height() - ITEM_SIZE * scale) / 2f);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }

    @Override
    public void tooltip(GuiGraphicsExtractor graphics, BlockState state, ScreenRectangle area,
                        int mouseX, int mouseY, int screenX, int screenY) {
        var block = state.getBlock();
        graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, List.of(
                block.getName(),
                Component.literal(BuiltInRegistries.BLOCK.getKey(block).toString())
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY)), screenX, screenY);
    }
}
