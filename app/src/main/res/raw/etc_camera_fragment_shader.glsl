precision mediump float;
varying vec2 aCoord;
uniform sampler2D vTexture;
uniform sampler2D vTextureAlpha;

void main() {
    vec4 color=texture2D( vTexture, aCoord); // 甚至rgb三个通道
    color.a=texture2D(vTextureAlpha,aCoord).r; //设置a通道
    gl_FragColor = color;
}
