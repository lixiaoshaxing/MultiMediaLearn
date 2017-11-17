//logo+灰度滤镜
#extension GL_OES_EGL_image_external:require
precision mediump float;
varying vec2 vTextureCoordinate;
uniform samplerExternalOES  uTextureUnit; //yuv-》rgb

void main() {
    vec4 color=texture2D( uTextureUnit, vTextureCoordinate);
    float rgb=color.g;
    vec4 c=vec4(rgb,rgb,rgb,color.a);
    gl_FragColor = c;
}
