#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
uniform sampler2D Sampler0;
layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec2 rectSize;
layout(location = 3) in float cornerRadius;
layout(location = 0) out vec4 fragColor;
float rdist(vec2 pos, vec2 size, float radius) {
    vec2 q = abs(pos) - size + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}
void main() {
    float dist = rdist(texCoord0 * rectSize - rectSize * 0.5, rectSize * 0.5, cornerRadius);
    float alpha = 1.0 - smoothstep(-1.0, 0.0, dist);
    vec4 color = vec4(1.0, 1.0, 1.0, alpha) * texture(Sampler0, texCoord0) * vertexColor;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
