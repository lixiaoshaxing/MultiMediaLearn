package com.lx.multimedialearn.camerastudy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.camerastudy.render.CameraRender;
import com.lx.multimedialearn.utils.CameraUtils;
import com.lx.multimedialearn.utils.FileUtils;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.ScreenUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * Task2：在Android平台使用Camera API进行视频的采集:分别使用SurfaceView、TextureView、GLSurfaceView来预览Camera+数据，
 * 取到NV21的数据回调, 总结Android图形图像架构
 * 1. OnCreate()初始化GLSurfaceView（设置Render）
 * 2. OnResume()初始化摄像头，并设置SurfaceTexture获取Camera数据回调
 * 3. startPreviewOn******() 进行预览，使用Handler3秒后takePhotoOn******()进行拍照，并显示在ImageView上
 * 4. 共享按钮：SurfaceTexture获取Camera数据后，在回调中把数据发送给SurfaceView，TextureView进行展示，并在GLSurfaceView上展示，
 * 5. 双GLSurfaceView，学习摄像头预览中同时预览所有滤镜的样式
 */
public class CameraUseActivity extends AppCompatActivity implements View.OnClickListener {

    private SurfaceView mSurfaceView; //使用SurfaceView预览
    private TextureView mTextureView; //使用TextureView预览
    private GLSurfaceView mGlSurfaceView; //使用GlSurfaceView预览
    private Button mBtnSurface; //切换到SurfaceView展示
    private Button mBtnTexture; //切换
    private Button mBtnGLSurface; //切换
    private ImageView mImgPhoto; //拍照后预览图片
    private Button mBtnShare; //共享摄像头，同时预览
    private Button mBtnDoubleGLSurface; //双GLSurfaceView预览
    private Button mBtnPhoto; //点击拍照，三种view拍照方式一样

    private Camera mCamera;
    private Camera.Parameters mParameters;
    private SurfaceTexture mSurfaceTexture;

    private CameraRender mRender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_use);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera_preview);
        mTextureView = (TextureView) findViewById(R.id.textureview_camera_preview);
        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_camera_preview);
        mBtnSurface = (Button) findViewById(R.id.btn_camera_surface);
        mBtnTexture = (Button) findViewById(R.id.btn_camera_texture);
        mBtnGLSurface = (Button) findViewById(R.id.btn_camera_glsurface);
        mImgPhoto = (ImageView) findViewById(R.id.img_camera_photo);
        mBtnShare = (Button) findViewById(R.id.btn_camera_double_share);
        mBtnDoubleGLSurface = (Button) findViewById(R.id.btn_camera_double_glsurface);
        mBtnPhoto = (Button) findViewById(R.id.btn_camera_take_photo);
        initGLSurfaceView(); //GLSurfaceView必须设置Render，防止crash

        mBtnSurface.setOnClickListener(this);
        mBtnSurface.setOnClickListener(this);
        mBtnTexture.setOnClickListener(this);
        mBtnGLSurface.setOnClickListener(this);
        mBtnDoubleGLSurface.setOnClickListener(this);
        mBtnShare.setOnClickListener(this);
        mBtnPhoto.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initCameraParameters(); //需要重新打开摄像头
        if (mGlSurfaceView != null) {
            mGlSurfaceView.onResume();
        }
        startPreviewOnGlSurfaceView();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_camera_surface:
                startPreviewOnSurfaceView();  //使用SurfaceView进行预览
                break;
            case R.id.btn_camera_texture:
                startPreviewOnTextureView(); //使用TextureView进行预览
                break;
            case R.id.btn_camera_glsurface:
                startPreviewOnGlSurfaceView(); //使用GlSurfaceView进行预览
                break;
            case R.id.btn_camera_double_glsurface:
                Intent intent = new Intent(CameraUseActivity.this, DoubleGLSurfaceViewActivity.class);
                CameraUseActivity.this.startActivity(intent);
                break;
            case R.id.btn_camera_double_share:
                Intent intent2 = new Intent(CameraUseActivity.this, CameraShareActivity.class);
                CameraUseActivity.this.startActivity(intent2);
                break;
            case R.id.btn_camera_take_photo:
                takePhotoOnView();
                break;
        }

    }

    /**
     * GLSurfaceView展示后需要设置Render等其它参数，这里先绑定
     */
    private void initGLSurfaceView() {
        mGlSurfaceView.setEGLContextClientVersion(2);
        mSurfaceTexture = new SurfaceTexture(GlUtil.createTextureID());//（1)创建SurfaceTexture获取Camera数据，并标定一个id
        mSurfaceTexture.detachFromGLContext(); //创建SurfaceTexture后必须立刻detach OpenGL的上下文，在CameraRender中的onDrawFrame前再进行关联，这样能够共享这个SurfaceTexture
        mRender = new CameraRender(this, mSurfaceTexture, GlUtil.createTextureID());//绑定id，startPreviewOnGlSurfaceView()时进行预览，向SurfaceTexture中填充数据
        mGlSurfaceView.setRenderer(mRender);//(2)把数据提供给Render，使用Render在SurfaceView上绘画预览图像
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY); //可以使用dirty进行手动触发更新界面
    }

    /**
     * 打开camera，设置camera的相关参数，在OnResume时进行设置，如果打开另外申请摄像头的Activity，回来后该摄像头需要重新设置才能生效
     */
    private void initCameraParameters() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); //这里可以进行前后置摄像头
        mParameters = mCamera.getParameters();
        Camera.Size supportPreviewSize = CameraUtils.getSupportPreviewSize(mCamera, ScreenUtils.getScreenWidth(CameraUseActivity.this) / 2 - 50);
        Camera.Size supportPictureSize = CameraUtils.getSupportPicSize(mCamera, ScreenUtils.getScreenWidth(CameraUseActivity.this) / 2 - 50);
        mParameters.setPreviewSize(supportPreviewSize.width, supportPreviewSize.height);
        mParameters.setPictureSize(supportPictureSize.width, supportPictureSize.height);
        mParameters.setPreviewFormat(ImageFormat.NV21);
        mParameters.setPictureFormat(ImageFormat.JPEG);
        mParameters.setFocusMode(CameraUtils.getSupportFocusMode(mCamera)); //对焦模式需要优化
        mCamera.setParameters(mParameters);

        CameraUtils.setCameraDisplayOrientation(CameraUseActivity.this, Camera.CameraInfo.CAMERA_FACING_FRONT, mCamera);
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

    /**
     * 在SurfaceView上预览
     * （1）Camera同时只能打开一个
     * （2）从这一个Camera中拿到的数据，放入queue
     * （3）把数据转换为jpg等bitmap格式，在其它SurfaceView上画出来
     */
    private void startPreviewOnSurfaceView() {
        SurfaceHolder holder = mSurfaceView.getHolder();
        mCamera.stopPreview();
        try {
            mCamera.setPreviewDisplay(mSurfaceView.getHolder());
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //！！！如果不是手动切换，在view创建后，即Activity出来后，可触发这里，可以在这里设置预览
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });
    }

    /**
     * 在TextureView上预览
     */
    private void startPreviewOnTextureView() {
        mCamera.stopPreview();
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        try {
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                //！！！如果不是手动切换，在view创建后，即Activity出来后，可触发这里，可以在这里设置预览
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    /**
     * 在GlSurfaceView上预览
     */
    private void startPreviewOnGlSurfaceView() {
        mCamera.stopPreview();
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    /**
     * 预览后拍照
     */
    private void takePhotoOnView() {
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                //对图片进行处理
                String fileName = UUID.randomUUID().toString() + ".jpeg"; //把图片存入外部存储器，然后读取后展示在ImageView上，学习用
                FileUtils.convertBitmap2File(CameraUseActivity.this, fileName, data);

                String path = getFileStreamPath(fileName).getAbsolutePath();//读取拍摄的图片，在ImageView中显示
                Bitmap bm = FileUtils.convertFile2Bitmap(CameraUseActivity.this, path, mImgPhoto.getWidth(), mImgPhoto.getHeight());
                BitmapDrawable drawable = new BitmapDrawable(getResources(), bm);
                mImgPhoto.setImageDrawable(drawable);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGlSurfaceView != null) {
            mGlSurfaceView.onPause();
        }
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }
}
