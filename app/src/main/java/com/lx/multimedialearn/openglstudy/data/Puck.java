package com.lx.multimedialearn.openglstudy.data;

import com.lx.multimedialearn.openglstudy.program.ColorShaderProgram;

import java.util.List;

/**
 * 冰球类
 *
 * @author lixiao
 * @since 2017-10-16 18:04
 */
public class Puck {
    private static final int POSITION_COMPONENT_COUNT = 3;
    public float radius, height;

    private VertexArray vertexArray;
    private List<ObjectBuilder.DrawCommand> drawList;

    public Puck(float radius, float height, int numPointsAroundPuck) {
        ObjectBuilder.GeneratedData data = ObjectBuilder.createPuck(
                new Geometry.Cylinder(new Geometry.Point(0f, 0f, 0f), radius, height),
                numPointsAroundPuck);
        this.radius = radius;
        this.height = height;
        vertexArray = new VertexArray(data.vertexData);
        drawList = data.drawList;
    }

    public void bindData(ColorShaderProgram program) {
        vertexArray.setVertexAttribPointer(0, program.getPositionAttributeLocation(), POSITION_COMPONENT_COUNT, 0);
    }

    public void draw() {
        for (ObjectBuilder.DrawCommand temp : drawList) {
            temp.draw();
        }
    }
}
