precision mediump float;

uniform samplerCube u_textureUnit; //画天空图
varying vec3 v_Position;

void main(){
    gl_FragColor = textureCube(u_textureUnit, v_Position); //这里有点复杂。。。把纹理坐标和被插值的立方体的位置一一对应？
}
