package com.lx.multimedialearn.openglstudy.data;

/**
 * 构建几何图形的基础类
 *
 * @author lixiao
 * @since 2017-10-16 16:27
 */
public class Geometry {

    /**
     * 构建几何中点
     */
    public static class Point {
        public float x, y, z;

        public Point(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /**
         * 平移
         *
         * @param distance
         * @return
         */
        public Point translateY(float distance) {
            return new Point(x, y + distance, z);
        }

        /**
         * 平移
         *
         * @param vector
         * @return
         */
        public Point translate(Vector vector) {
            return new Point(x + vector.x,
                    y + vector.y,
                    z + vector.z
            );
        }
    }

    /**
     * 构建几何中的圆
     */
    public static class Circle {
        public Point center;
        public float radius;

        public Circle(Point center, float radius) {
            this.center = center;
            this.radius = radius;
        }

        /**
         * 对圆进行缩放
         *
         * @param scale
         * @return
         */
        public Circle scale(float scale) {
            return new Circle(center, radius * scale);
        }
    }

    /**
     * 构建几何中的圆柱体
     */
    public static class Cylinder {
        public Point center;  //构建圆顶
        public float radius;
        public float height; //构建柱体

        public Cylinder(Point center, float radius, float height) {
            this.center = center;
            this.radius = radius;
            this.height = height;
        }
    }


    /**
     * 构建射线，射线由点+向量构建
     */
    public static class Ray {
        public Point point;
        public Vector vector;

        public Ray(Point point, Vector vector) {
            this.point = point;
            this.vector = vector;
        }
    }

    /**
     * 构建向量
     */
    public static class Vector {
        public float x, y, z;

        /**
         * 向量由三个数构建
         *
         * @param x
         * @param y
         * @param z
         */
        public Vector(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /**
         * 向量的长度
         *
         * @return
         */
        public float length() {
            return (float) Math.sqrt((double) x * x + (double) y * y + (double) z * z);
        }

        /**
         * 计算与other的交叉内积
         *
         * @param other
         * @return
         */
        public Vector crossProduct(Vector other) {
            return new Vector(
                    (y * other.z) - (z * other.y),
                    (z * other.x) - (x * other.z),
                    (x * other.y) - (y * other.x));
        }

        /**
         * 对点进行因子缩放
         *
         * @param f
         * @return
         */
        public Vector scale(float f) {
            return new Vector(x * f, y * f, z * f);
        }

        /**
         * 点积
         *
         * @param other
         * @return
         */
        public float dotProduct(Vector other) {
            return x * other.x
                    + y * other.y
                    + z * other.z;
        }

        /**
         * 对向量进行归一化处理
         * @return
         */
        public Vector normalize() {
            return scale(1f / length());
        }

    }

    /**
     * 球体
     */
    public static class Sphere {
        public Point center;
        public float radius;

        public Sphere(Point center, float radius) {
            this.center = center;
            this.radius = radius;
        }
    }

    public static class Plane {
        public Point point; //平面上任一个点
        public Vector normal; //该点对应的法向量

        public Plane(Point point, Vector normal) {
            this.point = point;
            this.normal = normal;
        }
    }

    /**
     * 判断射线和圆球是否相交
     * 通过射线到圆球的圆心的距离 < radius
     *
     * @param malletBoundingSphere
     * @param ray
     * @return
     */
    public static boolean intersects(Sphere malletBoundingSphere, Ray ray) {
        return distanceBetween(malletBoundingSphere.center, ray) < malletBoundingSphere.radius;
    }

    /**
     * 计算射线和平面相交的点
     *
     * @param ray
     * @param plane
     * @return
     */
    public static Point intersectionPoint(Ray ray, Plane plane) {
        Vector rayToPlaneVector = vectorBetween(ray.point, plane.point);

        float scaleFactor = rayToPlaneVector.dotProduct(plane.normal) /
                ray.vector.dotProduct(plane.normal);
        Point intersectionPoint = ray.point.translate(ray.vector.scale(scaleFactor));
        return intersectionPoint;
    }

    /**
     * 计算圆心到射线的距离，通过两个向量的交叉乘积 / 2为面积，面积/边长 = 高度
     *
     * @param point 中心点
     * @param ray
     * @return
     */
    public static float distanceBetween(Point point, Ray ray) {
        Vector p1ToPoint = vectorBetween(ray.point, point);
        Vector p2ToPoint = vectorBetween(ray.point.translate(ray.vector), point);

        float areaOfTriangleTimesTwo = p1ToPoint.crossProduct(p2ToPoint).length();
        float lengthOfBase = ray.vector.length();
        float distanceFromPointToRay = areaOfTriangleTimesTwo * 2 / lengthOfBase;
        return distanceFromPointToRay;
    }

    /**
     * 获取从一个近点指向远点的向量
     *
     * @param from
     * @param to
     * @return
     */
    public static Vector vectorBetween(Point from, Point to) {
        return new Vector(
                to.x - from.x,
                to.y - from.y,
                to.z - from.z
        );
    }
}
