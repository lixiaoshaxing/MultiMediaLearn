precision mediump float;

uniform sampler2D uTextureUnit;
uniform int uType; //处理颜色类型
uniform int uIsHalf; //是否处理一半
uniform vec3 uChangeColor; //进行颜色变化的颜色模板
uniform float uXY;

varying vec2 vCoordinate; //纹理坐标
varying vec4 vPosition; //经过投影变换后的位置
varying vec4 vOriPosition; //在OpenGL空间中的位置

void modifyColor(vec4 color){
    color.r = max(min(color.r, 1.0), 0.0);
    color.g = max(min(color.g, 1.0), 0.0);
    color.b = max(min(color.b, 1.0), 0.0);
    color.a = max(min(color.a, 1.0), 0.0);
}

void main(){
    vec4 tColor = texture2D(uTextureUnit, vCoordinate); //根据坐标进行绑定
    if(vOriPosition.x>0.0 || uIsHalf == 0){ //为0的时候处理一半，为1的时候为全部处理
        if(uType == 1){ //为1的时候，处理为黑白图片，每个像素三个通道的和 / 3，相当于保留亮度
            float c = tColor.r * uChangeColor.r + tColor.g * uChangeColor.g + tColor.b * uChangeColor.b;
            gl_FragColor = vec4(c, c, c, tColor.a);
        }else if(uType == 2){ //简单色彩处理，冷暖色调、增加亮度、降低亮度等，原理是减少rgb三个通道值
            vec4 deltaColor = tColor + vec4(uChangeColor, 0.0);
            modifyColor(deltaColor);
            gl_FragColor = deltaColor;
        }else if(uType == 3){ //模糊处理，每个点的值有周围点进行确定，高斯模糊
            tColor+=texture2D(uTextureUnit,vec2(vCoordinate.x-uChangeColor.r,vCoordinate.y-uChangeColor.r));
            tColor+=texture2D(uTextureUnit,vec2(vCoordinate.x-uChangeColor.r,vCoordinate.y+uChangeColor.r));
            tColor+=texture2D(uTextureUnit,vec2(vCoordinate.x+uChangeColor.r,vCoordinate.y-uChangeColor.r));
            tColor+=texture2D(uTextureUnit,vec2(vCoordinate.x+uChangeColor.r,vCoordinate.y+uChangeColor.r));
            tColor+=texture2D(uTextureUnit,vec2(vCoordinate.x-uChangeColor.g,vCoordinate.y-uChangeColor.g));
            tColor+=texture2D(uTextureUnit,vec2(vCoordinate.x-uChangeColor.g,vCoordinate.y+uChangeColor.g));
            tColor+=texture2D(uTextureUnit,vec2(vCoordinate.x+uChangeColor.g,vCoordinate.y-uChangeColor.g));
            tColor+=texture2D(uTextureUnit,vec2(vCoordinate.x+uChangeColor.g,vCoordinate.y+uChangeColor.g));
            tColor+=texture2D(uTextureUnit,vec2(vCoordinate.x-uChangeColor.b,vCoordinate.y-uChangeColor.b));
            tColor+=texture2D(uTextureUnit,vec2(vCoordinate.x-uChangeColor.b,vCoordinate.y+uChangeColor.b));
            tColor+=texture2D(uTextureUnit,vec2(vCoordinate.x+uChangeColor.b,vCoordinate.y-uChangeColor.b));
            tColor+=texture2D(uTextureUnit,vec2(vCoordinate.x+uChangeColor.b,vCoordinate.y+uChangeColor.b));
            tColor/=13.0;
            gl_FragColor=tColor;
        }else if(uType == 4){ //放大处理，对
            float dis = distance(vec2(vPosition.x, vPosition.y / uXY), vec2(uChangeColor.r, uChangeColor.g));
            if(dis < uChangeColor.b){ //小于一定范围才进行放大处理
                tColor = texture2D(uTextureUnit, vec2(vCoordinate.x / 2.0 + 0.25,vCoordinate.y / 2.0 + 0.25));
            }
            gl_FragColor=tColor;
        }else{
            gl_FragColor = tColor;
        }
    }else{
        gl_FragColor = tColor;
    }
}