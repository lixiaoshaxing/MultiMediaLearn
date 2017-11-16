precision mediump float;
varying vec2 vTextureCoordinate;
uniform sampler2D uTexture;
void main() {
    //vec4 color=texture2D(uTexture, vTextureCoordinate);
    //float rgb=color.g;
    //vec4 c=vec4(rgb,rgb,rgb,color.a);
    //gl_FragColor = c;
    gl_FragColor = texture2D(uTexture, vTextureCoordinate);
}
