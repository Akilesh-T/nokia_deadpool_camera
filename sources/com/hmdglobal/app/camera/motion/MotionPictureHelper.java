package com.hmdglobal.app.camera.motion;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

public class MotionPictureHelper {
    private static final int BITRATE_VIDEO = 10000000;
    public static final int COLOR_FormatI420 = 1;
    public static final int COLOR_FormatNV12 = 3;
    public static final int COLOR_FormatNV21 = 2;
    public static final int CROP_FRAME_HEIGHT_9 = 608;
    public static final int CROP_FRAME_WIDTH_18 = 1280;
    private static final boolean DEBUG = true;
    public static final int FRAME_HEIGHT_3 = 480;
    public static final int FRAME_HEIGHT_9 = 720;
    private static final int FRAME_INTERVAL = 1;
    private static final int FRAME_RATE = 30;
    private static final int FRAME_STORED = 45;
    public static final int FRAME_WIDTH_16 = 1280;
    public static final int FRAME_WIDTH_4 = 640;
    private static final String MIME_VIDEO = "video/avc";
    private static final String NS = "http://ns.google.com/photos/1.0/camera/";
    private static final String TAG = "MotionPictureHelper";
    private static final String TEMP_VIDEO_PATH = "/storage/emulated/0/DCIM/Camera/.temp.mp4";
    private static int mFrameHeight = -1;
    private static int mFrameWidth = -1;
    private static MotionPictureHelper mMotionPictureHelper;
    private boolean isMotionOn;
    private boolean mComposeEnd = true;
    private boolean mEncodeFinished;
    private MediaCodec mEncoder;
    private MediaMuxer mMediaMuxer;
    private boolean mMuxerStarted;
    private OnImageAvailableListener mOnPreviewImageAvailableListener = new OnImageAvailableListener() {
        /* JADX WARNING: Missing block: B:9:0x0042, code skipped:
            if (r0 != null) goto L_0x0044;
     */
        /* JADX WARNING: Missing block: B:10:0x0044, code skipped:
            if (r1 != null) goto L_0x0046;
     */
        /* JADX WARNING: Missing block: B:12:?, code skipped:
            r0.close();
     */
        /* JADX WARNING: Missing block: B:13:0x004a, code skipped:
            r3 = move-exception;
     */
        /* JADX WARNING: Missing block: B:14:0x004b, code skipped:
            r1.addSuppressed(r3);
     */
        /* JADX WARNING: Missing block: B:15:0x004f, code skipped:
            r0.close();
     */
        public void onImageAvailable(android.media.ImageReader r7) {
            /*
            r6 = this;
            r0 = r7.acquireNextImage();
            r1 = 0;
            r2 = com.hmdglobal.app.camera.motion.MotionPictureHelper.this;	 Catch:{ Throwable -> 0x0040 }
            r3 = 1;
            r2 = r2.getDataFromImage(r0, r3);	 Catch:{ Throwable -> 0x0040 }
            r3 = "MotionPictureHelper";
            r4 = new java.lang.StringBuilder;	 Catch:{ Throwable -> 0x0040 }
            r4.<init>();	 Catch:{ Throwable -> 0x0040 }
            r5 = "=====onImageAvailable=====: ";
            r4.append(r5);	 Catch:{ Throwable -> 0x0040 }
            r5 = r0.getFormat();	 Catch:{ Throwable -> 0x0040 }
            r4.append(r5);	 Catch:{ Throwable -> 0x0040 }
            r5 = ",data : ";
            r4.append(r5);	 Catch:{ Throwable -> 0x0040 }
            r5 = r2.length;	 Catch:{ Throwable -> 0x0040 }
            r4.append(r5);	 Catch:{ Throwable -> 0x0040 }
            r4 = r4.toString();	 Catch:{ Throwable -> 0x0040 }
            android.util.Log.d(r3, r4);	 Catch:{ Throwable -> 0x0040 }
            r3 = com.hmdglobal.app.camera.motion.MotionPictureHelper.this;	 Catch:{ Throwable -> 0x0040 }
            r4 = r0.getTimestamp();	 Catch:{ Throwable -> 0x0040 }
            r3.onPreview(r2, r4);	 Catch:{ Throwable -> 0x0040 }
            if (r0 == 0) goto L_0x003d;
        L_0x003a:
            r0.close();
        L_0x003d:
            return;
        L_0x003e:
            r2 = move-exception;
            goto L_0x0042;
        L_0x0040:
            r1 = move-exception;
            throw r1;	 Catch:{ all -> 0x003e }
        L_0x0042:
            if (r0 == 0) goto L_0x0052;
        L_0x0044:
            if (r1 == 0) goto L_0x004f;
        L_0x0046:
            r0.close();	 Catch:{ Throwable -> 0x004a }
            goto L_0x0052;
        L_0x004a:
            r3 = move-exception;
            r1.addSuppressed(r3);
            goto L_0x0052;
        L_0x004f:
            r0.close();
        L_0x0052:
            throw r2;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.motion.MotionPictureHelper$AnonymousClass1.onImageAvailable(android.media.ImageReader):void");
        }
    };
    private int mOrientation;
    private boolean mPicFinished;
    private String mPicPath;
    private Queue<Frame> mQueue = new LinkedList();
    private Queue<Frame> mQueueAfter = new LinkedList();
    private Queue<Frame> mQueueBefore = new LinkedList();
    private boolean mStarted;
    private int mTrackIndex = -1;
    private long mWaitTime;

    private class Frame {
        byte[] data;
        long timestamp;

        private Frame() {
        }

        /* synthetic */ Frame(MotionPictureHelper x0, AnonymousClass1 x1) {
            this();
        }
    }

    private MotionPictureHelper(int width, int height) {
        if (this.mEncoder == null) {
            initEncoder(initEncodeFormat(MIME_VIDEO, width, height));
            this.mEncoder.start();
        }
    }

    public static void createHelper(int width, int height) {
        if (mFrameWidth != width || mFrameHeight != height) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createHelper size : ");
            stringBuilder.append(width);
            stringBuilder.append("x");
            stringBuilder.append(height);
            Log.d(str, stringBuilder.toString());
            if (mMotionPictureHelper != null) {
                mMotionPictureHelper.release();
            }
            mMotionPictureHelper = new MotionPictureHelper(width, height);
            mFrameWidth = width;
            mFrameHeight = height;
        }
    }

    public static MotionPictureHelper getHelper() {
        return mMotionPictureHelper;
    }

    public boolean isMotionOn() {
        return this.isMotionOn;
    }

    public void setMotionOn(boolean motionOn) {
        this.isMotionOn = motionOn;
    }

    public void startMotion(boolean started) {
        this.mStarted = started;
        this.mQueue.addAll(this.mQueueBefore);
        this.mQueueBefore.clear();
    }

    public OnImageAvailableListener getOnPreviewImageAvailableListener() {
        return this.mOnPreviewImageAvailableListener;
    }

    public void onPreview(byte[] data, long timestamp) {
        Frame frame = new Frame(this, null);
        frame.data = data;
        frame.timestamp = timestamp;
        addValidFrame(frame);
    }

    public boolean isComposeDone() {
        return this.mComposeEnd;
    }

    public void setComposeEnd(boolean composeEnd) {
        this.mComposeEnd = composeEnd;
    }

    private void addValidFrame(Frame frame) {
        String str;
        StringBuilder stringBuilder;
        if (!this.mStarted) {
            if (this.mQueueBefore.size() >= 30) {
                this.mQueueBefore.poll();
            }
            this.mQueueBefore.offer(frame);
        } else if (this.mQueueAfter.size() < 15) {
            this.mQueueAfter.offer(frame);
        } else {
            this.mStarted = false;
            this.mQueue.addAll(this.mQueueAfter);
            this.mQueueAfter.clear();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("total size000 : ");
            stringBuilder.append(this.mQueue.size());
            Log.d(str, stringBuilder.toString());
            new Thread(new -$$Lambda$MotionPictureHelper$__3_y3rZuKOzymVwnEDl50dDJeg(this)).start();
        }
        if (this.mStarted) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mQueueBefore : ");
            stringBuilder.append(this.mQueueBefore.size());
            Log.d(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mQueueAfter : ");
            stringBuilder.append(this.mQueueAfter.size());
            Log.d(str, stringBuilder.toString());
        }
    }

    public static /* synthetic */ void lambda$addValidFrame$0(MotionPictureHelper motionPictureHelper) {
        try {
            motionPictureHelper.startVideoEncode(motionPictureHelper.mQueue);
            motionPictureHelper.mEncodeFinished = true;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mEncodeFinished : ");
            stringBuilder.append(motionPictureHelper.mEncodeFinished);
            stringBuilder.append(", mPicFinished : ");
            stringBuilder.append(motionPictureHelper.mPicFinished);
            Log.d(str, stringBuilder.toString());
            long beginWaitTime = System.currentTimeMillis();
            while (motionPictureHelper.mEncodeFinished) {
                motionPictureHelper.mWaitTime = System.currentTimeMillis() - beginWaitTime;
                if (motionPictureHelper.mPicFinished) {
                    Log.d(TAG, "createMotionPicture");
                    motionPictureHelper.createMotionPicture(motionPictureHelper.mPicPath, TEMP_VIDEO_PATH);
                    motionPictureHelper.mEncodeFinished = false;
                    Log.d(TAG, "mPicFinished set false");
                    motionPictureHelper.mPicFinished = false;
                    break;
                }
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.e(TAG, "video encode failed");
            motionPictureHelper.mPicFinished = false;
            motionPictureHelper.mEncodeFinished = false;
            motionPictureHelper.mComposeEnd = true;
            motionPictureHelper.mMuxerStarted = false;
            motionPictureHelper.mMediaMuxer = null;
        }
    }

    private void startVideoEncode(Queue<Frame> frames) {
        if (this.mMediaMuxer == null) {
            initMediaMuxer(TEMP_VIDEO_PATH);
        }
        while (frames.size() > 0) {
            Frame frame = (Frame) frames.poll();
            int inputBufferId = this.mEncoder.dequeueInputBuffer(-1);
            if (inputBufferId >= 0) {
                ByteBuffer inputBuffer = this.mEncoder.getInputBuffer(inputBufferId);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("queueInputBuffer, inputBuffer remaining : ");
                stringBuilder.append(inputBuffer.remaining());
                stringBuilder.append(",frame length : ");
                stringBuilder.append(frame.data.length);
                stringBuilder.append(", frames.size : ");
                stringBuilder.append(frames.size());
                Log.d(str, stringBuilder.toString());
                inputBuffer.put(frame.data);
                this.mEncoder.queueInputBuffer(inputBufferId, 0, frame.data.length, frame.timestamp / 1000, 0);
            }
            BufferInfo bufferInfo = new BufferInfo();
            int outputBufferId = this.mEncoder.dequeueOutputBuffer(bufferInfo, -1);
            if (outputBufferId >= 0) {
                ByteBuffer outputBuffer = this.mEncoder.getOutputBuffer(outputBufferId);
                if (bufferInfo.size > 0) {
                    if (!this.mMuxerStarted) {
                        this.mTrackIndex = this.mMediaMuxer.addTrack(this.mEncoder.getOutputFormat());
                        this.mMediaMuxer.start();
                        this.mMuxerStarted = true;
                    }
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    this.mMediaMuxer.writeSampleData(this.mTrackIndex, outputBuffer, bufferInfo);
                }
                this.mEncoder.releaseOutputBuffer(outputBufferId, false);
            } else if (outputBufferId == -2) {
                MediaFormat outputFormat = this.mEncoder.getOutputFormat();
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("encoder output format changed: ");
                stringBuilder2.append(outputFormat);
                Log.d(str2, stringBuilder2.toString());
                this.mTrackIndex = this.mMediaMuxer.addTrack(outputFormat);
                this.mMediaMuxer.start();
                this.mMuxerStarted = true;
            }
        }
        releaseMediaMuxer();
    }

    private void initEncoder(MediaFormat format) {
        try {
            this.mEncoder = MediaCodec.createEncoderByType(MIME_VIDEO);
            this.mEncoder.configure(format, null, null, 1);
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IOException : ");
            stringBuilder.append(e);
            Log.d(str, stringBuilder.toString());
        }
    }

    private MediaFormat initEncodeFormat(String mime, int width, int height) {
        MediaFormat format = MediaFormat.createVideoFormat(mime, width, height);
        format.setInteger("bitrate", BITRATE_VIDEO);
        format.setInteger("frame-rate", 30);
        format.setInteger("i-frame-interval", 1);
        format.setInteger("color-format", 2135033992);
        return format;
    }

    public void release() {
        Log.d(TAG, "released encoder");
        if (this.mEncoder != null) {
            this.mEncoder.stop();
            this.mEncoder.release();
            this.mEncoder = null;
        }
        clearQueue();
        mMotionPictureHelper = null;
        mFrameWidth = -1;
        mFrameHeight = -1;
    }

    private void releaseMediaMuxer() {
        if (this.mMediaMuxer != null) {
            this.mMediaMuxer.stop();
            this.mMediaMuxer.release();
            this.mMediaMuxer = null;
        }
        this.mMuxerStarted = false;
    }

    public void clearQueue() {
        Log.d(TAG, "clearQueue");
        this.mQueueBefore.clear();
        this.mQueueAfter.clear();
    }

    private boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        if (format == 17 || format == 35 || format == 842094169) {
            return true;
        }
        return false;
    }

    public byte[] getDataFromImage(Image image, int colorFormat) {
        int i = colorFormat;
        int i2 = 2;
        int i3 = 1;
        if (i != 1 && i != 2 && i != 3) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 and COLOR_FormatNV21");
        } else if (isImageFormatSupported(image)) {
            Rect crop = image.getCropRect();
            int width = crop.width();
            int height = crop.height();
            Plane[] planes = image.getPlanes();
            int h = 0;
            int yStride;
            int uStride;
            if (i == 3 && planes.length == 3) {
                ByteBuffer bufferY = planes[0].getBuffer();
                ByteBuffer bufferU = planes[1].getBuffer();
                ByteBuffer bufferV = planes[2].getBuffer();
                yStride = image.getPlanes()[0].getRowStride();
                uStride = image.getPlanes()[1].getRowStride();
                byte[] bytes = new byte[(((width * height) * 3) / 2)];
                if (width < yStride) {
                    for (i2 = 0; i2 < height; i2++) {
                        bufferY.position(i2 * yStride);
                        bufferY.get(bytes, i2 * width, width);
                    }
                } else {
                    bufferY.get(bytes, 0, width * height);
                }
                i2 = width * height;
                int rowStride = width;
                if (rowStride < uStride) {
                    while (h < (height / 2) - 1) {
                        bufferU.position(h * uStride);
                        bufferU.get(bytes, i2, rowStride);
                        i2 += rowStride;
                        h++;
                    }
                    bufferU.position(h * uStride);
                    bufferU.get(bytes, i2, rowStride - 1);
                    i2 += rowStride - 1;
                } else {
                    h = bufferU.remaining();
                    bufferU.get(bytes, i2, h);
                    i2 += h;
                }
                bufferV.position(bufferV.remaining() - 1);
                bufferV.get(bytes, i2, 1);
                return bytes;
            }
            int width2;
            int format = image.getFormat();
            byte[] data = new byte[(((width * height) * ImageFormat.getBitsPerPixel(format)) / 8)];
            byte[] rowData = new byte[planes[0].getRowStride()];
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get data from ");
            stringBuilder.append(planes.length);
            stringBuilder.append(" planes");
            Log.v(str, stringBuilder.toString());
            uStride = 1;
            yStride = 0;
            int i4 = 0;
            while (i4 < planes.length) {
                switch (i4) {
                    case 0:
                        yStride = 0;
                        uStride = 1;
                        break;
                    case 1:
                        if (i != i3) {
                            if (i == i2) {
                                yStride = (width * height) + 1;
                                uStride = 2;
                                break;
                            }
                        }
                        yStride = width * height;
                        uStride = 1;
                        break;
                        break;
                    case 2:
                        if (i != i3) {
                            if (i == i2) {
                                yStride = width * height;
                                uStride = 2;
                                break;
                            }
                        }
                        yStride = (int) (((double) (width * height)) * 1.25d);
                        uStride = 1;
                        break;
                        break;
                }
                ByteBuffer buffer = planes[i4].getBuffer();
                int rowStride2 = planes[i4].getRowStride();
                i2 = planes[i4].getPixelStride();
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("pixelStride ");
                stringBuilder2.append(i2);
                Log.v(str2, stringBuilder2.toString());
                String str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("rowStride ");
                stringBuilder2.append(rowStride2);
                Log.v(str3, stringBuilder2.toString());
                str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("width ");
                stringBuilder2.append(width);
                Log.v(str3, stringBuilder2.toString());
                str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("height ");
                stringBuilder2.append(height);
                Log.v(str3, stringBuilder2.toString());
                str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("buffer size ");
                stringBuilder2.append(buffer.remaining());
                Log.v(str3, stringBuilder2.toString());
                i = i4 == 0 ? 0 : 1;
                i3 = width >> i;
                h = height >> i;
                int format2 = format;
                width2 = width;
                buffer.position(((crop.top >> i) * rowStride2) + ((crop.left >> i) * i2));
                format = 0;
                while (format < h) {
                    int shift;
                    if (i2 == 1 && uStride == 1) {
                        width = i3;
                        buffer.get(data, yStride, width);
                        yStride += width;
                        shift = i;
                    } else {
                        width = ((i3 - 1) * i2) + 1;
                        shift = i;
                        buffer.get(rowData, 0, width);
                        int channelOffset = yStride;
                        for (yStride = 0; yStride < i3; yStride++) {
                            data[channelOffset] = rowData[yStride * i2];
                            channelOffset += uStride;
                        }
                        yStride = channelOffset;
                    }
                    if (format < h - 1) {
                        buffer.position((buffer.position() + rowStride2) - width);
                    }
                    format++;
                    i = shift;
                }
                str3 = TAG;
                format = new StringBuilder();
                format.append("Finished reading data from plane ");
                format.append(i4);
                Log.v(str3, format.toString());
                i4++;
                i3 = 1;
                format = format2;
                width = width2;
                i = colorFormat;
                i2 = 2;
            }
            width2 = width;
            return data;
        } else {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("can't convert Image to byte array, format ");
            stringBuilder3.append(image.getFormat());
            throw new RuntimeException(stringBuilder3.toString());
        }
    }

    public void mirrorYUV420SP(byte[] src, int w, int h) {
        int i;
        int a;
        int b;
        byte temp;
        int i2 = 0;
        for (i = 0; i < h; i++) {
            a = i * w;
            for (b = ((i + 1) * w) - 1; a < b; b--) {
                temp = src[a];
                src[a] = src[b];
                src[b] = temp;
                a++;
            }
        }
        i = w * h;
        while (i2 < h / 2) {
            a = i2 * w;
            for (b = ((i2 + 1) * w) - 2; a < b; b -= 2) {
                temp = src[a + i];
                src[a + i] = src[b + i];
                src[b + i] = temp;
                temp = src[(a + i) + 1];
                src[(a + i) + 1] = src[(b + i) + 1];
                src[(b + i) + 1] = temp;
                a += 2;
            }
            i2++;
        }
    }

    private void initMediaMuxer(String filePath) {
        try {
            this.mMediaMuxer = new MediaMuxer(filePath, 0);
            this.mMediaMuxer.setOrientationHint(this.mOrientation);
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IOException : ");
            stringBuilder.append(e);
            Log.d(str, stringBuilder.toString());
        }
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    public void onJPEGPictureFinished(String filePath) {
        this.mPicPath = filePath;
        Log.d(TAG, "mPicFinished set true");
        this.mPicFinished = true;
    }

    private String createMotionPicture(String jpegPath, String mp4Path) {
        setJpegXMPMeta(jpegPath, mp4Path);
        appendMp4ToJpeg(mp4Path, jpegPath);
        return jpegPath;
    }

    private void setJpegXMPMeta(String jpegPath, String mp4Path) {
        File mp4 = new File(mp4Path);
        String str;
        StringBuilder stringBuilder;
        if (mp4.exists()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("file size : ");
            stringBuilder.append((int) mp4.length());
            Log.d(str, stringBuilder.toString());
            XMPMeta xmpMeta = XmpUtil.createXMPMeta();
            try {
                xmpMeta.setPropertyInteger("http://ns.google.com/photos/1.0/camera/", CameraMode.MICRO_VIDEO, 1);
                xmpMeta.setPropertyInteger("http://ns.google.com/photos/1.0/camera/", "MicroVideoVersion", 1);
                xmpMeta.setPropertyInteger("http://ns.google.com/photos/1.0/camera/", "MicroVideoOffset", (int) mp4.length());
                xmpMeta.setPropertyLong("http://ns.google.com/photos/1.0/camera/", "MicroVideoPresentationTimestampUs", -1);
            } catch (XMPException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("XMPException : ");
                stringBuilder2.append(e);
                Log.d(str2, stringBuilder2.toString());
            }
            XmpUtil.writeXMPMeta(jpegPath, xmpMeta);
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append(mp4Path);
        stringBuilder.append(" not exist");
        Log.e(str, stringBuilder.toString());
    }

    private void appendMp4ToJpeg(String mp4Path, String jpegPath) {
        writeToFile(mp4Path, jpegPath, 0, true);
        this.mComposeEnd = true;
        Log.d(TAG, "write ended");
    }

    private String extractMp4FromJpeg(String jpegPath, long mp4Length, String targetMp4Path) {
        File jpeg = new File(jpegPath);
        File targetMp4 = new File(targetMp4Path);
        if (!targetMp4.exists()) {
            try {
                targetMp4.createNewFile();
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IOException : ");
                stringBuilder.append(e);
                Log.d(str, stringBuilder.toString());
            }
        }
        writeToFile(jpegPath, targetMp4Path, jpeg.length() - mp4Length, false);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("write ended : ");
        stringBuilder2.append(targetMp4.length());
        Log.d(str2, stringBuilder2.toString());
        return targetMp4Path;
    }

    private void writeToFile(String from, String to, long offset, boolean append) {
        Throwable th;
        Throwable th2;
        FileInputStream fis;
        try {
            fis = new FileInputStream(from);
            FileOutputStream fos = new FileOutputStream(to, append);
            if (offset != 0) {
                try {
                    long skipNum = fis.skip(offset);
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("skipNum : ");
                    stringBuilder.append(skipNum);
                    Log.d(str, stringBuilder.toString());
                } catch (Throwable th22) {
                    Throwable th3 = th22;
                    th22 = th;
                    th = th3;
                }
            }
            byte[] bytes = new byte[10240];
            int readNum = 0;
            while (true) {
                int read = fis.read(bytes);
                readNum = read;
                if (read != -1) {
                    fos.write(bytes, 0, readNum);
                    fos.flush();
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("readNum : ");
                    stringBuilder2.append(readNum);
                    Log.d(str2, stringBuilder2.toString());
                } else {
                    $closeResource(null, fos);
                    $closeResource(null, fis);
                    return;
                }
            }
            $closeResource(th22, fos);
            throw th;
        } catch (IOException e) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("IOException ");
            stringBuilder3.append(e);
            Log.d(str3, stringBuilder3.toString());
        } catch (Throwable th4) {
            $closeResource(r1, fis);
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }
}
