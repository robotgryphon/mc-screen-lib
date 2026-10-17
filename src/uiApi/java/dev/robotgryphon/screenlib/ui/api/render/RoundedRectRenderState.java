package dev.robotgryphon.screenlib.ui.api.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record RoundedRectRenderState(ScreenRectangle bounds,
                                        List<RoundedRect> entries,
                                        float cornerRadiusScaled,
                                        float featherScaled,
                                        float borderThicknessScaled,
                                        @Nullable ScreenRectangle scissorArea)
        implements PictureInPictureRenderState {
    public static final int MAX_ENTRIES = 64;

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
