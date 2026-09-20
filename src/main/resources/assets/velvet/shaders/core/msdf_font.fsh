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
layout(location = 2) in vec2 fontParams;
layout(location = 3) in float outlineThickness;
layout(location = 0) out vec4 fragColor;
float median(vec3 color) {
    return max(min(color.r, color.g), min(max(color.r, color.g), color.b));
}
void main() {
    float thickness = fontParams.x;
    float smoothness = fontParams.y;
    float dist = median(texture(Sampler0, texCoord0).rgb) - 0.5 + thickness;
    vec2 h = vec2(dFdx(texCoord0.x), dFdy(texCoord0.y)) * vec2(textureSize(Sampler0, 0));
    float pixels = RANGE * inversesqrt(h.x * h.x + h.y * h.y);
    float alpha = smoothstep(-smoothness, smoothness, dist * pixels);
    vec4 color = vec4(vertexColor.rgb, vertexColor.a * alpha);
#ifdef OUTLINE
    vec4 outlineColor = vec4(OUTLINE_R, OUTLINE_G, OUTLINE_B, 1.0);
    color = mix(outlineColor, vertexColor, alpha);
    color.a *= smoothstep(-smoothness, smoothness, (dist + outlineThickness) * pixels);
#endif
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
