package com.hmdglobal.app.camera.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.renderscript.Allocation;
import android.renderscript.Allocation.MipmapControl;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type.Builder;

public class NV21Convertor {
    private Context mContext;
    private RenderScript mRS = RenderScript.create(this.mContext);
    private ScriptIntrinsicYuvToRGB mScript = ScriptIntrinsicYuvToRGB.create(this.mRS, Element.U8_4(this.mRS));

    public NV21Convertor(Context context) {
        this.mContext = context;
    }

    public Bitmap convertNV21ToBitmap(byte[] nv21Data, int width, int height) {
        Builder inputBuilder = new Builder(this.mRS, Element.U8(this.mRS));
        inputBuilder.setX(nv21Data.length);
        Allocation inputAllocation = Allocation.createTyped(this.mRS, inputBuilder.create(), MipmapControl.MIPMAP_NONE, 1);
        inputAllocation.copyFrom(nv21Data);
        this.mScript.setInput(inputAllocation);
        Builder outputBuilder = new Builder(this.mRS, Element.RGBA_8888(this.mRS));
        outputBuilder.setX(width);
        outputBuilder.setY(height);
        Allocation outputAllocation = Allocation.createTyped(this.mRS, outputBuilder.create(), MipmapControl.MIPMAP_NONE, 1);
        this.mScript.forEach(outputAllocation);
        Bitmap bmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        outputAllocation.copyTo(bmp);
        inputAllocation.destroy();
        outputAllocation.destroy();
        return bmp;
    }

    @Deprecated
    public void release() {
    }
}
