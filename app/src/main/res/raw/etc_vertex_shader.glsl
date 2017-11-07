//渲染动画的顶点着色器
attribute vec4 vPosition;
attribute vec2 vCoord;
varying vec2 aCoord;
uniform mat4 vMatrix;

void main() {
    aCoord = vCoord;
    gl_Position = vMatrix*vPosition;
}
