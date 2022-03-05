#version 130

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;
in vec4 normal;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    color *= vertexColor;
    gl_FragColor = color;
}