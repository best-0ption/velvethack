#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};
layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec2 UV3;
layout(location = 6) in float LineWidth;
layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 localPos;
layout(location = 2) out vec2 rectSize;
layout(location = 3) out float cornerRadius;
layout(location = 4) out float borderThickness;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    localPos = Position.xy - UV0;
    rectSize = UV3;
    cornerRadius = LineWidth;
    borderThickness = float(UV2.x) / 16.0;
    vertexColor = Color;
}
