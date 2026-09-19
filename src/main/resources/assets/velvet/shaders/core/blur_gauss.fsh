#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D Sampler0;
layout(location = 0) in vec2 texCoord0;
layout(location = 0) out vec4 fragColor;
void main() {
    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 dir = vec2(VELVET_DIR_X, VELVET_DIR_Y) * VELVET_SCALE * texel;
    vec4 sum = texture(Sampler0, texCoord0) * 0.2270270270;
    sum += (texture(Sampler0, texCoord0 + dir * 1.3846153846) + texture(Sampler0, texCoord0 - dir * 1.3846153846)) * 0.3162162162;
    sum += (texture(Sampler0, texCoord0 + dir * 3.2307692308) + texture(Sampler0, texCoord0 - dir * 3.2307692308)) * 0.0702702703;
    fragColor = sum;
}
