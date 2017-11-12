uniform mat4 uMVPMatrix;
attribute vec4 vPosition;
attribute vec2 inputTextureCoordinate;
varying vec2 textureCoordinate;

void main() {
    gl_Position =  vPosition * uMVPMatrix ;
    textureCoordinate = inputTextureCoordinate;
}
