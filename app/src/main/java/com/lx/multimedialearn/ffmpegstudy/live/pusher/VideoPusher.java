package com.lx.multimedialearn.ffmpegstudy.live.pusher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.lx.multimedialearn.ffmpegstudy.FFmpegUtils;
import com.lx.multimedialearn.ffmpegstudy.live.params.VideoParam;

import java.io.IOException;

/**
 * 视频推送
 *
 * @author lixiao
 * @since 2017-08-09 18:24
 */
public class VideoPusher extends Pusher implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private VideoParam videoParam;
    private SurfaceView surfaceView;
    private Camera camera;
    private byte[] buffers;
    private boolean isPushing = false;
    private Context mContext;

    public VideoPusher(Context context, VideoParam videoParam, SurfaceView surfaceView) {
        this.videoParam = videoParam;
        this.mContext = context;
        this.surfaceView = surfaceView;
        //初始化相机 480, 320
        this.surfaceView.getHolder().addCallback(this);
        this.surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {

                    }
                });
            }
        });
    }

    @Override
    public void startPush() {
        FFmpegUtils.setVideoOptions(
                videoParam.getWidth(),
                videoParam.getHeight(),
                videoParam.getBitrate(),
                videoParam.getFps());
        isPushing = true;

    }

    @Override
    public void stopPush() {
        isPushing = false;
    }

    @Override
    public void release() {
        stopPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // camera.getParameters().getSupportedPreviewSizes();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (videoParam.getCameraId() == Camera.CameraInfo.CAMERA_FACING_BACK) {
            videoParam.setCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } else {
            videoParam.setCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        stopPreview();
        startPreview();
    }

    /**
     * 打开相机预览
     */
    private void startPreview() {
        try {
            camera = Camera.open(videoParam.getCameraId());
            camera.setPreviewDisplay(surfaceView.getHolder());
            Camera.Parameters params = camera.getParameters();
            params.setPreviewFormat(ImageFormat.NV21);
            params.setPictureSize(videoParam.getWidth(), videoParam.getHeight());
            params.setPreviewSize(videoParam.getWidth(), videoParam.getHeight());
            camera.setParameters(params);
            setCameraDisplayOrientation(findFrontFacingCameraID(), camera);
            //这里似乎*1越小，延迟越小？*1不行了，
            int bitsPerPixel = ImageFormat.getBitsPerPixel(params.getPreviewFormat());
            Log.i("sys.out", "每一个像素有" + bitsPerPixel + "个bits");
            buffers = new byte[videoParam.getWidth() * videoParam.getHeight() * bitsPerPixel / 8]; //这里需要字节数
            camera.addCallbackBuffer(buffers);
            camera.setPreviewCallbackWithBuffer(this);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //camera拍摄方向调整，这只是预览的是正确的，发出去的还是错误的，需要进行矩阵转换？？？？
    public void setCameraDisplayOrientation(
            int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        //这里要确保是Activity，WindowManager属于Activity
        int rotation = ((Activity) mContext).getWindowManager().getDefaultDisplay()
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
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * 查找后置摄像头id
     * //拿到后置摄像头，为什么前后置的方向都正确了？？？？
     *
     * @return
     */
    private int findFrontFacingCameraID() {
        int cameraID = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraID = i;
                break;
            }
        }
        return cameraID;
    }


    /**
     * 停止预览
     */
    private void stopPreview() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (camera != null) {
            camera.addCallbackBuffer(buffers);
        }
        //  Log.i("sys.out", "采集的长度：" + data.length + "-" + "buffer的长度：" + buffers.length);
        //第一种方法：在这里把data变为yuvImage，转换为bitmap，然后转换方向，再转回byte，传入ndk进行推流，这样
        //          会影响推流的速度吧
        // 第二种方法：在ndk中进行处理，在对图片转换x264之前，使用libyuv进行转向处理
//        //转向
//        byte[] rotatedData = new byte[data.length];
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++)
//                rotatedData[x * height + height - y - 1] = data[x + y * width];
//        }
        //      data = rotateYUV420Degree270(data, mWidth, mHeight);
        if (isPushing) {
            String output_path = Environment.getExternalStorageDirectory().getPath() + "/output_test.jpeg";
            FFmpegUtils.fireVideo(data, output_path, videoParam.getCameraId());
        }
    }


    /**
     * 顺时针旋转了90度---倒了。。。，应该是270
     *
     * @param data
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    private static byte[] rotateYUV420Degree180(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        int count = 0;
        for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
            yuv[count] = data[i];
            count++;
        }
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
                * imageHeight; i -= 2) {
            yuv[count++] = data[i - 1];
            yuv[count++] = data[i];
        }
        return yuv;
    }

    public static byte[] rotateYUV420Degree270(byte[] data, int imageWidth,
                                               int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int nWidth = 0, nHeight = 0;
        int wh = 0;
        int uvHeight = 0;
        if (imageWidth != nWidth || imageHeight != nHeight) {
            nWidth = imageWidth;
            nHeight = imageHeight;
            wh = imageWidth * imageHeight;
            uvHeight = imageHeight >> 1;// uvHeight = height / 2
        }
        // ??Y
        int k = 0;
        for (int i = 0; i < imageWidth; i++) {
            int nPos = 0;
            for (int j = 0; j < imageHeight; j++) {
                yuv[k] = data[nPos + i];
                k++;
                nPos += imageWidth;
            }
        }
        for (int i = 0; i < imageWidth; i += 2) {
            int nPos = wh;
            for (int j = 0; j < uvHeight; j++) {
                yuv[k] = data[nPos + i];
                yuv[k + 1] = data[nPos + i + 1];
                k += 2;
                nPos += imageWidth;
            }
        }
        return rotateYUV420Degree180(yuv, imageWidth, imageHeight);
    }

    public static Bitmap PicAngle(Bitmap bitmap, int angle) {
        Matrix m = new Matrix();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        m.setRotate(angle);
        Bitmap bitmaps = Bitmap.createBitmap(bitmap, 0, 0, width, height, m,
                true);
        return bitmaps;
    }
}
