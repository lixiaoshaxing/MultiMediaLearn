uniform mat4 u_Matrix; //投影变换，确定粒子位置
uniform float u_Time; //确定当前时间，计算出粒子运行了多长时间

attribute vec3 a_Position; //粒子的初始位置， 粒子属性
attribute vec3 a_Color; //粒子颜色
attribute vec3 a_DirectionVector; //粒子发射的方向
attribute float a_ParticleStartTime; //粒子发射时间

varying vec3 v_Color; //和片段着色器一起确定颜色
varying float v_ElapsedTime; //消耗的时间，确定颜色值

void main(){
    v_Color = a_Color;
    v_ElapsedTime = u_Time - a_ParticleStartTime; //确定耗时
    float gravityFactor = v_ElapsedTime * v_ElapsedTime / 8.0; //重力因子， 8.0是随便设置的
    vec3 currentPostion = a_Position + (a_DirectionVector * v_ElapsedTime); //确定当前位置
    currentPostion.y -= gravityFactor;  //考虑重力因素
    gl_Position = u_Matrix * vec4(currentPostion, 1.0); //确定位置，加入w分量
    gl_PointSize = 25.0;
}
