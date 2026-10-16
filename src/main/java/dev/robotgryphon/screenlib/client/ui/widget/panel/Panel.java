package dev.robotgryphon.screenlib.client.ui.widget.panel;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.ToolbarStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public abstract class Panel extends AbstractWidget {
    public static final int HEADER = 14;
    public static final int PADDING = 5;
    private static final int GRIP = 3;

    private record Grab(boolean move, boolean left, boolean top, boolean right, boolean bottom,
                        double startX, double startY, ScreenRectangle start) {
        CursorType cursor() {
            if (this.move) {
                return CursorTypes.RESIZE_ALL;
            }
            boolean horizontal = this.left || this.right;
            boolean vertical = this.top || this.bottom;
            if (horizontal && vertical) {
                return CursorTypes.RESIZE_ALL;
            }
            return horizontal ? CursorTypes.RESIZE_EW : CursorTypes.RESIZE_NS;
        }
    }

    private boolean closable = true;
    private boolean draggable;
    private boolean resizable;
    private int minWidth;
    private int minHeight;
    private boolean moved;
    private @Nullable ScreenRectangle bounds;
    private @Nullable Grab grab;

    protected Panel(int width, int height, Component title) {
        super(0, 0, width, height, title);
        this.visible = false;
        this.minWidth = width;
        this.minHeight = height;
    }

    public void closable(boolean closable) {
        this.closable = closable;
    }

    protected void draggable(boolean draggable) {
        this.draggable = draggable;
    }

    protected void resizable(int minWidth, int minHeight) {
        this.resizable = true;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
    }

    public boolean isOpen() {
        return this.visible;
    }

    public void open() {
        this.visible = true;
    }

    public void close() {
        this.visible = false;
    }

    public void toggle() {
        this.visible = !this.visible;
    }

    public boolean isMoved() {
        return this.moved;
    }

    public void constrain(ScreenRectangle bounds) {
        this.bounds = bounds;
        int width = Math.min(this.getWidth(), bounds.width());
        int height = Math.min(this.getHeight(), bounds.height());
        int x = Mth.clamp(this.getX(), bounds.left(), bounds.right() - width);
        int y = Mth.clamp(this.getY(), bounds.top(), bounds.bottom() - height);
        this.setRectangle(width, height, x, y);
    }

    public void removed() {
    }

    public @Nullable CursorType cursor(double mouseX, double mouseY) {
        Grab hover = this.grab != null ? this.grab : this.grabAt(mouseX, mouseY);
        return hover != null ? hover.cursor() : null;
    }

    protected ScreenRectangle header() {
        return new ScreenRectangle(this.getX(), this.getY(), this.getWidth(), HEADER);
    }

    protected ScreenRectangle closeButton() {
        return new ScreenRectangle(this.getRight() - HEADER, this.getY(), HEADER, HEADER);
    }

    protected ScreenRectangle contentArea() {
        return new ScreenRectangle(this.getX() + PADDING, this.getY() + HEADER,
                this.getWidth() - 2 * PADDING, this.getHeight() - HEADER - PADDING);
    }

    protected abstract void renderContent(GuiGraphicsExtractor graphics, ScreenRectangle content, int mouseX, int mouseY);

    protected boolean contentClicked(MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    protected boolean contentDragged(MouseButtonEvent event, double dx, double dy) {
        return false;
    }

    protected boolean contentReleased(MouseButtonEvent event) {
        return false;
    }

    protected boolean contentScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    private @Nullable Grab grabAt(double mouseX, double mouseY) {
        int x = Mth.floor(mouseX);
        int y = Mth.floor(mouseY);
        if (this.resizable) {
            boolean left = x < this.getX() + GRIP;
            boolean top = y < this.getY() + GRIP;
            boolean right = x >= this.getRight() - GRIP;
            boolean bottom = y >= this.getBottom() - GRIP;
            if (left || top || right || bottom) {
                return new Grab(false, left, top, right, bottom, mouseX, mouseY, this.getRectangle());
            }
        }
        if (this.draggable && this.header().containsPoint(x, y)
                && !(this.closable && this.closeButton().containsPoint(x, y))) {
            return new Grab(true, false, false, false, false, mouseX, mouseY, this.getRectangle());
        }
        return null;
    }

    private void applyGrab(Grab grab, double mouseX, double mouseY) {
        ScreenRectangle limit = this.bounds != null ? this.bounds : grab.start();
        ScreenRectangle start = grab.start();
        int dx = Mth.floor(mouseX - grab.startX());
        int dy = Mth.floor(mouseY - grab.startY());
        if (grab.move()) {
            int x = Mth.clamp(start.left() + dx, limit.left(), limit.right() - start.width());
            int y = Mth.clamp(start.top() + dy, limit.top(), limit.bottom() - start.height());
            this.setPosition(x, y);
        } else {
            int left = start.left();
            int top = start.top();
            int right = start.right();
            int bottom = start.bottom();
            if (grab.left()) {
                left = Mth.clamp(left + dx, limit.left(), right - this.minWidth);
            }
            if (grab.right()) {
                right = Mth.clamp(right + dx, left + this.minWidth, limit.right());
            }
            if (grab.top()) {
                top = Mth.clamp(top + dy, limit.top(), bottom - this.minHeight);
            }
            if (grab.bottom()) {
                bottom = Mth.clamp(bottom + dy, top + this.minHeight, limit.bottom());
            }
            this.setRectangle(right - left, bottom - top, left, top);
        }
        this.moved = true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && super.isMouseOver(mouseX, mouseY);
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo info) {
        return info.button() == InputConstants.MOUSE_BUTTON_LEFT || info.button() == InputConstants.MOUSE_BUTTON_RIGHT;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.isActive() || !this.visible || !this.isValidClickButton(event.buttonInfo())
                || !this.isMouseOver(event.x(), event.y())) {
            return false;
        }
        boolean primary = event.button() == InputConstants.MOUSE_BUTTON_LEFT;
        if (primary) {
            Grab grab = this.grabAt(event.x(), event.y());
            if (grab != null) {
                this.grab = grab;
                return true;
            }
            if (this.closable && this.closeButton().containsPoint(Mth.floor(event.x()), Mth.floor(event.y()))) {
                this.close();
                return true;
            }
        }
        if (this.contentArea().containsPoint(Mth.floor(event.x()), Mth.floor(event.y()))) {
            this.contentClicked(event, doubleClick);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.grab != null) {
            this.applyGrab(this.grab, event.x(), event.y());
            return true;
        }
        return this.contentDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.grab != null) {
            this.grab = null;
            return true;
        }
        return this.contentReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.contentArea().containsPoint(Mth.floor(mouseX), Mth.floor(mouseY))) {
            this.contentScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return true;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ToolbarStyle.background(graphics, this.getRectangle(), HEADER);
        Font font = Minecraft.getInstance().font;
        graphics.text(font, this.getMessage(), this.getX() + PADDING,
                this.getY() + (HEADER - font.lineHeight) / 2 + 1, ToolbarStyle.ICON, false);

        if (this.closable) {
            ScreenRectangle close = this.closeButton();
            if (close.containsPoint(mouseX, mouseY)) {
                graphics.fill(close.left() + 2, close.top() + 2, close.right() - 2, close.bottom() - 2, ToolbarStyle.BUTTON_HOVER);
            }
            cross(graphics, close.left() + (close.width() - 5) / 2, close.top() + (close.height() - 5) / 2);
        }

        this.renderContent(graphics, this.contentArea(), mouseX, mouseY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    private static void cross(GuiGraphicsExtractor graphics, int x, int y) {
        for (int i = 0; i < 5; i++) {
            graphics.fill(x + i, y + i, x + i + 1, y + i + 1, ToolbarStyle.ICON);
            graphics.fill(x + 4 - i, y + i, x + 5 - i, y + i + 1, ToolbarStyle.ICON);
        }
    }
}
