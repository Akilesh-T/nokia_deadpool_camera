package com.bumptech.glide.gifdecoder;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Build.VERSION;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class GifDecoder {
    private static final Config BITMAP_CONFIG = Config.ARGB_8888;
    private static final int DISPOSAL_BACKGROUND = 2;
    private static final int DISPOSAL_NONE = 1;
    private static final int DISPOSAL_PREVIOUS = 3;
    private static final int DISPOSAL_UNSPECIFIED = 0;
    private static final int INITIAL_FRAME_POINTER = -1;
    private static final int MAX_STACK_SIZE = 4096;
    private static final int NULL_CODE = -1;
    public static final int STATUS_FORMAT_ERROR = 1;
    public static final int STATUS_OK = 0;
    public static final int STATUS_OPEN_ERROR = 2;
    public static final int STATUS_PARTIAL_DECODE = 3;
    private static final String TAG = GifDecoder.class.getSimpleName();
    private int[] act;
    private BitmapProvider bitmapProvider;
    private final byte[] block = new byte[256];
    private byte[] data;
    private int framePointer;
    private GifHeader header;
    private byte[] mainPixels;
    private int[] mainScratch;
    private GifHeaderParser parser;
    private byte[] pixelStack;
    private short[] prefix;
    private Bitmap previousImage;
    private ByteBuffer rawData;
    private boolean savePrevious;
    private int status;
    private byte[] suffix;

    public interface BitmapProvider {
        Bitmap obtain(int i, int i2, Config config);

        void release(Bitmap bitmap);
    }

    public GifDecoder(BitmapProvider provider) {
        this.bitmapProvider = provider;
        this.header = new GifHeader();
    }

    public int getWidth() {
        return this.header.width;
    }

    public int getHeight() {
        return this.header.height;
    }

    public byte[] getData() {
        return this.data;
    }

    public int getStatus() {
        return this.status;
    }

    public void advance() {
        this.framePointer = (this.framePointer + 1) % this.header.frameCount;
    }

    public int getDelay(int n) {
        if (n < 0 || n >= this.header.frameCount) {
            return -1;
        }
        return ((GifFrame) this.header.frames.get(n)).delay;
    }

    public int getNextDelay() {
        if (this.header.frameCount <= 0 || this.framePointer < 0) {
            return -1;
        }
        return getDelay(this.framePointer);
    }

    public int getFrameCount() {
        return this.header.frameCount;
    }

    public int getCurrentFrameIndex() {
        return this.framePointer;
    }

    public void resetFrameIndex() {
        this.framePointer = -1;
    }

    public int getLoopCount() {
        return this.header.loopCount;
    }

    /* JADX WARNING: Missing block: B:40:0x00ba, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:46:0x00dc, code skipped:
            return null;
     */
    public synchronized android.graphics.Bitmap getNextFrame() {
        /*
        r10 = this;
        monitor-enter(r10);
        r0 = r10.header;	 Catch:{ all -> 0x00dd }
        r0 = r0.frameCount;	 Catch:{ all -> 0x00dd }
        r1 = 3;
        r2 = 1;
        if (r0 <= 0) goto L_0x000d;
    L_0x0009:
        r0 = r10.framePointer;	 Catch:{ all -> 0x00dd }
        if (r0 >= 0) goto L_0x003b;
    L_0x000d:
        r0 = TAG;	 Catch:{ all -> 0x00dd }
        r0 = android.util.Log.isLoggable(r0, r1);	 Catch:{ all -> 0x00dd }
        if (r0 == 0) goto L_0x0039;
    L_0x0015:
        r0 = TAG;	 Catch:{ all -> 0x00dd }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00dd }
        r3.<init>();	 Catch:{ all -> 0x00dd }
        r4 = "unable to decode frame, frameCount=";
        r3.append(r4);	 Catch:{ all -> 0x00dd }
        r4 = r10.header;	 Catch:{ all -> 0x00dd }
        r4 = r4.frameCount;	 Catch:{ all -> 0x00dd }
        r3.append(r4);	 Catch:{ all -> 0x00dd }
        r4 = " framePointer=";
        r3.append(r4);	 Catch:{ all -> 0x00dd }
        r4 = r10.framePointer;	 Catch:{ all -> 0x00dd }
        r3.append(r4);	 Catch:{ all -> 0x00dd }
        r3 = r3.toString();	 Catch:{ all -> 0x00dd }
        android.util.Log.d(r0, r3);	 Catch:{ all -> 0x00dd }
    L_0x0039:
        r10.status = r2;	 Catch:{ all -> 0x00dd }
    L_0x003b:
        r0 = r10.status;	 Catch:{ all -> 0x00dd }
        r3 = 0;
        if (r0 == r2) goto L_0x00bb;
    L_0x0040:
        r0 = r10.status;	 Catch:{ all -> 0x00dd }
        r4 = 2;
        if (r0 != r4) goto L_0x0047;
    L_0x0045:
        goto L_0x00bb;
    L_0x0047:
        r0 = 0;
        r10.status = r0;	 Catch:{ all -> 0x00dd }
        r4 = r10.header;	 Catch:{ all -> 0x00dd }
        r4 = r4.frames;	 Catch:{ all -> 0x00dd }
        r5 = r10.framePointer;	 Catch:{ all -> 0x00dd }
        r4 = r4.get(r5);	 Catch:{ all -> 0x00dd }
        r4 = (com.bumptech.glide.gifdecoder.GifFrame) r4;	 Catch:{ all -> 0x00dd }
        r5 = 0;
        r6 = r10.framePointer;	 Catch:{ all -> 0x00dd }
        r6 = r6 - r2;
        if (r6 < 0) goto L_0x0067;
    L_0x005c:
        r7 = r10.header;	 Catch:{ all -> 0x00dd }
        r7 = r7.frames;	 Catch:{ all -> 0x00dd }
        r7 = r7.get(r6);	 Catch:{ all -> 0x00dd }
        r7 = (com.bumptech.glide.gifdecoder.GifFrame) r7;	 Catch:{ all -> 0x00dd }
        r5 = r7;
    L_0x0067:
        r7 = r4.lct;	 Catch:{ all -> 0x00dd }
        if (r7 != 0) goto L_0x0072;
    L_0x006b:
        r7 = r10.header;	 Catch:{ all -> 0x00dd }
        r7 = r7.gct;	 Catch:{ all -> 0x00dd }
        r10.act = r7;	 Catch:{ all -> 0x00dd }
        goto L_0x0082;
    L_0x0072:
        r7 = r4.lct;	 Catch:{ all -> 0x00dd }
        r10.act = r7;	 Catch:{ all -> 0x00dd }
        r7 = r10.header;	 Catch:{ all -> 0x00dd }
        r7 = r7.bgIndex;	 Catch:{ all -> 0x00dd }
        r8 = r4.transIndex;	 Catch:{ all -> 0x00dd }
        if (r7 != r8) goto L_0x0082;
    L_0x007e:
        r7 = r10.header;	 Catch:{ all -> 0x00dd }
        r7.bgColor = r0;	 Catch:{ all -> 0x00dd }
    L_0x0082:
        r7 = 0;
        r8 = r4.transparency;	 Catch:{ all -> 0x00dd }
        if (r8 == 0) goto L_0x0094;
    L_0x0087:
        r8 = r10.act;	 Catch:{ all -> 0x00dd }
        r9 = r4.transIndex;	 Catch:{ all -> 0x00dd }
        r8 = r8[r9];	 Catch:{ all -> 0x00dd }
        r7 = r8;
        r8 = r10.act;	 Catch:{ all -> 0x00dd }
        r9 = r4.transIndex;	 Catch:{ all -> 0x00dd }
        r8[r9] = r0;	 Catch:{ all -> 0x00dd }
    L_0x0094:
        r0 = r10.act;	 Catch:{ all -> 0x00dd }
        if (r0 != 0) goto L_0x00ab;
    L_0x0098:
        r0 = TAG;	 Catch:{ all -> 0x00dd }
        r0 = android.util.Log.isLoggable(r0, r1);	 Catch:{ all -> 0x00dd }
        if (r0 == 0) goto L_0x00a7;
    L_0x00a0:
        r0 = TAG;	 Catch:{ all -> 0x00dd }
        r1 = "No Valid Color Table";
        android.util.Log.d(r0, r1);	 Catch:{ all -> 0x00dd }
    L_0x00a7:
        r10.status = r2;	 Catch:{ all -> 0x00dd }
        monitor-exit(r10);
        return r3;
    L_0x00ab:
        r0 = r10.setPixels(r4, r5);	 Catch:{ all -> 0x00dd }
        r1 = r4.transparency;	 Catch:{ all -> 0x00dd }
        if (r1 == 0) goto L_0x00b9;
    L_0x00b3:
        r1 = r10.act;	 Catch:{ all -> 0x00dd }
        r2 = r4.transIndex;	 Catch:{ all -> 0x00dd }
        r1[r2] = r7;	 Catch:{ all -> 0x00dd }
    L_0x00b9:
        monitor-exit(r10);
        return r0;
    L_0x00bb:
        r0 = TAG;	 Catch:{ all -> 0x00dd }
        r0 = android.util.Log.isLoggable(r0, r1);	 Catch:{ all -> 0x00dd }
        if (r0 == 0) goto L_0x00db;
    L_0x00c3:
        r0 = TAG;	 Catch:{ all -> 0x00dd }
        r1 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00dd }
        r1.<init>();	 Catch:{ all -> 0x00dd }
        r2 = "Unable to decode frame, status=";
        r1.append(r2);	 Catch:{ all -> 0x00dd }
        r2 = r10.status;	 Catch:{ all -> 0x00dd }
        r1.append(r2);	 Catch:{ all -> 0x00dd }
        r1 = r1.toString();	 Catch:{ all -> 0x00dd }
        android.util.Log.d(r0, r1);	 Catch:{ all -> 0x00dd }
    L_0x00db:
        monitor-exit(r10);
        return r3;
    L_0x00dd:
        r0 = move-exception;
        monitor-exit(r10);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bumptech.glide.gifdecoder.GifDecoder.getNextFrame():android.graphics.Bitmap");
    }

    public int read(InputStream is, int contentLength) {
        if (is != null) {
            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(contentLength > 0 ? contentLength + 4096 : 16384);
                byte[] data = new byte[16384];
                while (true) {
                    int read = is.read(data, 0, data.length);
                    int nRead = read;
                    if (read == -1) {
                        break;
                    }
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                read(buffer.toByteArray());
            } catch (IOException e) {
                Log.w(TAG, "Error reading data from stream", e);
            }
        } else {
            this.status = 2;
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException e2) {
                Log.w(TAG, "Error closing stream", e2);
            }
        }
        return this.status;
    }

    public void clear() {
        this.header = null;
        this.data = null;
        this.mainPixels = null;
        this.mainScratch = null;
        if (this.previousImage != null) {
            this.bitmapProvider.release(this.previousImage);
        }
        this.previousImage = null;
    }

    public void setData(GifHeader header, byte[] data) {
        this.header = header;
        this.data = data;
        this.status = 0;
        this.framePointer = -1;
        this.rawData = ByteBuffer.wrap(data);
        this.rawData.rewind();
        this.rawData.order(ByteOrder.LITTLE_ENDIAN);
        this.savePrevious = false;
        for (GifFrame frame : header.frames) {
            if (frame.dispose == 3) {
                this.savePrevious = true;
                break;
            }
        }
        this.mainPixels = new byte[(header.width * header.height)];
        this.mainScratch = new int[(header.width * header.height)];
    }

    private GifHeaderParser getHeaderParser() {
        if (this.parser == null) {
            this.parser = new GifHeaderParser();
        }
        return this.parser;
    }

    public int read(byte[] data) {
        this.data = data;
        this.header = getHeaderParser().setData(data).parseHeader();
        if (data != null) {
            this.rawData = ByteBuffer.wrap(data);
            this.rawData.rewind();
            this.rawData.order(ByteOrder.LITTLE_ENDIAN);
            this.mainPixels = new byte[(this.header.width * this.header.height)];
            this.mainScratch = new int[(this.header.width * this.header.height)];
            this.savePrevious = false;
            for (GifFrame frame : this.header.frames) {
                if (frame.dispose == 3) {
                    this.savePrevious = true;
                    break;
                }
            }
        }
        return this.status;
    }

    private Bitmap setPixels(GifFrame currentFrame, GifFrame previousFrame) {
        int c;
        GifFrame gifFrame = currentFrame;
        GifFrame gifFrame2 = previousFrame;
        int width = this.header.width;
        int height = this.header.height;
        int[] dest = this.mainScratch;
        if (gifFrame2 != null && gifFrame2.dispose > 0) {
            if (gifFrame2.dispose == 2) {
                c = 0;
                if (!gifFrame.transparency) {
                    c = this.header.bgColor;
                }
                Arrays.fill(dest, c);
            } else if (gifFrame2.dispose == 3 && this.previousImage != null) {
                this.previousImage.getPixels(dest, 0, width, 0, 0, width, height);
            }
        }
        decodeBitmapData(currentFrame);
        int i = 0;
        int pass = 1;
        int inc = 8;
        int iline = 0;
        while (true) {
            c = i;
            if (c < gifFrame.ih) {
                int line = c;
                if (gifFrame.interlace) {
                    if (iline >= gifFrame.ih) {
                        pass++;
                        switch (pass) {
                            case 2:
                                iline = 4;
                                break;
                            case 3:
                                iline = 2;
                                inc = 4;
                                break;
                            case 4:
                                iline = 1;
                                inc = 2;
                                break;
                        }
                    }
                    line = iline;
                    iline += inc;
                }
                line += gifFrame.iy;
                if (line < this.header.height) {
                    int k = this.header.width * line;
                    i = gifFrame.ix + k;
                    int dlim = gifFrame.iw + i;
                    if (this.header.width + k < dlim) {
                        dlim = k + this.header.width;
                    }
                    int sx = gifFrame.iw * c;
                    while (i < dlim) {
                        int sx2 = sx + 1;
                        int c2 = this.act[this.mainPixels[sx] & 255];
                        if (c2 != 0) {
                            dest[i] = c2;
                        }
                        i++;
                        sx = sx2;
                    }
                }
                i = c + 1;
            } else {
                if ((this.savePrevious && gifFrame.dispose == 0) || gifFrame.dispose == 1) {
                    if (this.previousImage == null) {
                        this.previousImage = getNextBitmap();
                    }
                    this.previousImage.setPixels(dest, 0, width, 0, 0, width, height);
                }
                Bitmap result = getNextBitmap();
                result.setPixels(dest, 0, width, 0, 0, width, height);
                return result;
            }
        }
    }

    private void decodeBitmapData(GifFrame frame) {
        int npix;
        int i;
        int first;
        GifFrame gifFrame = frame;
        if (gifFrame != null) {
            this.rawData.position(gifFrame.bufferFrameStart);
        }
        if (gifFrame == null) {
            npix = this.header.width;
            i = this.header.height;
        } else {
            npix = gifFrame.iw;
            i = gifFrame.ih;
        }
        npix *= i;
        if (this.mainPixels == null || this.mainPixels.length < npix) {
            this.mainPixels = new byte[npix];
        }
        if (this.prefix == null) {
            this.prefix = new short[4096];
        }
        if (this.suffix == null) {
            this.suffix = new byte[4096];
        }
        if (this.pixelStack == null) {
            this.pixelStack = new byte[FragmentTransaction.TRANSIT_FRAGMENT_OPEN];
        }
        i = read();
        int i2 = 1;
        int clear = 1 << i;
        int endOfInformation = clear + 1;
        int available = clear + 2;
        int codeSize = i + 1;
        int codeMask = (1 << codeSize) - 1;
        int code = 0;
        while (code < clear) {
            this.prefix[code] = (short) 0;
            this.suffix[code] = (byte) code;
            code++;
        }
        int bi = 0;
        int pi = 0;
        int first2 = 0;
        int count = 0;
        int bits = 0;
        int datum = 0;
        int top = 0;
        code = -1;
        int available2 = available;
        available = 0;
        while (available < npix) {
            int i3;
            int i4 = 3;
            if (count == 0) {
                count = readBlock();
                if (count <= 0) {
                    this.status = 3;
                    i3 = i;
                    break;
                }
                bi = 0;
            }
            datum += (this.block[bi] & 255) << bits;
            bi += i2;
            count--;
            int first3 = first2;
            first2 = pi;
            pi = available;
            available = bits + 8;
            while (available >= codeSize) {
                int code2 = datum & codeMask;
                datum >>= codeSize;
                available -= codeSize;
                if (code2 != clear) {
                    int top2;
                    if (code2 > available2) {
                        this.status = i4;
                    } else if (code2 != endOfInformation) {
                        if (code == -1) {
                            top2 = top + 1;
                            this.pixelStack[top] = this.suffix[code2];
                            code = code2;
                            first3 = code2;
                            top = top2;
                            i2 = 1;
                            i4 = 3;
                        } else {
                            i2 = code2;
                            if (code2 >= available2) {
                                top2 = top + 1;
                                i3 = i;
                                first = first3;
                                this.pixelStack[top] = (byte) first;
                                i = code;
                            } else {
                                i3 = i;
                                first = first3;
                                i = code2;
                                top2 = top;
                            }
                            while (i >= clear) {
                                i4 = top2 + 1;
                                int first4 = first;
                                this.pixelStack[top2] = this.suffix[i];
                                i = this.prefix[i];
                                top2 = i4;
                                first = first4;
                            }
                            first = this.suffix[i] & 255;
                            i4 = top2 + 1;
                            int code3 = i;
                            this.pixelStack[top2] = (byte) first;
                            if (available2 < 4096) {
                                this.prefix[available2] = (short) code;
                                this.suffix[available2] = (byte) first;
                                available2++;
                                if ((available2 & codeMask) == 0) {
                                    if (available2 < 4096) {
                                        codeSize++;
                                        codeMask += available2;
                                    }
                                }
                            }
                            code = i2;
                            top = i4;
                            while (top > 0) {
                                top--;
                                i4 = first2 + 1;
                                this.mainPixels[first2] = this.pixelStack[top];
                                pi++;
                                first2 = i4;
                            }
                            first3 = first;
                            i = i3;
                            first = frame;
                            i2 = 1;
                            i4 = 3;
                        }
                    }
                    top2 = code2;
                    bits = available;
                    available = pi;
                    pi = first2;
                    first2 = first3;
                    break;
                }
                codeSize = i + 1;
                codeMask = (i2 << codeSize) - 1;
                available2 = clear + 2;
                code = -1;
            }
            bits = available;
            available = pi;
            pi = first2;
            first2 = first3;
        }
        for (first = pi; first < npix; first++) {
            this.mainPixels[first] = (byte) 0;
        }
    }

    private int read() {
        try {
            return this.rawData.get() & 255;
        } catch (Exception e) {
            this.status = 1;
            return 0;
        }
    }

    private int readBlock() {
        int blockSize = read();
        int n = 0;
        if (blockSize > 0) {
            while (n < blockSize) {
                int count = blockSize - n;
                try {
                    this.rawData.get(this.block, n, count);
                    n += count;
                } catch (Exception e) {
                    Log.w(TAG, "Error Reading Block", e);
                    this.status = 1;
                }
            }
        }
        return n;
    }

    private Bitmap getNextBitmap() {
        Bitmap result = this.bitmapProvider.obtain(this.header.width, this.header.height, BITMAP_CONFIG);
        if (result == null) {
            result = Bitmap.createBitmap(this.header.width, this.header.height, BITMAP_CONFIG);
        }
        setAlpha(result);
        return result;
    }

    @TargetApi(12)
    private static void setAlpha(Bitmap bitmap) {
        if (VERSION.SDK_INT >= 12) {
            bitmap.setHasAlpha(true);
        }
    }
}
