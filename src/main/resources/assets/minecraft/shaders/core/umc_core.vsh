#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    //TODO model view matrix
    vec4 norm = ProjMat * vec4(Normal, 0.0);
    vertexDistance = fog_distance(gl_Position.xyz, FogShape);

    if (UV2.x == 240 && UV2.y == 240) {
        //Vanilla emissive
        //TODO can we optimize this by implementing new vertex format?
        vertexColor = Color;
        lightMapColor = texelFetch(Sampler2, ivec2(15, 15), 0);
    } else {
        vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, normalize(norm.xyz), Color);
        lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    }

    overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;
}
