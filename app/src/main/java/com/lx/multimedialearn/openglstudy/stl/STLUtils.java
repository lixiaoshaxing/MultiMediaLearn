package com.lx.multimedialearn.openglstudy.stl;

import java.util.List;

/**
 * 画bitmap通用工具类
 *
 * @author lixiao
 * @since 2017-09-06 18:26
 */
public class STLUtils {
    /**
     * 获取多个model中的最大最小Point
     *
     * @param models
     * @param isMin  获取最小：true
     * @return
     */
    public static Point modelBorder(List<Model> models, boolean isMin) {
        Point p;
        if (isMin) {
            p = new Point(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        } else {
            p = new Point(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
        }
        for (Model model : models) {
            if (isMin) { //获取多个model中的最小值
                if (model.minX < p.x) {
                    p.x = model.minX;
                }
                if (model.minY < p.y) {
                    p.y = model.minY;
                }
                if (model.minZ < p.z) {
                    p.z = model.minZ;
                }
            } else {
                if (model.maxX > p.x)
                    p.x = model.maxX;
                if (model.maxY > p.y)
                    p.y = model.maxY;
                if (model.maxZ > p.z)
                    p.z = model.maxZ;
            }
        }
        return p;
    }

    /**
     * 通过多个model获取中心点
     *
     * @param models
     * @return
     */
    public static Point getCenter(List<Model> models) {
        Point min = modelBorder(models, true);
        Point max = modelBorder(models, false);
        float cx = min.x + (max.x - min.x) / 2;
        float cy = min.y + (max.y - min.y) / 2;
        float cz = min.z + (max.z - min.z) / 2;
        return new Point(cx, cy, cz);
    }

    /**
     * 获取多个model的最大半径，这样能包住整个图片
     *
     * @param models
     * @return
     */
    public static float getR(List<Model> models) {
        Point minP = modelBorder(models, true);
        Point maxP = modelBorder(models, false);
        float rx = maxP.x - minP.x;
        float ry = maxP.y - minP.y;
        float rz = maxP.z - minP.z;
        float r = (float) (Math.sqrt(rx * rx + ry * ry + rz * rz) / 2);
        return r;
    }
}
