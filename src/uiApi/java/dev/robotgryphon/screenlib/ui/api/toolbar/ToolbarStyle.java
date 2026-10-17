package dev.robotgryphon.screenlib.ui.api.toolbar;

import dev.robotgryphon.screenlib.ui.api.widget.RoundedShapes;
import dev.robotgryphon.screenlib.ui.api.render.RoundedRectRenderState;
import dev.robotgryphon.screenlib.ui.api.render.RoundedRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.List;

public final class ToolbarStyle {
    public static final int THICKNESS = 14;
    public static final int GAP = 2;
    public static final int RADIUS = 4;
    public static final int TOOLBAR_RADIUS = 2;
    public static final int BUTTON_RADIUS = 2;
    private static final int SEPARATOR_INSET = 3;

    public static final int BACKGROUND = 0xCC1F1F23;
    public static final int HEADER = 0xFF3A3A45;
    public static final int OUTLINE = 0xFF7F7F8C;
    public static final int BORDER = 0xFF3A3B44;
    public static final int BUTTON_HOVER = 0x22FFFFFF;
    public static final int BUTTON_ACTIVE = 0x33FFFFFF;
    public static final int ICON = 0xFFC8CAD4;
    public static final int TEXT = 0xFFE0E0E0;
    public static final int BADGE = 0xFF3B8EEA;
    public static final int BADGE_SIZE = 5;

    private ToolbarStyle() {}

    public static void background(GuiGraphicsExtractor graphics, ScreenRectangle area) {
        background(graphics, area, 0);
    }

    public static void background(GuiGraphicsExtractor graphics, ScreenRectangle area, int headerHeight) {
        background(graphics, area, headerHeight, RADIUS);
    }

    public static void background(GuiGraphicsExtractor graphics, ScreenRectangle area, int headerHeight, int radius) {
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        float zoom = pose.m00;
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();

        Vector2f topLeft = pose.transformPosition(area.left(), area.top(), new Vector2f());
        Vector2f bottomRight = pose.transformPosition(area.right(), area.bottom(), new Vector2f());
        int x0 = (int) Math.floor(topLeft.x()) - 1;
        int y0 = (int) Math.floor(topLeft.y()) - 1;
        int x1 = (int) Math.ceil(bottomRight.x()) + 1;
        int y1 = (int) Math.ceil(bottomRight.y()) + 1;
        ScreenRectangle textureBounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0);

        var entry = new RoundedRect(
                new Vector4f((topLeft.x() - x0) * guiScale, (topLeft.y() - y0) * guiScale,
                        (bottomRight.x() - topLeft.x()) * guiScale, (bottomRight.y() - topLeft.y()) * guiScale),
                color(BACKGROUND), color(HEADER), color(OUTLINE),
                headerHeight * zoom * guiScale, false);

        graphics.submitPictureInPictureRenderState(new RoundedRectRenderState(textureBounds, List.of(entry),
                radius * zoom * guiScale, 1f, Math.max(1f, 0.5f * zoom * guiScale), graphics.peekScissorStack()));
        graphics.nextStratum();
    }

    private static Vector4f color(int argb) {
        return new Vector4f(ARGB.redFloat(argb), ARGB.greenFloat(argb), ARGB.blueFloat(argb), ARGB.alphaFloat(argb));
    }

    public static void buttonBackground(GuiGraphicsExtractor graphics, ScreenRectangle area, boolean hovered, boolean active) {
        if (active || hovered) {
            RoundedShapes.fillRoundedRect(graphics, area.left(), area.top(), area.right(), area.bottom(), BUTTON_RADIUS,
                    active ? BUTTON_ACTIVE : BUTTON_HOVER);
        }
    }

    public static void separator(GuiGraphicsExtractor graphics, ScreenRectangle bar, ToolbarOrientation orientation, int position) {
        if (orientation == ToolbarOrientation.HORIZONTAL) {
            graphics.fill(position, bar.top() + SEPARATOR_INSET, position + 1, bar.bottom() - SEPARATOR_INSET, BORDER);
        } else {
            graphics.fill(bar.left() + SEPARATOR_INSET, position, bar.right() - SEPARATOR_INSET, position + 1, BORDER);
        }
    }

    public static void chevron(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 5, y + 1, ICON);
        graphics.fill(x + 1, y + 1, x + 4, y + 2, ICON);
        graphics.fill(x + 2, y + 2, x + 3, y + 3, ICON);
    }

    public static void badge(GuiGraphicsExtractor graphics, int x, int y) {
        RoundedShapes.fillRoundedRect(graphics, x, y, x + BADGE_SIZE, y + BADGE_SIZE, BADGE_SIZE / 2, BADGE);
    }
}
