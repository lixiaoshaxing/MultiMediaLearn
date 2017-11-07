//光栅化使得图形的基本图元分割为片段，对于每一个片段，执行一次片段着色器
precision mediump float; //中等精度，片段着色器使用中等精度，顶点着色器使用的默认精度为高精度
//顶点加入颜色属性，就不需要这个参数了
 uniform vec4 u_Color; //uniform会让每个顶点使用同一个值，attribute会使每个顶点使用单一的值
//varying vec4 v_Color; //必须和顶点着色器重的varying变量链接起来

void main(){
    gl_FragColor = u_Color; //如果对u_Color不赋值，默认为0，是四分量，红，绿，蓝，阿尔法，这里得到的值是当前片段的颜色
  // gl_FragColor = v_Color;
}