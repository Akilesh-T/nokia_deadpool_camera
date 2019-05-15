package com.bumptech.glide.gifencoder;

class NeuQuant {
    protected static final int alphabiasshift = 10;
    protected static final int alpharadbias = 262144;
    protected static final int alpharadbshift = 18;
    protected static final int beta = 64;
    protected static final int betagamma = 65536;
    protected static final int betashift = 10;
    protected static final int gamma = 1024;
    protected static final int gammashift = 10;
    protected static final int initalpha = 1024;
    protected static final int initrad = 32;
    protected static final int initradius = 2048;
    protected static final int intbias = 65536;
    protected static final int intbiasshift = 16;
    protected static final int maxnetpos = 255;
    protected static final int minpicturebytes = 1509;
    protected static final int ncycles = 100;
    protected static final int netbiasshift = 4;
    protected static final int netsize = 256;
    protected static final int prime1 = 499;
    protected static final int prime2 = 491;
    protected static final int prime3 = 487;
    protected static final int prime4 = 503;
    protected static final int radbias = 256;
    protected static final int radbiasshift = 8;
    protected static final int radiusbias = 64;
    protected static final int radiusbiasshift = 6;
    protected static final int radiusdec = 30;
    protected int alphadec;
    protected int[] bias = new int[256];
    protected int[] freq = new int[256];
    protected int lengthcount;
    protected int[] netindex = new int[256];
    protected int[][] network;
    protected int[] radpower = new int[32];
    protected int samplefac;
    protected byte[] thepicture;

    public NeuQuant(byte[] thepic, int len, int sample) {
        this.thepicture = thepic;
        this.lengthcount = len;
        this.samplefac = sample;
        this.network = new int[256][];
        for (int i = 0; i < 256; i++) {
            this.network[i] = new int[4];
            int[] p = this.network[i];
            int i2 = (i << 12) / 256;
            p[2] = i2;
            p[1] = i2;
            p[0] = i2;
            this.freq[i] = 256;
            this.bias[i] = 0;
        }
    }

    public byte[] colorMap() {
        int i;
        byte[] map = new byte[768];
        int[] index = new int[256];
        for (i = 0; i < 256; i++) {
            index[this.network[i][3]] = i;
        }
        int k = 0;
        i = 0;
        while (i < 256) {
            int j = index[i];
            int k2 = k + 1;
            map[k] = (byte) this.network[j][0];
            k = k2 + 1;
            map[k2] = (byte) this.network[j][1];
            k2 = k + 1;
            map[k] = (byte) this.network[j][2];
            i++;
            k = k2;
        }
        return map;
    }

    public void inxbuild() {
        int startpos = 0;
        int previouscol = 0;
        for (int i = 0; i < 256; i++) {
            int j;
            int[] p = this.network[i];
            int smallpos = i;
            int smallval = p[1];
            for (j = i + 1; j < 256; j++) {
                int[] q = this.network[j];
                if (q[1] < smallval) {
                    smallpos = j;
                    smallval = q[1];
                }
            }
            int[] q2 = this.network[smallpos];
            if (i != smallpos) {
                j = q2[0];
                q2[0] = p[0];
                p[0] = j;
                j = q2[1];
                q2[1] = p[1];
                p[1] = j;
                j = q2[2];
                q2[2] = p[2];
                p[2] = j;
                j = q2[3];
                q2[3] = p[3];
                p[3] = j;
            }
            if (smallval != previouscol) {
                this.netindex[previouscol] = (startpos + i) >> 1;
                for (j = previouscol + 1; j < smallval; j++) {
                    this.netindex[j] = i;
                }
                previouscol = smallval;
                startpos = i;
            }
        }
        this.netindex[previouscol] = (startpos + 255) >> 1;
        for (int j2 = previouscol + 1; j2 < 256; j2++) {
            this.netindex[j2] = 255;
        }
    }

    public void learn() {
        int i;
        int i2 = this.lengthcount;
        int step = minpicturebytes;
        if (i2 < minpicturebytes) {
            this.samplefac = 1;
        }
        this.alphadec = 30 + ((this.samplefac - 1) / 3);
        byte[] p = this.thepicture;
        int lim = this.lengthcount;
        int samplepixels = this.lengthcount / (3 * this.samplefac);
        int delta = samplepixels / 100;
        int rad = 2048 >> 6;
        if (rad <= 1) {
            rad = 0;
        }
        for (i = 0; i < rad; i++) {
            this.radpower[i] = ((((rad * rad) - (i * i)) * 256) / (rad * rad)) * 1024;
        }
        if (this.lengthcount < minpicturebytes) {
            step = 3;
        } else if (this.lengthcount % prime1 != 0) {
            step = 1497;
        } else if (this.lengthcount % prime2 != 0) {
            step = 1473;
        } else if (this.lengthcount % prime3 != 0) {
            step = 1461;
        }
        int step2 = step;
        int pix = 0;
        i = 0;
        int delta2 = delta;
        int alpha = 1024;
        int radius = 2048;
        int rad2 = rad;
        while (i < samplepixels) {
            rad = (p[pix + 0] & 255) << 4;
            int i3 = (p[pix + 1] & 255) << 4;
            int r = (p[pix + 2] & 255) << 4;
            int j = contest(rad, i3, r);
            int r2 = r;
            int g = i3;
            int b = rad;
            altersingle(alpha, j, rad, i3, r2);
            if (rad2 != 0) {
                alterneigh(rad2, j, b, g, r2);
            }
            pix += step2;
            if (pix >= lim) {
                pix -= this.lengthcount;
            }
            i++;
            if (delta2 == 0) {
                delta2 = 1;
            }
            if (i % delta2 == 0) {
                alpha -= alpha / this.alphadec;
                radius -= radius / 30;
                i2 = radius >> 6;
                if (i2 <= 1) {
                    i2 = 0;
                }
                rad2 = i2;
                for (i2 = 0; i2 < rad2; i2++) {
                    this.radpower[i2] = ((((rad2 * rad2) - (i2 * i2)) * 256) / (rad2 * rad2)) * alpha;
                }
            }
        }
    }

    public int map(int b, int g, int r) {
        int bestd = 1000;
        int best = -1;
        int i = this.netindex[g];
        int j = i - 1;
        while (true) {
            if (i >= 256 && j < 0) {
                return best;
            }
            int[] p;
            if (i < 256) {
                p = this.network[i];
                int dist = p[1] - g;
                if (dist >= bestd) {
                    i = 256;
                } else {
                    i++;
                    if (dist < 0) {
                        dist = -dist;
                    }
                    int a = p[0] - b;
                    if (a < 0) {
                        a = -a;
                    }
                    dist += a;
                    if (dist < bestd) {
                        int a2 = p[2] - r;
                        if (a2 < 0) {
                            a2 = -a2;
                        }
                        dist += a2;
                        if (dist < bestd) {
                            bestd = dist;
                            best = p[3];
                        }
                    }
                }
            }
            if (j >= 0) {
                p = this.network[j];
                int dist2 = g - p[1];
                if (dist2 >= bestd) {
                    j = -1;
                } else {
                    j--;
                    if (dist2 < 0) {
                        dist2 = -dist2;
                    }
                    int a3 = p[0] - b;
                    if (a3 < 0) {
                        a3 = -a3;
                    }
                    dist2 += a3;
                    if (dist2 < bestd) {
                        int a4 = p[2] - r;
                        if (a4 < 0) {
                            a4 = -a4;
                        }
                        dist2 += a4;
                        if (dist2 < bestd) {
                            bestd = dist2;
                            best = p[3];
                        }
                    }
                }
            }
        }
    }

    public byte[] process() {
        learn();
        unbiasnet();
        inxbuild();
        return colorMap();
    }

    public void unbiasnet() {
        for (int i = 0; i < 256; i++) {
            int[] iArr = this.network[i];
            iArr[0] = iArr[0] >> 4;
            iArr = this.network[i];
            iArr[1] = iArr[1] >> 4;
            iArr = this.network[i];
            iArr[2] = iArr[2] >> 4;
            this.network[i][3] = i;
        }
    }

    /* Access modifiers changed, original: protected */
    public void alterneigh(int rad, int i, int b, int g, int r) {
        int lo = i - rad;
        if (lo < -1) {
            lo = -1;
        }
        int lo2 = lo;
        lo = i + rad;
        if (lo > 256) {
            lo = 256;
        }
        int hi = lo;
        int k = i - 1;
        int k2 = i + 1;
        lo = 1;
        while (true) {
            if (k2 < hi || k > lo2) {
                int j;
                int m = lo + 1;
                int a = this.radpower[lo];
                if (k2 < hi) {
                    j = k2 + 1;
                    int[] p = this.network[k2];
                    try {
                        p[0] = p[0] - (((p[0] - b) * a) / 262144);
                        p[1] = p[1] - (((p[1] - g) * a) / 262144);
                        p[2] = p[2] - (((p[2] - r) * a) / 262144);
                    } catch (Exception e) {
                    }
                    k2 = j;
                }
                if (k > lo2) {
                    j = k - 1;
                    int[] p2 = this.network[k];
                    try {
                        p2[0] = p2[0] - (((p2[0] - b) * a) / 262144);
                        p2[1] = p2[1] - (((p2[1] - g) * a) / 262144);
                        p2[2] = p2[2] - (((p2[2] - r) * a) / 262144);
                    } catch (Exception e2) {
                    }
                    lo = m;
                    k = j;
                } else {
                    lo = m;
                }
            } else {
                return;
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public void altersingle(int alpha, int i, int b, int g, int r) {
        int[] n = this.network[i];
        n[0] = n[0] - (((n[0] - b) * alpha) / 1024);
        n[1] = n[1] - (((n[1] - g) * alpha) / 1024);
        n[2] = n[2] - (((n[2] - r) * alpha) / 1024);
    }

    /* Access modifiers changed, original: protected */
    public int contest(int b, int g, int r) {
        int bestd = ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        int bestpos = -1;
        int bestbiaspos = -1;
        int bestbiasd = bestd;
        for (int i = 0; i < 256; i++) {
            int[] n = this.network[i];
            int dist = n[0] - b;
            if (dist < 0) {
                dist = -dist;
            }
            int a = n[1] - g;
            if (a < 0) {
                a = -a;
            }
            dist += a;
            int a2 = n[2] - r;
            if (a2 < 0) {
                a2 = -a2;
            }
            dist += a2;
            if (dist < bestd) {
                bestd = dist;
                bestpos = i;
            }
            a = dist - (this.bias[i] >> 12);
            if (a < bestbiasd) {
                bestbiasd = a;
                bestbiaspos = i;
            }
            int betafreq = this.freq[i] >> 10;
            int[] iArr = this.freq;
            iArr[i] = iArr[i] - betafreq;
            iArr = this.bias;
            iArr[i] = iArr[i] + (betafreq << 10);
        }
        int[] iArr2 = this.freq;
        iArr2[bestpos] = iArr2[bestpos] + 64;
        iArr2 = this.bias;
        iArr2[bestpos] = iArr2[bestpos] - 65536;
        return bestbiaspos;
    }
}
