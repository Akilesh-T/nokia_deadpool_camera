package com.android.grafika.gles;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GlUtil {
    public static final float[] IDENTITY_MATRIX = new float[16];
    private static final int SIZEOF_FLOAT = 4;
    public static final String TAG = "Grafika";

    static {
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }

    private GlUtil() {
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(35633, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(35632, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }
        int program = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, 35714, linkStatus, 0);
        if (linkStatus[0] != 1) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("glCreateShader type=");
        stringBuilder.append(shaderType);
        checkGlError(stringBuilder.toString());
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, 35713, compiled, 0);
        if (compiled[0] != 0) {
            return shader;
        }
        String str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Could not compile shader ");
        stringBuilder2.append(shaderType);
        stringBuilder2.append(":");
        Log.e(str, stringBuilder2.toString());
        str = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" ");
        stringBuilder2.append(GLES20.glGetShaderInfoLog(shader));
        Log.e(str, stringBuilder2.toString());
        GLES20.glDeleteShader(shader);
        return 0;
    }

    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != 0) {
            String msg = new StringBuilder();
            msg.append(op);
            msg.append(": glError 0x");
            msg.append(Integer.toHexString(error));
            msg = msg.toString();
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }
    }

    public static void checkLocation(int location, String label) {
        if (location < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to locate '");
            stringBuilder.append(label);
            stringBuilder.append("' in program");
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    public static int createImageTexture(ByteBuffer data, int width, int height, int format) {
        int[] textureHandles = new int[1];
        GLES20.glGenTextures(1, textureHandles, 0);
        int textureHandle = textureHandles[0];
        checkGlError("glGenTextures");
        GLES20.glBindTexture(3553, textureHandle);
        GLES20.glTexParameteri(3553, 10241, 9729);
        GLES20.glTexParameteri(3553, 10240, 9729);
        checkGlError("loadImageTexture");
        GLES20.glTexImage2D(3553, 0, format, width, height, 0, format, 5121, data);
        checkGlError("loadImageTexture");
        return textureHandle;
    }

    public static FloatBuffer createFloatBuffer(float[] coords) {
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    public static void logVersionInfo() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("vendor  : ");
        stringBuilder.append(GLES20.glGetString(7936));
        Log.i(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("renderer: ");
        stringBuilder.append(GLES20.glGetString(7937));
        Log.i(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("version : ");
        stringBuilder.append(GLES20.glGetString(7938));
        Log.i(str, stringBuilder.toString());
    }
}
