package dev.robotgryphon.screenlib.nodes.client.render;

import dev.robotgryphon.screenlib.nodes.api.render.PropertyValueRenderer;
import dev.robotgryphon.screenlib.nodes.client.render.block.BlockPreviews;

import dev.robotgryphon.screenlib.graph.schema.types.ItemHandlerAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ItemHandlerBlockRenderer implements PropertyValueRenderer<ItemHandlerAccess> {
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
        if (state == null || state.isAir()) {
            return false;
        }
        if (area.containsPoint(mouseX, mouseY)) {
            graphics.fill(area.left(), area.top(), area.right(), area.bottom(), HOVER_FILL);
        }
        ScreenRectangle icon = new ScreenRectangle(area.left() + (area.width() - ICON_SIZE) / 2,
                area.top() + (area.height() - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE);
        BlockPreviews.submit(graphics, icon, value.level(), value.pos(), state);
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
