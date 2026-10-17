package dev.robotgryphon.screenlib.ui.client;

import dev.robotgryphon.screenlib.ui.api.layout.Placement;
import dev.robotgryphon.screenlib.ui.api.widget.ContextMenu;
import dev.robotgryphon.screenlib.ui.api.panel.Panel;
import dev.robotgryphon.screenlib.ui.api.panel.PanelType;
import dev.robotgryphon.screenlib.ui.api.panel.PanelTypes;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import dev.robotgryphon.screenlib.ui.api.UiHost;

public final class ScreenUiHost implements UiHost {
    public record PanelSlot(Placement placement, List<ResourceKey<PanelType<?>>> panels, boolean tabbed) {}

    public record PinnedPanel(Placement placement, Panel panel) {}

    private final List<PanelSlot> slots;
    private final List<PinnedPanel> pinned = new ArrayList<>();
    private final Map<ResourceKey<PanelType<?>>, Panel> panels = new LinkedHashMap<>();
    private @Nullable ContextMenu menu;

    ScreenUiHost(List<PanelSlot> slots) {
        this.slots = List.copyOf(slots);
    }

    void pin(Placement placement, PanelType<?> type) {
        this.pinned.add(new PinnedPanel(placement, type.create(this)));
    }

    public Optional<Panel> panel(ResourceKey<PanelType<?>> key) {
        Panel existing = this.panels.get(key);
        if (existing != null) {
            return Optional.of(existing);
        }
        PanelType<?> type = PanelTypes.get(key);
        if (type == null) {
            return Optional.empty();
        }
        Panel created = type.create(this);
        if (this.slot(key).filter(PanelSlot::tabbed).isPresent()) {
            created.closable(false);
        }
        this.panels.put(key, created);
        return Optional.of(created);
    }

    @Override
    public <P extends Panel> Optional<P> panel(Class<P> type) {
        return this.panels(type).stream().findFirst();
    }

    @Override
    public <P extends Panel> List<P> panels(Class<P> type) {
        return Stream.concat(this.pinned.stream().map(PinnedPanel::panel), this.panels.values().stream())
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    private Optional<PanelSlot> slot(ResourceKey<PanelType<?>> key) {
        return this.slots.stream().filter(candidate -> candidate.panels().contains(key)).findFirst();
    }

    @Override
    public boolean isPanelOpen(ResourceKey<PanelType<?>> key) {
        Panel panel = this.panels.get(key);
        return panel != null && panel.isOpen();
    }

    @Override
    public void togglePanel(ResourceKey<PanelType<?>> key) {
        Optional<PanelSlot> slot = this.slot(key);
        Optional<Panel> panel = this.panel(key);
        if (slot.isEmpty() || panel.isEmpty()) {
            return;
        }
        if (panel.get().isOpen()) {
            panel.get().close();
            return;
        }
        for (ResourceKey<PanelType<?>> other : slot.get().panels()) {
            Panel sibling = this.panels.get(other);
            if (sibling != null) {
                sibling.close();
            }
        }
        panel.get().open();
    }

    @Override
    public void openMenu(ContextMenu menu) {
        this.menu = menu;
    }

    @Nullable ContextMenu takeMenu() {
        ContextMenu menu = this.menu;
        this.menu = null;
        return menu;
    }

    @Nullable ContextMenu menu() {
        return this.menu;
    }

    List<Panel> arrangePinned(ScreenRectangle area) {
        List<Panel> arranged = new ArrayList<>(this.pinned.size());
        for (PinnedPanel entry : this.pinned) {
            arranged.add(place(entry.panel(), entry.placement(), area));
        }
        return arranged;
    }

    List<Panel> arrangeSlots(ScreenRectangle area) {
        List<Panel> arranged = new ArrayList<>();
        for (PanelSlot slot : this.slots) {
            for (ResourceKey<PanelType<?>> key : slot.panels()) {
                this.panel(key).ifPresent(panel -> arranged.add(place(panel, slot.placement(), area)));
            }
        }
        return arranged;
    }

    void removed() {
        this.pinned.forEach(entry -> entry.panel().removed());
        this.panels.values().forEach(Panel::removed);
    }

    private static Panel place(Panel panel, Placement placement, ScreenRectangle area) {
        if (!panel.isMoved()) {
            placement.place(panel, area);
        }
        panel.constrain(area);
        return panel;
    }
}
