#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 localPos;
layout(location = 2) in vec2 rectSize;
layout(location = 3) in float cornerRadius;
layout(location = 4) in float borderThickness;
layout(location = 0) out vec4 fragColor;
float rdist(vec2 pos, vec2 size, float radius) {
    vec2 q = abs(pos) - size + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}
void main() {
    float dist = rdist(localPos - rectSize * 0.5, rectSize * 0.5, cornerRadius);
    float thickness = borderThickness;
    float alpha = smoothstep(-thickness - 1.0, -thickness, dist);
    alpha *= 1.0 - smoothstep(-1.0, 0.0, dist);
    vec4 color = vec4(vertexColor.rgb, vertexColor.a * alpha);
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
