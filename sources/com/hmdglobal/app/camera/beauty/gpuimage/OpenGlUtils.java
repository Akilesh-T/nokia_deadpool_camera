package com.hmdglobal.app.camera.beauty.gpuimage;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.hardware.Camera.Size;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import java.nio.IntBuffer;

public class OpenGlUtils {
    public static final int NO_TEXTURE = -1;

    public static int loadTexture(Bitmap img, int usedTexId) {
        return loadTexture(img, usedTexId, true);
    }

    public static int loadTexture(Bitmap img, int usedTexId, boolean recycle) {
        int[] textures = new int[1];
        if (usedTexId == -1) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(3553, textures[0]);
            GLES20.glTexParameterf(3553, 10240, 9729.0f);
            GLES20.glTexParameterf(3553, 10241, 9729.0f);
            GLES20.glTexParameterf(3553, 10242, 33071.0f);
            GLES20.glTexParameterf(3553, 10243, 33071.0f);
            GLUtils.texImage2D(3553, 0, img, 0);
            GLES20.glGetError();
        } else {
            GLES20.glBindTexture(3553, usedTexId);
            GLUtils.texSubImage2D(3553, 0, 0, 0, img);
            textures[0] = usedTexId;
        }
        if (recycle) {
            img.recycle();
        }
        return textures[0];
    }

    public static int loadTexture(IntBuffer data, Size size, int usedTexId) {
        Size size2 = size;
        int i = usedTexId;
        int[] textures = new int[1];
        if (i == -1) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(3553, textures[0]);
            GLES20.glTexParameterf(3553, 10240, 9729.0f);
            GLES20.glTexParameterf(3553, 10241, 9729.0f);
            GLES20.glTexParameterf(3553, 10242, 33071.0f);
            GLES20.glTexParameterf(3553, 10243, 33071.0f);
            GLES20.glTexImage2D(3553, 0, 6408, size2.width, size2.height, 0, 6408, 5121, data);
        } else {
            GLES20.glBindTexture(3553, i);
            GLES20.glTexSubImage2D(3553, 0, 0, 0, size2.width, size2.height, 6408, 5121, data);
            textures[0] = i;
        }
        return textures[0];
    }

    public static int loadTextureAsBitmap(IntBuffer data, Size size, int usedTexId) {
        return loadTexture(Bitmap.createBitmap(data.array(), size.width, size.height, Config.ARGB_8888), usedTexId);
    }

    public static int loadShader(String strSource, int iType) {
        int[] compiled = new int[1];
        int iShader = GLES20.glCreateShader(iType);
        GLES20.glShaderSource(iShader, strSource);
        GLES20.glCompileShader(iShader);
        GLES20.glGetShaderiv(iShader, 35713, compiled, 0);
        if (compiled[0] != 0) {
            return iShader;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Compilation\n");
        stringBuilder.append(GLES20.glGetShaderInfoLog(iShader));
        Log.d("Load Shader Failed", stringBuilder.toString());
        return 0;
    }

    public static int loadProgram(String strVSource, String strFSource) {
        int[] link = new int[1];
        int iVShader = loadShader(strVSource, 35633);
        if (iVShader == 0) {
            Log.d("Load Program", "Vertex Shader Failed");
            return 0;
        }
        int iFShader = loadShader(strFSource, 35632);
        if (iFShader == 0) {
            Log.d("Load Program", "Fragment Shader Failed");
            return 0;
        }
        int iProgId = GLES20.glCreateProgram();
        GLES20.glAttachShader(iProgId, iVShader);
        GLES20.glAttachShader(iProgId, iFShader);
        GLES20.glLinkProgram(iProgId);
        GLES20.glGetProgramiv(iProgId, 35714, link, 0);
        if (link[0] <= 0) {
            Log.d("Load Program", "Linking Failed");
            return 0;
        }
        GLES20.glDeleteShader(iVShader);
        GLES20.glDeleteShader(iFShader);
        return iProgId;
    }

    public static float rnd(float min, float max) {
        return ((max - min) * ((float) Math.random())) + min;
    }
}
