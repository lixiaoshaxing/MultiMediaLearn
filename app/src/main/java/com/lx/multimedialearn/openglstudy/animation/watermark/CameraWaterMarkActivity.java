package com.lx.multimedialearn.openglstudy.animation.watermark;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.CameraUtils;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.ScreenUtils;
import com.lx.multimedialearn.utils.ToastUtils;

import java.io.IOException;

/**
 * 在相机预览界面上画水印
 * 1. 进行相机预览
 * 2. 预览界面上加载图片，进行矩阵变换，改变水印的位置
 * 用到的包：watermark，utils
 */
public class CameraWaterMarkActivity extends AppCompatActivity {

    private GLSurfaceView mGLSurfaceView;
    private CameraWaterMarkRender mRender;
    private SurfaceTexture mSurfaceTexture; //使用SurfaceTexture承载camera回调数据，渲染到Gl上
    private Camera mCamera;
    private Camera.Parameters mParameters;
    private int mTextureID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_water_mark);
        if (!GlUtil.checkGLEsVersion_2(this)) {
            ToastUtils.show(this, "不支持gl 2.0！");
        }
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_camera_water_mark);
        mTextureID = GlUtil.createCameraTextureID();
        initCamreaParameters();
        mSurfaceTexture = new SurfaceTexture(mTextureID); //surfacetexture用来承载相机的预览数据，绑定到TextureID上，GLSurfaceView拿到ID进行绘画
        mRender = new CameraWaterMarkRender(this, mSurfaceTexture, mTextureID);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(mRender);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
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
