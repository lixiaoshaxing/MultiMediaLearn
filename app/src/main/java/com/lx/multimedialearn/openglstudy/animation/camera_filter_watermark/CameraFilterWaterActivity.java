package com.lx.multimedialearn.openglstudy.animation.camera_filter_watermark;

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
 * 相机+滤镜1+滤镜2+...+水印
 * 思考1：（不可行）把滤镜加在相机的glsl中，如果同时需要多个滤镜，就没法处理了
 * 思考2:（不可行）相机,水印,滤镜分三层绘画，由于滤镜
 * 思考3：(接近可行)使用FBO，在缓冲区中绘制，下一步拿上一步的Texture处理，并输入最后的纹理，在GLSurfaceView上画出处理后的结果
 * 这里就要封装了，如何把滤镜封装出来
 * 使用FBO：创建Framebuffer，创建TextureBuffer，RenderBuffer，绑定到FrameBuffer，之后渲染都是在Framebuffer上
 * 纹理是在TextureBuffer中，深度检测，模板是在RenderBuffer中，最后在屏幕上把TextureBuffer作为普通的一帧渲染出来
 * 思考4：（可行）相机使用Fbo，创建一个输出，输出传给滤镜，滤镜使用fbo，多重滤镜，绑定两个textureid，轮流换，最后有一个输出，加水印，输出，画出来
 */
public class CameraFilterWaterActivity extends AppCompatActivity {
    private GLSurfaceView mGlSurfaceView;
    private CameraFilterWaterRender mRender;
    private SurfaceTexture mSurfaceTexture; //使用SurfaceTexture承载camera回调数据，渲染到Gl上
    private Camera mCamera;
    private Camera.Parameters mParameters;
    private int mTextureID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_filter_water);
        if (!GlUtil.checkGLEsVersion_2(this)) {
            ToastUtils.show(this, "不支持gl 2.0！");
        }
        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_filter_water_player);
        mTextureID = GlUtil.createCameraTextureID();
        initCamreaParameters();
        mSurfaceTexture = new SurfaceTexture(mTextureID); //surfacetexture用来承载相机的预览数据，绑定到TextureID上，GLSurfaceView拿到ID进行绘画
        mRender = new CameraFilterWaterRender(this, mSurfaceTexture, mTextureID);
        mGlSurfaceView.setEGLContextClientVersion(2);
        mGlSurfaceView.setRenderer(mRender);
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
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
        mGlSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGlSurfaceView.onResume();
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
