package com.lx.multimedialearn.utils;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.view.Surface;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 相机使用工具类
 *
 * @author lixiao
 * @since 2017-09-07 19:03
 */
public class CameraUtils {

    /**
     * 获取Camera支持的预览Size
     *
     * @return
     */
    public static Size getSupportPreviewSize(Camera camera, int minWidth) {
        List<Size> previewSize = camera.getParameters().getSupportedPreviewSizes();
        Collections.sort(previewSize, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                if (o1.width == o2.width) {
                    return 0;
                } else if (o1.width > o2.width) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        for (Size size : previewSize) {
            if (size.width >= minWidth) {
                return size;
            }
        }

        return previewSize.get(0);
    }

    /**
     * 获取Camera支持的图片size
     *
     * @param camera
     * @param minWidth
     * @return
     */
    public static Size getSupportPicSize(Camera camera, int minWidth) {
        List<Size> previewSize = camera.getParameters().getSupportedPictureSizes();
        Collections.sort(previewSize, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                if (o1.width == o2.width) {
                    return 0;
                } else if (o1.width > o2.width) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        for (Size size : previewSize) {
            if (size.width >= minWidth) {
                return size;
            }
        }

        return previewSize.get(0);
    }


    /**
     * 设置预览方向
     *
     * @param context
     * @param cameraId
     * @param camera
     */
    public static void setCameraDisplayOrientation(Context context, int cameraId, android.hardware.Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        //这里要确保是Activity，WindowManager属于Activity
        int rotation = ((Activity) context).getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * 返回支持的对焦模式
     *
     * @param camera
     * @return
     */
    public static String getSupportFocusMode(Camera camera) {
        List<String> supports = camera.getParameters().getSupportedFocusModes();
        if (supports.contains("continuous-video")) {
            return Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
        }
        return camera.getParameters().getSupportedFocusModes().get(0);
    }

    /**
     * 判断宽高比是否相同
     *
     * @param s
     * @param rate
     * @return
     */
    public static boolean equalRate(Size s, float rate) {
        float r = (float) s.width / s.height;
        if (Math.abs(r - rate) <= 0.03) {
            return true;
        } else {
            return false;
        }
    }
}
