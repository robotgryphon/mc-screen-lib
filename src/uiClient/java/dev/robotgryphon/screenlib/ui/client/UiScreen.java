package dev.robotgryphon.screenlib.ui.client;

import dev.robotgryphon.screenlib.ui.api.layout.Placement;
import dev.robotgryphon.screenlib.ui.client.layout.UiScreenLayout;
import dev.robotgryphon.screenlib.ui.api.widget.ContextMenu;
import dev.robotgryphon.screenlib.ui.api.panel.Panel;
import dev.robotgryphon.screenlib.ui.api.panel.PanelType;
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
import dev.robotgryphon.screenlib.ui.common.HostedScreen;
import dev.robotgryphon.screenlib.ui.api.UiHost;
import dev.robotgryphon.screenlib.ui.api.UiContext;
import dev.robotgryphon.screenlib.ui.api.UiScreenBuilder;
import dev.robotgryphon.screenlib.ui.api.layout.UiLayout;

public class UiScreen extends Screen implements HostedScreen {
    private final Function<UiContext, LayoutElement> root;
    private final boolean pausesGame;
    private final ScreenUiHost host;

    protected UiScreen(Builder builder) {
        super(builder.title);
        this.root = builder.root;
        this.pausesGame = builder.pausesGame;
        this.host = new ScreenUiHost(builder.panelSlots);
        builder.pinnedPanels.forEach(pinned -> this.host.pin(pinned.placement(), pinned.type()));
    }

    public static Builder builder(Component title) {
        return new Builder(title);
    }

    @Override
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

    public static final class Builder implements UiScreenBuilder {
        private final Component title;
        private Function<UiContext, LayoutElement> root = context -> new FrameLayout(context.screen().width(), context.screen().height());
        private final List<ScreenUiHost.PanelSlot> panelSlots = new ArrayList<>();
        private final List<PinnedPanelType> pinnedPanels = new ArrayList<>();
        private boolean pausesGame;

        private Builder(Component title) {
            this.title = title;
        }

        @Override
        public Builder root(Function<UiContext, LayoutElement> root) {
            this.root = root;
            return this;
        }

        @Override
        public Builder layout(BiConsumer<UiLayout, UiHost> configure) {
            return this.root(context -> {
                UiScreenLayout layout = new UiScreenLayout(context.screen());
                configure.accept(layout, context.host());
                return layout;
            });
        }

        @Override
        public Builder panels(Placement placement, List<ResourceKey<PanelType<?>>> panels) {
            this.panelSlots.add(new ScreenUiHost.PanelSlot(placement, List.copyOf(panels), false));
            return this;
        }

        @Override
        public Builder tabs(Placement placement, List<ResourceKey<PanelType<?>>> panels) {
            this.panelSlots.add(new ScreenUiHost.PanelSlot(placement, List.copyOf(panels), true));
            return this;
        }

        @Override
        public Builder panel(Placement placement, PanelType<?> panel) {
            this.pinnedPanels.add(new PinnedPanelType(placement, panel));
            return this;
        }

        @Override
        public Builder pausesGame(boolean pausesGame) {
            this.pausesGame = pausesGame;
            return this;
        }

        @Override
        public UiScreen build() {
            return new UiScreen(this);
        }

        @Override
        public void open() {
            this.build().open();
        }

        public <M extends AbstractContainerMenu> MenuUiScreen<M> build(M menu) {
            return new MenuUiScreen<>(this, menu);
        }
    }
}
