package com.lx.multimedialearn.openglstudy.data;

import android.opengl.Matrix;

import java.util.Random;

/**
 * 粒子喷泉，使用粒子系统生成
 *
 * @author lixiao
 * @since 2017-10-18 21:08
 */
public class ParticleShooter {
    private Geometry.Point position;
    private Geometry.Vector direction;
    private int color;

    //增加粒子发射角度，速度
    private float angleVariance; //角度
    private float speedVariance; //速度

    private Random random = new Random();

    private float[] rotationMatrix = new float[16];
    private float[] directionVector = new float[4];
    private float[] resultVector = new float[4]; //通过传入的角度和速度变量，加上随机数，生成最终的方向向量

    /**
     * 生成粒子喷泉
     *
     * @param position               初始位置
     * @param direction              方向向量
     * @param color                  颜色
     * @param angleVarianceInDegrees 方向角度
     * @param speedVariance          速度
     */
    public ParticleShooter(Geometry.Point position, Geometry.Vector direction, int color, float angleVarianceInDegrees, float speedVariance) {
        this.position = position;
        this.direction = direction;
        this.color = color;
        this.angleVariance = angleVarianceInDegrees;
        this.speedVariance = speedVariance;

        directionVector[0] = direction.x; //对方向进行赋值，增加角度和速度，生成最终的向量
        directionVector[1] = direction.y;
        directionVector[2] = direction.z;
    }

    /**
     * 使用粒子系统，传入当前时间，画一定数量的粒子
     *
     * @param particleSystem
     * @param currentTime
     * @param count
     */
    public void addParticles(ParticleSystem particleSystem, float currentTime, int count) {
        for (int i = 0; i < count; i++) {
            Matrix.setRotateEulerM(rotationMatrix, 0,  //创建旋转矩阵
                    (random.nextFloat() - 0.5f) * angleVariance,
                    (random.nextFloat() - 0.5f) * angleVariance,
                    (random.nextFloat() - 0.5f) * angleVariance
            );
            Matrix.multiplyMV(
                    resultVector, 0,
                    rotationMatrix, 0,
                    directionVector, 0); //根据，旋转过的方向，生成最终的方向

            float speedAdjustment = 1f + random.nextFloat() * speedVariance;

            Geometry.Vector thisDirection = new Geometry.Vector(
                    resultVector[0] * speedAdjustment,
                    resultVector[1] * speedAdjustment,
                    resultVector[2] * speedAdjustment
            );

            particleSystem.addParticle(position, color, thisDirection, currentTime);
        }
    }
}
