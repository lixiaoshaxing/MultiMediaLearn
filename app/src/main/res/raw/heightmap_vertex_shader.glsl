
//uniform mat4 u_Matrix;
//uniform vec3 u_VectorToLight; //存储指向方向光源的归一化向量，特定向量，太阳光来源的地方
//attribute vec3 a_Position;
//varying vec3 v_Color;
//
//attribute vec3 a_Normal; //高度图的法线向量，每个点都有自己的值
//
//
//void main(){
//    v_Color = mix(vec3(0.180, 0.467, 0.153), //深绿
//    vec3(0.660, 0.670, 0.680), //石灰
//    a_Position.y); //mix做插值，在两种颜色之间，高度取中间的比例值 高度图的值
//
//    vec3 scaledNormal = a_Normal;
//    scaledNormal.y *= 10.0; //法线高于高度的10倍，这里的数学知识很麻烦。。。。
//    scaledNormal = normalize(scaledNormal);
//
//    float diffuse = max(dot(scaledNormal, u_VectorToLight), 0.0);//方向光和表面的点击就是夹角的余弦，这里的数学又很麻烦。。。
//    diffuse *= 0.3; //黑夜才需要加，调低光的强度
//    v_Color *= diffuse; //把颜色值添加上去，处于黑色和原色之间
//
//    //加入环境光线
//    float ambient = 0.1; //太阳是0.2
//    v_Color += ambient;
//
//    gl_Position = u_Matrix * vec4(a_Position, 1.0);
//}

//以上可以显示黑夜中的，月光单一点光源的效果

//以下使用每个粒子都有点光源
uniform mat4 u_MVMatrix; //模型视图矩阵
uniform mat4 u_IT_MVMatrix; //倒置矩阵
uniform mat4 u_MVPMatrix; //合并后的模型视图投影矩阵，这里是最终矩阵

uniform vec3 u_VectorToLight; //使用矩阵变换，置换到眼睛空间， 这里的数学计算又是很复杂。。。
uniform vec4 u_PointLightPositions[3]; //转换到眼睛空间
uniform vec3 u_PointLightColors[3]; //传递颜色，使用数组可以一个变量传递多个值

attribute vec4 a_Position; //要使用的属性
attribute vec3 a_Normal;

varying vec3 v_Color;

vec3 materialColor; //计算光照
vec4 eyeSpacePosition;
vec3 eyeSpaceNormal;

vec3 getAmbientLighting();
vec3 getDirectionalLighting();
vec3 getPointLighting();

void main(){
    materialColor = mix(
    vec3(0.180, 0.467, 0.153),
    vec3(0.660, 0.670, 0.680), a_Position.y); //线性插值，获取对应的颜色值
    eyeSpacePosition = u_MVMatrix * a_Position;
    eyeSpaceNormal = normalize(vec3(u_IT_MVMatrix * vec4(a_Normal, 0.0)));

    v_Color = getAmbientLighting();
    v_Color += getDirectionalLighting();
    v_Color += getPointLighting();

    gl_Position = u_MVPMatrix * a_Position;

}

vec3 getAmbientLighting(){
    return materialColor * 0.1;
}

vec3 getDirectionalLighting(){
    return materialColor * 0.3 * max(dot(eyeSpaceNormal, u_VectorToLight), 0.0);
}

vec3 getPointLighting(){
    vec3 lightingSum = vec3(0.0);

    for(int i = 0;i < 3; i ++){ //循环计算每一个通过的点
       vec3 toPointLight = vec3(u_PointLightPositions[i]) - vec3(eyeSpacePosition); //这里数组的使用不是太明白
       float distance = length(toPointLight);

       float cosine = max(dot(eyeSpaceNormal, toPointLight), 0.0);
       lightingSum += (materialColor * u_PointLightColors[i] * 5.0 * cosine) /distance;
    }
    return lightingSum;
}





