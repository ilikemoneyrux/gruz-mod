#version 150

// Gruz Client – motion blur: mieszanie bieżącej klatki z poprzednią
uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;
uniform float BlendFactor;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 current = texture(DiffuseSampler, texCoord);
    vec4 previous = texture(PrevSampler, texCoord);
    fragColor = vec4(mix(current.rgb, previous.rgb, clamp(BlendFactor, 0.0, 0.95)), 1.0);
}
