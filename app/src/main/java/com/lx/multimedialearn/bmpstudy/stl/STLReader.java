package com.lx.multimedialearn.bmpstudy.stl;

import android.content.Context;
import android.content.res.AssetManager;

import com.lx.multimedialearn.bmpstudy.DrawBmpUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * stl文件解析器
 * 通过读取文件，解析后使用Model进行包装
 *
 * @author lixiao
 * @since 2017-09-04 14:26
 */
public class STLReader {

    private StlLoadListener stlLoadListener;

    public Model parserStlFromSDCard(String path) {
        Model model = null;
        File file = new File(path);
        try {
            FileInputStream fis = new FileInputStream(path);
            return parseBinStl(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            if (stlLoadListener != null) {
                stlLoadListener.onFailure(e);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (stlLoadListener != null) {
                stlLoadListener.onFailure(e);
            }
        } finally {
            if (stlLoadListener != null) {
                stlLoadListener.onFinished();
            }
        }
        return model;
    }

    public Model parserStlFromAssert(Context context, String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);
            return parseBinStl(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 打开带有纹理的stl文件，是一系列model
     *
     * @param context
     * @param name
     * @return
     */
    public Model parseStlWithTextureFromAsset(Context context, String name) {
        Model model = null;
        AssetManager am = context.getAssets();
        try {
            InputStream stlInput = am.open(name + ".stl");
            InputStream textureInput = am.open(name + ".pxy");
            model = parseStlWithTexture(stlInput, textureInput);
            model.setPictureName(name + ".jpg");
            return model;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return model;
    }

    private Model parseStlWithTexture(InputStream stlInput, InputStream textureInput) throws IOException {
        Model model = parseBinStl(stlInput);
        int facetCount = model.getFacetCount();
        // 三角面片有3个顶点，一个顶点有2个坐标轴数据，每个坐标轴数据是float类型（4字节）
        // 读取数据后设置到texture
        byte[] textureBytes = new byte[facetCount * 4 * 3 * 2];
        textureInput.read(textureBytes); //将所有的纹理坐标读出来
        parseTexture(model, textureBytes);
        return model;
    }

    /**
     * 读出纹理像素，并放入model中，绘画的时候要读取所有的model，循环绘画
     *
     * @param model
     * @param textureBytes
     */
    private void parseTexture(Model model, byte[] textureBytes) {
        int facetCount = model.getFacetCount();
        // 三角面个数有三个顶点，一个顶点对应纹理二维坐标
        float[] textures = new float[facetCount * 3 * 2];
        int textureOffset = 0;
        for (int i = 0; i < facetCount; i++) {
            //第i个顶点对应的纹理坐标
            //tx和ty的取值范围为[0,1],表示的坐标位置是在纹理图片上的对应比例
            float tx = DrawBmpUtils.byte4ToFloat(textureBytes, textureOffset);
            float ty = DrawBmpUtils.byte4ToFloat(textureBytes, textureOffset + 4);
            textureOffset += 8;
            textures[i * 2] = tx; //一个点坐标占用了两个位置
            //我们的pxy文件原点是在左下角，因此需要用1减去y坐标值
            textures[i * 2 + 1] = 1 - ty;
        }
        model.setTextures(textures);
    }

    /**
     * 根据字段长度读取合适的字节放入到对应的Model中
     *
     * @param in
     * @return
     */
    private Model parseBinStl(InputStream in) throws IOException {
        if (stlLoadListener != null) {
            stlLoadListener.onStart();
        }
        Model model = new Model();
        //开头一行，80个字节是文件头，用来储存文件名
        in.skip(80);
        //4个字节描述三角面的个数
        byte[] bytes = new byte[4];
        in.read(bytes); //读取到了三角面片个数
        int facetCount = DrawBmpUtils.byte4ToInt(bytes, 0);
        model.setFacetCount(facetCount);
        if (facetCount == 0) {
            in.close();
            return model;
        }
        //读取所有的三角面片，设置对应的值，每个三角面片对应50byte的固定大小
        byte[] facetBytes = new byte[facetCount * 50];
        in.read(facetBytes);

        in.close();

        parseModel(model, facetBytes);

        if (stlLoadListener != null) {
            stlLoadListener.onFinished();
        }
        return model;
    }

    /**
     * 50个字节一组，进行处理，放入buffer：50个字节中：顶点数据，法向量数据，所占空间范围
     *
     * @param model
     * @param facetBytes
     */
    private void parseModel(Model model, byte[] facetBytes) {
        int facetCount = model.getFacetCount();
        /**
         *  每个三角面片占用固定的50个字节,50字节当中：
         *  三角片的法向量：（1个向量相当于一个点）*（3维/点）*（4字节浮点数/维）=12字节
         *  三角片的三个点坐标：（3个点）*（3维/点）*（4字节浮点数/维）=36字节
         *  最后2个字节用来描述三角面片的属性信息
         * **/
        //三个坐标点的位置，*3（3个坐标点） *3（一个坐标点有x,y,z三个值）
        float[] verts = new float[facetCount * 3 * 3];
        // 保存所有三角面对应的法向量位置，
        // 一个三角面对应一个法向量，一个法向量有3个点
        // 而绘制模型时，是针对需要每个顶点对应的法向量，因此存储长度需要*3
        // 又同一个三角面的三个顶点的法向量是相同的，
        // 因此后面写入法向量数据的时候，只需连续写入3个相同的法向量即可
        float[] vnorms = new float[facetCount * 3 * 3];

        //存储一些属性信息
        short[] remarks = new short[facetCount];

        int stlOffset = 0;
        try {
            for (int i = 0; i < facetCount; i++) {
                if (stlLoadListener != null) {
                    stlLoadListener.onLoading(i, facetCount);
                }
                for (int j = 0; j < 4; j++) { // 50 = 12 * 4 + 2, 每次读出来12个字节，设置为法向量，顶点坐标
                    //每次读出来4个字节？
                    float x = DrawBmpUtils.byte4ToFloat(facetBytes, stlOffset);
                    float y = DrawBmpUtils.byte4ToFloat(facetBytes, stlOffset + 4);
                    float z = DrawBmpUtils.byte4ToFloat(facetBytes, stlOffset + 8);
                    stlOffset += 12;

                    if (j == 0) {  //读取的法向量
                        vnorms[i * 9] = x;
                        vnorms[i * 9 + 1] = y;
                        vnorms[i * 9 + 2] = z;
                        vnorms[i * 9 + 3] = x;
                        vnorms[i * 9 + 4] = y;
                        vnorms[i * 9 + 5] = z;
                        vnorms[i * 9 + 6] = x;
                        vnorms[i * 9 + 7] = y;
                        vnorms[i * 9 + 8] = z;
                    } else { //读取的三个顶点的位置
                        verts[i * 9 + (j - 1) * 3] = x;
                        verts[i * 9 + (j - 1) * 3 + 1] = y;
                        verts[i * 9 + (j - 1) * 3 + 2] = z;

                        if (i == 0 && j == 1) { //设置初始值
                            model.minX = model.maxX = x;
                            model.minY = model.maxY = y;
                            model.minZ = model.maxZ = z;
                        } else {
                            model.minX = Math.min(model.minX, x);
                            model.minY = Math.min(model.minY, y);
                            model.minZ = Math.min(model.minZ, z);
                            model.maxX = Math.max(model.maxX, x);
                            model.maxY = Math.max(model.maxY, y);
                            model.maxZ = Math.max(model.maxZ, z);
                        }
                    }
                }
                short r = DrawBmpUtils.byte2ToShort(facetBytes, stlOffset);
                stlOffset += 2;
                remarks[i] = r;
            }
        } catch (Exception e) {
            if (stlLoadListener != null) {
                stlLoadListener.onFailure(e);
            }
        }
        model.setVerts(verts);
        model.setVnorms(vnorms);
        model.setRemarks(remarks);
    }

    public interface StlLoadListener {
        void onStart();

        void onLoading(int cur, int total);

        void onFinished();

        void onFailure(Exception e);
    }
}
