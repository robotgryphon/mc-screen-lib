package dev.robotgryphon.screenlib.client.render;

import dev.robotgryphon.screenlib.graph.types.ItemHandlerAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ItemHandlerBlockRenderer implements PropertyValueRenderer<ItemHandlerAccess> {
    private static final int ITEM_SIZE = 16;
    private static final int ICON_SIZE = 12;
    private static final int HOVER_FILL = 0x30FFFFFF;

    @Override
    public Class<ItemHandlerAccess> valueType() {
        return ItemHandlerAccess.class;
    }

    @Override
    public int width() {
        return ICON_SIZE + 2;
    }

    @Override
    public boolean render(GuiGraphicsExtractor graphics, ItemHandlerAccess value, ScreenRectangle area,
                          int mouseX, int mouseY, float alpha) {
        BlockState state = blockAt(value);
        ItemStack stack = state == null ? ItemStack.EMPTY : new ItemStack(state.getBlock());
        if (stack.isEmpty()) {
            return false;
        }
        if (area.containsPoint(mouseX, mouseY)) {
            graphics.fill(area.left(), area.top(), area.right(), area.bottom(), HOVER_FILL);
        }
        float scale = ICON_SIZE / (float) ITEM_SIZE;
        graphics.pose().pushMatrix();
        graphics.pose().translate(area.left() + (area.width() - ICON_SIZE) / 2f,
                area.top() + (area.height() - ICON_SIZE) / 2f);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
        return true;
    }

    @Override
    public boolean tooltip(GuiGraphicsExtractor graphics, ItemHandlerAccess value, ScreenRectangle area,
                           int mouseX, int mouseY, int screenX, int screenY) {
        BlockState state = blockAt(value);
        if (state == null) {
            return false;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(state.getBlock().getName());
        var pos = value.pos();
        lines.add(Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ()).withStyle(ChatFormatting.GRAY));
        if (value.side() != null) {
            lines.add(Component.literal(value.side().getSerializedName()).withStyle(ChatFormatting.DARK_GRAY));
        }
        graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, lines, screenX, screenY);
        return true;
    }

    private static @Nullable BlockState blockAt(ItemHandlerAccess value) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().equals(value.level()) || !level.isLoaded(value.pos())) {
            return null;
        }
        return level.getBlockState(value.pos());
    }
}
