package dev.robotgryphon.screenlib.client.ui.render.pip;

import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record NodeBackgroundRenderState(ScreenRectangle bounds,
                                        List<NodeBackgroundUniform.Entry> entries,
                                        float cornerRadiusScaled,
                                        float featherScaled,
                                        float borderThicknessScaled,
                                        @Nullable ScreenRectangle scissorArea)
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
}
