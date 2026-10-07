package dev.robotgryphon.screenlib.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record NodeTooltip(List<Component> lines, ItemStack stack) {
    public static NodeTooltip of(Component... lines) {
        return new NodeTooltip(List.of(lines), ItemStack.EMPTY);
    }

    public static NodeTooltip of(ItemStack stack) {
        return new NodeTooltip(List.of(), stack);
    }

    public void show(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        if (!this.stack.isEmpty()) {
            graphics.setTooltipForNextFrame(font, this.stack, mouseX, mouseY);
        } else if (!this.lines.isEmpty()) {
            graphics.setTooltipForNextFrame(font, this.lines, Optional.empty(), ItemStack.EMPTY, mouseX, mouseY);
        }
    }
}
