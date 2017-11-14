package com.lx.multimedialearn.openglstudy.stl;

import com.lx.multimedialearn.utils.GlUtil;

import java.nio.FloatBuffer;

/**
 * 模型相关信息
 * 从Stl文件中解析出来，把数据使用model进行包装，在进行处理：所有的数据对应一种模型
 * stl文件：文件名，三角面个数 + （n个）三角面（顶点+法向量+属性）
 * 加入描述纹理的数组，每一个stl model对应一个纹理数组，根据纹理进行绘画
 *
 * @author lixiao
 * @since 2017-09-04 12:51
 */
public class Model {
    //1. 三角面个数
    private int facetCount;
    //2. 顶点坐标数组
    private float[] verts;
    //3. 每个顶点对应的法向量数组（法向量是什么？）
    private float[] vnorms;
    //4. 每个三角面的属性信息
    private short[] remarks;

    //纹理图片
    private String pictureName;
    //纹理ID
    int[] textureIds;
    //每个顶点对应的图片的坐标位置
    private float[] textures;

    //存储顶点的buffer
    private FloatBuffer vertBuffer;
    //存储法向量数组
    private FloatBuffer vnormBuffer;
    //每个顶点对应的纹理坐标转换来的buffer
    private FloatBuffer textureBuffer;

    //为了保证所有的点都在surfaceView里显示，通过统计最大最小的顶点值，设置中心点的位置
    public float minX;
    public float maxX;
    public float minY;
    public float maxY;
    public float minZ;
    public float maxZ;

    // picture->id->textures
    public void setTextures(float[] textures) {
        this.textures = textures;
        textureBuffer = GlUtil.createFloatBuffer(textures);
    }

    public String getPictureName() {
        return pictureName;
    }

    public void setPictureName(String pictureName) {
        this.pictureName = pictureName;
    }

    public int[] getTextureIds() {
        return textureIds;
    }

    public void setTextureIds(int[] textureIds) {
        this.textureIds = textureIds;
    }

    public FloatBuffer getTextureBuffer() {
        return textureBuffer;
    }

    //通过最大，最小点计算中心点的位置
    public Point getCenterPoint() {
        float cx = minX + (maxX - minX) / 2;
        float cy = minY + (maxY - minY) / 2;
        float cz = minZ + (maxZ - minZ) / 2;
        return new Point(cx, cy, cz);
    }

    //通过中心点，最大值，最小值，计算能够包裹3d图像的半径，这里应该是最小的半径？
    public float getR() {
        float dx = (maxX - minX);
        float dy = (maxY - minY);
        float dz = (maxZ - minZ);

        float max = dx;
        if (dy > max) {
            max = dy;
        }
        if (dz > max) {
            max = dz;
        }
        return max;
    }

    public int getFacetCount() {
        return facetCount;
    }

    public void setFacetCount(int facetCount) {
        this.facetCount = facetCount;
    }

    public float[] getVerts() {
        return verts;
    }

    public void setVerts(float[] verts) {
        this.verts = verts;
        vertBuffer = GlUtil.createFloatBuffer(verts);
    }

    public FloatBuffer getVertBuffer() {
        return vertBuffer;
    }

    public float[] getVnorms() {
        return vnorms;
    }

    public void setVnorms(float[] vnorms) {
        this.vnorms = vnorms;
        vnormBuffer = GlUtil.createFloatBuffer(vnorms);
    }

    public FloatBuffer getVnormBuffer() {
        return vnormBuffer;
    }

    public short[] getRemarks() {
        return remarks;
    }

    public void setRemarks(short[] remarks) {
        this.remarks = remarks;
    }
}
