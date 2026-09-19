#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D Sampler0;
layout(location = 0) in vec2 texCoord0;
layout(location = 0) out vec4 fragColor;
void main() {
    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 o = VELVET_OFFSET * texel;
    vec4 sum = texture(Sampler0, texCoord0) * 4.0;
    sum += texture(Sampler0, texCoord0 + o);
    sum += texture(Sampler0, texCoord0 - o);
    sum += texture(Sampler0, texCoord0 + vec2(o.x, -o.y));
    sum += texture(Sampler0, texCoord0 - vec2(o.x, -o.y));
    fragColor = sum / 8.0;
}
