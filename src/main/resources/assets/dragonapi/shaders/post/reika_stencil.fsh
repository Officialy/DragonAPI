#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:globals.glsl>

// Focus-point accumulation pass. Ported from DragonAPI's 1.7.10 reika_stencil.frag.
//
// The 1.7.10 version ran this once per focus point, ping-ponging the framebuffer, with the point's
// world coords in dx/dy/dz uniforms and the projection done shader-side by lib_geometry's
// getScreenPos(). Modern Minecraft bakes a PostChain's uniforms at compile time, so instead the CPU
// (ReikaShaderSystem) projects each point to screen space and uploads the whole set as one dynamic
// UBO, and this pass resolves them all in a single fullscreen draw.
//
// The 1.7.10 falloff was:
//     factor = clamp(1.5 - 5*distv*distance/intensity, 0, 1)
// with distv the aspect-corrected squared screen distance to the point. Writing k = 5*distance/
// intensity and defining the point's screen radius r as the distance where the falloff reaches zero
// (1.5 - k*r^2 = 0, so k*r^2 = 1.5), that is identically:
//     factor = clamp(1.5*(1 - distv/r^2), 0, 1)
// which is what this evaluates. The CPU sends r, so the per-point distance/intensity uniforms the
// original needed disappear into the geometry of the falloff.

const int MAX_FOCUS_POINTS = 16;

layout(std140) uniform FocusPoints {
    ivec4 FocusCount;             // .x = number of populated entries in Focus
    vec4 Focus[MAX_FOCUS_POINTS]; // .xy = screen UV, .z = screen radius, .w = strength
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    // Screen distances are measured x-normalised, matching lib_math.txt's distsq().
    float aspect = ScreenSize.y / ScreenSize.x;

    // Winner-take-all: the strongest point owns the pixel and its UV goes to the effect pass. The
    // 1.7.10 version got this from max()-ing successive ping-ponged passes; here it is just a loop.
    float best = 0.0;
    vec2 bestFocus = vec2(0.0);

    for (int i = 0; i < FocusCount.x; i++) {
        vec2 focus = Focus[i].xy;
        float radius = Focus[i].z;
        float strength = Focus[i].w;

        vec2 d = texCoord - focus;
        d.y *= aspect;
        float distNorm = dot(d, d) / (radius * radius);

        float factor = clamp(1.5 * (1.0 - distNorm), 0.0, 1.0) * strength;
        if (factor > best) {
            best = factor;
            bestFocus = focus;
        }
    }

    fragColor = vec4(best, bestFocus.x, bestFocus.y, 1.0);
}
