package dev.robotgryphon.screenlib.client.render;

import dev.robotgryphon.screenlib.graph.render.BlockPreviewState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class BlockPreviewRenderer implements NodeStateRenderer<BlockPreviewState> {
    private static final int ITEM_SIZE = 16;
    private static final int HOVER_FILL = 0x30FFFFFF;

    @Override
    public void render(GuiGraphicsExtractor graphics, BlockPreviewState state,
                       RenderArea area, int mouseX, int mouseY, float alpha) {
        ItemStack stack = new ItemStack(state.state().getBlock());
        if (stack.isEmpty()) {
            return;
        }
        if (area.contains(mouseX, mouseY)) {
            graphics.fill(area.x(), area.y(), area.x() + area.width(), area.y() + area.height(), HOVER_FILL);
        }
        float scale = Math.min(area.width(), area.height()) / (float) ITEM_SIZE;
        graphics.pose().pushMatrix();
        graphics.pose().translate(area.x() + (area.width() - ITEM_SIZE * scale) / 2f,
                area.y() + (area.height() - ITEM_SIZE * scale) / 2f);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }

    @Override
    public Optional<NodeTooltip> tooltip(BlockPreviewState state, RenderArea area, double mouseX, double mouseY) {
        var block = state.state().getBlock();
        return Optional.of(NodeTooltip.of(
                block.getName(),
                Component.literal(BuiltInRegistries.BLOCK.getKey(block).toString())
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY)));
    }
}
