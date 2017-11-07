precision mediump float; //中等精度

uniform sampler2D u_TextureUnit; //纹理单元，接收图片的纹理数据
varying vec2 v_TextureCoordinates; //获取顶点着色器中纹理坐标，进行纹理单元赋值

void main(){
    //获取特定坐标，获取坐标处的纹理数据，然后对color赋值，进行片段渲染
    gl_FragColor = texture2D(u_TextureUnit, v_TextureCoordinates); //使用texture2D进行纹理颜色赋值
}