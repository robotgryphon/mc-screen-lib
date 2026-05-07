#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};


layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 position;
in vec4 color;

out vec4 vertexPosition;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);
    vertexColor = color;
    vertexPosition = gl_Position;
}
