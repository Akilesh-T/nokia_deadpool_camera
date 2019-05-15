package com.hmdglobal.app.camera.beauty.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicResize;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type.Builder;

public class ScriptInsics {

    @TargetApi(20)
    public static class YUVScaler {
        private Allocation in;
        private byte[] inData;
        private int inH;
        private int inW;
        private Allocation out;
        private byte[] outData;
        private int outH;
        private int outW;
        RenderScript rs;
        private ScriptIntrinsicResize sIns;

        public void setIn(byte[] inData, int inW, int inH) {
            this.inData = inData;
            this.inW = inW;
            this.inH = inH;
        }

        public void setOut(byte[] outData, int outW, int outH) {
            this.outData = outData;
            this.outW = outW;
            this.outH = outH;
        }

        public void process(Context context) {
            if (this.sIns == null) {
                this.rs = RenderScript.create(context);
                this.sIns = ScriptIntrinsicResize.create(this.rs);
                Builder yuvTypeBuilder = new Builder(this.rs, Element.U8(this.rs));
                yuvTypeBuilder.setX(this.inW);
                yuvTypeBuilder.setY(this.inH);
                yuvTypeBuilder.setZ(1);
                this.in = Allocation.createTyped(this.rs, yuvTypeBuilder.create(), 33);
                yuvTypeBuilder.setX(this.outW);
                yuvTypeBuilder.setY(this.outH);
                yuvTypeBuilder.setZ(1);
                this.out = Allocation.createTyped(this.rs, yuvTypeBuilder.create(), 1);
            }
            this.in.copyFrom(this.inData);
            this.sIns.setInput(this.in);
            this.sIns.forEach_bicubic(this.out);
            this.out.copyTo(this.outData);
        }
    }

    public static class YUV_2_RGB_Decoder {
        Allocation in;
        Allocation out;
        RenderScript rs;
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;

        public void GPUDecodeYUV420SPRGB(Context context, byte[] rgb, byte[] yuv, int W, int H) {
            if (this.yuvToRgbIntrinsic == null) {
                this.rs = RenderScript.create(context);
                this.yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(this.rs, Element.U8_4(this.rs));
                this.in = Allocation.createTyped(this.rs, new Builder(this.rs, Element.U8(this.rs)).setX(yuv.length).create(), 1);
                this.out = Allocation.createTyped(this.rs, new Builder(this.rs, Element.RGBA_8888(this.rs)).setX(W).setY(H).create(), 1);
            }
            this.in.copyFrom(yuv);
            this.yuvToRgbIntrinsic.setInput(this.in);
            this.yuvToRgbIntrinsic.forEach(this.out);
            this.out.copyTo(rgb);
        }
    }

    public static YUVScaler createYUVScaler() {
        return new YUVScaler();
    }

    public static YUV_2_RGB_Decoder createNV21_2RGB() {
        return new YUV_2_RGB_Decoder();
    }
}
