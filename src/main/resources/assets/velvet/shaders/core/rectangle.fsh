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
layout(location = 4) in vec3 gradTo;
layout(location = 5) in vec2 gradFlow;
layout(location = 0) out vec4 fragColor;
float rdist(vec2 pos, vec2 size, float radius) {
    vec2 q = abs(pos) - size + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}
vec4 flowColor(vec4 from) {
    float axis = gradFlow.x > 1.5 ? localPos.y / max(rectSize.y, 1.0) : localPos.x / max(rectSize.x, 1.0);
    float m = 0.5 - 0.5 * cos(6.2831853 * fract(gradFlow.y - axis));
    return vec4(mix(from.rgb, gradTo, m), from.a);
}
void main() {
    float dist = rdist(localPos - rectSize * 0.5, rectSize * 0.5, cornerRadius);
    float alpha = 1.0 - smoothstep(-1.0, 0.0, dist);
    vec4 base = gradFlow.x > 0.5 ? flowColor(vertexColor) : vertexColor;
    vec4 color = vec4(base.rgb, base.a * alpha);
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
