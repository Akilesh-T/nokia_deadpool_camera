package com.hmdglobal.app.camera.beauty.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Matrix;
import com.hmdglobal.app.camera.HelpTip;
import com.hmdglobal.app.camera.LiveBokehModule;
import com.hmdglobal.app.camera.PhotoModule.PictureTaskListener;
import com.hmdglobal.app.camera.VideoModule;
import com.hmdglobal.app.camera.app.CameraAppUI.TakePictureRunnable;
import com.hmdglobal.app.camera.beauty.cameragl.CameraRender.TakePictureCallBack;
import com.hmdglobal.app.camera.beauty.cameragl.TexureToBufferHelper;
import com.hmdglobal.app.camera.motion.MotionPictureHelper;
import com.hmdglobal.app.camera.util.BlurUtil;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptureUtil {
    private Context mContext;
    private GLExecutor mGlExecutor;
    private TexureToBufferHelper mTexureToBufferHelper;
    private ExecutorService threadPoolExecutor = Executors.newSingleThreadExecutor();

    public interface GLExecutor {
        void runOnRenderThread(Runnable runnable);
    }

    public CaptureUtil(Context context, GLExecutor glExecutor) {
        this.mContext = context.getApplicationContext();
        this.mGlExecutor = glExecutor;
        this.mTexureToBufferHelper = new TexureToBufferHelper();
    }

    public void takePicture(int textureId, int texWidth, int texHeight, int width, int height, boolean isFrontCam, Runnable callback, byte[] cache) {
        final Runnable runnable = callback;
        final int i = width;
        final int i2 = height;
        final byte[] bArr = cache;
        takePicture(new TakePictureCallBack() {
            public void onTakPicture(final Buffer byteBuffer) {
                CaptureUtil.this.threadPoolExecutor.execute(new Runnable() {
                    public void run() {
                        if (runnable instanceof PictureTaskListener) {
                            PictureTaskListener back = runnable;
                            back.modifiedBytes = CaptureUtil.this.getBitmap(byteBuffer, i, i2);
                            back.originalBytes = bArr;
                            back.run();
                        } else if (runnable instanceof LiveBokehModule.PictureTaskListener) {
                            LiveBokehModule.PictureTaskListener back2 = runnable;
                            back2.modifiedBytes = CaptureUtil.this.getBitmap(byteBuffer, i, i2);
                            back2.originalBytes = bArr;
                            back2.run();
                        } else if (runnable instanceof VideoModule.PictureTaskListener) {
                            VideoModule.PictureTaskListener back3 = runnable;
                            back3.modifiedBytes = CaptureUtil.this.getBitmap(byteBuffer, i, i2);
                            back3.originalBytes = bArr;
                            back3.run();
                        } else if (runnable instanceof TakePictureRunnable) {
                            TakePictureRunnable run = runnable;
                            Bitmap map = CaptureUtil.this.getBitmap(byteBuffer, i, i2);
                            if (960 == i2) {
                                run.mBitmap = BlurUtil.getSuitableMap(map, 170);
                                return;
                            }
                            int targetWidth;
                            int targetHeight;
                            int offset;
                            if (1200 == i2) {
                                targetWidth = MotionPictureHelper.FRAME_HEIGHT_9;
                                targetHeight = MotionPictureHelper.FRAME_HEIGHT_9;
                                offset = 280;
                            } else {
                                targetWidth = MotionPictureHelper.FRAME_HEIGHT_9;
                                targetHeight = 1280;
                                offset = HelpTip.HELP_TIP_SHOW_DELAY;
                            }
                            run.mBitmap = BlurUtil.getSuitableMap(BlurUtil.scaleBitmap(map, ((float) targetWidth) / ((float) i), ((float) targetHeight) / ((float) i2)), offset);
                        }
                    }
                });
            }
        }, textureId, texWidth, texHeight, width, height, isFrontCam);
    }

    private Bitmap rotateBitmap(Bitmap origin, float alpha) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(alpha);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    public void takePicture(TakePictureCallBack callback, int texId, int texWidth, int texHeight, int width, int height, boolean isFrontCam) {
        if (this.mGlExecutor != null) {
            final boolean z = isFrontCam;
            final int i = width;
            final int i2 = height;
            final int i3 = texWidth;
            final int i4 = texHeight;
            final int i5 = texId;
            final TakePictureCallBack takePictureCallBack = callback;
            this.mGlExecutor.runOnRenderThread(new Runnable() {
                public void run() {
                    CaptureUtil.this.mTexureToBufferHelper.switchTextureCoords(z);
                    CaptureUtil.this.mTexureToBufferHelper.onOutputSizeChanged(i, i2, i3, i4);
                    takePictureCallBack.onTakPicture(CaptureUtil.this.mTexureToBufferHelper.getTextureBufferCompress(i5));
                }
            });
        }
    }

    private void reverseBuf(ByteBuffer buf, int width, int height) {
        long ts = System.currentTimeMillis();
        int i = 0;
        byte[] tmp = new byte[(width * 4)];
        while (true) {
            int i2 = i + 1;
            if (i < height / 2) {
                buf.get(tmp);
                System.arraycopy(buf.array(), buf.limit() - buf.position(), buf.array(), buf.position() - (width * 4), width * 4);
                System.arraycopy(tmp, 0, buf.array(), buf.limit() - buf.position(), width * 4);
                i = i2;
            } else {
                buf.rewind();
                return;
            }
        }
    }

    public Bitmap getBitmap(Buffer buf, int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(buf);
        return bmp;
    }

    public void save(Bitmap bmp, String filename) {
    }
}
