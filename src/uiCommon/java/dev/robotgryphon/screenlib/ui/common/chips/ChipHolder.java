package dev.robotgryphon.screenlib.ui.common.chips;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ChipHolder extends AbstractWidget {
    private static final int CHIP_GAP = 4;

    private final List<Chip> chips = new ArrayList<>();
    private @Nullable Runnable onChange;

    public ChipHolder(int x, int y) {
        super(x, y, 0, Chip.CHIP_HEIGHT, Component.empty());
    }

    public ChipHolder setOnChange(@Nullable Runnable onChange) {
        this.onChange = onChange;
        return this;
    }

    public Chip addChip(Chip.Builder builder) {
        return insertChip(builder.build(this));
    }

    public Chip addChip(Consumer<Chip.Builder> configurator) {
        Chip.Builder builder = Chip.builder();
        configurator.accept(builder);
        return insertChip(builder.build(this));
    }

    private Chip insertChip(Chip chip) {
        this.chips.add(chip);
        layoutChips();
        notifyChange();
        return chip;
    }

    public boolean removeChip(@Nullable Chip chip) {
        if (chip == null || !this.chips.remove(chip)) {
            return false;
        }
        layoutChips();
        notifyChange();
        return true;
    }

    public void clearChips() {
        if (this.chips.isEmpty()) {
            return;
        }
        this.chips.clear();
        layoutChips();
        notifyChange();
    }

    private void notifyChange() {
        if (this.onChange != null) {
            this.onChange.run();
        }
    }

    public Stream<Chip> chips() {
        return Collections.unmodifiableList(this.chips).stream();
    }

    public int chipCount() {
        return this.chips.size();
    }

    public boolean isEmpty() {
        return this.chips.isEmpty();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        layoutChips();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        layoutChips();
    }

    private void layoutChips() {
        int cursor = this.getX();
        int chipY = this.getY();
        for (Chip chip : this.chips) {
            chip.setX(cursor);
            chip.setY(chipY);
            cursor += chip.getWidth() + CHIP_GAP;
        }
        int totalWidth = this.chips.isEmpty()
                ? 0
                : (cursor - this.getX() - CHIP_GAP);
        this.setWidth(totalWidth);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.active || !this.visible) {
            return false;
        }
        for (int i = this.chips.size() - 1; i >= 0; i--) {
            Chip chip = this.chips.get(i);
            if (chip.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        for (Chip chip : this.chips) {
            chip.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
