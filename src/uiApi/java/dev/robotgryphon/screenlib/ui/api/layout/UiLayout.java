package dev.robotgryphon.screenlib.ui.api.layout;

import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public interface UiLayout {
    <T extends LayoutElement> T add(T element, Placement placement);

    default <T extends LayoutElement> T anchor(T element, HorizontalAnchor horizontal, VerticalAnchor vertical,
                                               int offsetX, int offsetY) {
        return this.add(element, Placement.anchored(horizontal, vertical, offsetX, offsetY));
    }

    default <T extends LayoutElement> T absolute(T element, int x, int y) {
        return this.add(element, Placement.absolute(x, y));
    }

    ScreenRectangle screen();
}
