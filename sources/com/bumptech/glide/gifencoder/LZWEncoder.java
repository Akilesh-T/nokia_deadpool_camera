package com.bumptech.glide.gifencoder;

import android.support.v4.app.FrameMetricsAggregator;
import android.support.v4.internal.view.SupportMenu;
import java.io.IOException;
import java.io.OutputStream;

class LZWEncoder {
    static final int BITS = 12;
    private static final int EOF = -1;
    static final int HSIZE = 5003;
    int ClearCode;
    int EOFCode;
    int a_count;
    byte[] accum = new byte[256];
    boolean clear_flg = false;
    int[] codetab = new int[HSIZE];
    private int curPixel;
    int cur_accum = 0;
    int cur_bits = 0;
    int free_ent = 0;
    int g_init_bits;
    int hsize = HSIZE;
    int[] htab = new int[HSIZE];
    private int imgH;
    private int imgW;
    private int initCodeSize;
    int[] masks = new int[]{0, 1, 3, 7, 15, 31, 63, 127, 255, FrameMetricsAggregator.EVERY_DURATION, 1023, 2047, 4095, 8191, 16383, 32767, SupportMenu.USER_MASK};
    int maxbits = 12;
    int maxcode;
    int maxmaxcode = 4096;
    int n_bits;
    private byte[] pixAry;
    private int remaining;

    LZWEncoder(int width, int height, byte[] pixels, int color_depth) {
        this.imgW = width;
        this.imgH = height;
        this.pixAry = pixels;
        this.initCodeSize = Math.max(2, color_depth);
    }

    /* Access modifiers changed, original: 0000 */
    public void char_out(byte c, OutputStream outs) throws IOException {
        byte[] bArr = this.accum;
        int i = this.a_count;
        this.a_count = i + 1;
        bArr[i] = c;
        if (this.a_count >= 254) {
            flush_char(outs);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void cl_block(OutputStream outs) throws IOException {
        cl_hash(this.hsize);
        this.free_ent = this.ClearCode + 2;
        this.clear_flg = true;
        output(this.ClearCode, outs);
    }

    /* Access modifiers changed, original: 0000 */
    public void cl_hash(int hsize) {
        for (int i = 0; i < hsize; i++) {
            this.htab[i] = -1;
        }
    }

    /* Access modifiers changed, original: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0098  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0089  */
    public void compress(int r10, java.io.OutputStream r11) throws java.io.IOException {
        /*
        r9 = this;
        r9.g_init_bits = r10;
        r0 = 0;
        r9.clear_flg = r0;
        r1 = r9.g_init_bits;
        r9.n_bits = r1;
        r1 = r9.n_bits;
        r1 = r9.MAXCODE(r1);
        r9.maxcode = r1;
        r1 = r10 + -1;
        r2 = 1;
        r1 = r2 << r1;
        r9.ClearCode = r1;
        r1 = r9.ClearCode;
        r1 = r1 + r2;
        r9.EOFCode = r1;
        r1 = r9.ClearCode;
        r1 = r1 + 2;
        r9.free_ent = r1;
        r9.a_count = r0;
        r0 = r9.nextPixel();
        r1 = 0;
        r2 = r9.hsize;
    L_0x002c:
        r3 = 65536; // 0x10000 float:9.18355E-41 double:3.2379E-319;
        if (r2 >= r3) goto L_0x0035;
    L_0x0030:
        r1 = r1 + 1;
        r2 = r2 * 2;
        goto L_0x002c;
    L_0x0035:
        r1 = 8 - r1;
        r3 = r9.hsize;
        r9.cl_hash(r3);
        r4 = r9.ClearCode;
        r9.output(r4, r11);
    L_0x0041:
        r4 = r9.nextPixel();
        r5 = r4;
        r6 = -1;
        if (r4 == r6) goto L_0x009c;
    L_0x0049:
        r4 = r9.maxbits;
        r4 = r5 << r4;
        r2 = r4 + r0;
        r4 = r5 << r1;
        r4 = r4 ^ r0;
        r6 = r9.htab;
        r6 = r6[r4];
        if (r6 != r2) goto L_0x005d;
    L_0x0058:
        r6 = r9.codetab;
        r0 = r6[r4];
        goto L_0x0041;
    L_0x005d:
        r6 = r9.htab;
        r6 = r6[r4];
        if (r6 < 0) goto L_0x007f;
    L_0x0063:
        r6 = r3 - r4;
        if (r4 != 0) goto L_0x0068;
    L_0x0067:
        r6 = 1;
    L_0x0068:
        r7 = r4 - r6;
        r4 = r7;
        if (r7 >= 0) goto L_0x006e;
    L_0x006d:
        r4 = r4 + r3;
    L_0x006e:
        r7 = r9.htab;
        r7 = r7[r4];
        if (r7 != r2) goto L_0x0079;
    L_0x0074:
        r7 = r9.codetab;
        r0 = r7[r4];
        goto L_0x0041;
    L_0x0079:
        r7 = r9.htab;
        r7 = r7[r4];
        if (r7 >= 0) goto L_0x0068;
    L_0x007f:
        r9.output(r0, r11);
        r0 = r5;
        r6 = r9.free_ent;
        r7 = r9.maxmaxcode;
        if (r6 >= r7) goto L_0x0098;
    L_0x0089:
        r6 = r9.codetab;
        r7 = r9.free_ent;
        r8 = r7 + 1;
        r9.free_ent = r8;
        r6[r4] = r7;
        r6 = r9.htab;
        r6[r4] = r2;
        goto L_0x0041;
    L_0x0098:
        r9.cl_block(r11);
        goto L_0x0041;
    L_0x009c:
        r9.output(r0, r11);
        r4 = r9.EOFCode;
        r9.output(r4, r11);
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bumptech.glide.gifencoder.LZWEncoder.compress(int, java.io.OutputStream):void");
    }

    /* Access modifiers changed, original: 0000 */
    public void encode(OutputStream os) throws IOException {
        os.write(this.initCodeSize);
        this.remaining = this.imgW * this.imgH;
        this.curPixel = 0;
        compress(this.initCodeSize + 1, os);
        os.write(0);
    }

    /* Access modifiers changed, original: 0000 */
    public void flush_char(OutputStream outs) throws IOException {
        if (this.a_count > 0) {
            outs.write(this.a_count);
            outs.write(this.accum, 0, this.a_count);
            this.a_count = 0;
        }
    }

    /* Access modifiers changed, original: final */
    public final int MAXCODE(int n_bits) {
        return (1 << n_bits) - 1;
    }

    private int nextPixel() {
        if (this.remaining == 0) {
            return -1;
        }
        this.remaining--;
        byte pix = this.pixAry;
        int i = this.curPixel;
        this.curPixel = i + 1;
        return pix[i] & 255;
    }

    /* Access modifiers changed, original: 0000 */
    public void output(int code, OutputStream outs) throws IOException {
        this.cur_accum &= this.masks[this.cur_bits];
        if (this.cur_bits > 0) {
            this.cur_accum |= code << this.cur_bits;
        } else {
            this.cur_accum = code;
        }
        this.cur_bits += this.n_bits;
        while (this.cur_bits >= 8) {
            char_out((byte) (this.cur_accum & 255), outs);
            this.cur_accum >>= 8;
            this.cur_bits -= 8;
        }
        if (this.free_ent > this.maxcode || this.clear_flg) {
            if (this.clear_flg) {
                int i = this.g_init_bits;
                this.n_bits = i;
                this.maxcode = MAXCODE(i);
                this.clear_flg = false;
            } else {
                this.n_bits++;
                if (this.n_bits == this.maxbits) {
                    this.maxcode = this.maxmaxcode;
                } else {
                    this.maxcode = MAXCODE(this.n_bits);
                }
            }
        }
        if (code == this.EOFCode) {
            while (this.cur_bits > 0) {
                char_out((byte) (this.cur_accum & 255), outs);
                this.cur_accum >>= 8;
                this.cur_bits -= 8;
            }
            flush_char(outs);
        }
    }
}
