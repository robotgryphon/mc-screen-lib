#version 450

// Required for location qualifiers on outputs under #version 330.
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

//layout(std140) uniform DynamicTransforms {
//    mat4 ModelViewMat;
//    vec4 ColorModulator;
//    vec3 ModelOffset;
//    mat4 TextureMat;
//};
//
//
//layout(std140) uniform Projection {
//    mat4 ProjMat;
//};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

layout(location = 0) out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
}
