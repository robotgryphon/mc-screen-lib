package dev.robotgryphon.screenlib.client.ui.widget.property;

import dev.robotgryphon.screenlib.client.ui.render.pip.NodeBackgroundRenderState;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
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

/**
 * Shared base for the inline editors that render inside a property row's
 * value area — currently {@link NumericPropertyEditor} (the [− value +]
 * spinner), {@link DropdownEditor} (the value-and-chevron trigger), and
 * {@link BooleanPropertyEditor} (the pill toggle).
 *
 * <p>Extends {@link AbstractWidget} so each editor plugs into Minecraft's
 * standard widget event loop: a host (the row, the node widget, or any
 * layout container) just forwards {@code mouseClicked} / {@code keyPressed}
 * / {@code charTyped} into the editor and the editor decides whether to
 * consume the event. That keeps the host from having to know which
 * subclass it's hosting — the dispatch fans out through virtual
 * {@code onClick} / {@code mouseClicked} overrides on each editor type
 * rather than an {@code if (codec == INT) … else if (codec == BOOL) …}
 * ladder.
 *
 * <p>Bounds, the {@link AbstractWidget#getAlpha alpha} multiplier, the
 * default {@link #mouseClicked} path (active + bounds check + sound + a
 * call to the subclass's {@link AbstractWidget#onClick onClick}), and the
 * standard layout-element getters / setters all come from
 * {@link AbstractWidget} for free; this class only adds the rounded-pill
 * background shader hookup and the two text-positioning helpers shared by
 * every editor subclass.
 *
 * <p>Subclasses construct themselves with a bounds rectangle, an optional
 * value-changed callback (so they can write back without re-discovering
 * the host), and any subclass-specific state (current value, codec, …).
 * They implement {@link AbstractWidget#extractWidgetRenderState
 * extractWidgetRenderState} to draw themselves, and override
 * {@code mouseClicked} (or {@code onClick} when the click target is
 * uniform across the editor surface) to react to input.
 */
public abstract class PropertyEditor extends AbstractWidget {

    /**
     * Corner radius applied to every editor's pill background. Smaller
     * than the node's outer radius so the editor reads as a control
     * "inside" the node rather than a second card sitting on top.
     */
    public static final int CORNER_RADIUS = 2;

    /**
     * Default pill background — the sunk shade both editors share when
     * idle. Editors that want a hover-elevated background (the dropdown
     * trigger) override with their own color at the call site.
     */
    protected static final int BG_COLOR = 0xCC1A1B22;

    /** Bright white for the value text in the middle of any editor. */
    protected static final int VALUE_TEXT_COLOR = 0xFFFFFFFF;

    protected PropertyEditor(int x, int y, int width, int height) {
        // Editors carry no narration message of their own yet — the row
        // label is what's narrated when navigating the node. Passing
        // {@link Component#empty()} keeps {@link AbstractWidget}'s
        // narration scaffolding happy without committing to specific
        // wording per editor type.
        super(x, y, width, height, Component.empty());
    }

    /**
     * Editors don't carry narration text of their own yet — node-level
     * narration covers the surrounding row's label. Overriding here as
     * a no-op satisfies {@link AbstractWidget}'s abstract requirement
     * without each subclass having to repeat the same empty body.
     */
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // Intentionally empty.
    }

    /**
     * No-op down-sound. The standard {@link AbstractWidget} button click
     * is loud for the rapid +/- nudges these editors expect — the click
     * already produces enough visual feedback (the value tick, the
     * column hover) that an audio cue per press becomes noise.
     */
    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager soundManager) {
        // Intentionally silent.
    }

    /**
     * Bounds test — true when the screen-space point lands inside the
     * editor's outer rectangle. Available as a static so callers that
     * already have the row geometry on hand (the per-frame hover checks
     * inside the render path, mostly) can skip going through an editor
     * instance.
     */
    public static boolean isInside(double mouseX, double mouseY,
                                   int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    /**
     * The y-coordinate to pass to {@code graphics.text} so the text
     * lands vertically centered inside the editor. The {@code +1} is
     * an optical-center nudge — the font's pixel grid has a small bias
     * toward the top of the line height, so the geometric center sits
     * one pixel low without it.
     */
    protected static int textCenterY(int y, int height, Font font) {
        return y + (height - font.lineHeight) / 2 + 1;
    }

    /**
     * Draws the editor's rounded pill background through the
     * SDF-based fragment shader that already paints node backgrounds,
     * piggy-backing on it as a single-entry batch.
     *
     * <p>Going through the shader (instead of the CPU-rasterized
     * pixel-art quarter-circles previously used) gives sub-pixel
     * anti-aliased corners at any zoom — the prior rounding looked
     * chamfered at the {@link #CORNER_RADIUS}=2 radius the editors
     * use. Each call submits one PiP render state with one entry whose
     * title/border colors match the body color and whose title height
     * and shared border thickness are zero, so the only visible output
     * is a uniformly-colored rounded rectangle.
     *
     * <p>Bounds are pose-transformed from canvas pixels (where editor
     * callers live, since they render inside the canvas widget's pan +
     * zoom pose) into screen pixels, which is what the PiP framework
     * expects. The texture gets a one-screen-pixel padding on each
     * side so the SDF's feather band has room to fade out cleanly.
     */
    protected static void renderPill(GuiGraphicsExtractor graphics,
                                     int x, int y, int width, int height,
                                     int color, float alpha) {
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        // The pose is a uniform scale + translate (the canvas viewport
        // applies translate(pan); scale(zoom)), so .m00 is the zoom
        // factor — used to lift the corner radius from canvas-space
        // into the shader's scaled-pixel space.
        float zoom = pose.m00;
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();

        Vector2f tl = pose.transformPosition(x, y, new Vector2f());
        Vector2f br = pose.transformPosition(x + width, y + height, new Vector2f());

        // Pad by one screen pixel on each side so the SDF's AA feather
        // doesn't get clipped at the texture's edge — without the
        // padding the smoothstep falloff would terminate at a hard
        // pixel boundary and the corners would alias just inside the
        // shape.
        int sx0 = (int) Math.floor(tl.x()) - 1;
        int sy0 = (int) Math.floor(tl.y()) - 1;
        int sx1 = (int) Math.ceil(br.x()) + 1;
        int sy1 = (int) Math.ceil(br.y()) + 1;
        var textureBounds = new net.minecraft.client.gui.navigation.ScreenRectangle(sx0, sy0, sx1 - sx0, sy1 - sy0);

        // Pill bounds relative to the texture's top-left, in the same
        // scaled-pixel space the shader's gl_FragCoord reports.
        float pillRelX = (float) ((tl.x() - sx0) * guiScale);
        float pillRelY = (float) ((tl.y() - sy0) * guiScale);
        float pillW = (float) ((br.x() - tl.x()) * guiScale);
        float pillH = (float) ((br.y() - tl.y()) * guiScale);

        int finalColor = ARGB.multiply(color, ARGB.white(alpha));
        Vector4f colorVec = argbToVec4(finalColor);

        // Single-entry batch built so the node-background shader's
        // per-entry code paths all collapse to "paint the body color":
        //   * titleHeight = 0       → no title strip
        //   * titleColor = bodyColor → if any title strip is computed, invisible
        //   * borderColor = bodyColor → border zone (if any) blends in
        //   * dropShadow = false    → no shadow under the pill
        // Combined with borderThicknessScaled = 0 on the state, the
        // result is a uniformly-colored AA rounded rect.
        var entry = new NodeBackgroundUniform.Entry(
                new Vector4f(pillRelX, pillRelY, pillW, pillH),
                colorVec, colorVec, colorVec,
                0f, false);

        float cornerRadiusScaled = CORNER_RADIUS * zoom * guiScale;
        float featherScaled = 1f;
        float borderThicknessScaled = 0f;

        var state = new NodeBackgroundRenderState(textureBounds, List.of(entry),
                cornerRadiusScaled, featherScaled, borderThicknessScaled);
        graphics.submitPictureInPictureRenderState(state);
    }

    /** Normalize a 0xAARRGGBB int color into a {@code (r, g, b, a)} 0..1 vector. */
    private static Vector4f argbToVec4(int argb) {
        return new Vector4f(
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f,
                ((argb >>> 24) & 0xFF) / 255f);
    }
}
