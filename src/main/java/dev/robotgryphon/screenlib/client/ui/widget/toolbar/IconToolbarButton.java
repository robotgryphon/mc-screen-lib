package dev.robotgryphon.screenlib.client.ui.widget.toolbar;

import dev.robotgryphon.screenlib.client.ui.widget.panel.PanelType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class IconToolbarButton implements ToolbarButton {
    private static final int ICON_SIZE = 8;
    private static final int PADDING = 1;
    private static final int SPACING = 2;
    private static final int CHEVRON_WIDTH = 5;

    private final Identifier icon;
    private final Component tooltip;
    private final @Nullable Component label;
    private final boolean dropdown;
    private final Predicate<ToolbarContext> active;
    private final Predicate<ToolbarContext> badge;
    private final BiConsumer<ToolbarContext, ScreenRectangle> action;

    private IconToolbarButton(Builder builder) {
        this.icon = builder.icon;
        this.tooltip = builder.tooltip;
        this.label = builder.label;
        this.dropdown = builder.dropdown;
        this.active = builder.active;
        this.badge = builder.badge;
        this.action = builder.action;
    }

    public static Builder builder(Identifier icon, Component tooltip) {
        return new Builder(icon, tooltip);
    }

    public static IconToolbarButton panelTab(Identifier icon, Component tooltip, ResourceKey<PanelType<?>> panel) {
        return builder(icon, tooltip)
                .active(context -> context.isPanelOpen(panel))
                .onClick((context, area) -> context.togglePanel(panel))
                .build();
    }

    @Override
    public Component tooltip(ToolbarContext context) {
        return this.tooltip;
    }

    @Override
    public int width(ToolbarContext context) {
        if (this.label == null && !this.dropdown) {
            return SIZE;
        }
        int width = PADDING + ICON_SIZE + PADDING;
        if (this.label != null) {
            width += SPACING + font().width(this.label);
        }
        if (this.dropdown) {
            width += SPACING + CHEVRON_WIDTH;
        }
        return width + SPACING;
    }

    @Override
    public boolean isActive(ToolbarContext context) {
        return this.active.test(context);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, ScreenRectangle area, ToolbarContext context) {
        boolean iconOnly = this.label == null && !this.dropdown;
        int iconX = iconOnly ? area.left() + (area.width() - ICON_SIZE) / 2 : area.left() + PADDING;
        int iconY = area.top() + (area.height() - ICON_SIZE) / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.icon, iconX, iconY, ICON_SIZE, ICON_SIZE);

        int cursor = iconX + ICON_SIZE + PADDING;
        if (this.label != null) {
            Font font = font();
            cursor += SPACING;
            graphics.text(font, this.label, cursor, area.top() + (area.height() - font.lineHeight) / 2 + 1,
                    ToolbarStyle.TEXT, false);
            cursor += font.width(this.label);
        }
        if (this.dropdown) {
            ToolbarStyle.chevron(graphics, cursor + SPACING, area.top() + area.height() / 2 - 1);
        }
        if (this.badge.test(context)) {
            ToolbarStyle.badge(graphics, area.right() - ToolbarStyle.BADGE_SIZE + 1, area.top() - 1);
        }
    }

    @Override
    public void onClick(ToolbarContext context, ScreenRectangle area) {
        this.action.accept(context, area);
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    public static final class Builder {
        private final Identifier icon;
        private final Component tooltip;
        private @Nullable Component label;
        private boolean dropdown;
        private Predicate<ToolbarContext> active = context -> false;
        private Predicate<ToolbarContext> badge = context -> false;
        private BiConsumer<ToolbarContext, ScreenRectangle> action = (context, area) -> {};

        private Builder(Identifier icon, Component tooltip) {
            this.icon = icon;
            this.tooltip = tooltip;
        }

        public Builder label(Component label) {
            this.label = label;
            return this;
        }

        public Builder dropdown() {
            this.dropdown = true;
            return this;
        }

        public Builder active(Predicate<ToolbarContext> active) {
            this.active = active;
            return this;
        }

        public Builder badge(Predicate<ToolbarContext> badge) {
            this.badge = badge;
            return this;
        }

        public Builder onClick(BiConsumer<ToolbarContext, ScreenRectangle> action) {
            this.action = action;
            return this;
        }

        public IconToolbarButton build() {
            return new IconToolbarButton(this);
        }
    }
}
