precision mediump float;
varying vec3 v_Color; //和片段着色器一起确定颜色
varying float v_ElapsedTime; //消耗的时间，确定颜色值

//使用图片纹理，替代图形
uniform sampler2D u_TextureUnit;

void main(){
    //把方形变换为圆形，每个点都是gl_PointCoord表明坐标
//    float xDistance = 0.5 - gl_PointCoord.x;
//    float yDistance = 0.5 - gl_PointCoord.y;
//    float distanceFromCenter = sqrt(xDistance * xDistance + yDistance * yDistance);
//
//    if(distanceFromCenter > 0.5){ //计算点到圆心的距离，如果大于0.5，就不绘画
//        discard;
//    }else{
//        gl_FragColor = vec4(v_Color / v_ElapsedTime, 1.0); //对于color使用4维表示
//    }

    gl_FragColor = vec4(v_Color / v_ElapsedTime, 1.0) *
                    texture2D(u_TextureUnit, gl_PointCoord);

}