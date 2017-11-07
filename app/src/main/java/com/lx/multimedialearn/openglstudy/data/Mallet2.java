package com.lx.multimedialearn.openglstudy.data;

import com.lx.multimedialearn.openglstudy.program.ColorShaderProgram;

import java.util.List;

/**
 * 通过画几何体画木槌,替换只是一个点的Mallet
 * 1. 描述几何体的数据结构
 *
 * @author lixiao
 * @since 2017-10-16 18:14
 */
public class Mallet2 {

    private static final int POSITION_COMPONENT_COUNT = 3;

    public float radius;
    public float height;

    private VertexArray vertexArray;
    private List<ObjectBuilder.DrawCommand> drawList;

    public Mallet2(float radius, float height, int numPointsAroundMallet) {
        ObjectBuilder.GeneratedData generatedData = ObjectBuilder.createMallet(
                new Geometry.Point(0f, 0f, 0f),
                radius,
                height,
                numPointsAroundMallet);
        this.radius = radius;
        this.height = height;

        vertexArray = new VertexArray(generatedData.vertexData);
        drawList = generatedData.drawList;
    }

    public void bindData(ColorShaderProgram colorProgram) {
        vertexArray.setVertexAttribPointer(
                0,
                colorProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                0);
    }

    public void draw() {
        for (ObjectBuilder.DrawCommand drawCommand : drawList) {
            drawCommand.draw();
        }
    }
}
