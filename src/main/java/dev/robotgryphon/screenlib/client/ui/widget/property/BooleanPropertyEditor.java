package dev.robotgryphon.screenlib.client.ui.widget.property;

import dev.robotgryphon.screenlib.client.ui.widget.RoundedShapes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.ARGB;

import java.util.function.Consumer;

public final class BooleanPropertyEditor extends PropertyEditor {
    private static final int TRACK_WIDTH = 20;
    private static final int TRACK_HEIGHT = 10;
    private static final int THUMB_INSET = 1;
    private static final int THUMB_SIZE = TRACK_HEIGHT - 2 * THUMB_INSET;
    private static final int RIGHT_PADDING = 2;

    private static final int TRACK_OFF_COLOR = 0xFF4A4A55;
    private static final int THUMB_OFF_COLOR = 0xFFB0B3BC;
    private static final int TRACK_ON_COLOR = 0xFF4A90E2;
    private static final int THUMB_ON_COLOR = 0xFF1F1F23;
    private static final int HOVER_OVERLAY = 0x22FFFFFF;

    private boolean value;

    private final Consumer<Boolean> onValueChanged;

    public BooleanPropertyEditor(int x, int y, int width, int height,
                                 boolean value, Consumer<Boolean> onValueChanged) {
        super(x, y, width, height);
        this.value = value;
        this.onValueChanged = onValueChanged;
    }

    public boolean getValue() {
        return this.value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics,
                                            int mouseX, int mouseY, float partialTick) {
        render(graphics, getX(), getY(), getWidth(), getHeight(),
                mouseX, mouseY, this.value, getAlpha());
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.value = !this.value;
        this.onValueChanged.accept(this.value);
    }

    public static void render(GuiGraphicsExtractor graphics,
                              int editorX, int editorY, int editorWidth, int editorHeight,
                              int mouseX, int mouseY,
                              boolean value, float alpha) {
        int trackX = editorX + editorWidth - RIGHT_PADDING - TRACK_WIDTH;
        int trackY = editorY + (editorHeight - TRACK_HEIGHT) / 2;

        int trackColor = value ? TRACK_ON_COLOR : TRACK_OFF_COLOR;
        int thumbColor = value ? THUMB_ON_COLOR : THUMB_OFF_COLOR;
        int thumbX = value
                ? trackX + TRACK_WIDTH - THUMB_INSET - THUMB_SIZE
                : trackX + THUMB_INSET;
        int thumbY = trackY + THUMB_INSET;

        int trackRadius = TRACK_HEIGHT / 2;
        RoundedShapes.fillRoundedRect(graphics, trackX, trackY,
                trackX + TRACK_WIDTH, trackY + TRACK_HEIGHT,
                trackRadius,
                ARGB.multiply(trackColor, ARGB.white(alpha)));

        if (isInside(mouseX, mouseY, editorX, editorY, editorWidth, editorHeight)) {
            RoundedShapes.fillRoundedRect(graphics, trackX, trackY,
                    trackX + TRACK_WIDTH, trackY + TRACK_HEIGHT,
                    trackRadius,
                    ARGB.multiply(HOVER_OVERLAY, ARGB.white(alpha)));
        }

        RoundedShapes.fillRoundedRect(graphics, thumbX, thumbY,
                thumbX + THUMB_SIZE, thumbY + THUMB_SIZE,
                THUMB_SIZE / 2,
                ARGB.multiply(thumbColor, ARGB.white(alpha)));
    }
}
