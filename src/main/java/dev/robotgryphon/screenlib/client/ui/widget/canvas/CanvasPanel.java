package dev.robotgryphon.screenlib.client.ui.widget.canvas;

import com.mojang.blaze3d.platform.cursor.CursorType;
import dev.robotgryphon.screenlib.client.ui.CanvasSession;
import dev.robotgryphon.screenlib.client.ui.core.UiHost;
import dev.robotgryphon.screenlib.client.ui.widget.CanvasWidget;
import dev.robotgryphon.screenlib.client.ui.widget.ContextMenu;
import dev.robotgryphon.screenlib.client.ui.widget.panel.Panel;
import dev.robotgryphon.screenlib.client.ui.widget.panel.PanelType;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.Toolbar;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.ToolbarContext;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.ToolbarOrientation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class CanvasPanel extends Panel {
    private static final int MIN_WIDTH = 160;
    private static final int MIN_HEIGHT = 120;
    private static final int OFFSCREEN = -10_000;

    private final UiHost host;
    private final CanvasSession session;
    private final CanvasWidget canvas;
    private final Toolbar toolbar;
    private final MinimapPanel minimap;
    private @Nullable AbstractWidget pressed;
    private boolean placed;

    public CanvasPanel(UiHost host, CanvasSession session, Component title) {
        super(MIN_WIDTH, MIN_HEIGHT, title);
        this.host = host;
        this.session = session;
        this.canvas = new CanvasWidget(session.canvas(), 0, 0, 0, 0);
        this.canvas.menus(host::openMenu);
        this.toolbar = new Toolbar(new Context(), ToolbarOrientation.VERTICAL,
                List.of(List.of(CanvasTools.FIT, CanvasTools.ZOOM, CanvasTools.MINIMAP)));
        this.minimap = new MinimapPanel(this);
        this.closable(false);
        this.draggable(true);
        this.resizable(MIN_WIDTH, MIN_HEIGHT);
        this.open();
    }

    public CanvasSession session() {
        return this.session;
    }

    public boolean isMinimapOpen() {
        return this.minimap.isOpen();
    }

    public void toggleMinimap() {
        this.minimap.toggle();
    }

    public CanvasWidget canvas() {
        ScreenRectangle content = this.contentArea();
        if (!content.equals(this.canvas.getRectangle())) {
            this.canvas.setPosition(content.left(), content.top());
            this.canvas.setSize(content.width(), content.height());
        }
        if (!this.placed) {
            this.placed = true;
            this.session.savedViewport().ifPresentOrElse(state -> this.canvas.viewport().apply(state, content),
                    this.canvas::fitToContent);
        }
        return this.canvas;
    }

    private void layoutOverlays() {
        ScreenRectangle content = this.contentArea();
        this.toolbar.setPosition(content.right() - PADDING - this.toolbar.getWidth(),
                content.bottom() - PADDING - this.toolbar.getHeight());
        this.minimap.setSize(MinimapPanel.WIDTH, MinimapPanel.HEIGHT);
        this.minimap.setPosition(this.toolbar.getX() - Toolbar.GAP * 4 - this.minimap.getWidth(),
                this.toolbar.getBottom() - this.minimap.getHeight());
        this.minimap.constrain(content);
    }

    private @Nullable AbstractWidget overlayAt(double mouseX, double mouseY) {
        this.layoutOverlays();
        if (this.toolbar.isMouseOver(mouseX, mouseY)) {
            return this.toolbar;
        }
        if (this.minimap.isMouseOver(mouseX, mouseY)) {
            return this.minimap;
        }
        return null;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, ScreenRectangle content, int mouseX, int mouseY) {
        boolean overlay = this.overlayAt(mouseX, mouseY) != null;
        this.canvas().extractRenderState(graphics, overlay ? OFFSCREEN : mouseX, overlay ? OFFSCREEN : mouseY, 0);
        this.toolbar.extractRenderState(graphics, mouseX, mouseY, 0);
        this.minimap.extractRenderState(graphics, mouseX, mouseY, 0);
    }

    @Override
    public @Nullable CursorType cursor(double mouseX, double mouseY) {
        if (this.overlayAt(mouseX, mouseY) == this.minimap) {
            return this.minimap.cursor(mouseX, mouseY);
        }
        return super.cursor(mouseX, mouseY);
    }

    @Override
    protected boolean contentClicked(MouseButtonEvent event, boolean doubleClick) {
        AbstractWidget overlay = this.overlayAt(event.x(), event.y());
        this.pressed = overlay != null ? overlay : this.canvas();
        return this.pressed.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean contentDragged(MouseButtonEvent event, double dx, double dy) {
        return this.pressed != null && this.pressed.mouseDragged(event, dx, dy);
    }

    @Override
    protected boolean contentReleased(MouseButtonEvent event) {
        AbstractWidget pressed = this.pressed;
        this.pressed = null;
        return pressed != null && pressed.mouseReleased(event);
    }

    @Override
    protected boolean contentScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.overlayAt(mouseX, mouseY) != null) {
            return true;
        }
        return this.canvas().mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.canvas().mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.canvas().keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return this.canvas().charTyped(event);
    }

    @Override
    public void removed() {
        if (this.placed) {
            this.session.rememberViewport(this.canvas.viewport().state(this.canvas.getRectangle()));
        }
    }

    private final class Context implements ToolbarContext {
        @Override
        public void openMenu(ContextMenu menu) {
            CanvasPanel.this.host.openMenu(menu);
        }

        @Override
        public boolean isPanelOpen(ResourceKey<PanelType<?>> panel) {
            return CanvasPanel.this.host.isPanelOpen(panel);
        }

        @Override
        public void togglePanel(ResourceKey<PanelType<?>> panel) {
            CanvasPanel.this.host.togglePanel(panel);
        }

        @Override
        public <P extends Panel> Optional<P> panel(Class<P> type) {
            if (type.isInstance(CanvasPanel.this)) {
                return Optional.of(type.cast(CanvasPanel.this));
            }
            return CanvasPanel.this.host.panel(type);
        }
    }
}
