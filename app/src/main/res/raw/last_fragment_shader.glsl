//使用fbo后最后要切换到默认缓冲区，进行最后一次绘制
precision mediump float;
varying vec2 vTextureCoordinate;
uniform sampler2D uTexture;
void main() {
    gl_FragColor = texture2D(uTexture, vTextureCoordinate);
}
