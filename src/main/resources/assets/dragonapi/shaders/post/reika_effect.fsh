#version 330
#extension GL_ARB_separate_shader_objects : require

#include <dragonapi:reika_color.glsl>

// The screen warp itself. Ported from DragonAPI's 1.7.10 reika_effect.frag.
//
// Reads the accumulation buffer written by reika_stencil.fsh: .r is the falloff factor at this
// pixel and .gb is the screen UV of the point that owns it. The scene is sampled with the UV pulled
// toward that point (the drag), then pushed toward violet and saturated (the glow).
//
// The original multiplied the factor by an "intensity" uniform here; that is now folded into the
// stencil's strength channel CPU-side, since a PostChain cannot carry per-frame uniforms.

uniform sampler2D InSampler;
uniform sampler2D StencilSampler;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 stencil = texture(StencilSampler, texCoord);
    float factor = stencil.r;
    vec2 focusXY = stencil.gb;

    vec2 texUV = mix(texCoord, focusXY, factor / 4.0);

    vec4 color = texture(InSampler, texUV);

    float f = 1.0 + 0.05 * factor;
    float f2 = 1.0 + 0.25 * factor;
    float d = factor / 8.0;
    float d2 = factor / 6.0;

    color.rgb = rgb2hsb(color.rgb);
    color.r = mix(color.r, 280.0 / 360.0, pow(factor, 2.0) * 0.2);
    color.g = min(1.0, color.g * f2 + d2);
    color.rgb = hsb2rgb(color.rgb);
    color.r = min(1.0, color.r * f + d);
    color.g = min(1.0, color.g * f + d);
    color.b = min(1.0, color.b * f + d);

    fragColor = vec4(color.r, color.g, color.b, color.a);
}
