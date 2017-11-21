package com.lx.multimedialearn.camerastudy;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.camerastudy.render.CameraRender;
import com.lx.multimedialearn.camerastudy.render.CameraRender2;
import com.lx.multimedialearn.utils.CameraUtils;
import com.lx.multimedialearn.utils.ScreenUtils;

import java.io.IOException;

import static com.lx.multimedialearn.utils.GlUtil.createCameraTextureID;

/**
 * 一个Activity上四个GLSurfaceView同时预览摄像头返回的图像
 * blog：http://blog.sina.com.cn/s/blog_68dc52970102wjhy.html
 */
public class DoubleGLSurfaceViewActivity extends AppCompatActivity {

    private GLSurfaceView mGLOne;
    private GLSurfaceView mGLTwo;
    private GLSurfaceView mGLThree;
    private GLSurfaceView mGLFour;
    private CameraRender mRenderOne;
    private CameraRender2 mRenderTwo;
    private CameraRender2 mRenderThree;
    private CameraRender mRenderFour; //openglStudy-camera_filter_watermark下的render，模拟小米相机同时预览多个滤镜
    private Button mBtnSwitch;
    private Button mBtnShare;
    boolean isOne = true;

    private Camera mCamera;
    private Camera.Parameters mParameters;
    private SurfaceTexture mSurfaceTexture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_double_glsurface_view);
        mGLOne = (GLSurfaceView) findViewById(R.id.glsurface_double_one);
        mGLTwo = (GLSurfaceView) findViewById(R.id.glsurface_double_two);
        mGLThree = (GLSurfaceView) findViewById(R.id.glsurface_double_three);
        mGLFour = (GLSurfaceView) findViewById(R.id.glsurface_double_four);
        mBtnSwitch = (Button) findViewById(R.id.btn_double_switch);
        mBtnShare = (Button) findViewById(R.id.btn_double_share);
        initCameraParameters(); //初始化摄像头

        /************************以下初始化两个GL，共用一个SurfaceTexture**************************************/
        mGLOne.setEGLContextClientVersion(2);
        mRenderOne = new CameraRender(this, mSurfaceTexture, mTextureID);
        mGLOne.setRenderer(mRenderOne);
        mGLOne.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);//设置刷新模式

        mGLTwo.setEGLContextClientVersion(2);
        mRenderTwo = new CameraRender2(this, mSurfaceTexture, mTextureID);
        mGLTwo.setRenderer(mRenderTwo);
        mGLTwo.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY); //设置刷新模式

        mGLThree.setEGLContextClientVersion(2);
        mRenderThree = new CameraRender2(this, mSurfaceTexture, mTextureID);
        mGLThree.setRenderer(mRenderThree);
        mGLThree.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY); //设置刷新模式

        mGLFour.setEGLContextClientVersion(2);
        mRenderFour = new CameraRender(this, mSurfaceTexture, mTextureID);
        mGLFour.setRenderer(mRenderFour);
        mGLFour.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY); //设置刷新模式


        mBtnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOne) {
                    mGLOne.onResume();
                    mGLTwo.onPause();
                    mGLThree.onPause();
                    mGLFour.onResume();
                    isOne = false;
                } else {
                    mGLOne.onPause();
                    mGLTwo.onResume();
                    mGLThree.onResume();
                    mGLFour.onPause();
                    isOne = true;
                }
            }
        });

        mBtnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGLOne.onResume();
                mGLTwo.onResume();
                mGLThree.onResume();
                mGLFour.onResume();
            }
        });
    }

    private int mTextureID;

    /**
     * 打开camera
     */
    private void initCameraParameters() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); //这里可以进行前后置摄像头
        mParameters = mCamera.getParameters();
        Camera.Size supportPreviewSize = CameraUtils.getSupportPreviewSize(mCamera, ScreenUtils.getScreenWidth(DoubleGLSurfaceViewActivity.this) / 2 - 50);
        Camera.Size supportPictureSize = CameraUtils.getSupportPicSize(mCamera, ScreenUtils.getScreenWidth(DoubleGLSurfaceViewActivity.this) / 2 - 50);
        mParameters.setPreviewSize(supportPreviewSize.width, supportPreviewSize.height);
        mParameters.setPictureSize(supportPictureSize.width, supportPictureSize.height);
        mParameters.setPreviewFormat(ImageFormat.NV21);
        mParameters.setPictureFormat(ImageFormat.JPEG);
        mParameters.setFocusMode(CameraUtils.getSupportFocusMode(mCamera)); //对焦模式需要优化
        mCamera.setParameters(mParameters);
        CameraUtils.setCameraDisplayOrientation(DoubleGLSurfaceViewActivity.this, Camera.CameraInfo.CAMERA_FACING_FRONT, mCamera);
        int bitsPerPixel = ImageFormat.getBitsPerPixel(mParameters.getPreviewFormat()); //这里需要搞清楚WithBuffer和直接callback的区别
        final byte[] buffers = new byte[supportPreviewSize.width * supportPreviewSize.height * bitsPerPixel / 8]; //官方建议这么设置
        mCamera.addCallbackBuffer(buffers);
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() { //设置回调几个方法的区别：http://blog.csdn.net/lb377463323/article/details/53338045
            @Override
            public void onPreviewFrame(final byte[] data, final Camera camera) {
                //这里能够接收到在预览界面上的数据，NV21格式即yuv420sp
                mCamera.addCallbackBuffer(buffers);
            }
        });
        mTextureID = createCameraTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.detachFromGLContext(); //创建完成后立刻detachOpenGL世界的上下文，在CameraRender中进行绘画时，根据ID attachToGLContext，attach到当前GLSurfaceView，进行绘画，注意线程同步
        mCamera.stopPreview();
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera != null) {
            mCamera.startPreview();
        }
        if (mGLOne != null) {
            mGLOne.onResume();
        }
        if (mGLTwo != null) {
            mGLTwo.onResume();
        }
        if (mGLThree != null) {
            mGLThree.onResume();
        }
        if (mGLFour != null) {
            mGLFour.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGLOne != null) {
            mGLOne.onPause();
        }
        if (mGLTwo != null) {
            mGLTwo.onPause();
        }
        if (mGLThree != null) {
            mGLThree.onPause();
        }
        if (mGLFour != null) {
            mGLFour.onPause();
        }
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCamera != null) {
            mCamera.release();
        }
    }
}
