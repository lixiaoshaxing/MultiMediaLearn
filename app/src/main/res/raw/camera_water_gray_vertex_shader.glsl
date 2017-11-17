//logo+灰度滤镜
attribute vec4 aPosition;
attribute vec2 aCoord;
uniform mat4 uMatrix;

varying vec2 vTextureCoordinate;

void main(){
    gl_Position = uMatrix*aPosition;
    vTextureCoordinate = aCoord;
}
