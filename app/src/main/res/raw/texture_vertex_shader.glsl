//可以渲染纹理的顶点着色器
uniform mat4 u_Matrix; //进行投影矩阵，模型变换

attribute vec4 a_Position;
attribute vec2 a_TextureCoordinates; //纹理坐标点

varying vec2 v_TextureCoordinates; //和片段着色器共同标记纹理s,t坐标

void main(){
    v_TextureCoordinates = a_TextureCoordinates;
    gl_Position = u_Matrix * a_Position;
}

