package com.lx.multimedialearn.openglstudy.hockey.data;

import android.opengl.GLES20;

import java.util.ArrayList;
import java.util.List;

/**
 * 物体构建器，组装几何体的数据，画几何体
 *
 * @author lixiao
 * @since 2017-10-16 16:38
 */
public class ObjectBuilder {
    public static final int FLOATS_PER_VERTEX = 3;
    private float[] vertexData;  //构建几何体的数据集合
    private List<DrawCommand> drawList = new ArrayList<>(); //绘图命令集合
    private int offset = 0;

    private ObjectBuilder(int sizeInVertices) {
        vertexData = new float[FLOATS_PER_VERTEX * sizeInVertices];
    }


    /**
     * 构建冰球
     *
     * @param puck
     * @param numPoints
     * @return
     */
    static GeneratedData createPuck(Geometry.Cylinder puck, int numPoints) {
        int size = sizeOfCircleInVertices(numPoints)
                + sizeOfOpenCylinderInVertices(numPoints); //画圆柱体，需要顶部+柱面个点
        ObjectBuilder builder = new ObjectBuilder(size); //本地坐标需要顶部+柱面的点的数组集合

        Geometry.Circle puckTop = new Geometry.Circle(
                puck.center.translateY(puck.height / 2f), //center的y轴是在柱体中心，所以需要向上偏移y/2距离
                puck.radius); //画顶部的坐标
        builder.appendCircle(puckTop, numPoints);
        builder.appendOpenCylinder(puck, numPoints);
        return builder.build();
    }

    /**
     * 构建木槌
     *
     * @return
     */
    static GeneratedData createMallet(Geometry.Point center, float radius, float height, int numPoints) {
        int size = sizeOfCircleInVertices(numPoints) * 2
                + sizeOfOpenCylinderInVertices(numPoints) * 2;
        ObjectBuilder builder = new ObjectBuilder(size);
        float baseHeight = height * 0.25f;
        Geometry.Circle baseCircle = new Geometry.Circle(center.translateY(-baseHeight),
                radius);

        Geometry.Cylinder baseCylinder = new Geometry.Cylinder(
                baseCircle.center.translateY(-baseHeight / 2f),
                radius,
                baseHeight);

        builder.appendCircle(baseCircle, numPoints);
        builder.appendOpenCylinder(baseCylinder, numPoints);  //加入木槌的基部

        //加入木槌的手柄
        float handleHeight = height * 0.75f;
        float handleRadius = radius / 3f;
        Geometry.Circle handleCircle = new Geometry.Circle(
                center.translateY(height * 0.5f),
                handleRadius);
        Geometry.Cylinder handlerCylinder = new Geometry.Cylinder(
                handleCircle.center.translateY(-handleHeight / 2f),
                handleRadius,
                handleHeight);

        builder.appendCircle(handleCircle, numPoints);
        builder.appendOpenCylinder(handlerCylinder, numPoints);
        return builder.build();
    }


    /**
     * 根据柱体顶部的需求，构建顶部圆形
     *
     * @param circle
     * @param numPoints
     */
    private void appendCircle(Geometry.Circle circle, int numPoints) {
        final int startVertex = offset / FLOATS_PER_VERTEX;
        final int numVertices = sizeOfCircleInVertices(numPoints);
        vertexData[offset++] = circle.center.x;
        vertexData[offset++] = circle.center.y;
        vertexData[offset++] = circle.center.z;

        for (int i = 0; i <= numPoints; i++) {
            float angleInRadians = ((float) i / (float) numPoints) * ((float) Math.PI * 2f); //2PI是360的弧度，通过弧度，确定每个三角形扇的顶点
            vertexData[offset++] = circle.center.x + circle.radius * (float) Math.cos(angleInRadians); //计算x轴
            vertexData[offset++] = circle.center.y;
            vertexData[offset++] = circle.center.z + circle.radius * (float) Math.sin(angleInRadians);//计算z轴

        }
        drawList.add(new DrawCommand() {
            @Override
            public void draw() {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, startVertex, numVertices);
            }
        });
    }

    /**
     * 使用三角形带 构建圆柱体侧面
     *
     * @param cylinder
     * @param numPoints
     */
    private void appendOpenCylinder(Geometry.Cylinder cylinder, int numPoints) {
        final int startVertex = offset / FLOATS_PER_VERTEX;
        final int numVertices = sizeOfOpenCylinderInVertices(numPoints);
        float yStart = cylinder.center.y - (cylinder.height / 2f);
        float yEnd = cylinder.center.y + (cylinder.height / 2f); //通过圆柱体中心，获取圆柱体的两个平面的y轴范围
        for (int i = 0; i <= numPoints; i++) {
            float angleInRadians =
                    ((float) i / (float) numPoints) * ((float) Math.PI * 2f);

            float xPosition = cylinder.center.x + cylinder.radius * (float) Math.cos(angleInRadians);
            float zPosition = cylinder.center.z + cylinder.radius * (float) Math.sin(angleInRadians);

            vertexData[offset++] = xPosition;
            vertexData[offset++] = yStart;
            vertexData[offset++] = zPosition;

            vertexData[offset++] = xPosition;
            vertexData[offset++] = yEnd;
            vertexData[offset++] = zPosition;
        }

        drawList.add(new DrawCommand() {
            @Override
            public void draw() {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, startVertex, numVertices);
            }
        });

    }

    /**
     * 使用三角扇形构建圆柱体顶部需要顶点数量
     *
     * @param numPoints
     * @return
     */
    public static int sizeOfCircleInVertices(int numPoints) {
        return 1  //画圆柱体顶部，使用三角扇形，需要1个中心点
                + (numPoints //每个三角扇形都需要一个顶点
                + 1); //结束的时候需要重复第一个顶点，达到闭合
    }

    /**
     * 使用三角形带构建圆柱体的圆柱需要顶点数目，两个顶点构建一个三角形，桥梁架构
     *
     * @return
     */
    public static int sizeOfOpenCylinderInVertices(int numPoints) {
        return (numPoints + 1) //构建三角形带需要：围着顶部圆的每个顶点都需要两个顶点，为了闭合+1
                * 2;
    }

    /**
     * 画图形的数据结构，顶点+画图命令集合
     */
    static class GeneratedData {
        float[] vertexData;
        List<DrawCommand> drawList;

        public GeneratedData(float[] vertexData, List<DrawCommand> drawList) {
            this.vertexData = vertexData;
            this.drawList = drawList;
        }
    }

    /**
     * 组装数据
     *
     * @return
     */
    private GeneratedData build() {
        return new GeneratedData(vertexData, drawList);
    }


    /**
     * 画图命令
     */
    interface DrawCommand {
        void draw();
    }
}
