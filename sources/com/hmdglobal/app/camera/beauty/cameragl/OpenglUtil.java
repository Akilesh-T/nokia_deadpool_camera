package com.hmdglobal.app.camera.beauty.cameragl;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;
import com.bumptech.glide.load.Key;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class OpenglUtil {
    public static final float[] CUBE = new float[]{-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
    public static final int NOT_INIT = -1;
    public static final int NO_TEXTURE = -1;
    public static final int ON_DRAWN = 1;
    public static final float[] TEXTURE_NO_ROTATION = new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f};
    public static final float[] TEXTURE_ROTATED_BACK = new float[]{1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f};
    public static final float[] TEXTURE_ROTATED_FRONT = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f};

    public static float[] getRotation(int rotation, boolean flipHorizontal, boolean flipVertical) {
        return TEXTURE_ROTATED_FRONT;
    }

    public static int[] initTextureID(int width, int height) {
        int[] mTextureOutID = new int[2];
        int i = 0;
        GLES20.glGenTextures(2, mTextureOutID, 0);
        while (true) {
            int i2 = i;
            if (i2 >= mTextureOutID.length) {
                return mTextureOutID;
            }
            GLES20.glBindTexture(3553, mTextureOutID[i2]);
            GLES20.glTexParameterf(3553, 10240, 9729.0f);
            GLES20.glTexParameterf(3553, 10241, 9729.0f);
            GLES20.glTexParameterf(3553, 10242, 33071.0f);
            GLES20.glTexParameterf(3553, 10243, 33071.0f);
            GLES20.glTexImage2D(3553, 0, 6408, width, height, 0, 6408, 5121, null);
            i = i2 + 1;
        }
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

    private static int loadShader(String strSource, int iType) {
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
        Log.e("Load Shader Failed", stringBuilder.toString());
        return 0;
    }

    public static String loadFromRawFile(Context context, int rawId) {
        try {
            InputStream in = context.getResources().openRawResource(rawId);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (true) {
                int read = in.read();
                int ch = read;
                if (read != -1) {
                    baos.write(ch);
                } else {
                    byte[] buff = baos.toByteArray();
                    baos.close();
                    in.close();
                    return new String(buff, Key.STRING_CHARSET_NAME).replaceAll("\\r\\n", "\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
