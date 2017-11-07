package com.lx.multimedialearn.openglstudy.animation.loadetc;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.ETC1;
import android.opengl.ETC1Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 从pkm的zip压缩文件中读取Texture
 *
 * @author lixiao
 * @since 2017-11-06 23:38
 */
public class ZipPkmReader {
    private String path;
    private ZipInputStream mZipStream;
    private AssetManager mManager;
    private ZipEntry mZipEntry;
    private ByteBuffer headerBuffer;

    public ZipPkmReader(Context context) {
        this(context.getAssets());
    }

    public ZipPkmReader(AssetManager manager) {
        this.mManager = manager;
    }

    /**
     * 设置zip path
     *
     * @param path
     */
    public void setZipPath(String path) {
        this.path = path;
    }

    public boolean open() {
        if (path == null) return false;
        try {
            if (path.startsWith("assets/")) {
                InputStream s = mManager.open(path.substring(7));
                mZipStream = new ZipInputStream(s);
            } else {
                File f = new File(path); //从外部文件系统中读取
                mZipStream = new ZipInputStream(new FileInputStream(path));
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void close() {
        if (mZipStream != null) {
            try {
                mZipStream.closeEntry();
                mZipStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (headerBuffer != null) {
                headerBuffer.clear();
                headerBuffer = null;
            }
        }
    }

    private boolean hasElements() {
        try {
            if (mZipStream != null) {
                mZipEntry = mZipStream.getNextEntry();
                if (mZipEntry != null) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public InputStream getNextStream() {
        if (hasElements()) {
            return mZipStream;
        }
        return null;
    }

    public ETC1Util.ETC1Texture getNextTexture() {
        if (hasElements()) {
            try {
                ETC1Util.ETC1Texture e = createTexture(mZipStream);
                return e;
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 从inputstream中读取texture，ETC1Util提供的有，这里做了一定修改
     *
     * @param input
     * @return
     * @throws IOException
     */
    private ETC1Util.ETC1Texture createTexture(InputStream input) throws IOException {
        int width = 0;
        int height = 0;
        byte[] ioBuffer = new byte[4096];
        {
            if (input.read(ioBuffer, 0, ETC1.ETC_PKM_HEADER_SIZE) != ETC1.ETC_PKM_HEADER_SIZE) {
                throw new IOException("Unable to read PKM file header.");
            }
            if (headerBuffer == null) {
                headerBuffer = ByteBuffer.allocateDirect(ETC1.ETC_PKM_HEADER_SIZE)
                        .order(ByteOrder.nativeOrder());
            }
            headerBuffer.put(ioBuffer, 0, ETC1.ETC_PKM_HEADER_SIZE).position(0);
            if (!ETC1.isValid(headerBuffer)) {
                throw new IOException("Not a PKM file.");
            }
            width = ETC1.getWidth(headerBuffer);
            height = ETC1.getHeight(headerBuffer);
        }
        int encodedSize = ETC1.getEncodedDataSize(width, height);
        ByteBuffer dataBuffer = ByteBuffer.allocateDirect(encodedSize).order(ByteOrder.nativeOrder());
        int len;
        while ((len = input.read(ioBuffer)) != -1) {
            dataBuffer.put(ioBuffer, 0, len);
        }
        dataBuffer.position(0);
        return new ETC1Util.ETC1Texture(width, height, dataBuffer);
    }

}
