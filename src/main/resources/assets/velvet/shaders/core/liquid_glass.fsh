#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    float GlintAlpha;
    vec3 CameraOffset;
    float GameTime;
    vec2 ScreenSize;
    int MenuBlurRadius;
    int UseRgss;
};
uniform sampler2D Sampler0;
layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 localPos;
layout(location = 2) in vec2 rectSize;
layout(location = 3) in float cornerRadius;
layout(location = 0) out vec4 fragColor;
float rdist(vec2 pos, vec2 size, float radius) {
    vec2 q = abs(pos) - size + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}
void main() {
    vec2 hs = rectSize * 0.5;
    float radius = min(cornerRadius, min(rectSize.x, rectSize.y) * 0.5);
    float dist = rdist(localPos - hs, hs, radius);
    float alpha = 1.0 - smoothstep(-1.0, 0.0, dist);

    vec2 e = vec2(1.5, 0.0);
    vec2 grad = vec2(
        rdist(localPos + e - hs, hs, radius) - rdist(localPos - e - hs, hs, radius),
        rdist(localPos + e.yx - hs, hs, radius) - rdist(localPos - e.yx - hs, hs, radius)
    );
    grad = normalize(grad + vec2(0.0001));

    float edge = smoothstep(-VELVET_EDGE, 0.0, dist);
    vec2 uv = vec2(gl_FragCoord.x, ScreenSize.y - gl_FragCoord.y) / ScreenSize;
    vec2 bend = grad * edge * (VELVET_REFRACT / ScreenSize);
    vec3 glass = vec3(
        texture(Sampler0, uv + bend * 1.15).r,
        texture(Sampler0, uv + bend).g,
        texture(Sampler0, uv + bend * 0.85).b
    );

    float luma = dot(glass, vec3(0.299, 0.587, 0.114));
    glass = mix(vec3(luma), glass, VELVET_SATURATION) * 1.06 + 0.02;

    float spec = smoothstep(0.3, 1.0, -grad.y) * edge;
    glass += vec3(0.30) * spec;
    float shadow = smoothstep(0.3, 1.0, grad.y) * edge;
    glass -= vec3(0.10) * shadow;

    float grain = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
    glass += (grain - 0.5) * 0.015;

    vec4 color = vec4(glass * vertexColor.rgb, vertexColor.a * alpha);
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
