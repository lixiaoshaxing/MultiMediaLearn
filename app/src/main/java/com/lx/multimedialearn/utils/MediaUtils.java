package com.lx.multimedialearn.utils;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * 多媒体工具类
 * 1. PCM转WAV格式（注意大小端）
 *
 * @author lixiao
 * @since 2017-11-27 15:44
 */
public class MediaUtils {

    public static void addADTSToPacket(byte[] buffer, int length) {
        int profile = 2;  //AAC LC
        int freqIdx = 4;  //44.1KHz
        int chanCfg = 2;  //CPE
        buffer[0] = (byte) 0xFF;
        buffer[1] = (byte) 0xF9;
        buffer[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        buffer[3] = (byte) (((chanCfg & 3) << 6) + (length >> 11));
        buffer[4] = (byte) ((length & 0x7FF) >> 3);
        buffer[5] = (byte) (((length & 7) << 5) + 0x1F);
        buffer[6] = (byte) 0xFC;
    }

    /**
     * pcm转wav
     *
     * @param sampleRateInHz 采样率：44100，16000等
     * @param bitsPerSample  每个采样的位数：16 （不要传入AudioFormat.ENCODING_PCM_16BIT）
     * @param channels       通道数：2
     * @param bufferSize     录入是buffersize大小，没有用录制，传入0
     * @param inputPath      输入文件
     * @param outPath        输出文件
     */
    public static void pcm2wav(int sampleRateInHz, int bitsPerSample, int channels, int bufferSize, String inputPath, String outPath) {
        FileInputStream in = null;
        DataOutputStream out = null;
        long totalSize = 0;
        try {
            in = new FileInputStream(inputPath);
            out = new DataOutputStream(new FileOutputStream(outPath));
            writeHeader(out, sampleRateInHz, bitsPerSample, channels);
            if (bufferSize == 0) {
                bufferSize = 1024;
            }
            byte[] data = new byte[bufferSize];
            while (in.read(data) != -1) {
                out.write(data, 0, data.length);
                totalSize += data.length;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //关闭流，写入总大小
            if (out != null) {
                writeDataSize(out, outPath, totalSize);
                try {
                    out.close();
                    out = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void writeHeader(DataOutputStream dataOutputStream, int sampleRateInHz, int bitsPerSample, int channels) {
        if (dataOutputStream == null) {
            return;
        }
        WavFileHeader header = new WavFileHeader(sampleRateInHz, bitsPerSample, channels);
        try {
            dataOutputStream.writeBytes(header.mChunkID);
            //关闭文件时需要追加设置这里的大小
            dataOutputStream.write(intToByteArray((int) header.mChunkSize), 0, 4);
            dataOutputStream.writeBytes(header.mFormat);
            dataOutputStream.writeBytes(header.mSubChunk1ID);
            dataOutputStream.write(intToByteArray((int) header.mSubChunk1Size), 0, 4);
            dataOutputStream.write(shortToByteArray((short) header.mAudioFormat), 0, 2);
            dataOutputStream.write(shortToByteArray((short) header.mNumChannel), 0, 2);
            dataOutputStream.write(intToByteArray((int) header.mSampleRate), 0, 4);
            dataOutputStream.write(intToByteArray((int) header.mByteRate), 0, 4);
            dataOutputStream.write(shortToByteArray((short) header.mBlockAlign), 0, 2);
            dataOutputStream.write(shortToByteArray((short) header.mBitsPerSample), 0, 2);
            dataOutputStream.writeBytes(header.mSubChunk2ID);
            dataOutputStream.write(intToByteArray((int) header.mSubChunk2Size), 0, 4);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeDataSize(DataOutputStream outputStream, String filePath, long totalSize) {

        if (outputStream == null) {
            return;
        }
        try {
            RandomAccessFile wavFile = new RandomAccessFile(filePath, "rw");
            wavFile.seek(com.lx.multimedialearn.mediastudy.utils.wav.WavFileHeader.WAV_CHUNKSIZE_OFFSET);
            wavFile.write(intToByteArray((int) (totalSize + WavFileHeader.WAV_CHUNKSIZE_EXCLUDE_DATA)), 0, 4);
            wavFile.seek(com.lx.multimedialearn.mediastudy.utils.wav.WavFileHeader.WAV_SUB_CHUNKSIZE2_OFFSET);
            wavFile.write(intToByteArray((int) (totalSize)), 0, 4);
            wavFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        return;
    }

    //基本数据类型转为byte数组
    private static byte[] intToByteArray(int data) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(data).array();
    }

    private static byte[] shortToByteArray(short data) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(data).array();
    }

    /**
     * wav文件的头信息
     */
    private static class WavFileHeader {
        public static final int WAV_FILE_HEADER_SIZE = 44;
        public static final int WAV_CHUNKSIZE_EXCLUDE_DATA = 36;
        public static final int WAV_CHUNKSIZE_OFFSET = 4;
        public static final int WAV_SUB_CHUNKSIZE1_OFFSET = 16;
        public static final int WAV_SUB_CHUNKSIZE2_OFFSET = 40;
        public String mChunkID = "RIFF";
        public int mChunkSize = 0;
        public String mFormat = "WAVE";
        public String mSubChunk1ID = "fmt ";
        public int mSubChunk1Size = 16;
        public short mAudioFormat = 1;
        public short mNumChannel = 1;
        public int mSampleRate = 8000;
        public int mByteRate = 0;
        public short mBlockAlign = 0;
        public short mBitsPerSample = 8;

        public String mSubChunk2ID = "data";
        public int mSubChunk2Size = 0;

        public WavFileHeader(int sampleRateInHz, int bitsPerSample, int channels) {
            mSampleRate = sampleRateInHz;
            mBitsPerSample = (short) bitsPerSample;
            mNumChannel = (short) channels;
            mByteRate = mSampleRate * mNumChannel * mBitsPerSample / 8;
            mBlockAlign = (short) (mNumChannel * mBitsPerSample / 8);
        }
    }

    public static final int RGBA_YUV420SP = 0x00004012;
    public static final int BGRA_YUV420SP = 0x00004210;
    public static final int RGBA_YUV420P = 0x00014012;
    public static final int BGRA_YUV420P = 0x00014210;
    public static final int RGB_YUV420SP = 0x00003012;
    public static final int RGB_YUV420P = 0x00013012;
    public static final int BGR_YUV420SP = 0x00003210;
    public static final int BGR_YUV420P = 0x00013210;

    /**
     * 检查MediaCodec支持的颜色类型
     *
     * @param mime 文件类型
     * @return 0：颜色类型 1：rgb->yuv使用的类型，libyuv使用
     */
    public static int[] checkColorFormat(String mime) {
        int[] result = new int[2];
        if (Build.MODEL.equals("HUAWEI P6-C00")) {
            result[0] = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
            result[1] = BGRA_YUV420SP;
            return result;
        }
        for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if (info.isEncoder()) {
                String[] types = info.getSupportedTypes();
                for (String type : types) {
                    if (type.equals(mime)) {
                        Log.e("YUV", "type-->" + type);
                        MediaCodecInfo.CodecCapabilities c = info.getCapabilitiesForType(type);
                        Log.e("YUV", "color-->" + Arrays.toString(c.colorFormats));
                        for (int j = 0; j < c.colorFormats.length; j++) {
                            if (c.colorFormats[j] == MediaCodecInfo.CodecCapabilities
                                    .COLOR_FormatYUV420Planar) {
                                result[0] = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
                                result[1] = RGBA_YUV420P;
                                return result;
                            } else if (c.colorFormats[j] == MediaCodecInfo.CodecCapabilities
                                    .COLOR_FormatYUV420SemiPlanar) {
                                result[0] = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
                                result[1] = RGBA_YUV420SP;
                                return result;
                            }
                        }
                    }
                }
            }
        }
        result[0] = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        result[1] = RGBA_YUV420SP;
        return result;
    }
}
