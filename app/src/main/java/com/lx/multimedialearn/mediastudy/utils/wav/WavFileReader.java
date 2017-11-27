package com.lx.multimedialearn.mediastudy.utils.wav;

import android.util.Log;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * wav文件解析类
 * 1. 读取头文件，设置头文件，打开文件时读取到头文件的结束部分
 * 2. 读取data部分，并使用线程传到AudioTracker中
 *
 * @author lixiao
 * @since 2017-07-18 10:17
 */
public class WavFileReader {
    private static final String TAG = "sys.out";
    private DataInputStream mDataInputStream; //从wav文件中读入
    private WavFileHeader mWavFileHeader;

    public boolean openFile(String filepath) throws IOException {
        if (mDataInputStream != null) {
            closeFile();
        }
        //创建数据流，打开数据流，读取头部
        mDataInputStream = new DataInputStream(new FileInputStream(filepath));
        return readHeader();
    }

    public void closeFile() throws IOException {
        if (mDataInputStream != null) {
            mDataInputStream.close();
            mDataInputStream = null;
        }
    }

    public int readData(byte[] buffer, int offset, int count) {
        if (mDataInputStream == null || mWavFileHeader == null) {
            return -1;
        }
        try {
            //从文件中把数据暂时存放在buffer，如果读取nbytes的大小为0或者-1表示读取完毕
            int nbytes = mDataInputStream.read(buffer, offset, count);
            if (nbytes == -1) {
                return 0;
            }
            return nbytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public WavFileHeader getmWavFileHeader() {
        return mWavFileHeader;
    }

    /**
     * 读取头文件，按照头文件的原始数据格式，读取后进行赋值
     *
     * @return
     */
    private boolean readHeader() {
        if (mDataInputStream == null) {
            return false;
        }
        WavFileHeader header = new WavFileHeader();
        byte[] intValue = new byte[4];
        byte[] shortValue = new byte[2];

        try {
            header.mChunkID = "" + (char) mDataInputStream.readByte() + (char) mDataInputStream.readByte() + (char) mDataInputStream.readByte() + (char) mDataInputStream.readByte();
            Log.d(TAG, "Read file chunkID:" + header.mChunkID);

            mDataInputStream.read(intValue);
            header.mChunkSize = byteArrayToInt(intValue);
            Log.d(TAG, "Read file chunkSize:" + header.mChunkSize);

            header.mFormat = "" + (char) mDataInputStream.readByte() + (char) mDataInputStream.readByte() + (char) mDataInputStream.readByte() + (char) mDataInputStream.readByte();
            Log.d(TAG, "Read file format:" + header.mFormat);

            header.mSubChunk1ID = "" + (char) mDataInputStream.readByte() + (char) mDataInputStream.readByte() + (char) mDataInputStream.readByte() + (char) mDataInputStream.readByte();
            Log.d(TAG, "Read fmt chunkID:" + header.mSubChunk1ID);

            mDataInputStream.read(intValue);
            header.mSubChunk1Size = byteArrayToInt(intValue);
            Log.d(TAG, "Read fmt chunkSize:" + header.mSubChunk1Size);

            mDataInputStream.read(shortValue);
            header.mAudioFormat = byteArrayToShort(shortValue);
            Log.d(TAG, "Read audioFormat:" + header.mAudioFormat);

            mDataInputStream.read(shortValue);
            header.mNumChannel = byteArrayToShort(shortValue);
            Log.d(TAG, "Read channel number:" + header.mNumChannel);

            mDataInputStream.read(intValue);
            header.mSampleRate = byteArrayToInt(intValue);
            Log.d(TAG, "Read samplerate:" + header.mSampleRate);

            mDataInputStream.read(intValue);
            header.mByteRate = byteArrayToInt(intValue);
            Log.d(TAG, "Read byterate:" + header.mByteRate);

            mDataInputStream.read(shortValue);
            header.mBlockAlign = byteArrayToShort(shortValue);
            Log.d(TAG, "Read blockalign:" + header.mBlockAlign);

            mDataInputStream.read(shortValue);
            header.mBitsPerSample = byteArrayToShort(shortValue);
            Log.d(TAG, "Read bitspersample:" + header.mBitsPerSample);

            header.mSubChunk2ID = "" + (char) mDataInputStream.readByte() + (char) mDataInputStream.readByte() + (char) mDataInputStream.readByte() + (char) mDataInputStream.readByte();
            Log.d(TAG, "Read data chunkID:" + header.mSubChunk2ID);

            mDataInputStream.read(intValue);
            header.mSubChunk2Size = byteArrayToInt(intValue);
            Log.d(TAG, "Read data chunkSize:" + header.mSubChunk2Size);

            Log.d(TAG, "Read wav file success !");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        mWavFileHeader = header;

        return true;
    }

    //数组转换为短整型
    private static short byteArrayToShort(byte[] b) {
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    private static int byteArrayToInt(byte[] b) {
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
}
