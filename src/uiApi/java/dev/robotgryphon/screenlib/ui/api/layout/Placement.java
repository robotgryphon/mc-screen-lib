package dev.robotgryphon.screenlib.ui.api.layout;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public sealed interface Placement {
    static Placement absolute(int x, int y) {
        return new Absolute(x, y);
    }

    static Placement anchored(HorizontalAnchor horizontal, VerticalAnchor vertical) {
        return new Anchored(horizontal, vertical, 0, 0);
    }

    static Placement anchored(HorizontalAnchor horizontal, VerticalAnchor vertical, int offsetX, int offsetY) {
        return new Anchored(horizontal, vertical, offsetX, offsetY);
    }

    static Placement inset(int left, int top, int right, int bottom) {
        return new Inset(left, top, right, bottom);
    }

    void place(LayoutElement element, ScreenRectangle screen);

    record Inset(int left, int top, int right, int bottom) implements Placement {
        @Override
        public void place(LayoutElement element, ScreenRectangle screen) {
            if (element instanceof AbstractWidget widget) {
                widget.setSize(Math.max(0, screen.width() - this.left - this.right),
                        Math.max(0, screen.height() - this.top - this.bottom));
            }
            element.setPosition(screen.left() + this.left, screen.top() + this.top);
        }
    }

    record Absolute(int x, int y) implements Placement {
        @Override
        public void place(LayoutElement element, ScreenRectangle screen) {
            element.setPosition(screen.left() + this.x, screen.top() + this.y);
        }
    }

    record Anchored(HorizontalAnchor horizontal, VerticalAnchor vertical, int offsetX, int offsetY) implements Placement {
        @Override
        public void place(LayoutElement element, ScreenRectangle screen) {
            element.setPosition(
                    this.horizontal.position(screen.left(), screen.width(), element.getWidth(), this.offsetX),
                    this.vertical.position(screen.top(), screen.height(), element.getHeight(), this.offsetY));
        }
    }
}
