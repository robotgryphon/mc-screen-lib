package dev.robotgryphon.screenlib.client.ui.widget.property;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import java.util.Objects;
import java.util.function.Consumer;

public final class NumericPropertyEditor extends PropertyEditor {
    public static final int BUTTON_WIDTH = 12;

    private static final float FLOAT_STEP = 0.1f;
    private static final double DOUBLE_STEP = 0.1;

    private static final int BUTTON_GLYPH_COLOR = 0xFFB0B3BC;
    private static final int BUTTON_GLYPH_HOVER_COLOR = 0xFFFFFFFF;

    private static final Component MINUS_GLYPH = Component.literal("−");
    private static final Component PLUS_GLYPH = Component.literal("+");

    private Number value;

    private final Codec<?> codec;
    private final Consumer<Number> onValueChanged;

    private final SpacerElement minusSpacer;
    private final SpacerElement plusSpacer;
    private final EditBox editBox;
    private final EqualSpacingLayout layout;

    private boolean editing;

    public NumericPropertyEditor(int x, int y, int width, int height,
                                 Number value, Codec<?> codec,
                                 Consumer<Number> onValueChanged) {
        super(x, y, width, height);
        this.value = value;
        this.codec = codec;
        this.onValueChanged = onValueChanged;

        Font font = Minecraft.getInstance().font;
        this.editBox = new EditBox(font, 0, 0,
                Math.max(0, width - 2 * BUTTON_WIDTH), height,
                Component.empty());
        this.editBox.setBordered(false);
        this.editBox.setMaxLength(32);
        this.editBox.setTextColor(VALUE_TEXT_COLOR);
        this.editBox.setCentered(true);

        this.minusSpacer = new SpacerElement(BUTTON_WIDTH, height);
        this.plusSpacer = new SpacerElement(BUTTON_WIDTH, height);
        this.layout = new EqualSpacingLayout(x, y, width, height,
                EqualSpacingLayout.Orientation.HORIZONTAL);
        this.layout.addChild(this.minusSpacer);
        this.layout.addChild(this.editBox);
        this.layout.addChild(this.plusSpacer);
        this.layout.arrangeElements();

        int textOffset = (height - font.lineHeight) / 2 + 1;
        this.editBox.setY(this.editBox.getY() + textOffset);
    }

    public Number getValue() {
        return this.value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.layout.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.layout.setY(y);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics,
                                            int mouseX, int mouseY, float partialTick) {
        int editorX = getX();
        int editorY = getY();
        int editorWidth = getWidth();
        int editorHeight = getHeight();

        renderPill(graphics, editorX, editorY, editorWidth, editorHeight, BG_COLOR, this.alpha);

        graphics.nextStratum();

        int minusX = this.minusSpacer.getX();
        int plusX = this.plusSpacer.getX();
        boolean minusHovered = isInside(mouseX, mouseY, minusX, editorY, BUTTON_WIDTH, editorHeight);
        boolean plusHovered = isInside(mouseX, mouseY, plusX, editorY, BUTTON_WIDTH, editorHeight);

        final var font = Minecraft.getInstance().font;

        int textY = textCenterY(editorY, editorHeight, font);

        int buttonColor = ARGB.multiply(BUTTON_GLYPH_COLOR, ARGB.white(this.alpha));
        int buttonHoverColor = ARGB.multiply(BUTTON_GLYPH_HOVER_COLOR, ARGB.white(this.alpha));

        graphics.text(font, MINUS_GLYPH,
                minusX + BUTTON_WIDTH / 2 - font.width(MINUS_GLYPH) / 2,
                textY, minusHovered ? buttonHoverColor : buttonColor, false);
        graphics.text(font, PLUS_GLYPH,
                plusX + BUTTON_WIDTH / 2 - font.width(PLUS_GLYPH) / 2,
                textY, plusHovered ? buttonHoverColor : buttonColor, false);

        if (!this.editing) {
            int valueCenterX = this.editBox.getX() + this.editBox.getWidth() / 2;
            int valueColor = ARGB.multiply(VALUE_TEXT_COLOR, ARGB.white(this.alpha));
            graphics.centeredText(font, Component.literal(this.value.toString()), valueCenterX, textY, valueColor);
        } else {
            this.editBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (isInside(mouseX, mouseY,
                this.minusSpacer.getX(), this.minusSpacer.getY(),
                this.minusSpacer.getWidth(), this.minusSpacer.getHeight())) {
            applyStep(-1);
            return;
        }
        if (isInside(mouseX, mouseY,
                this.plusSpacer.getX(), this.plusSpacer.getY(),
                this.plusSpacer.getWidth(), this.plusSpacer.getHeight())) {
            applyStep(+1);
            return;
        }
        enterEditMode();
    }

    private void applyStep(int direction) {
        Number next = step(direction);
        if (!Objects.equals(next, this.value)) {
            this.value = next;
            this.onValueChanged.accept(next);
        }
    }

    private Number step(int direction) {
        if (this.codec == Codec.INT) {
            return this.value.intValue() + direction;
        }
        if (this.codec == Codec.FLOAT) {
            return roundToTenth(this.value.floatValue() + direction * FLOAT_STEP);
        }
        if (this.codec == Codec.DOUBLE) {
            return roundToTenth(this.value.doubleValue() + direction * DOUBLE_STEP);
        }
        return this.value;
    }

    public boolean isEditing() {
        return this.editing;
    }

    public void enterEditMode() {
        if (this.editing) return;
        this.editing = true;
        this.editBox.setValue(this.value.toString());
        this.editBox.moveCursorToEnd(false);
        this.editBox.setFocused(true);
    }

    public void commitEdit() {
        if (!this.editing) return;
        String text = this.editBox.getValue().trim();
        try {
            Number parsed = null;
            if (this.codec == Codec.INT) {
                parsed = Integer.parseInt(text);
            } else if (this.codec == Codec.FLOAT) {
                parsed = Float.parseFloat(text);
            } else if (this.codec == Codec.DOUBLE) {
                parsed = Double.parseDouble(text);
            }
            if (parsed != null && !Objects.equals(parsed, this.value)) {
                this.value = parsed;
                this.onValueChanged.accept(parsed);
            }
        } catch (NumberFormatException ignored) {
        }
        this.editing = false;
        this.editBox.setFocused(false);
    }

    public void cancelEdit() {
        if (!this.editing) return;
        this.editing = false;
        this.editBox.setFocused(false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.editing) return false;
        if (event.isConfirmation()) {
            commitEdit();
            return true;
        }
        if (event.isEscape()) {
            cancelEdit();
            return true;
        }
        return this.editBox.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!this.editing) return false;
        return this.editBox.charTyped(event);
    }

    public boolean handleClickWhileEditing(MouseButtonEvent event, boolean doubleClick) {
        if (!this.editing) return false;
        if (this.editBox.isMouseOver(event.x(), event.y())) {
            this.editBox.mouseClicked(event, doubleClick);
            return true;
        }
        commitEdit();
        return false;
    }

    private static float roundToTenth(float v) {
        return Math.round(v * 10f) / 10f;
    }

    private static double roundToTenth(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
