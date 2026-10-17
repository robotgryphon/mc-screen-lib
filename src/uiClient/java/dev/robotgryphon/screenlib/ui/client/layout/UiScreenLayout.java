package dev.robotgryphon.screenlib.ui.client.layout;

import dev.robotgryphon.screenlib.ui.api.layout.UiLayout;
import net.minecraft.client.gui.layouts.AbstractLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import dev.robotgryphon.screenlib.ui.api.layout.HorizontalAnchor;
import dev.robotgryphon.screenlib.ui.api.layout.VerticalAnchor;
import dev.robotgryphon.screenlib.ui.api.layout.Placement;

public class UiScreenLayout extends AbstractLayout implements UiLayout {
    private final List<Entry> entries = new ArrayList<>();

    public UiScreenLayout(ScreenRectangle screen) {
        super(screen.left(), screen.top(), screen.width(), screen.height());
    }

    private record Entry(LayoutElement element, Placement placement) {}

    @Override
    public <T extends LayoutElement> T add(T element, Placement placement) {
        this.entries.add(new Entry(element, placement));
        return element;
    }

    @Override
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
