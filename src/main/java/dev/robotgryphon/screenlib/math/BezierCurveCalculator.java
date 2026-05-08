package dev.robotgryphon.screenlib.math;

import dev.robotgryphon.screenlib.client.ui.widget.Node;
import dev.robotgryphon.screenlib.graph.NodeConnection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.NonNull;

public class BezierCurveCalculator {

    /**
     * Pixels of headroom added around the curve's natural bounding box so the
     * line — which has a fixed pixel width in the shader — has somewhere to
     * render even when the curve is axis-aligned (bbox height or width near
     * zero would otherwise clip the line down to a sliver). Must agree with
     * the padding applied in {@link NodeConnection#bounds()}.
     */
    public static final int LINE_PADDING_PX = 6;

    /**
     * Half the visible line thickness, in canvas pixels (i.e., independent of
     * zoom and guiScale). The shader actually receives this value scaled by
     * {@code zoom * guiScale}, so the rendered line stays a consistent
     * thickness in canvas coordinates as the user zooms in or out.
     *
     * <p>The port-attachment offset uses this value too (see
     * {@link Node#portAttachment}),
     * so that the line edge always meets the port edge cleanly at any zoom.
     */
    public static final float LINE_HALFWIDTH = 0.5f;

    /** Width, in canvas pixels, of the antialiasing fade past {@link #LINE_HALFWIDTH}. */
    public static final float LINE_FEATHER = 0.5f;

    public static Vector2fc[] calculateRightToLeft(Vector2fc start, Vector2fc end) {
        Vector2fc point1 = new Vector2f((start.x() + end.x()) / 2f, start.y());
        Vector2fc point2 = new Vector2f((start.x() + end.x()) / 2f, end.y());

        return new Vector2fc[]{start, point1, point2, end};
    }

    public static @NonNull ScreenRectangle getBounds(
            Vector2fc[] points,
            Matrix3x2fc pose
    ) {
        int minX = (int) Math.floor(Math.min(Math.min(points[0].x(), points[1].x()), Math.min(points[2].x(), points[3].x()))) - LINE_PADDING_PX;
        int minY = (int) Math.floor(Math.min(Math.min(points[0].y(), points[1].y()), Math.min(points[2].y(), points[3].y()))) - LINE_PADDING_PX;
        int maxX = (int) Math.ceil(Math.max(Math.max(points[0].x(), points[1].x()), Math.max(points[2].x(), points[3].x()))) + LINE_PADDING_PX;
        int maxY = (int) Math.ceil(Math.max(Math.max(points[0].y(), points[1].y()), Math.max(points[2].y(), points[3].y()))) + LINE_PADDING_PX;

        return new ScreenRectangle(minX, minY, maxX - minX, maxY - minY).transformMaxBounds(pose);
    }
}
