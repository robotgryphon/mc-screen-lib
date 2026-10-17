package dev.robotgryphon.screenlib.ui.api;

import net.minecraft.client.gui.navigation.ScreenRectangle;

public record UiContext(ScreenRectangle screen, UiHost host) {
}
