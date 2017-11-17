attribute vec4 aPosition;
attribute vec2 aCoord;
uniform mat4 uMatrix;

varying vec2 textureCoordinate;

void main(){
    gl_Position = uMatrix*aPosition;
    textureCoordinate = aCoord;
}
