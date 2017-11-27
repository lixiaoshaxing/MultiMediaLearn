package com.lx.multimedialearn.mediastudy.utils.wav;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * wav文件结构：头文件+data
 * 该类提供wav文件的数据结构，写入数据的方法：打开，关闭文件，写入头文件（基本类型转byte数组方法）， 写入数据文件
 *
 * @author lixiao
 * @since 2017-07-17 19:13
 */
public class WavFileWriter {
    private String mFilepath;
    private int mDataSize = 0; //统计全部数据的大小
    private DataOutputStream mDataOutputStream;

    /**
     * 写入wav文件
     * @param filepath 文件地址
     * @param sampleRateInHz 采样率：44100
     * @param bitsPerSample 每一个采样的位数：PCM编码是16位
     * @param channels 通道数：2
     * @return
     * @throws IOException
     */
    public boolean openFile(String filepath, int sampleRateInHz, int bitsPerSample, int channels) throws IOException {
        if (mDataOutputStream != null) {
            closeFile();
        }
        mFilepath = filepath;
        mDataSize = 0;
        mDataOutputStream = new DataOutputStream(new FileOutputStream(mFilepath));
        return writeHeader(sampleRateInHz, bitsPerSample, channels);
    }

    //关闭文件
    public boolean closeFile() throws IOException {
        boolean ret = true;
        if (mDataOutputStream != null) {
            //写入wav总文件大小，把数据部分的大小写入进去
            ret = writeDataSize();
            mDataOutputStream.close();
            mDataOutputStream = null;

        }
        return ret;
    }

    //写入头文件(单独处理最终文件的总大小，基本数据类型转为byte数组传入)
    private boolean writeHeader(int sampleRateInHz, int bitsPerSample, int channels) {
        if (mDataOutputStream == null) {
            return false;
        }
        WavFileHeader header = new WavFileHeader(sampleRateInHz, bitsPerSample, channels);
        try {
            mDataOutputStream.writeBytes(header.mChunkID);
            //关闭文件时需要追加设置这里的大小
            mDataOutputStream.write(intToByteArray((int) header.mChunkSize), 0, 4);
            mDataOutputStream.writeBytes(header.mFormat);
            mDataOutputStream.writeBytes(header.mSubChunk1ID);
            mDataOutputStream.write(intToByteArray((int) header.mSubChunk1Size), 0, 4);
            mDataOutputStream.write(shortToByteArray((short) header.mAudioFormat), 0, 2);
            mDataOutputStream.write(shortToByteArray((short) header.mNumChannel), 0, 2);
            mDataOutputStream.write(intToByteArray((int) header.mSampleRate), 0, 4);
            mDataOutputStream.write(intToByteArray((int) header.mByteRate), 0, 4);
            mDataOutputStream.write(shortToByteArray((short) header.mBlockAlign), 0, 2);
            mDataOutputStream.write(shortToByteArray((short) header.mBitsPerSample), 0, 2);
            mDataOutputStream.writeBytes(header.mSubChunk2ID);
            mDataOutputStream.write(intToByteArray((int) header.mSubChunk2Size), 0, 4);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean writeDataSize() {

        if (mDataOutputStream == null) {
            return false;
        }

        try {
            RandomAccessFile wavFile = new RandomAccessFile(mFilepath, "rw");
            wavFile.seek(WavFileHeader.WAV_CHUNKSIZE_OFFSET);
            wavFile.write(intToByteArray((int) (mDataSize + WavFileHeader.WAV_CHUNKSIZE_EXCLUDE_DATA)), 0, 4);
            wavFile.seek(WavFileHeader.WAV_SUB_CHUNKSIZE2_OFFSET);
            wavFile.write(intToByteArray((int) (mDataSize)), 0, 4);
            wavFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    //写入数据文件
    public boolean writeData(byte[] buffer, int offset, int count) {
        if (mDataOutputStream == null) {
            return false;
        }
        try {
            mDataOutputStream.write(buffer, offset, count);
            mDataSize += buffer.length;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //基本数据类型转为byte数组
    private byte[] intToByteArray(int data) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(data).array();
    }

    private byte[] shortToByteArray(short data) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(data).array();
    }

}
