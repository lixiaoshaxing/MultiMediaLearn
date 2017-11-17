//使用fbo后最后要切换到默认缓冲区，进行最后一次绘制
attribute vec4 aPosition;
attribute vec2 aCoord;
uniform mat4 uMatrix;

varying vec2 vTextureCoordinate;

void main(){
    gl_Position = uMatrix*aPosition;
    vTextureCoordinate = aCoord;
}
