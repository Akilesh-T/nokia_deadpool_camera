package com.hmdglobal.app.camera.beauty.cameragl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v4.internal.view.SupportMenu;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class LandMarkView extends View {
    public static volatile LandMarkView markViewDrawYUV;
    private Bitmap bmp;
    private boolean drawYUV;
    private int[] points;
    private boolean visible;

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        invalidate();
    }

    public void setYUV(byte[] data, int width, int height) {
        if (this.visible) {
            int[] mIntArray = new int[(width * height)];
            decodeYUV420SP(mIntArray, data, width, height);
            this.bmp = Bitmap.createBitmap(mIntArray, width, height, Config.ARGB_8888);
            invalidate();
        }
    }

    public void setPoints(int[] points) {
        if (this.visible) {
            this.points = points;
            invalidate();
        }
    }

    public LandMarkView(Context context) {
        super(context);
        this.points = new int[162];
        this.drawYUV = false;
    }

    public LandMarkView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.points = new int[162];
        this.drawYUV = false;
    }

    public LandMarkView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.points = new int[162];
        this.drawYUV = false;
    }

    /* Access modifiers changed, original: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        markViewDrawYUV = this;
    }

    /* Access modifiers changed, original: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        markViewDrawYUV = null;
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.visible) {
            int i = 0;
            if (this.drawYUV) {
                if (this.bmp != null) {
                    canvas.drawBitmap(this.bmp, 0.0f, 0.0f, null);
                }
                Paint paint = new Paint();
                paint.setStrokeWidth(10.0f);
                paint.setColor(SupportMenu.CATEGORY_MASK);
                while (i < this.points.length) {
                    canvas.drawPoint((float) this.points[i], (float) this.points[i + 1], paint);
                    i += 2;
                }
            } else {
                Matrix m = new Matrix();
                m.postRotate(270.0f, 0.0f, 0.0f);
                m.postTranslate(0.0f, (float) canvas.getHeight());
                m.postScale(-1.0f, 1.0f);
                m.postTranslate((float) canvas.getWidth(), 0.0f);
                canvas.setMatrix(m);
                if (this.bmp != null) {
                    canvas.drawBitmap(this.bmp, 0.0f, 0.0f, null);
                }
                Paint paint2 = new Paint();
                paint2.setStrokeWidth(10.0f);
                paint2.setColor(SupportMenu.CATEGORY_MASK);
                while (i < this.points.length) {
                    canvas.drawPoint((float) this.points[i], (float) this.points[i + 1], paint2);
                    i += 2;
                }
            }
        }
    }

    public static void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        byte[] bArr = yuv420sp;
        int i = width;
        int i2 = height;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("decodeYUV420SP width is ");
        stringBuilder.append(i);
        Log.e("wangshuai", stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("decodeYUV420SP height is ");
        stringBuilder.append(i2);
        Log.e("wangshuai", stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("decodeYUV420SP yuv420sp is ");
        stringBuilder.append(bArr.length);
        Log.e("wangshuai", stringBuilder.toString());
        int frameSize = i * i2;
        int j = 0;
        int yp = 0;
        while (j < i2) {
            int uvp = ((j >> 1) * i) + frameSize;
            int u = 0;
            int v = 0;
            int yp2 = yp;
            yp = 0;
            while (yp < i) {
                int uvp2;
                int y = (bArr[yp2] & 255) - 16;
                if (y < 0) {
                    y = 0;
                }
                if ((yp & 1) == 0) {
                    uvp2 = uvp + 1;
                    u = (bArr[uvp2] & 255) - 128;
                    int i3 = uvp2 + 1;
                    v = (bArr[uvp] & 255) - 128;
                    uvp = i3;
                }
                uvp2 = 1192 * y;
                int r = (1634 * v) + uvp2;
                int g = (uvp2 - (833 * v)) - (400 * u);
                int b = uvp2 + (2066 * u);
                if (r < 0) {
                    r = 0;
                } else if (r > 262143) {
                    r = 262143;
                }
                if (g < 0) {
                    g = 0;
                } else if (g > 262143) {
                    g = 262143;
                }
                if (b < 0) {
                    b = 0;
                } else if (b > 262143) {
                    b = 262143;
                }
                rgb[yp2] = ((b >> 10) & 255) | ((ViewCompat.MEASURED_STATE_MASK | ((r << 6) & 16711680)) | ((g >> 2) & MotionEventCompat.ACTION_POINTER_INDEX_MASK));
                yp++;
                yp2++;
                bArr = yuv420sp;
                i = width;
            }
            j++;
            yp = yp2;
            bArr = yuv420sp;
            i = width;
        }
    }

    public static Matrix reverseH() {
        Matrix matrix = new Matrix();
        matrix.setValues(new float[]{-1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f});
        return matrix;
    }
}
