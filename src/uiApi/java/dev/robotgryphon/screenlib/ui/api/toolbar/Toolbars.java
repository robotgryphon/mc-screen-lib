package dev.robotgryphon.screenlib.ui.api.toolbar;

import dev.robotgryphon.screenlib.ui.api.spi.UiProvider;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceKey;

import java.util.List;

public final class Toolbars {
    private Toolbars() {}

    public static AbstractWidget create(ToolbarContext context, ToolbarOrientation orientation,
                                        List<List<ResourceKey<ToolbarButton>>> groups) {
        return UiProvider.SLOT.get().toolbar(context, orientation, groups);
    }
}
