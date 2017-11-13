package com.lx.multimedialearn.openglstudy.animation.cameraetc;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.CameraUtils;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.ScreenUtils;

import java.io.IOException;

/**
 * 加载了动画，并结合相机使用
 * 1. 初始化 相机预览
 * 2. 结合相机预览界面和动画的界面
 * 3. 使用两个渲染器，先画相机，再画动画
 */
public class AnimOnCameraActivity extends AppCompatActivity {
    private GLSurfaceView mGLSurfaceView;
    private AnimOnCameraRender mRender;
    private SurfaceTexture mSurfaceTexture; //使用SurfaceTexture承载camera回调数据，渲染到Gl上
    private Camera mCamera;
    private Camera.Parameters mParameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anim_on_camera);
        initCamreaParameters();
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_anim_camera);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mSurfaceTexture = new SurfaceTexture(GlUtil.createTextureID());
        mRender = new AnimOnCameraRender(this, mSurfaceTexture, mGLSurfaceView, GlUtil.createTextureID());
        mGLSurfaceView.setRenderer(mRender);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mCamera.stopPreview();
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mGLSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRender.replay();
            }
        });
        mCamera.startPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }

    private void initCamreaParameters() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); //这里可以进行前后置摄像头
        mParameters = mCamera.getParameters();
        Camera.Size supportPreviewSize = CameraUtils.getSupportPreviewSize(mCamera, ScreenUtils.getScreenWidth(this) / 2 - 50);
        Camera.Size supportPictureSize = CameraUtils.getSupportPicSize(mCamera, ScreenUtils.getScreenWidth(this) / 2 - 50);
        mParameters.setPreviewSize(supportPreviewSize.width, supportPreviewSize.height);
        mParameters.setPictureSize(supportPictureSize.width, supportPictureSize.height);
        mParameters.setPreviewFormat(ImageFormat.NV21);
        mParameters.setPictureFormat(ImageFormat.JPEG);
        mParameters.setFocusMode(CameraUtils.getSupportFocusMode(mCamera)); //对焦模式需要优化
        mCamera.setParameters(mParameters);

        CameraUtils.setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_FRONT, mCamera);
        int bitsPerPixel = ImageFormat.getBitsPerPixel(mParameters.getPreviewFormat()); //这里需要搞清楚WithBuffer和直接callback的区别
        final byte[] buffers = new byte[supportPreviewSize.width * supportPreviewSize.height * bitsPerPixel / 8]; //官方建议这么设置
        mCamera.addCallbackBuffer(buffers);
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() { //设置回调几个方法的区别：http://blog.csdn.net/lb377463323/article/details/53338045
            @Override
            public void onPreviewFrame(final byte[] data, final Camera camera) {
                mCamera.addCallbackBuffer(buffers);//这里能够接收到在预览界面上的数据，NV21格式即yuv420sp
            }
        });
    }
}
