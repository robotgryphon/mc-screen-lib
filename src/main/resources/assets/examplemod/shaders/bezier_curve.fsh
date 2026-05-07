#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

layout(std140) uniform BezierCurve {
    vec2 point1;
    vec2 point2;
    vec2 point3;
    vec2 point4;
    vec2 size;
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec4 vertexPosition;
in vec4 vertexColor;

out vec4 fragColor;

float length2(in vec2 v) { return dot(v, v); }

float sdSegmentSq(in vec2 p, in vec2 a, in vec2 b)
{
    vec2 pa = p - a, ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length2(pa - ba * h);
}

float udBezier(vec2 p0, in vec2 p1, in vec2 p2, in vec2 p3, vec2 pos)
{
    const int kNum = 120;
    vec2 dist = vec2(1e10, 0.0);
    vec2 a = p0;

    for (int i = 1; i < kNum; i++)
    {
        float t = float(i) / float(kNum - 1);
        float s = 1.0 - t;
        vec2 b = p0 * s * s * s + p1 * 3.0 * s * s * t + p2 * 3.0 * s * t * t + p3 * t * t * t;
        float d = sdSegmentSq(pos, a, b);
        if (d < dist.x) dist = vec2(d, t);
        a = b;
    }

    return dist.x;
}

void main() {
    float f = udBezier(
        point1,
        point2,
        point3,
        point4,
        (gl_FragCoord.xy / size) - 0.5
    );

    float alpha = 1.0 - smoothstep(0.0, 0.0002, f);
    if (alpha < 0.0002) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
