package dev.robotgryphon.screenlib.client.ui.widget.panel;

import dev.robotgryphon.screenlib.client.ui.core.UiHost;

@FunctionalInterface
public interface PanelType<P extends Panel> {
    P create(UiHost host);
}
