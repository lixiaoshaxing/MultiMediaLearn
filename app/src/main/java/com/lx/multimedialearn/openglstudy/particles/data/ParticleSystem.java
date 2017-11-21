package com.lx.multimedialearn.openglstudy.particles.data;

import android.graphics.Color;
import android.opengl.GLES20;

import com.lx.multimedialearn.openglstudy.hockey.data.Geometry;
import com.lx.multimedialearn.openglstudy.particles.program.ParticleShaderProgram;
import com.lx.multimedialearn.utils.GlUtil;

/**
 * 粒子系统，绑定所有粒子的数据
 *
 * @author lixiao
 * @since 2017-10-18 17:56
 */
public class ParticleSystem {
    private static final int POSITION_COMPONENT_COUNT = 3; //粒子初始位置
    private static final int COLOR_COMPONENT_COUNT = 3; //粒子颜色
    private static final int VECTOR_COMPONENT_COUNT = 3; //粒子发射方向
    private static final int PARTICLE_START_TIME_COMPONENT_COUNT = 1; //粒子发射时间

    private static final int TOTAL_COMPONENT_COUNT = //一个粒子占的位数
            POSITION_COMPONENT_COUNT
                    + COLOR_COMPONENT_COUNT
                    + VECTOR_COMPONENT_COUNT
                    + PARTICLE_START_TIME_COMPONENT_COUNT;

    private static final int STRIDE = TOTAL_COMPONENT_COUNT * GlUtil.SIZEOF_FLOAT; //一个粒子在本地占的步长

    private float[] particles; //存储所有的粒子
    private VertexArray vertexArray; //存储粒子数据
    private int maxParticleCount; //最大粒子数，如果粒子超过数量，就从数组前朝后填充
    private int currentParticleCount; //当前粒子总数，对数组进行赋值
    private int nextParticle; //下一个粒子

    public ParticleSystem(int maxParticleCount) {
        particles = new float[maxParticleCount * TOTAL_COMPONENT_COUNT]; //存储粒子数据
        vertexArray = new VertexArray(particles);
        this.maxParticleCount = maxParticleCount;
    }

    /**
     * 绑定数据
     *
     * @param particleProgram
     */
    public void bindData(ParticleShaderProgram particleProgram) {
        int dataOffset = 0;
        vertexArray.setVertexAttribPointer(
                dataOffset, particleProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT, STRIDE);
        dataOffset += POSITION_COMPONENT_COUNT;

        vertexArray.setVertexAttribPointer(
                dataOffset, particleProgram.getColorAttributeLocation(),
                COLOR_COMPONENT_COUNT, STRIDE);
        dataOffset += COLOR_COMPONENT_COUNT;

        vertexArray.setVertexAttribPointer(
                dataOffset, particleProgram.getDirectionVectorAttributeLocation(),
                VECTOR_COMPONENT_COUNT, STRIDE);
        dataOffset += VECTOR_COMPONENT_COUNT;

        vertexArray.setVertexAttribPointer(
                dataOffset, particleProgram.getParticleStartTimeAttributeLocation(),
                PARTICLE_START_TIME_COMPONENT_COUNT, STRIDE);
    }

    public void draw() {
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, currentParticleCount); //按照点的顺序画
    }

    /**
     * 添加粒子
     *
     * @param position  初始位置
     * @param color     初始颜色
     * @param direction 初始方向，根据方向进行运动
     */
    public void addParticle(Geometry.Point position, int color, Geometry.Vector direction, float particleStartTime) {
        int particleOffset = nextParticle * TOTAL_COMPONENT_COUNT; //设置粒子的顺序，填充粒子到大数组，粒子从哪里开始
        int currentOffset = particleOffset; //属性计数
        nextParticle++;

        if (currentParticleCount < maxParticleCount) {
            currentParticleCount++;
        }

        if (nextParticle == maxParticleCount) { //如果到了最大粒子数
            nextParticle = 0;
        }

        particles[currentOffset++] = position.x; //存储位置
        particles[currentOffset++] = position.y;
        particles[currentOffset++] = position.z;

        particles[currentOffset++] = Color.red(color) / 255f; //存储颜色
        particles[currentOffset++] = Color.green(color) / 255f;
        particles[currentOffset++] = Color.blue(color) / 255f;

        particles[currentOffset++] = direction.x;
        particles[currentOffset++] = direction.y; //存储方向
        particles[currentOffset++] = direction.z;

        particles[currentOffset++] = particleStartTime; //存储开始时间

        vertexArray.updateBuffer(particles, particleOffset, TOTAL_COMPONENT_COUNT);
    }
}
