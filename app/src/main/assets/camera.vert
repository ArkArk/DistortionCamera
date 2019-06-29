attribute vec4 position;

uniform mat4 camTextureTransform;
attribute vec4 camTexCoordinate;

varying vec2 v_CamTexCoordinate;

void main() {
    v_CamTexCoordinate = (camTextureTransform * camTexCoordinate).xy;
    gl_Position = position;
}