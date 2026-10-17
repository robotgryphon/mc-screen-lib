package dev.robotgryphon.screenlib.nodes.client.widget.property;

import dev.robotgryphon.screenlib.nodes.common.properties.PropertyManager;
import dev.robotgryphon.screenlib.graph.schema.properties.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.ui.api.render.RoundedRectRenderState;
import dev.robotgryphon.screenlib.ui.api.render.RoundedRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Optional;

public abstract class PropertyEditor<T> extends AbstractWidget {
    public static final int CORNER_RADIUS = 2;

    protected static final int BG_COLOR = 0xCC1A1B22;

    protected static final int VALUE_TEXT_COLOR = 0xFFFFFFFF;

    protected final PropertyManager properties;
    protected final StoredPropertyDefinition<T> property;

    protected PropertyEditor(int x, int y, int width, int height,
                             PropertyManager properties, StoredPropertyDefinition<T> property) {
        super(x, y, width, height, Component.empty());
        this.properties = properties;
        this.property = property;
    }

    protected Optional<T> current() {
        return this.properties.get(this.property);
    }

    protected void commit(Optional<T> value) {
        this.properties.set(this.property, value);
    }

    protected T toValue(Object value) {
        return this.property.type().dataType().cast(value);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager soundManager) {
    }

    public static boolean isInside(double mouseX, double mouseY,
                                   int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    protected static int textCenterY(int y, int height, Font font) {
        return y + (height - font.lineHeight) / 2 + 1;
    }

    protected static void renderPill(GuiGraphicsExtractor graphics,
                                     int x, int y, int width, int height,
                                     int color, float alpha) {
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        float zoom = pose.m00;
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();

        Vector2f tl = pose.transformPosition(x, y, new Vector2f());
        Vector2f br = pose.transformPosition(x + width, y + height, new Vector2f());

        int sx0 = (int) Math.floor(tl.x()) - 1;
        int sy0 = (int) Math.floor(tl.y()) - 1;
        int sx1 = (int) Math.ceil(br.x()) + 1;
        int sy1 = (int) Math.ceil(br.y()) + 1;
        var textureBounds = new net.minecraft.client.gui.navigation.ScreenRectangle(sx0, sy0, sx1 - sx0, sy1 - sy0);

        float pillRelX = (float) ((tl.x() - sx0) * guiScale);
        float pillRelY = (float) ((tl.y() - sy0) * guiScale);
        float pillW = (float) ((br.x() - tl.x()) * guiScale);
        float pillH = (float) ((br.y() - tl.y()) * guiScale);

        int finalColor = ARGB.multiply(color, ARGB.white(alpha));
        Vector4f colorVec = argbToVec4(finalColor);

        var entry = new RoundedRect(
                new Vector4f(pillRelX, pillRelY, pillW, pillH),
                colorVec, colorVec, colorVec,
                0f, false);

        float cornerRadiusScaled = CORNER_RADIUS * zoom * guiScale;
        float featherScaled = 1f;
        float borderThicknessScaled = 0f;

        var state = new RoundedRectRenderState(textureBounds, List.of(entry),
                cornerRadiusScaled, featherScaled, borderThicknessScaled, graphics.peekScissorStack());
        graphics.submitPictureInPictureRenderState(state);
    }

    private static Vector4f argbToVec4(int argb) {
        return new Vector4f(
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f,
                ((argb >>> 24) & 0xFF) / 255f);
    }
}
