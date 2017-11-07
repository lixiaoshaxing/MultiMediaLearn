package com.lx.multimedialearn.openglstudy.data;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES20;
import android.util.Log;

import com.lx.multimedialearn.openglstudy.program.HeightmapShaderProgram;
import com.lx.multimedialearn.utils.GlUtil;

import static com.lx.multimedialearn.openglstudy.data.Geometry.Point;
import static com.lx.multimedialearn.openglstudy.data.Geometry.Vector;

/**
 * 加载高度图数据
 * 1. 解析高度图，获取所有像素点数据
 * 2. 把数据放入到缓冲区
 *
 * @author lixiao
 * @since 2017-10-19 21:07
 */
public class HeightMap {
    private static final int POSITION_COMPONENT_COUNT = 3;

    //每个顶点增加一个法线向量，构建朗伯体反射，反射光线与平面的倾斜角度有关，法线可以标明倾斜角度
    private static final int NORMAL_COMPONENT_COUNT = 3;
    private static final int TOTAL_COMPONENT_COUNT = POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT;
    private static final int STRIDE = TOTAL_COMPONENT_COUNT * GlUtil.SIZEOF_FLOAT; //存储了两种属性，所以需要步长

    private int width;
    private int height;
    private int numElements; //生成的方块个数
    private VertexBuffer vertexBuffer; //顶点缓冲区
    private IndexBuffer indexBuffer; // 索引缓冲区

    public HeightMap(Bitmap bitmap) {
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        if (width * height > 65536) {
            Log.w("sys.out", "高度图太大了！");
        }
        numElements = calculateNumElements();
        vertexBuffer = new VertexBuffer(loadBitmapData(bitmap));
        indexBuffer = new IndexBuffer(createIndexData());
    }

    public void bindData(HeightmapShaderProgram heightmapProgram) {
        vertexBuffer.setVertexAttribPointer(
                0,
                heightmapProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE);
        //在缓冲区中存储每个点的法线向量
        vertexBuffer.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT * GlUtil.SIZEOF_FLOAT,
                heightmapProgram.getNormalAttributeLocation(),
                NORMAL_COMPONENT_COUNT,
                STRIDE
        );

    }

    public void draw() {
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.getBufferId());
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numElements, GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    /**
     * 计算高度图生成的方块的数目
     *
     * @return
     */
    private int calculateNumElements() {
        return (width - 1) * (height - 1) * 2 * 3; //一条边上4个点，就会有3条线，则构成3 * 3 个面
        //每个面有两个三角形，每个三角形有3个索引，构建立方体的时候，有三个索引构建一个三角形
    }

    /**
     * 创建索引缓冲区
     * 1. 创建索引数组
     * 2. 循环遍历，获取正确的索引，放入对应的数组位置上
     * 3. 生成索引缓冲区
     *
     * @return
     */
    private short[] createIndexData() {
        final short[] indexData = new short[numElements];
        int offset = 0;
        for (int row = 0; row < height - 1; row++) {
            for (int col = 0; col < width - 1; col++) {
                short topLeftIndexNum = (short) (row * width + col);
                short topRightIndexNum = (short) (row * width + col + 1);
                short bottomLeftIndexNum = (short) ((row + 1) * width + col);
                short bottomRightIndexNum = (short) ((row + 1) * width + col + 1);

                indexData[offset++] = topLeftIndexNum;
                indexData[offset++] = bottomLeftIndexNum;
                indexData[offset++] = topRightIndexNum;

                indexData[offset++] = topRightIndexNum;
                indexData[offset++] = bottomLeftIndexNum;
                indexData[offset++] = bottomRightIndexNum;
            }
        }
        return indexData;
    }

    /**
     * 从高度图中获取高度数据
     * 1. 读取所有像素值,一行一行的读取数据，cpu效率更高
     * 2. 根据像素进行分解，分解为x-z平面和y轴的亮度分量
     * 3. 对缓冲区进行赋值
     *
     * @param bitmap
     * @return
     */
    private float[] loadBitmapData(Bitmap bitmap) {
        float[] heightmapVertices = new float[width * height * TOTAL_COMPONENT_COUNT];
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height); //返回的是一维数组
        bitmap.recycle(); //回收
        int offset = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Point point = getPoint(pixels, row, col);

                heightmapVertices[offset++] = point.x;
                heightmapVertices[offset++] = point.y;
                heightmapVertices[offset++] = point.z;

                //找到这个点的相邻点，构建平面，生成平面发现
                Point top = getPoint(pixels, row - 1, col);
                Point left = getPoint(pixels, row, col - 1);
                Point right = getPoint(pixels, row, col + 1);
                Point bottom = getPoint(pixels, row + 1, col);

                //构建从右向左，从上到下的向量，这样法向量朝上
                Vector rightToLeft = Geometry.vectorBetween(right, left);
                Vector topToBottom = Geometry.vectorBetween(top, bottom);

                Vector normal = rightToLeft.crossProduct(topToBottom).normalize(); //叉积，并归一化

                heightmapVertices[offset++] = normal.x;
                heightmapVertices[offset++] = normal.y;
                heightmapVertices[offset++] = normal.z;
            }
        }
        return heightmapVertices;
    }

    /**
     * 根据高度图像素的值，计算位置，高度值
     *
     * @param pixels
     * @param row
     * @param col
     * @return
     */
    private Point getPoint(int[] pixels, int row, int col) {
        float x = ((float) col / (float) (width - 1)) - 0.5f;
        //计算颜色分量，算出亮度，亮度表示高度
        float z = ((float) row / (float) (height - 1)) - 0.5f;

        row = clamp(row, 0, width - 1);
        col = clamp(col, 0, height - 1); //控制范围，对于边界上的顶点，赋予它们和中间顶点一样的高度

        float y = (float) Color.red(pixels[(row * height) + col]) / (float) 255;
        return new Point(x, y, z);
    }

    /**
     * 取min~max中间的值
     *
     * @param val
     * @param min
     * @param max
     * @return
     */
    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

}
