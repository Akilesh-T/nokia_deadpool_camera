package com.hmdglobal.app.camera.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.renderscript.Allocation;
import android.renderscript.Allocation.MipmapControl;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v4.view.ViewCompat;

public class BlurUtil {
    static RenderScript mRS;
    static ScriptIntrinsicBlur mScriptBlur;

    public static void initialize(Context context) {
        mRS = RenderScript.create(context);
        mScriptBlur = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS));
    }

    public static Bitmap blur(Bitmap input) {
        Bitmap output = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Config.ARGB_8888);
        Allocation inputAllocation = Allocation.createFromBitmap(mRS, input, MipmapControl.MIPMAP_NONE, 1);
        Allocation outputAllocation = Allocation.createFromBitmap(mRS, output);
        mScriptBlur.setInput(inputAllocation);
        mScriptBlur.setRadius(25.0f);
        mScriptBlur.forEach(outputAllocation);
        outputAllocation.copyTo(output);
        inputAllocation.destroy();
        outputAllocation.destroy();
        input.recycle();
        return output;
    }

    public static Bitmap cropBitmap(Bitmap input, int offsetY) {
        Bitmap output = Bitmap.createBitmap(input, 0, offsetY, input.getWidth(), input.getHeight() - offsetY, null, false);
        input.recycle();
        return output;
    }

    public static Bitmap getSuitableMap(Bitmap src, int offset) {
        Bitmap b = Bitmap.createBitmap(src.getWidth(), src.getHeight() + offset, Config.ARGB_8888);
        new Canvas(b).drawBitmap(src, 0.0f, (float) offset, new Paint());
        src.recycle();
        return b;
    }

    public static void fixBlackRect(Bitmap src, int height) {
        Canvas c = new Canvas(src);
        Paint p = new Paint();
        p.setColor(ViewCompat.MEASURED_STATE_MASK);
        c.drawRect(0.0f, 0.0f, (float) src.getWidth(), (float) (src.getHeight() - height), p);
    }

    public static Bitmap scaleBitmap(Bitmap input, float ratioWidth, float ratioHeight) {
        int width = input.getWidth();
        int height = input.getHeight();
        Matrix matrix = new Matrix();
        matrix.preScale(ratioWidth, ratioHeight);
        Bitmap output = Bitmap.createBitmap(input, 0, 0, width, height, matrix, false);
        if (output.equals(input)) {
            return output;
        }
        input.recycle();
        return output;
    }

    @Deprecated
    public static void destroy() {
    }
}
