package dev.robotgryphon.screenlib.client.ui.core;

import dev.robotgryphon.screenlib.client.ui.core.layout.Placement;
import dev.robotgryphon.screenlib.client.ui.core.layout.UiScreenLayout;
import dev.robotgryphon.screenlib.client.ui.widget.ContextMenu;
import dev.robotgryphon.screenlib.client.ui.widget.panel.Panel;
import dev.robotgryphon.screenlib.client.ui.widget.panel.PanelType;
import com.mojang.blaze3d.platform.cursor.CursorType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class UiScreen extends Screen {
    private final Function<UiContext, LayoutElement> root;
    private final boolean pausesGame;
    private final UiHost host;

    protected UiScreen(Builder builder) {
        super(builder.title);
        this.root = builder.root;
        this.pausesGame = builder.pausesGame;
        this.host = new UiHost(builder.panelSlots);
        builder.pinnedPanels.forEach(pinned -> this.host.pin(pinned.placement(), pinned.type()));
    }

    public static Builder builder(Component title) {
        return new Builder(title);
    }

    public UiHost host() {
        return this.host;
    }

    public void open() {
        Minecraft.getInstance().gui.setScreen(this);
    }

    @Override
    protected void init() {
        ScreenRectangle area = new ScreenRectangle(0, 0, this.width, this.height);
        this.host.arrangePinned(area).forEach(this::addRenderableWidget);

        LayoutElement element = this.root.apply(new UiContext(area, this.host));
        if (element instanceof Layout layout) {
            layout.arrangeElements();
        }
        element.setPosition(area.left(), area.top());
        element.visitWidgets(this::addRenderableWidget);

        this.host.arrangeSlots(area).forEach(this::addRenderableWidget);
    }

    @Override
    public Optional<GuiEventListener> getChildAt(double x, double y) {
        List<? extends GuiEventListener> children = this.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            GuiEventListener child = children.get(i);
            if (child.isMouseOver(x, y)) {
                return Optional.of(child);
            }
        }
        return Optional.empty();
    }

    @Override
    public void removed() {
        this.host.removed();
        super.removed();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        GuiEventListener target = this.isDragging() && this.getFocused() != null
                ? this.getFocused()
                : this.getChildAt(mouseX, mouseY).orElse(null);
        if (target instanceof Panel panel) {
            CursorType cursor = panel.cursor(mouseX, mouseY);
            if (cursor != null) {
                graphics.requestCursor(cursor);
            }
        }
        ContextMenu menu = this.host.menu();
        if (menu != null) {
            graphics.nextStratum();
            menu.render(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        ContextMenu menu = this.host.takeMenu();
        if (menu != null) {
            menu.mouseClicked(event.x(), event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return this.pausesGame;
    }

    private record PinnedPanelType(Placement placement, PanelType<?> type) {}

    public static final class Builder {
        private final Component title;
        private Function<UiContext, LayoutElement> root = context -> new FrameLayout(context.screen().width(), context.screen().height());
        private final List<UiHost.PanelSlot> panelSlots = new ArrayList<>();
        private final List<PinnedPanelType> pinnedPanels = new ArrayList<>();
        private boolean pausesGame;

        private Builder(Component title) {
            this.title = title;
        }

        public Builder root(Function<UiContext, LayoutElement> root) {
            this.root = root;
            return this;
        }

        public Builder layout(BiConsumer<UiScreenLayout, UiHost> configure) {
            return this.root(context -> {
                UiScreenLayout layout = new UiScreenLayout(context.screen());
                configure.accept(layout, context.host());
                return layout;
            });
        }

        @SafeVarargs
        public final Builder panels(Placement placement, ResourceKey<PanelType<?>>... panels) {
            this.panelSlots.add(new UiHost.PanelSlot(placement, List.of(panels), false));
            return this;
        }

        @SafeVarargs
        public final Builder tabs(Placement placement, ResourceKey<PanelType<?>>... panels) {
            this.panelSlots.add(new UiHost.PanelSlot(placement, List.of(panels), true));
            return this;
        }

        public Builder panel(Placement placement, PanelType<?> panel) {
            this.pinnedPanels.add(new PinnedPanelType(placement, panel));
            return this;
        }

        public Builder pausesGame(boolean pausesGame) {
            this.pausesGame = pausesGame;
            return this;
        }

        public UiScreen build() {
            return new UiScreen(this);
        }

        public <M extends AbstractContainerMenu> MenuUiScreen<M> build(M menu) {
            return new MenuUiScreen<>(this, menu);
        }
    }
}
