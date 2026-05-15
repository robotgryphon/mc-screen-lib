package dev.robotgryphon.screenlib.client.ui.render.pip;

import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * PiP render state describing a single batched draw of node backgrounds.
 * One state represents the whole batch — the texture covers the bounding
 * box of every node in {@link #entries}, and the shader iterates the
 * per-node uniform arrays to paint each node's body / title / border into
 * the matching screen region.
 *
 * <p>Each {@link NodeBackgroundUniform.Entry} bound is in <em>scaled
 * (window) pixels</em> relative to the texture's top-left corner, which
 * is the same space {@code gl_FragCoord} reports. The PiP framework
 * uses this state's {@link #bounds()} to size the texture and to know
 * where to blit it on screen; the per-entry rects are how the shader
 * recovers each node's region inside that texture.
 *
 * <p>{@code cornerRadiusScaled} / {@code featherScaled} /
 * {@code borderThicknessScaled} are precomputed scaled-pixel values so
 * the shader doesn't need to know about canvas zoom or GUI scale. The
 * caller (currently {@code CanvasWidget}) multiplies the canvas-space
 * values by {@code zoom * guiScale} when constructing the state.
 */
public record NodeBackgroundRenderState(ScreenRectangle bounds,
                                        List<NodeBackgroundUniform.Entry> entries,
                                        float cornerRadiusScaled,
                                        float featherScaled,
                                        float borderThicknessScaled)
        implements PictureInPictureRenderState {

    @Override
    public int x0() { return bounds.left(); }

    @Override
    public int x1() { return bounds.right(); }

    @Override
    public int y0() { return bounds.top(); }

    @Override
    public int y1() { return bounds.bottom(); }

    @Override
    public float scale() { return 1; }

    @Override
    public @Nullable ScreenRectangle scissorArea() { return null; }
}
