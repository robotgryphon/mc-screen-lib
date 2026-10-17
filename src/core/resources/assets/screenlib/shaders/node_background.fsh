#version 450

#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

const int MAX_NODES = 64;

layout(location = 0) in vec4 vertexColor;
layout(std140) uniform NodeBatch {
    vec4 size;
    vec4 params;

    vec4 nodeBounds[MAX_NODES];
    vec4 nodeBodyColors[MAX_NODES];
    vec4 nodeTitleColors[MAX_NODES];
    vec4 nodeBorderColors[MAX_NODES];
    vec4 nodeExtras[MAX_NODES];
    vec4 nodeGlowColors[MAX_NODES];
};

layout(location = 0) out vec4 fragColor;

const float SHADOW_OFFSET_X_FACTOR = 0.4;
const float SHADOW_OFFSET_Y_FACTOR = 0.6;
const float SHADOW_BLUR_FACTOR     = 1.0;
const float SHADOW_ALPHA           = 0.45;

const float GLOW_BLUR_FACTOR = 0.6;

float sdRoundedRect(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 fragPos = vec2(gl_FragCoord.x, size.y - gl_FragCoord.y);

    float feather = params.y;
    int nodeCount = int(params.w);

    vec4 outColor = vec4(0.0);

    for (int i = 0; i < MAX_NODES; ++i) {
        if (i >= nodeCount) break;

        vec4 bounds = nodeBounds[i];
        vec2 nodeMin = bounds.xy;
        vec2 nodeSize = bounds.zw;
        vec2 nodeCenter = nodeMin + nodeSize * 0.5;
        vec2 halfExtents = nodeSize * 0.5;

        float radius = (nodeExtras[i].z > 0.0) ? nodeExtras[i].z : params.x;
        float borderThickness = (nodeExtras[i].w > 0.0) ? nodeExtras[i].w : params.z;

        float d = sdRoundedRect(fragPos - nodeCenter, halfExtents, radius);

        float shadowFlag = nodeExtras[i].y;
        if (shadowFlag >= 0.5 && d > 0.0) {
            vec2 shadowOffset = vec2(radius * SHADOW_OFFSET_X_FACTOR,
                                     radius * SHADOW_OFFSET_Y_FACTOR);
            float shadowBlur = radius * SHADOW_BLUR_FACTOR;
            float shadowD = sdRoundedRect(
                    fragPos - nodeCenter - shadowOffset, halfExtents, radius);
            if (shadowD < shadowBlur) {
                float falloff = 1.0 - smoothstep(0.0, shadowBlur, max(shadowD, 0.0));
                float shadowAlpha = SHADOW_ALPHA * falloff;
                if (shadowAlpha > outColor.a) {
                    outColor.rgb = vec3(0.0);
                    outColor.a = shadowAlpha;
                }
            }
        }

        vec4 glowColor = nodeGlowColors[i];
        if (glowColor.a > 0.0 && d > 0.0) {
            float glowBlur = radius * GLOW_BLUR_FACTOR;
            if (d < glowBlur) {
                float glowFalloff = 1.0 - smoothstep(0.0, glowBlur, d);
                float glowAlpha = glowColor.a * glowFalloff;
                if (glowAlpha > outColor.a) {
                    outColor.rgb = glowColor.rgb;
                    outColor.a = glowAlpha;
                }
            }
        }

        if (d > feather) continue;

        float titleHeight = nodeExtras[i].x;
        float relY = fragPos.y - nodeMin.y;
        vec4 fillColor = (relY < titleHeight) ? nodeTitleColors[i] : nodeBodyColors[i];

        if (d > -borderThickness) {
            fillColor = nodeBorderColors[i];
        }

        if (abs(relY - titleHeight) < 0.5 && d < -radius) {
            fillColor = nodeBorderColors[i];
        }

        float aa = 1.0 - smoothstep(0.0, feather, d);
        if (aa <= 0.0) continue;

        outColor.rgb = fillColor.rgb;
        outColor.a = fillColor.a * aa;
    }

    if (outColor.a < 0.002) {
        discard;
    }
    fragColor = outColor * vertexColor;
}
