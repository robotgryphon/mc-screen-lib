package dev.robotgryphon.screenlib.client.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public record BoundState<S>(NodeStateRenderer<S> renderer, S state) {
    public static <S> Optional<BoundState<S>> of(NodeStateRenderer<S> renderer, @Nullable Object state) {
        Class<S> type = renderer.stateType();
        return type.isInstance(state) ? Optional.of(new BoundState<>(renderer, type.cast(state))) : Optional.empty();
    }

    public void render(GuiGraphicsExtractor graphics, ScreenRectangle area, int mouseX, int mouseY, float alpha) {
        this.renderer.render(graphics, this.state, area, mouseX, mouseY, alpha);
    }

    public void mouseMoved(ScreenRectangle area, double mouseX, double mouseY) {
        this.renderer.mouseMoved(this.state, area, mouseX, mouseY);
    }

    public boolean mouseClicked(ScreenRectangle area, MouseButtonEvent event, boolean doubleClick) {
        return this.renderer.mouseClicked(this.state, area, event, doubleClick);
    }

    public boolean mouseScrolled(ScreenRectangle area, double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.renderer.mouseScrolled(this.state, area, mouseX, mouseY, scrollX, scrollY);
    }

    public void tooltip(GuiGraphicsExtractor graphics, ScreenRectangle area, int mouseX, int mouseY, int screenX, int screenY) {
        this.renderer.tooltip(graphics, this.state, area, mouseX, mouseY, screenX, screenY);
    }
}
