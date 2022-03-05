#version 130

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform sampler2D Sampler1;
uniform vec2 lightmapCoord;

out vec4 vertexColor;
out vec2 texCoord0;
out vec4 normal;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color * texture(Sampler1, (lightmapCoord + vec2(8, 8)) / 256);
    texCoord0 = UV0;
    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
}