//在glsl中把rgb数据转换为yuv存储，readPiexs数据为yuv，推送，转x264效率更高，与libyuv在cpu中效率有待对比
attribute vec4 aPosition;
attribute vec2 aCoord;
uniform mat4 uMatrix;

varying vec2 vTextureCoordinate;

void main(){
    gl_Position = uMatrix*aPosition;
    vTextureCoordinate = aCoord;
}
