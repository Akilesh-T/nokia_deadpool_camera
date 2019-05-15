package com.hmdglobal.app.camera.beauty.cameragl;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

public class Shader {
    private String fragmentSource;
    private int mProgram = 0;
    private int mShaderFragment = 0;
    private final HashMap<String, Integer> mShaderHandleMap = new HashMap();
    private int mShaderVertex = 0;
    private String vertexSource;

    public void setProgram(int vertexShader, int fragmentShader, Context context) throws Exception {
        this.vertexSource = loadRawString(vertexShader, context);
        this.fragmentSource = loadRawString(fragmentShader, context);
        this.mShaderVertex = loadShader(35633, this.vertexSource);
        this.mShaderFragment = loadShader(35632, this.fragmentSource);
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, this.mShaderVertex);
            GLES20.glAttachShader(program, this.mShaderFragment);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, 35714, linkStatus, 0);
            if (linkStatus[0] != 1) {
                String error = GLES20.glGetProgramInfoLog(program);
                deleteProgram();
                throw new Exception(error);
            }
        }
        this.mProgram = program;
        this.mShaderHandleMap.clear();
    }

    public void useProgram() {
        GLES20.glUseProgram(this.mProgram);
    }

    public void deleteProgram() {
        GLES20.glDeleteShader(this.mShaderVertex);
        GLES20.glDeleteShader(this.mShaderFragment);
        GLES20.glDeleteProgram(this.mProgram);
        this.mShaderFragment = 0;
        this.mShaderVertex = 0;
        this.mProgram = 0;
    }

    public int programHandle() {
        return this.mProgram;
    }

    public int getHandle(String name) {
        if (this.mShaderHandleMap.containsKey(name)) {
            return ((Integer) this.mShaderHandleMap.get(name)).intValue();
        }
        int handle = GLES20.glGetAttribLocation(this.mProgram, name);
        if (handle == -1) {
            handle = GLES20.glGetUniformLocation(this.mProgram, name);
        }
        if (handle == -1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not get attrib location for ");
            stringBuilder.append(name);
            Log.d("GLSL shader", stringBuilder.toString());
        } else {
            this.mShaderHandleMap.put(name, Integer.valueOf(handle));
        }
        return handle;
    }

    public int[] getHandles(String... names) {
        int[] res = new int[names.length];
        for (int i = 0; i < names.length; i++) {
            res[i] = getHandle(names[i]);
        }
        return res;
    }

    private int loadShader(int shaderType, String source) throws Exception {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, 35713, compiled, 0);
            if (compiled[0] == 0) {
                String error = GLES20.glGetShaderInfoLog(shader);
                GLES20.glDeleteShader(shader);
                throw new Exception(error);
            }
        }
        return shader;
    }

    private String loadRawString(int rawId, Context context) throws Exception {
        InputStream is = context.getResources().openRawResource(rawId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (true) {
            int read = is.read(buf);
            int len = read;
            if (read == -1) {
                return baos.toString();
            }
            baos.write(buf, 0, len);
        }
    }
}
