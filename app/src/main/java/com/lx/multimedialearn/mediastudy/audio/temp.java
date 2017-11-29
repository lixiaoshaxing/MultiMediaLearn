package com.lx.multimedialearn.mediastudy.audio;

/**
 * 文件描述
 *
 * @author lixiao
 * @since 2017-11-29 10:46
 */
public class temp {
//    private static final String TAG="VideoEncoder";
//    private MediaCodec mEnc;
//    private String mime="video/avc";
//    private int rate=256000;
//    private int frameRate=24;
//    private int frameInterval=1;
//
//    private int fpsTime;
//
//    private Thread mThread;
//    private boolean mStartFlag=false;
//    private int width;
//    private int height;
//    private byte[] mHeadInfo=null;
//
//    private byte[] nowFeedData;
//    private long nowTimeStep;
//    private boolean hasNewData=false;
//
//    private FileOutputStream fos;
//    private String mSavePath;
//
//    public VideoEncoder(){
//        fpsTime=1000/frameRate;
//    }
//
//    public void setMime(String mime){
//        this.mime=mime;
//    }
//
//    public void setRate(int rate){
//        this.rate=rate;
//    }
//
//    public void setFrameRate(int frameRate){
//        this.frameRate=frameRate;
//    }
//
//    public void setFrameInterval(int frameInterval){
//        this.frameInterval=frameInterval;
//    }
//
//    public void setSavePath(String path){
//        this.mSavePath=path;
//    }
//
//
//    /**
//     * 准备录制
//     * @param width 视频宽度
//     * @param height 视频高度
//     * @throws IOException
//     */
//    public void prepare(int width,int height) throws IOException {
//        mHeadInfo=null;
//        this.width=width;
//        this.height=height;
//        File file=new File(mSavePath);
//        File folder=file.getParentFile();
//        if(!folder.exists()){
//            boolean b=folder.mkdirs();
//            Log.e("wuwang","create "+folder.getAbsolutePath()+" "+b);
//        }
//        if(file.exists()){
//            boolean b=file.delete();
//        }
//        fos=new FileOutputStream(mSavePath);
//        MediaFormat format=MediaFormat.createVideoFormat(mime,width,height);
//        format.setInteger(MediaFormat.KEY_BIT_RATE,rate);
//        format.setInteger(MediaFormat.KEY_FRAME_RATE,frameRate);
//        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,frameInterval);
//        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities
//                .COLOR_FormatYUV420Planar);
//        mEnc= MediaCodec.createEncoderByType(mime);
//        mEnc.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
//    }
//
//    /**
//     * 开始录制
//     * @throws InterruptedException
//     */
//    public void start() throws InterruptedException {
//        if(mThread!=null&&mThread.isAlive()){
//            mStartFlag=false;
//            mThread.join();
//        }
//        mEnc.start();
//        mStartFlag=true;
//        mThread=new Thread(this);
//        mThread.start();
//    }
//
//    /**
//     * 停止录制
//     */
//    public void stop(){
//        try {
//            mStartFlag=false;
//            mThread.join();
//            mEnc.stop();
//            mEnc.release();
//            fos.flush();
//            fos.close();
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 由外部喂入一帧数据
//     * @param data RGBA数据
//     * @param timeStep camera附带时间戳
//     */
//    public void feedData(final byte[] data, final long timeStep){
//        hasNewData=true;
//        nowFeedData=data;
//        nowTimeStep=timeStep;
//    }
//
//    private ByteBuffer getInputBuffer(int index){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            return mEnc.getInputBuffer(index);
//        }else{
//            return mEnc.getInputBuffers()[index];
//        }
//    }
//
//    private ByteBuffer getOutputBuffer(int index){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            return mEnc.getOutputBuffer(index);
//        }else{
//            return mEnc.getOutputBuffers()[index];
//        }
//    }
//
//    //TODO 定时调用，如果没有新数据，就用上一个数据
//    private void readOutputData(byte[] data,long timeStep) throws IOException {
//        int index=mEnc.dequeueInputBuffer(-1);
//        if(index>=0){
//            if(hasNewData){
//                if(yuv==null){
//                    yuv=new byte[width*height*3/2];
//                }
//                rgbaToYuv(data,width,height,yuv);
//            }
//            ByteBuffer buffer=getInputBuffer(index);
//            buffer.clear();
//            buffer.put(yuv);
//            mEnc.queueInputBuffer(index,0,yuv.length,timeStep,0);
//        }
//        MediaCodec.BufferInfo mInfo=new MediaCodec.BufferInfo();
//        int outIndex=mEnc.dequeueOutputBuffer(mInfo,0);
//        while (outIndex>=0){
//            ByteBuffer outBuf=getOutputBuffer(outIndex);
//            byte[] temp=new byte[mInfo.size];
//            outBuf.get(temp);
//            if(mInfo.flags==MediaCodec.BUFFER_FLAG_CODEC_CONFIG){
//                Log.e(TAG,"start frame");
//                mHeadInfo=new byte[temp.length];
//                mHeadInfo=temp;
//            }else if(mInfo.flags%8==MediaCodec.BUFFER_FLAG_KEY_FRAME){
//                Log.e(TAG,"key frame");
//                byte[] keyframe = new byte[temp.length + mHeadInfo.length];
//                System.arraycopy(mHeadInfo, 0, keyframe, 0, mHeadInfo.length);
//                System.arraycopy(temp, 0, keyframe, mHeadInfo.length, temp.length);
//                Log.e(TAG,"other->"+mInfo.flags);
//                fos.write(keyframe,0,keyframe.length);
//            }else if(mInfo.flags==MediaCodec.BUFFER_FLAG_END_OF_STREAM){
//                Log.e(TAG,"end frame");
//            }else{
//                fos.write(temp,0,temp.length);
//            }
//            mEnc.releaseOutputBuffer(outIndex,false);
//            outIndex=mEnc.dequeueOutputBuffer(mInfo,0);
//            Log.e("wuwang","outIndex-->"+outIndex);
//        }
//
//    }
//
//    byte[] yuv;
//    private void rgbaToYuv(byte[] rgba,int width,int height,byte[] yuv){
//        final int frameSize = width * height;
//
//        int yIndex = 0;
//        int uIndex = frameSize;
//        int vIndex = frameSize + frameSize/4;
//
//        int R, G, B, Y, U, V;
//        int index = 0;
//        for (int j = 0; j < height; j++) {
//            for (int i = 0; i < width; i++) {
//                index = j * width + i;
//                if(rgba[index*4]>127||rgba[index*4]<-128){
//                    Log.e("color","-->"+rgba[index*4]);
//                }
//                R = rgba[index*4]&0xFF;
//                G = rgba[index*4+1]&0xFF;
//                B = rgba[index*4+2]&0xFF;
//
//                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
//                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
//                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;
//
//                yuv[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
//                if (j % 2 == 0 && index % 2 == 0) {
//                    yuv[uIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
//                    yuv[vIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
//                }
//            }
//        }
//    }
//
//    @Override
//    public void run() {
//        while (mStartFlag){
//            long time=System.currentTimeMillis();
//            if(nowFeedData!=null){
//                try {
//                    readOutputData(nowFeedData,nowTimeStep);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            long lt=System.currentTimeMillis()-time;
//            if(fpsTime>lt){
//                try {
//                    Thread.sleep(fpsTime-lt);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
}
