package dev.robotgryphon.screenlib.geometry;

import org.joml.Vector2fc;

/**
 * Optional decoration painted alongside a {@link BezierCurve}: a filled
 * circle (currently used as the connection-delete affordance) rendered by
 * the same shader as the curve itself.
 *
 * <p>Living inside the shader pass — rather than as a separate pixel-rasterized
 * overlay — means the circle is anti-aliased at any GUI scale or canvas zoom
 * and automatically takes the curve's color (matching the wire makes the
 * "this control belongs to this connection" affordance obvious at a glance).
 *
 * @param center  canvas-space center of the indicator (typically the curve's
 *                midpoint, but any in-bbox point works)
 * @param radius  radius in canvas pixels; the renderer scales it by
 *                {@code zoom * guiScale} before handing it to the shader, the
 *                same way line thickness is scaled
 * @param hovered whether the cursor is currently over the indicator; the
 *                shader uses this to brighten the fill and slightly grow the
 *                circle so the affordance reacts to cursor proximity. The
 *                renderer sign-encodes this onto the radius uniform — a
 *                negative scaled radius means "hovered", and the shader
 *                reads {@code abs(radius)} for the SDF
 */
public record CurveIndicator(Vector2fc center, float radius, boolean hovered) {

    /** Non-hovered convenience overload — keeps existing call sites working. */
    public CurveIndicator(Vector2fc center, float radius) {
        this(center, radius, false);
    }
}
