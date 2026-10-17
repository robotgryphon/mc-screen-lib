package dev.robotgryphon.screenlib.ui.client.toolbar;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarButtons;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarOrientation;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarContext;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarButton;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarStyle;

public class Toolbar extends AbstractWidget {
    private static final int THICKNESS = ToolbarStyle.THICKNESS;
    private static final int GAP = ToolbarStyle.GAP;

    protected final ToolbarContext context;
    private final ToolbarOrientation orientation;
    private final List<List<ResourceKey<ToolbarButton>>> groups;

    public Toolbar(ToolbarContext context, ToolbarOrientation orientation, List<List<ResourceKey<ToolbarButton>>> groups) {
        super(0, 0, THICKNESS, THICKNESS, Component.empty());
        this.context = context;
        this.orientation = orientation;
        this.groups = groups.stream().map(List::copyOf).toList();
        this.slots();
    }

    protected record Slot(ToolbarButton button, ScreenRectangle area, boolean endsGroup) {}

    public ToolbarOrientation orientation() {
        return this.orientation;
    }

    private boolean horizontal() {
        return this.orientation == ToolbarOrientation.HORIZONTAL;
    }

    private List<List<ToolbarButton>> resolvedGroups() {
        List<List<ToolbarButton>> resolved = new ArrayList<>();
        for (List<ResourceKey<ToolbarButton>> group : this.groups) {
            List<ToolbarButton> buttons = new ArrayList<>();
            for (ResourceKey<ToolbarButton> key : group) {
                ToolbarButton button = ToolbarButtons.get(key);
                if (button != null) {
                    buttons.add(button);
                }
            }
            if (!buttons.isEmpty()) {
                resolved.add(buttons);
            }
        }
        return resolved;
    }

    protected List<Slot> slots() {
        List<List<ToolbarButton>> groups = this.resolvedGroups();
        int cross = 0;
        for (List<ToolbarButton> group : groups) {
            for (ToolbarButton button : group) {
                cross = Math.max(cross, this.horizontal() ? ToolbarButton.SIZE : button.width(this.context));
            }
        }
        int thickness = Math.max(THICKNESS, cross + 2 * GAP);

        List<Slot> slots = new ArrayList<>();
        int main = GAP;
        for (int g = 0; g < groups.size(); g++) {
            if (g > 0) {
                main += 1 + GAP;
            }
            List<ToolbarButton> group = groups.get(g);
            for (int i = 0; i < group.size(); i++) {
                ToolbarButton button = group.get(i);
                int width = button.width(this.context);
                ScreenRectangle area = this.horizontal()
                        ? new ScreenRectangle(this.getX() + main, this.getY() + (thickness - ToolbarButton.SIZE) / 2,
                                width, ToolbarButton.SIZE)
                        : new ScreenRectangle(this.getX() + (thickness - width) / 2, this.getY() + main,
                                width, ToolbarButton.SIZE);
                slots.add(new Slot(button, area, i == group.size() - 1 && g < groups.size() - 1));
                main += (this.horizontal() ? width : ToolbarButton.SIZE) + GAP;
            }
        }

        if (this.horizontal()) {
            this.setSize(main, thickness);
        } else {
            this.setSize(thickness, main);
        }
        return slots;
    }

    protected static boolean contains(ScreenRectangle area, double x, double y) {
        return area.containsPoint(Mth.floor(x), Mth.floor(y));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        List<Slot> slots = this.slots();
        ScreenRectangle bar = this.getRectangle();
        ToolbarStyle.background(graphics, bar, 0, ToolbarStyle.TOOLBAR_RADIUS);
        for (Slot slot : slots) {
            boolean hovered = contains(slot.area(), mouseX, mouseY);
            ToolbarStyle.buttonBackground(graphics, slot.area(), hovered, slot.button().isActive(this.context));
            slot.button().render(graphics, slot.area(), this.context);
            if (slot.endsGroup()) {
                int position = this.horizontal() ? slot.area().right() + GAP : slot.area().bottom() + GAP;
                ToolbarStyle.separator(graphics, bar, this.orientation, position);
            }
            if (hovered) {
                graphics.setTooltipForNextFrame(slot.button().tooltip(this.context), mouseX, mouseY);
            }
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        for (Slot slot : this.slots()) {
            if (contains(slot.area(), event.x(), event.y())) {
                slot.button().onClick(this.context, slot.area());
                return;
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
