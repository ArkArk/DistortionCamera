#extension GL_OES_EGL_image_external : require

#define PI 3.14159265359

precision highp float;
uniform samplerExternalOES camTexture;

varying vec2 v_CamTexCoordinate;
varying vec2 v_TexCoordinate;

uniform vec3 resolution;

// t \in [0, 1]
vec2 rotate(vec2 p, float t) {
    float c = cos(t * 2.0 * PI);
    float s = sin(t * 2.0 * PI);
    return vec2(p.x*c - p.y*s, p.y*c + p.x*s);
}

void main () {
    vec2 uv = (2.0*v_CamTexCoordinate - resolution.xy) / min(resolution.x, resolution.y);

    float t = length(uv);
    gl_FragColor = texture2D(camTexture, rotate(uv, t)*0.5 + vec2(0.5));
}