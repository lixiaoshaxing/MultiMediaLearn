uniform mat4 u_Matrix; //对虚拟坐标系做归一化处理
attribute vec4 a_Position; //对于每一个定义过的单一顶点，顶点着色器都会被调用一次，会使用a_Position接受当前顶点的位置，vec4表示4个分量，默认情况下，前三个为0，w分量为1
//attribute vec4 a_Color; //在顶点着色器中添加颜色属性，使用平滑着色替代uniform进行着色
//varying vec4 v_Color;  //varying变量传給片段着色器进行颜色设置

void main(){
   // v_Color = a_Color; //对顶点进行颜色赋值，顶点与顶点之间进行平滑着色
    gl_Position = u_Matrix * a_Position; //matrix左乘， 构建虚拟空间，以后的点都会出现在虚拟空间里，赋值给gl_Position，这里会被当做单一顶点的最终位置，并使用一系列顶点绘制点，直线，三角形
   // gl_PointSize = 10.0; //指定点的大小应该是10，OpenGL把点分割为片段时，以gl_Position为中心的四边形，四条边的长度是10
}
