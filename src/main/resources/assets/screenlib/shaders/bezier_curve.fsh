#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

layout(std140) uniform BezierCurve {
    vec2 point1;
    vec2 point2;
    vec2 point3;
    vec2 point4;
    vec2 size;
    // x = halfWidth, y = feather, both in scaled (window) pixels.
    // Java side computes these as canvasUnits * zoom * guiScale so the line
    // stays a consistent thickness in canvas coordinates regardless of zoom.
    vec2 lineParams;
    // Optional indicator (close-button) circle drawn alongside the curve.
    // xy = center relative to size (each in [0, 1]), z = radius in scaled px,
    // w = AA feather in scaled px. radius <= 0 disables the indicator.
    vec4 indicator;
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec4 vertexPosition;
in vec4 vertexColor;

out vec4 fragColor;

// 1 / sqrt(2) — projection scale for the X-glyph diagonals' perpendicular
// distance. Cached as a constant to avoid the sqrt every fragment.
const float INV_SQRT_2 = 0.70710678;

// How dark the X glyph reads compared to the indicator's fill. 0.5 gives a
// clearly visible darker shade across the saturated palette colors the
// curves use without losing hue.
const float X_DARKEN = 0.5;

// X-arm bounding-square half-side, expressed as a fraction of the indicator
// radius. Just shy of 1/sqrt(2) (≈0.707) so the arms stop comfortably
// inside the indicator circle rather than poking at its edge.
const float X_EXTENT = 0.55;

// Hover state — encoded in the sign of the indicator's scaled radius (Java
// side flips the sign when the cursor is inside the close button). When
// hovered, the indicator grows slightly and brightens toward white so the
// affordance reacts visibly to cursor proximity.
const float HOVER_RADIUS_BOOST = 1.0;  // extra scaled pixels added to the radius
const float HOVER_BRIGHTEN     = 0.25; // mix factor toward white for the fill

float length2(in vec2 v) { return dot(v, v); }

// Squared perpendicular distance from `p` to the line through `a`-`b`,
// returning a sentinel "infinity" if the projection lies OUTSIDE the segment.
// Skipping out-of-slab projections gives the overall curve butt caps
// (perpendicular cuts at the endpoints) rather than the half-disk caps that
// a clamped-`t` SDF would produce — the shape connection lines need to
// terminate flush with a port edge instead of bulging past it.
float sdSegmentSqInSlab(in vec2 p, in vec2 a, in vec2 b)
{
    vec2 pa = p - a;
    vec2 ba = b - a;
    float baLen2 = max(dot(ba, ba), 1e-6);
    float h = dot(pa, ba) / baLen2;
    if (h < 0.0 || h > 1.0) return 1e30;
    return length2(pa - ba * h);
}

// Squared distance from `posPx` to the cubic Bezier defined by control points
// (p0..p3) given in normalized [0,1] space. Sampling and measuring in scaled-
// pixel space keeps the rendered line a consistent thickness regardless of the
// bbox aspect ratio — without this, an axis-aligned curve has a near-zero
// extent on one axis and the line collapses to a fraction of a pixel.
float udBezierPx(vec2 p0, in vec2 p1, in vec2 p2, in vec2 p3, vec2 posPx, vec2 sizePx)
{
    const int kNum = 120;
    float best = 1e30;
    vec2 a = p0 * sizePx;
    for (int i = 1; i < kNum; i++)
    {
        float t = float(i) / float(kNum - 1);
        float s = 1.0 - t;
        vec2 b = (p0 * (s*s*s) + p1 * (3.0*s*s*t) + p2 * (3.0*s*t*t) + p3 * (t*t*t)) * sizePx;
        float d = sdSegmentSqInSlab(posPx, a, b);
        if (d < best) best = d;
        a = b;
    }
    return best;
}

void main() {
    // PiP framework uses a y-flipped projection (vertex y=0 → NDC top), so
    // gl_FragCoord.y=0 ends up at the bottom of the texture while controlPoints
    // put y=0 at the top. Flip y here to bring them into the same space.
    vec2 fragPosPx = vec2(gl_FragCoord.x, size.y - gl_FragCoord.y);

    // --- Curve (cubic bezier with butt-capped half-width thickness) ----------
    float fSq = udBezierPx(point1, point2, point3, point4, fragPosPx, size);

    float halfWidth = lineParams.x;
    float feather   = lineParams.y;
    float fInner = halfWidth * halfWidth;
    float fOuter = (halfWidth + feather) * (halfWidth + feather);

    float curveAlpha = 1.0 - smoothstep(fInner, fOuter, fSq);

    // --- Indicator circle + X glyph ------------------------------------------
    // Drawn in the same pass as the curve so it inherits vertexColor and gets
    // proper SDF anti-aliasing at any GUI scale or canvas zoom. Skipping it
    // when radius <= 0 keeps the cost negligible for non-hovered connections.
    //
    // Java sign-encodes the close-circle hover state into indicator.z:
    // negative scaled radius == cursor inside the circle. We take abs() for
    // the SDF and use the original sign to gate the hover-only branches
    // (radius bump + fill brightening).
    float alpha = curveAlpha;
    vec3 color = vertexColor.rgb;

    float baseRadius = abs(indicator.z);
    if (baseRadius > 0.0) {
        bool hovered = indicator.z < 0.0;
        float radius = baseRadius + (hovered ? HOVER_RADIUS_BOOST : 0.0);
        vec3 fillColor = hovered
                ? mix(vertexColor.rgb, vec3(1.0), HOVER_BRIGHTEN)
                : vertexColor.rgb;

        vec2 indCenterPx = indicator.xy * size;
        vec2 p = fragPosPx - indCenterPx;

        float indDist = length(p);
        float indicatorAlpha = 1.0 - smoothstep(radius, radius + indicator.w, indDist);
        alpha = max(alpha, indicatorAlpha);

        if (indicatorAlpha > 0.0) {
            // Paint the (possibly-brightened) fill over the underlying curve
            // color. For non-hovered fragments fillColor == vertexColor.rgb,
            // so this mix is a no-op there.
            color = mix(color, fillColor, indicatorAlpha);

            // X glyph — two crossed diagonal strokes inside the indicator.
            // Drawn here (rather than as separate text on the Java side) so
            // its color can be derived from vertexColor.rgb and stay readable
            // across every connection color, with no font scaling artifacts.
            float xExtent = radius * X_EXTENT;
            // Bounding-square test stops the arms at a consistent length
            // regardless of how far the diagonals would extend if unbounded.
            if (abs(p.x) <= xExtent && abs(p.y) <= xExtent) {
                // Perpendicular distance to each of the two diagonals through
                // the center — |y ± x| / sqrt(2).
                float dist1 = abs(p.y - p.x) * INV_SQRT_2;
                float dist2 = abs(p.y + p.x) * INV_SQRT_2;
                float xDist = min(dist1, dist2);

                // X stroke shares the curve's halfWidth so its thickness
                // scales the same way with zoom; clamp to 0.5 so the X
                // doesn't disappear at very low zoom.
                float xHalf = max(halfWidth, 0.5);
                float xAlpha = 1.0 - smoothstep(xHalf, xHalf + feather, xDist);

                // Darken the (potentially brightened) fill in the X strokes.
                // Multiplied by indicatorAlpha so the dark mix only kicks in
                // where the indicator is actually solid.
                color = mix(color, color * X_DARKEN, xAlpha * indicatorAlpha);
            }
        }
    }

    if (alpha < 0.0002) {
        discard;
    }
    fragColor = vec4(color, vertexColor.a * alpha);
}
