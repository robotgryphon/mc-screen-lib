package dev.robotgryphon.screenlib.client.ui.core.layout;

import net.minecraft.client.gui.layouts.AbstractLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UiScreenLayout extends AbstractLayout {
    private final List<Entry> entries = new ArrayList<>();

    public UiScreenLayout(ScreenRectangle screen) {
        super(screen.left(), screen.top(), screen.width(), screen.height());
    }

    private record Entry(LayoutElement element, Placement placement) {}

    public <T extends LayoutElement> T add(T element, Placement placement) {
        this.entries.add(new Entry(element, placement));
        return element;
    }

    public <T extends LayoutElement> T anchor(T element, HorizontalAnchor horizontal, VerticalAnchor vertical,
                                              int offsetX, int offsetY) {
        return this.add(element, Placement.anchored(horizontal, vertical, offsetX, offsetY));
    }

    public <T extends LayoutElement> T absolute(T element, int x, int y) {
        return this.add(element, Placement.absolute(x, y));
    }

    public ScreenRectangle screen() {
        return this.getRectangle();
    }

    @Override
    public void arrangeElements() {
        for (Entry entry : this.entries) {
            if (entry.element() instanceof Layout layout) {
                layout.arrangeElements();
            }
            entry.placement().place(entry.element(), this.screen());
        }
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        this.entries.forEach(entry -> visitor.accept(entry.element()));
    }

    @Override
    public void removeChildren() {
        this.entries.clear();
    }
}
