package com.lx.multimedialearn.openglstudy.particles.data;

import android.opengl.GLES20;

import com.lx.multimedialearn.openglstudy.particles.program.SkyBoxShaderProgram;

import java.nio.ByteBuffer;

/**
 * 创建天空图
 * 1. 加载六张图片
 * 2. 使用索引数组（创建三角形扇形），创建立方体
 * 3. 绑定立方体和图片
 *
 * @author lixiao
 * @since 2017-10-19 15:20
 */
public class SkyBox {
    private static final int POSITION_COMPONENT_COUNT = 3; //每个顶点由三个浮点数表示
    private VertexArray vertexArray;
    private ByteBuffer indexArray; //使用索引数组，即一个立方体，由八个顶点表示，每个面由四个顶点表示，这个缓冲区存储画当前面需要哪几个顶点

    public SkyBox() {
        vertexArray = new VertexArray(new float[]{ //八个顶点坐标
                -1, 1, 1, //上-左-近
                1, 1, 1, //上-右-近
                -1, -1, 1, //下-左-近
                1, -1, 1, //下-右-近
                -1, 1, -1,//上-左-远
                1, 1, -1,//上-右-远
                -1, -1, -1,//下-左-远
                1, -1, -1//下-右-远
        });

        indexArray = ByteBuffer.allocateDirect(6 * 6).put(new byte[]{
                1, 3, 0,
                0, 3, 2, //前

                4, 6, 5,
                5, 6, 7, //后

                0, 2, 4,
                4, 2, 6, //左

                5, 7, 1,
                1, 7, 3, //右

                5, 1, 4,
                4, 1, 0, //上

                6, 2, 7,
                7, 2, 3 //下
        });
        indexArray.position(0);

    }

    public void bindData(SkyBoxShaderProgram skyboxProgram) {
        vertexArray.setVertexAttribPointer(0, skyboxProgram.getPositionAttributeLocation(), POSITION_COMPONENT_COUNT, 0);
    }

    public void draw() {
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 36, GLES20.GL_UNSIGNED_BYTE, indexArray);
    }
}
