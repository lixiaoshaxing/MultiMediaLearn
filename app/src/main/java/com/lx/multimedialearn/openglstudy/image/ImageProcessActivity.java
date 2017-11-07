package com.lx.multimedialearn.openglstudy.image;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.lx.fit7.Fit7Utils;
import com.lx.multimedialearn.R;
import com.lx.multimedialearn.openglstudy.image.render.ImageProcessRender;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.ToastUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 使用OpenGL对图像进行处理
 * 1. 画出bitmap
 * 2. 对bitmap进行处理
 */
public class ImageProcessActivity extends AppCompatActivity {

    private GLSurfaceView mGlSurfaceView;
    private boolean mRenderSet = false;
    private boolean isHalf = false; //是否处理一半
    private ImageProcessRender mRender;
    private String mImagePath; //选择图片地址

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_process);
        if (!GlUtil.checkGLEsVersion_2(this)) {
            ToastUtils.show(this, "不支持OpenGL 2.0");
            finish();
            return;
        }
        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_image_display);
        mGlSurfaceView.setEGLContextClientVersion(2);
        mRender = new ImageProcessRender(this);
        mGlSurfaceView.setRenderer(mRender);
        mRender.setInfo(0, new float[]{0.0f, 0.0f, 0.0f});
        mRenderSet = true;
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); //设置渲染模式
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_image_choose:
                //选择图片
                new AlertDialog.Builder(this).setTitle("选择图片").setItems(new String[]{"拍照", "相册"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            takePicture(); //拍照
                        } else {
                            choosePicture(); //从相册中取一张图片，代码不重要
                        }
                    }
                }).create().show();
                break;
            case R.id.menu_image_config:
                isHalf = !isHalf;
                if (isHalf) {
                    item.setTitle("处理一半");
                } else {
                    item.setTitle("全部处理");
                }
                mRender.setIsHalf(isHalf);
                break;
            case R.id.menu_image_origin:
                mRender.setInfo(0, new float[]{0.0f, 0.0f, 0.0f});
                break;
            case R.id.menu_image_gray:
                mRender.setInfo(1, new float[]{0.299f, 0.587f, 0.114f});
                break;
            case R.id.menu_image_cool:
                mRender.setInfo(2, new float[]{0.0f, 0.0f, 0.1f});
                break;
            case R.id.menu_image_warm:
                mRender.setInfo(2, new float[]{0.1f, 0.1f, 0.0f});
                break;
            case R.id.menu_image_blur:
                mRender.setInfo(3, new float[]{0.006f, 0.004f, 0.002f});
                break;
            case R.id.menu_image_magn:
                mRender.setInfo(4, new float[]{0.0f, 0.0f, 0.4f});
                break;
        }
        //设置渲染器
        mGlSurfaceView.requestRender();
        return super.onOptionsItemSelected(item);
    }

    /**
     * 拍照，fileProvider的适配
     */
    private void takePicture() {
        //学习hongyang 7.0处理FileProvider适配(http://blog.csdn.net/lmj623565791/article/details/72859156)
        String fileName = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA).format(new Date()) + ".png";
        File file = new File(Environment.getExternalStorageDirectory() + "/photos/", fileName);
        mImagePath = file.getAbsolutePath();//render使用
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdir();
        }
        Uri fileUri = Fit7Utils.getUriForFile(this, file);
        // grantUriPermission(getPackageName(), fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        // grantUriPermission(getPackageName(), fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        startActivityForResult(cameraIntent, 101);
    }

    /**
     * 从相册中选择照片
     */
    private void choosePicture() {
        Intent albumIntent = new Intent(Intent.ACTION_PICK);
        albumIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(albumIntent, 102);
    }

    private String parseFilePath(Uri uri) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();
        return picturePath;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //获取图片地址后，进行更新
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case 101: //拍照
                break;
            case 102: //选择照片
                Uri uri = data.getData();
                mImagePath = parseFilePath(uri);
                break;
        }
        if (TextUtils.isEmpty(mImagePath)) {
            return;
        }
        mRender.setImagePath(mImagePath);
        mGlSurfaceView.requestRender();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRenderSet) {
            mGlSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRenderSet) {
            mGlSurfaceView.onPause();
        }
    }
}
