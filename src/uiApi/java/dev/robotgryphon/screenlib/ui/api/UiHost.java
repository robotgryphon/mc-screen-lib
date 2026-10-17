package dev.robotgryphon.screenlib.ui.api;

import dev.robotgryphon.screenlib.ui.api.panel.Panel;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarContext;

import java.util.List;

public interface UiHost extends ToolbarContext {
    <P extends Panel> List<P> panels(Class<P> type);
}
