#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

// MAX_NODES must match the Java-side constant (NodeBackgroundUniform.MAX_NODES).
// Each array slot occupies 16 bytes in std140 regardless of how much we pack
// into it — boosting this just to push the cap means more UBO bytes per batch.
const int MAX_NODES = 64;

// Drop-shadow visual constants, expressed as multiples of the node's
// corner radius so the shadow scales naturally with canvas zoom. The
// shadow extends down-and-right and fades out smoothly via its own SDF.
const float SHADOW_OFFSET_X_FACTOR = 0.4;
const float SHADOW_OFFSET_Y_FACTOR = 0.6;
const float SHADOW_BLUR_FACTOR     = 1.0;
const float SHADOW_ALPHA           = 0.45;

layout(std140) uniform NodeBatch {
    // x, y = PiP texture dimensions in scaled (window) pixels; z, w unused.
    vec4 size;
    // x = corner radius in scaled pixels.
    // y = AA feather width in scaled pixels.
    // z = border thickness in scaled pixels.
    // w = active node count (cast to int per fragment).
    vec4 params;

    // Per-node entries — every array has the same stride (16 bytes), so the
    // four parallel arrays let us pull all per-node attributes with a single
    // index. Each {@code nodeBounds[i]} is (x, y, width, height) in
    // texture-pixel space (top-left origin, y growing downward).
    vec4 nodeBounds[MAX_NODES];
    vec4 nodeBodyColors[MAX_NODES];
    vec4 nodeTitleColors[MAX_NODES];
    vec4 nodeBorderColors[MAX_NODES];
    // x = title-bar height in scaled pixels (height of the title-colored strip
    // measured from the node's top edge).
    // y = drop-shadow flag (>= 0.5 → on, < 0.5 → off).
    // z, w reserved for future per-node visual toggles.
    vec4 nodeExtras[MAX_NODES];
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec4 vertexColor;

out vec4 fragColor;

// Signed distance from `p` to an axis-aligned rounded rectangle of half-
// extents `b` and corner radius `r`. Negative inside, positive outside,
// linear near the edge — exactly what we need for cheap AA via smoothstep
// on the distance.
float sdRoundedRect(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    // PiP framework uses a y-flipped projection (gl_FragCoord.y=0 at the
    // texture bottom) but the Java side reports node coordinates with
    // y growing downward from the top. Flip y here so both agree.
    vec2 fragPos = vec2(gl_FragCoord.x, size.y - gl_FragCoord.y);

    float radius = params.x;
    float feather = params.y;
    float borderThickness = params.z;
    int nodeCount = int(params.w);

    // Output is in straight-alpha form (matches the pipeline's
    // TRANSLUCENT blend function: src_alpha, one_minus_src_alpha).
    // Within a batch, nodes are assumed not to overlap visually; the
    // last node that covers a given fragment paints it.
    vec4 outColor = vec4(0.0);

    for (int i = 0; i < MAX_NODES; ++i) {
        if (i >= nodeCount) break;

        vec4 bounds = nodeBounds[i];
        vec2 nodeMin = bounds.xy;
        vec2 nodeSize = bounds.zw;
        vec2 nodeCenter = nodeMin + nodeSize * 0.5;
        vec2 halfExtents = nodeSize * 0.5;

        // SDF for the full rounded outer rectangle of the node itself.
        float d = sdRoundedRect(fragPos - nodeCenter, halfExtents, radius);

        // Drop shadow — painted FIRST so the node body below overwrites
        // the part of the shadow that should be hidden behind the node.
        // The shadow shape is the same rounded rect, translated by the
        // shadow offset; its SDF gives a soft falloff via smoothstep.
        // Only contributes where the fragment is outside the node (d > 0)
        // so the body doesn't darken itself.
        float shadowFlag = nodeExtras[i].y;
        if (shadowFlag >= 0.5 && d > 0.0) {
            vec2 shadowOffset = vec2(radius * SHADOW_OFFSET_X_FACTOR,
                                     radius * SHADOW_OFFSET_Y_FACTOR);
            float shadowBlur = radius * SHADOW_BLUR_FACTOR;
            float shadowD = sdRoundedRect(
                    fragPos - nodeCenter - shadowOffset, halfExtents, radius);
            if (shadowD < shadowBlur) {
                // Inside-shadow: full alpha; outside the shape but within
                // blur: linear-ish falloff to 0.
                float falloff = 1.0 - smoothstep(0.0, shadowBlur, max(shadowD, 0.0));
                float shadowAlpha = SHADOW_ALPHA * falloff;
                if (shadowAlpha > outColor.a) {
                    outColor.rgb = vec3(0.0);
                    outColor.a = shadowAlpha;
                }
            }
        }

        // Outside the rounded shape (plus a feather band for AA falloff)?
        // The shadow check above already ran; skip the body work.
        if (d > feather) continue;

        // Decide which fill color this fragment belongs to — title bar
        // (top strip) or body (everything below).
        float titleHeight = nodeExtras[i].x;
        float relY = fragPos.y - nodeMin.y;
        vec4 fillColor = (relY < titleHeight) ? nodeTitleColors[i] : nodeBodyColors[i];

        // Border band — anywhere within `borderThickness` of the outer
        // edge, swap to the border color.
        if (d > -borderThickness) {
            fillColor = nodeBorderColors[i];
        }

        // Title separator — a thin border-colored strip exactly where the
        // title bar's bottom meets the body's top, clamped to a safe
        // distance from the rounded corners so it doesn't poke into the arc.
        if (abs(relY - titleHeight) < 0.5 && d < -radius) {
            fillColor = nodeBorderColors[i];
        }

        // AA factor — 1 inside, 0 outside, smooth in the feather band.
        float aa = 1.0 - smoothstep(0.0, feather, d);
        if (aa <= 0.0) continue;

        // Node body overwrites whatever was there before (including its
        // own shadow), so the shadow only appears where the node isn't.
        outColor.rgb = fillColor.rgb;
        outColor.a = fillColor.a * aa;
    }

    if (outColor.a < 0.002) {
        discard;
    }
    fragColor = outColor * vertexColor;
}
