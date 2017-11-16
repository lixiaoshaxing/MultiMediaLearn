attribute vec4 aPosition; //基本位置坐标
attribute vec2 aCoord; //纹理图片坐标
uniform mat4 uMatrix;

varying vec2 vTextureCoordinate;

void main(){
    gl_Position = uMatrix*aPosition;
    vTextureCoordinate = aCoord;
}
