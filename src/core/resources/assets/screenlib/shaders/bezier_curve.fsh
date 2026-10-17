#version 450

#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(std140) uniform BezierCurve {
    vec2 point1;
    vec2 point2;
    vec2 point3;
    vec2 point4;
    vec2 size;
    vec2 lineParams;
    vec4 indicator;
};

layout(location = 0) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

const float INV_SQRT_2 = 0.70710678;

const float X_DARKEN = 0.5;

const float X_EXTENT = 0.55;

const float HOVER_RADIUS_BOOST = 1.0;
const float HOVER_BRIGHTEN     = 0.25;

float length2(in vec2 v) { return dot(v, v); }

float sdSegmentSqInSlab(in vec2 p, in vec2 a, in vec2 b)
{
    vec2 pa = p - a;
    vec2 ba = b - a;
    float baLen2 = max(dot(ba, ba), 1e-6);
    float h = dot(pa, ba) / baLen2;
    if (h < 0.0 || h > 1.0) return 1e30;
    return length2(pa - ba * h);
}

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
    vec2 fragPosPx = vec2(gl_FragCoord.x, size.y - gl_FragCoord.y);

    float fSq = udBezierPx(point1, point2, point3, point4, fragPosPx, size);

    float halfWidth = lineParams.x;
    float feather   = lineParams.y;
    float fInner = halfWidth * halfWidth;
    float fOuter = (halfWidth + feather) * (halfWidth + feather);

    float curveAlpha = 1.0 - smoothstep(fInner, fOuter, fSq);

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
            color = mix(color, fillColor, indicatorAlpha);

            float xExtent = radius * X_EXTENT;
            if (abs(p.x) <= xExtent && abs(p.y) <= xExtent) {
                float dist1 = abs(p.y - p.x) * INV_SQRT_2;
                float dist2 = abs(p.y + p.x) * INV_SQRT_2;
                float xDist = min(dist1, dist2);

                float xHalf = max(halfWidth, 0.5);
                float xAlpha = 1.0 - smoothstep(xHalf, xHalf + feather, xDist);

                color = mix(color, color * X_DARKEN, xAlpha * indicatorAlpha);
            }
        }
    }

    if (alpha < 0.0002) {
        discard;
    }
    fragColor = vec4(color, vertexColor.a * alpha);
}
