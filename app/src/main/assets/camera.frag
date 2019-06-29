#extension GL_OES_EGL_image_external : require

#define PI 3.14159265359

precision highp float;
uniform samplerExternalOES camTexture;

varying vec2 v_CamTexCoordinate;
varying vec2 v_TexCoordinate;

uniform vec3 resolution;
uniform float angle; // [rad]

// t \in [0, 1]
vec2 rotate(vec2 p, float t) {
    float c = cos(t * 2.0 * PI);
    float s = sin(t * 2.0 * PI);
    return vec2(p.x*c - p.y*s, p.y*c + p.x*s);
}

vec4 getPixel(vec2 p) {
    return texture2D(camTexture, p*0.5 + vec2(0.5));
}

void main () {
    vec2 uv = (2.0*v_CamTexCoordinate - resolution.xy) / min(resolution.x, resolution.y);
    float len = length(uv);

    float t = len;
    t = t*t*t*t;
    t = t * angle / (2.0 * PI);

    uv = rotate(uv, t);

    gl_FragColor = getPixel(uv);
}
