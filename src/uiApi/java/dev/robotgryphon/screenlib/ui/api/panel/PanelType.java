package dev.robotgryphon.screenlib.ui.api.panel;

import dev.robotgryphon.screenlib.ui.api.UiHost;

@FunctionalInterface
public interface PanelType<P extends Panel> {
    P create(UiHost host);
}
