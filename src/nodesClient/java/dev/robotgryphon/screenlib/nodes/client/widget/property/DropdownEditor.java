package dev.robotgryphon.screenlib.nodes.client.widget.property;

import dev.robotgryphon.screenlib.nodes.common.properties.PropertyManager;
import dev.robotgryphon.screenlib.graph.schema.properties.StoredPropertyDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public final class DropdownEditor<T> extends PropertyEditor<T> {
    private static final int BG_HOVER_COLOR = 0xCC242530;
    private static final int CHEVRON_COLOR = 0xFFB0B3BC;
    private static final int CHEVRON_HOVER_COLOR = 0xFFFFFFFF;

    private static final int H_PADDING = 6;
    private static final int CHEVRON_HALF_WIDTH = 2;

    private final Runnable onOpenRequested;

    public DropdownEditor(int x, int y, int width, int height,
                          PropertyManager properties, StoredPropertyDefinition<T> property, Runnable onOpenRequested) {
        super(x, y, width, height, properties, property);
        this.onOpenRequested = onOpenRequested;
    }

    public static int widthFor(Font font, java.util.Collection<?> options) {
        int widest = 0;
        for (Object option : options) {
            widest = Math.max(widest, font.width(PropertyValues.display(option)));
        }
        return widest + 3 * H_PADDING + 2 * CHEVRON_HALF_WIDTH + 1;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics,
                                            int mouseX, int mouseY, float partialTick) {
        String display = this.current().map(PropertyValues::display).orElse("—");
        render(graphics, Minecraft.getInstance().font,
                getX(), getY(), getWidth(), getHeight(),
                mouseX, mouseY, display, getAlpha());
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.onOpenRequested.run();
    }

    public static void render(GuiGraphicsExtractor graphics, Font font,
                              int editorX, int editorY, int editorWidth, int editorHeight,
                              int mouseX, int mouseY,
                              String value, float alpha) {
        boolean hovered = isInside(mouseX, mouseY, editorX, editorY, editorWidth, editorHeight);
        int bg = hovered ? BG_HOVER_COLOR : BG_COLOR;

        renderPill(graphics, editorX, editorY, editorWidth, editorHeight, bg, alpha);

        int textY = textCenterY(editorY, editorHeight, font);
        int valueColor = ARGB.multiply(VALUE_TEXT_COLOR, ARGB.white(alpha));
        graphics.text(font, Component.literal(value),
                editorX + H_PADDING, textY, valueColor, false);

        int chevronColor = ARGB.multiply(
                hovered ? CHEVRON_HOVER_COLOR : CHEVRON_COLOR,
                ARGB.white(alpha));
        int chevronCenterX = editorX + editorWidth - H_PADDING - CHEVRON_HALF_WIDTH;
        int chevronTopY = editorY + (editorHeight - (CHEVRON_HALF_WIDTH + 1)) / 2;
        fillTriangleDown(graphics, chevronCenterX, chevronTopY, CHEVRON_HALF_WIDTH, chevronColor);
    }

    private static void fillTriangleDown(GuiGraphicsExtractor graphics, int cx, int topY,
                                         int halfWidth, int color) {
        int height = halfWidth + 1;
        for (int dy = 0; dy < height; dy++) {
            int rowHalf = halfWidth - dy;
            graphics.fill(cx - rowHalf, topY + dy, cx + rowHalf + 1, topY + dy + 1, color);
        }
    }
}
