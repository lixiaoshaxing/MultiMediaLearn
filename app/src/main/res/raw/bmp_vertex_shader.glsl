uniform mat4 uMatrix; //变换矩阵

attribute vec4 aPosition; //需要位置信息
attribute vec2 aCoordinate; //纹理坐标

varying vec2 vCoordinate; //传到片元着色器
varying vec4 vPosition; //经过投影变换后的位置
varying vec4 vOriPosition; //在OpenGL空间中的位置

void main(){
    vOriPosition = aPosition;
    gl_Position = uMatrix * aPosition;
    vCoordinate = aCoordinate;  //对其进行赋值
    vPosition = gl_Position;
}
