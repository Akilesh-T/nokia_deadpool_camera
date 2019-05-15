package com.hmdglobal.app.camera.beauty.cameragl;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.opengl.GLES20;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class TexureToBufferHelper {
    public static final int COMPRESS_TYPE = 0;
    public static final int ORIGIN_TYPE = 1;
    private int count = 0;
    private final String mFragmentShader = "precision mediump float;varying vec2 textureCoordinate;uniform sampler2D inputImageTexture;void main(){    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);}";
    protected int[] mFrameBufferTextures = null;
    protected int[] mFrameBuffers = null;
    private int mFrameHeight = -1;
    private int mFrameWidth = -1;
    protected int mGLAttribPosition;
    protected int mGLAttribTextureCoordinate;
    protected FloatBuffer mGLCubeBuffer = ByteBuffer.allocateDirect(this.vertexPoint.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    protected int mGLProgId;
    protected FloatBuffer mGLTextureBuffer;
    protected int mGLUniformTexture;
    protected boolean mIsInitialized;
    protected int mOrginHeight;
    protected int mOrignWidth;
    protected int mOutputHeight;
    protected int mOutputWidth;
    private final String mVertexShader = "attribute vec4 position;attribute vec4 inputTextureCoordinate;varying vec2 textureCoordinate;void main(){  textureCoordinate = (inputTextureCoordinate).xy;  gl_Position = position;}";
    private int num = 10;
    byte[] readCompressArray;
    ByteBuffer readCompressBuffer;
    byte[] readOriginArray;
    ByteBuffer readOriginBuffer;
    private long sum;
    private final float[] textureBackCam = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f};
    private final float[] texturePoint = new float[]{1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f};
    private long time = System.currentTimeMillis();
    private final float[] vertexPoint = new float[]{-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};

    public TexureToBufferHelper() {
        this.mGLCubeBuffer.put(this.vertexPoint).position(0);
        this.mGLTextureBuffer = ByteBuffer.allocateDirect(OpenglUtil.TEXTURE_NO_ROTATION.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mGLTextureBuffer.put(this.texturePoint).position(0);
    }

    public void switchTextureCoords(boolean isFrontCam) {
        this.mGLTextureBuffer.put(isFrontCam ? this.texturePoint : this.textureBackCam).position(0);
    }

    private void init(int type) {
        onInit();
        this.mIsInitialized = true;
    }

    /* Access modifiers changed, original: protected */
    public void onInit() {
        this.mGLProgId = OpenglUtil.loadProgram("attribute vec4 position;attribute vec4 inputTextureCoordinate;varying vec2 textureCoordinate;void main(){  textureCoordinate = (inputTextureCoordinate).xy;  gl_Position = position;}", "precision mediump float;varying vec2 textureCoordinate;uniform sampler2D inputImageTexture;void main(){    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);}");
        this.mGLAttribPosition = GLES20.glGetAttribLocation(this.mGLProgId, "position");
        this.mGLUniformTexture = GLES20.glGetUniformLocation(this.mGLProgId, "inputImageTexture");
        this.mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(this.mGLProgId, "inputTextureCoordinate");
    }

    public void onOutputSizeChanged(int outputWidth, int outputHeight, int orginWidth, int originHeight) {
        init(0);
        this.mOutputWidth = outputWidth;
        this.mOutputHeight = outputHeight;
        this.readCompressArray = new byte[((outputWidth * outputHeight) * 4)];
        this.readCompressBuffer = ByteBuffer.wrap(this.readCompressArray);
        this.mOrignWidth = orginWidth;
        this.mOrginHeight = originHeight;
        initCameraFrameBuffer(this.mOutputWidth, this.mOutputHeight, 0);
    }

    public void onOutputSizeChanged(int orginWidth, int originHeight) {
        init(1);
        this.readOriginArray = new byte[((orginWidth * originHeight) * 4)];
        this.readOriginBuffer = ByteBuffer.wrap(this.readOriginArray);
        this.mOrignWidth = orginWidth;
        this.mOrginHeight = originHeight;
        initCameraFrameBuffer(orginWidth, originHeight, 1);
    }

    public ByteBuffer getTextureBufferCompress(int textureId) {
        return textureToByteBuffer(textureId);
    }

    public ByteBuffer getOriginPixels(int textureId) {
        int[] currentFrameid = new int[1];
        GLES20.glGetIntegerv(36006, currentFrameid, 0);
        GLES20.glBindFramebuffer(36160, this.mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(36160, 36064, 3553, textureId, 0);
        ByteBuffer buffer = readOriginPixels();
        GLES20.glBindTexture(3553, 0);
        GLES20.glBindFramebuffer(36160, currentFrameid[0]);
        return buffer;
    }

    public ByteBuffer getOriginPixelsByBindTexureId(int textureId) {
        int[] currentTexured = new int[1];
        GLES20.glGetIntegerv(32873, currentTexured, 0);
        GLES20.glBindTexture(3553, textureId);
        ByteBuffer buffer = readOriginPixels();
        GLES20.glBindTexture(3553, currentTexured[0]);
        return buffer;
    }

    public final void destroy() {
        this.mIsInitialized = false;
        destroyFramebuffers();
        GLES20.glDeleteProgram(this.mGLProgId);
    }

    private void initCameraFrameBuffer(int width, int height, int type) {
        if (!(this.mFrameBuffers == null || (this.mFrameWidth == width && this.mFrameHeight == height))) {
            destroyFramebuffers();
        }
        if (this.mFrameBuffers == null) {
            this.mFrameWidth = width;
            this.mFrameHeight = height;
            this.mFrameBuffers = new int[1];
            GLES20.glGenFramebuffers(1, this.mFrameBuffers, 0);
            if (type == 0) {
                this.mFrameBufferTextures = new int[1];
                GLES20.glGenTextures(1, this.mFrameBufferTextures, 0);
                bindFrameBuffer(this.mFrameBufferTextures[0], this.mFrameBuffers[0], width, height);
            }
        }
    }

    private void bindFrameBuffer(int textureId, int frameBuffer, int width, int height) {
        GLES20.glBindTexture(3553, textureId);
        GLES20.glTexImage2D(3553, 0, 6408, width, height, 0, 6408, 5121, null);
        GLES20.glTexParameterf(3553, 10240, 9729.0f);
        GLES20.glTexParameterf(3553, 10241, 9729.0f);
        GLES20.glTexParameterf(3553, 10242, 33071.0f);
        GLES20.glTexParameterf(3553, 10243, 33071.0f);
        GLES20.glBindFramebuffer(36160, frameBuffer);
        GLES20.glFramebufferTexture2D(36160, 36064, 3553, textureId, 0);
        GLES20.glBindTexture(3553, 0);
        GLES20.glBindFramebuffer(36160, 0);
    }

    private ByteBuffer textureToByteBuffer(int textureId) {
        int i = textureId;
        int[] currentFrameid = new int[1];
        GLES20.glGetIntegerv(36006, currentFrameid, 0);
        if (this.mFrameBuffers == null) {
            return null;
        }
        GLES20.glBindFramebuffer(36160, this.mFrameBuffers[0]);
        GLES20.glViewport(0, 0, this.mOutputWidth, this.mOutputHeight);
        GLES20.glUseProgram(this.mGLProgId);
        if (!this.mIsInitialized) {
            return null;
        }
        this.mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(this.mGLAttribPosition, 2, 5126, false, 0, this.mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(this.mGLAttribPosition);
        this.mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(this.mGLAttribTextureCoordinate, 2, 5126, false, 0, this.mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(this.mGLAttribTextureCoordinate);
        if (i != -1) {
            GLES20.glActiveTexture(33984);
            GLES20.glBindTexture(3553, i);
            GLES20.glUniform1i(this.mGLUniformTexture, 0);
        }
        GLES20.glDrawArrays(5, 0, 4);
        this.readCompressBuffer.clear();
        long time1 = System.currentTimeMillis();
        GLES20.glReadPixels(0, 0, this.mOutputWidth, this.mOutputHeight, 6408, 5121, this.readCompressBuffer);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("textureToByteBuffer: readtime");
        stringBuilder.append(System.currentTimeMillis() - time1);
        Log.e("xie", stringBuilder.toString());
        GLES20.glDisableVertexAttribArray(this.mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(this.mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(3553, 0);
        GLES20.glBindFramebuffer(36160, currentFrameid[0]);
        return this.readCompressBuffer;
    }

    private ByteBuffer readOriginPixels() {
        this.readOriginBuffer.clear();
        long time1 = System.currentTimeMillis();
        GLES20.glReadPixels(0, 0, this.mOrignWidth, this.mOrginHeight, 6408, 5121, this.readOriginBuffer);
        return this.readOriginBuffer;
    }

    private void destroyFramebuffers() {
        if (this.mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(1, this.mFrameBufferTextures, 0);
            this.mFrameBufferTextures = null;
        }
        if (this.mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(1, this.mFrameBuffers, 0);
            this.mFrameBuffers = null;
        }
        this.mFrameWidth = -1;
        this.mFrameHeight = -1;
    }

    public static Bitmap getTextureTestBitmap(ByteBuffer buffer, int width, int height) {
        byte[] bitmapBuffer = buffer.array();
        int[] bitmapSource = new int[(width * height)];
        int i = 0;
        int j = 0;
        while (i < (width * height) * 4) {
            int i2 = i + 1;
            int i3 = i2 + 1;
            i = ((bitmapBuffer[i] & 255) << 16) | ((bitmapBuffer[i2] & 255) << 8);
            i2 = i3 + 1;
            bitmapSource[j] = (i | (bitmapBuffer[i3] & 255)) | ((bitmapBuffer[i2] & 255) << 24);
            i = i2 + 1;
            j++;
        }
        return Bitmap.createBitmap(bitmapSource, width, height, Config.ARGB_8888);
    }

    private static void readStaticPixels(int width, int height) {
        Buffer intBuffer = ByteBuffer.wrap(new byte[((width * height) * 4)]);
        intBuffer.position(0);
        long time1 = System.currentTimeMillis();
        GLES20.glReadPixels(0, 0, width, height, 6408, 5121, intBuffer);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("xie getreadpixels  sanguo time");
        stringBuilder.append(System.currentTimeMillis() - time1);
        Log.e("xie", stringBuilder.toString());
    }

    public static void saveBitmap(Bitmap bm) {
        File f = new File("/sdcard/", "beauty.png");
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }
}
