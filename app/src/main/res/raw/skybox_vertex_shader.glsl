uniform mat4 u_Matrix;
attribute vec3 a_Position;
varying vec3 v_Position;

void main(){
    v_Position = a_Position;
    v_Position.z = -v_Position.z; //右手坐标系转换为左手坐标系

    gl_Position = u_Matrix * vec4(a_Position, 1.0);
    gl_Position = gl_Position.xyww; //用w值替换z值，使天空图w值为1，永远在所有场景的后边
}