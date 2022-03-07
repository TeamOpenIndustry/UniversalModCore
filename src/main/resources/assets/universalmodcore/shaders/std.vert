#version 130

#define LIGHT0_POS normalize(vec3(0.20000000298023224, 1.0, -0.699999988079071))
#define LIGHT1_POS normalize(vec3(-0.20000000298023224, 1.0, 0.699999988079071))

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform sampler2D Sampler1;
uniform vec2 lightmapCoord;
uniform vec4 colorMult;

out vec4 vertexColor;
out vec2 texCoord0;
out vec4 normal;

void main() {
    float lightAccum = min(1.0, (max(0.0, dot(LIGHT0_POS, Normal)) + max(0.0, dot(LIGHT1_POS, Normal))) * 0.6 + 0.4);
    vec4 light = texture(Sampler1, (lightmapCoord + vec2(8, 8)) / 256);
    vertexColor = vec4(Color.rgb * lightAccum, Color.a) * light * colorMult;

    texCoord0 = UV0;

    gl_Position = gl_ModelViewProjectionMatrix * ProjMat * ModelViewMat * vec4(Position, 1.0);
    normal = gl_ModelViewProjectionMatrix * ProjMat * ModelViewMat * vec4(Normal, 0.0);
}