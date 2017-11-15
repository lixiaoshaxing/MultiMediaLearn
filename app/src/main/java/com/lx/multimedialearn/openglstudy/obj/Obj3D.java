package com.lx.multimedialearn.openglstudy.obj;

import java.nio.FloatBuffer;

/**
 * 3D Obj模型数据集合
 *
 * @author lixiao
 * @since 2017-11-14 22:46
 */
public class Obj3D {
    public FloatBuffer vert; //顶点坐标，按顺序排好，按三角形开始画，步长为3
    public int vertCount; //顶点总数
    public FloatBuffer vertNorl; //法向量
}
