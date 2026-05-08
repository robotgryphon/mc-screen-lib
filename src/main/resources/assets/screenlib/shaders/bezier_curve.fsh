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
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec4 vertexPosition;
in vec4 vertexColor;

out vec4 fragColor;

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

    float fSq = udBezierPx(point1, point2, point3, point4, fragPosPx, size);

    float halfWidth = lineParams.x;
    float feather   = lineParams.y;
    float fInner = halfWidth * halfWidth;
    float fOuter = (halfWidth + feather) * (halfWidth + feather);

    float alpha = 1.0 - smoothstep(fInner, fOuter, fSq);
    if (alpha < 0.0002) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
