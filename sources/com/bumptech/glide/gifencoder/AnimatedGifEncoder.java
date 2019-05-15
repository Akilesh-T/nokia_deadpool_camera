package com.bumptech.glide.gifencoder;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class AnimatedGifEncoder {
    private static final double MIN_TRANSPARENT_PERCENTAGE = 4.0d;
    private static final String TAG = "AnimatedGifEncoder";
    private boolean closeStream = false;
    private int colorDepth;
    private byte[] colorTab;
    private int delay = 0;
    private int dispose = -1;
    private boolean firstFrame = true;
    private boolean hasTransparentPixels;
    private int height;
    private Bitmap image;
    private byte[] indexedPixels;
    private OutputStream out;
    private int palSize = 7;
    private byte[] pixels;
    private int repeat = -1;
    private int sample = 10;
    private boolean sizeSet = false;
    private boolean started = false;
    private int transIndex;
    private Integer transparent = null;
    private boolean[] usedEntry = new boolean[256];
    private int width;

    public void setDelay(int ms) {
        this.delay = Math.round(((float) ms) / 10.0f);
    }

    public void setDispose(int code) {
        if (code >= 0) {
            this.dispose = code;
        }
    }

    public void setRepeat(int iter) {
        if (iter >= 0) {
            this.repeat = iter;
        }
    }

    public void setTransparent(int color) {
        this.transparent = Integer.valueOf(color);
    }

    public boolean addFrame(Bitmap im) {
        if (im == null || !this.started) {
            return false;
        }
        boolean ok = true;
        try {
            if (!this.sizeSet) {
                setSize(im.getWidth(), im.getHeight());
            }
            this.image = im;
            getImagePixels();
            analyzePixels();
            if (this.firstFrame) {
                writeLSD();
                writePalette();
                if (this.repeat >= 0) {
                    writeNetscapeExt();
                }
            }
            writeGraphicCtrlExt();
            writeImageDesc();
            if (!this.firstFrame) {
                writePalette();
            }
            writePixels();
            this.firstFrame = false;
        } catch (IOException e) {
            ok = false;
        }
        return ok;
    }

    public boolean finish() {
        if (!this.started) {
            return false;
        }
        boolean ok = true;
        this.started = false;
        try {
            this.out.write(59);
            this.out.flush();
            if (this.closeStream) {
                this.out.close();
            }
        } catch (IOException e) {
            ok = false;
        }
        this.transIndex = 0;
        this.out = null;
        this.image = null;
        this.pixels = null;
        this.indexedPixels = null;
        this.colorTab = null;
        this.closeStream = false;
        this.firstFrame = true;
        return ok;
    }

    public void setFrameRate(float fps) {
        if (fps != 0.0f) {
            this.delay = Math.round(100.0f / fps);
        }
    }

    public void setQuality(int quality) {
        if (quality < 1) {
            quality = 1;
        }
        this.sample = quality;
    }

    public void setSize(int w, int h) {
        if (!this.started || this.firstFrame) {
            this.width = w;
            this.height = h;
            if (this.width < 1) {
                this.width = 320;
            }
            if (this.height < 1) {
                this.height = 240;
            }
            this.sizeSet = true;
        }
    }

    public boolean start(OutputStream os) {
        if (os == null) {
            return false;
        }
        boolean ok = true;
        this.closeStream = false;
        this.out = os;
        try {
            writeString("GIF89a");
        } catch (IOException e) {
            ok = false;
        }
        this.started = ok;
        return ok;
    }

    public boolean start(String file) {
        boolean ok = true;
        try {
            this.out = new BufferedOutputStream(new FileOutputStream(file));
            ok = start(this.out);
            this.closeStream = true;
        } catch (IOException e) {
            ok = false;
        }
        this.started = ok;
        return ok;
    }

    private void analyzePixels() {
        int i;
        int len = this.pixels.length;
        int nPix = len / 3;
        this.indexedPixels = new byte[nPix];
        NeuQuant nq = new NeuQuant(this.pixels, len, this.sample);
        this.colorTab = nq.process();
        for (i = 0; i < this.colorTab.length; i += 3) {
            byte temp = this.colorTab[i];
            this.colorTab[i] = this.colorTab[i + 2];
            this.colorTab[i + 2] = temp;
            this.usedEntry[i / 3] = false;
        }
        int k = 0;
        i = 0;
        while (i < nPix) {
            int k2 = k + 1;
            int k3 = k2 + 1;
            int k4 = k3 + 1;
            k = nq.map(this.pixels[k] & 255, this.pixels[k2] & 255, this.pixels[k3] & 255);
            this.usedEntry[k] = true;
            this.indexedPixels[i] = (byte) k;
            i++;
            k = k4;
        }
        this.pixels = null;
        this.colorDepth = 8;
        this.palSize = 7;
        if (this.transparent != null) {
            this.transIndex = findClosest(this.transparent.intValue());
        } else if (this.hasTransparentPixels) {
            this.transIndex = findClosest(0);
        }
    }

    private int findClosest(int color) {
        if (this.colorTab == null) {
            return -1;
        }
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int minpos = 0;
        int dmin = 16777216;
        int len = this.colorTab.length;
        int i = 0;
        while (i < len) {
            int i2 = i + 1;
            i = r - (this.colorTab[i] & 255);
            int i3 = i2 + 1;
            int dg = g - (this.colorTab[i2] & 255);
            i2 = b - (this.colorTab[i3] & 255);
            int d = ((i * i) + (dg * dg)) + (i2 * i2);
            int index = i3 / 3;
            if (this.usedEntry[index] && d < dmin) {
                dmin = d;
                minpos = index;
            }
            i = i3 + 1;
        }
        return minpos;
    }

    private void getImagePixels() {
        int w = this.image.getWidth();
        int h = this.image.getHeight();
        if (!(w == this.width && h == this.height)) {
            Bitmap temp = Bitmap.createBitmap(this.width, this.height, Config.ARGB_8888);
            new Canvas(temp).drawBitmap(temp, 0.0f, 0.0f, null);
            this.image = temp;
        }
        int[] pixelsInt = new int[(w * h)];
        boolean z = false;
        this.image.getPixels(pixelsInt, 0, w, 0, 0, w, h);
        this.pixels = new byte[(pixelsInt.length * 3)];
        this.hasTransparentPixels = false;
        int totalTransparentPixels = 0;
        int length = pixelsInt.length;
        int pixelsIndex = 0;
        int pixelsIndex2 = 0;
        while (pixelsIndex2 < length) {
            int pixel = pixelsInt[pixelsIndex2];
            if (pixel == 0) {
                totalTransparentPixels++;
            }
            int pixelsIndex3 = pixelsIndex + 1;
            this.pixels[pixelsIndex] = (byte) (pixel & 255);
            int pixelsIndex4 = pixelsIndex3 + 1;
            this.pixels[pixelsIndex3] = (byte) ((pixel >> 8) & 255);
            pixelsIndex3 = pixelsIndex4 + 1;
            this.pixels[pixelsIndex4] = (byte) ((pixel >> 16) & 255);
            pixelsIndex2++;
            pixelsIndex = pixelsIndex3;
        }
        double transparentPercentage = ((double) (100 * totalTransparentPixels)) / ((double) pixelsInt.length);
        if (transparentPercentage > MIN_TRANSPARENT_PERCENTAGE) {
            z = true;
        }
        this.hasTransparentPixels = z;
        if (Log.isLoggable(TAG, 3)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("got pixels for frame with ");
            stringBuilder.append(transparentPercentage);
            stringBuilder.append("% transparent pixels");
            Log.d(str, stringBuilder.toString());
        }
    }

    private void writeGraphicCtrlExt() throws IOException {
        int transp;
        int disp;
        this.out.write(33);
        this.out.write(249);
        this.out.write(4);
        if (this.transparent != null || this.hasTransparentPixels) {
            transp = 1;
            disp = 2;
        } else {
            transp = 0;
            disp = 0;
        }
        if (this.dispose >= 0) {
            disp = this.dispose & 7;
        }
        this.out.write(((0 | (disp << 2)) | 0) | transp);
        writeShort(this.delay);
        this.out.write(this.transIndex);
        this.out.write(0);
    }

    private void writeImageDesc() throws IOException {
        this.out.write(44);
        writeShort(0);
        writeShort(0);
        writeShort(this.width);
        writeShort(this.height);
        if (this.firstFrame) {
            this.out.write(0);
        } else {
            this.out.write(128 | this.palSize);
        }
    }

    private void writeLSD() throws IOException {
        writeShort(this.width);
        writeShort(this.height);
        this.out.write(this.palSize | 240);
        this.out.write(0);
        this.out.write(0);
    }

    private void writeNetscapeExt() throws IOException {
        this.out.write(33);
        this.out.write(255);
        this.out.write(11);
        writeString("NETSCAPE2.0");
        this.out.write(3);
        this.out.write(1);
        writeShort(this.repeat);
        this.out.write(0);
    }

    private void writePalette() throws IOException {
        this.out.write(this.colorTab, 0, this.colorTab.length);
        int n = 768 - this.colorTab.length;
        for (int i = 0; i < n; i++) {
            this.out.write(0);
        }
    }

    private void writePixels() throws IOException {
        new LZWEncoder(this.width, this.height, this.indexedPixels, this.colorDepth).encode(this.out);
    }

    private void writeShort(int value) throws IOException {
        this.out.write(value & 255);
        this.out.write((value >> 8) & 255);
    }

    private void writeString(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            this.out.write((byte) s.charAt(i));
        }
    }
}
