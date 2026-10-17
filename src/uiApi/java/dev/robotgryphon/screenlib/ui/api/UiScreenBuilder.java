package dev.robotgryphon.screenlib.ui.api;

import dev.robotgryphon.screenlib.ui.api.layout.Placement;
import dev.robotgryphon.screenlib.ui.api.layout.UiLayout;
import dev.robotgryphon.screenlib.ui.api.panel.PanelType;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface UiScreenBuilder {
    UiScreenBuilder root(Function<UiContext, LayoutElement> root);

    UiScreenBuilder layout(BiConsumer<UiLayout, UiHost> configure);

    UiScreenBuilder panel(Placement placement, PanelType<?> panel);

    UiScreenBuilder panels(Placement placement, List<ResourceKey<PanelType<?>>> panels);

    UiScreenBuilder tabs(Placement placement, List<ResourceKey<PanelType<?>>> panels);

    UiScreenBuilder pausesGame(boolean pausesGame);

    Screen build();

    void open();
}
