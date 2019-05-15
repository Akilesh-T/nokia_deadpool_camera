package com.morphoinc.utils.VideoRec;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import com.morphoinc.app.panoramagp3.CaptureImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class VideoRecorderRaw extends BaseVideoRecoder {
    private ExecutorService mEncodeExecutor;
    private EncodeRunnable mEncodeRunnable;
    private final LinkedBlockingQueue<FrameData> mFrameDataQueue;
    private final FrameData sAttachExit;
    private final long startTimeUs;

    private class EncodeRunnable implements Runnable {
        private EncodeRunnable() {
        }

        public void run() {
            while (true) {
                try {
                    FrameData access$100 = VideoRecorderRaw.this.takeFrameDataQueue();
                    FrameData frameData = access$100;
                    if (access$100 == VideoRecorderRaw.this.sAttachExit) {
                        break;
                    }
                    VideoRecorderRaw.this.encodeFrame(frameData._data, frameData._timestamp);
                    frameData._data = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            VideoRecorderRaw.this.initFrameDataQueue();
        }
    }

    private class FrameData {
        private byte[] _data;
        private long _timestamp;

        FrameData(byte[] data, long timestamp) {
            this._data = data;
            this._timestamp = timestamp;
        }
    }

    public interface RawRenderListener2 {
        void onDraw(byte[] bArr, CaptureImage captureImage);
    }

    public interface RawRenderListener {
        int onDraw(ByteBuffer byteBuffer, CaptureImage captureImage);
    }

    public /* bridge */ /* synthetic */ String getOutputFilePath() {
        return super.getOutputFilePath();
    }

    public VideoRecorderRaw(int width, int height, int bitrate, float frame_rate, String output_path) {
        this(width, height, bitrate, 30.0f, "", output_path);
    }

    public VideoRecorderRaw(int width, int height, int bitrate, float frame_rate, String image_format, String output_path) {
        super(width, height, bitrate, 30.0f, image_format.startsWith("YUV") ? 2135033992 : getColorFormat(width, height, bitrate, frame_rate), output_path);
        this.mEncodeExecutor = null;
        this.mFrameDataQueue = new LinkedBlockingQueue();
        this.sAttachExit = new FrameData(null, -1);
        this.startTimeUs = System.nanoTime() / 1000;
    }

    public void encodeFrameAsync(CaptureImage image, long timestamp, RawRenderListener2 listener) {
        if (this.mEncodeExecutor == null) {
            this.mEncodeExecutor = Executors.newCachedThreadPool();
            if (this.mEncodeRunnable == null) {
                this.mEncodeRunnable = new EncodeRunnable();
            }
            initFrameDataQueue();
            this.mEncodeExecutor.submit(this.mEncodeRunnable);
        }
        byte[] buffer = new byte[(((image.getWidth() * image.getHeight()) * 3) / 2)];
        listener.onDraw(buffer, image);
        addFrameDataQueue(new FrameData(buffer, timestamp));
    }

    public void encodeFrame(byte[] data, long timestamp) {
        long tm = System.nanoTime();
        int indexInput = this.mEncoder.dequeueInputBuffer(-1);
        if (indexInput >= 0) {
            ByteBuffer buffer = this.mEncoder.getInputBuffer(indexInput);
            buffer.rewind();
            int size = data.length;
            buffer.put(data);
            buffer.position(size);
            this.mEncoder.queueInputBuffer(indexInput, 0, size, timestamp, 0);
        }
        super.drainEncoder(false);
    }

    @SuppressLint({"WrongCall"})
    public void encodeFrame(CaptureImage image, long timestamp, RawRenderListener listener) {
        int indexInput = this.mEncoder.dequeueInputBuffer(-1);
        if (indexInput >= 0) {
            ByteBuffer buffer = this.mEncoder.getInputBuffer(indexInput);
            buffer.rewind();
            int size = listener.onDraw(buffer, image);
            buffer.position(size);
            this.mEncoder.queueInputBuffer(indexInput, 0, size, timestamp, 0);
        }
        super.drainEncoder(false);
    }

    public void encodeFrame(CaptureImage image, RawRenderListener listener) {
        encodeFrame(image, (System.nanoTime() / 1000) - this.startTimeUs, listener);
    }

    public void end() {
        if (this.mEncodeExecutor != null) {
            addFrameDataQueue(this.sAttachExit);
            this.mEncodeExecutor.shutdown();
            boolean terminated = false;
            while (!terminated) {
                try {
                    terminated = this.mEncodeExecutor.awaitTermination(200, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    this.mEncodeExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            this.mEncodeExecutor = null;
        }
        super.drainEncoder(true);
        super.releaseEncoder();
    }

    private static int getColorFormat(int width, int height, int bitrate, float frame_rate) {
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger("bitrate", bitrate);
        format.setFloat("frame-rate", frame_rate);
        format.setInteger("i-frame-interval", 1);
        MediaCodec codec = null;
        try {
            codec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }
        int[] formats = new int[]{21, 2141391872, 39, 2130706688};
        if (codec != null) {
            int i = 0;
            while (i < formats.length) {
                format.setInteger("color-format", formats[i]);
                try {
                    codec.configure(format, null, null, 1);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("format: ");
                    stringBuilder.append(i);
                    Log.d("VideoRecorderRaw", stringBuilder.toString());
                    codec.stop();
                    codec.release();
                    break;
                } catch (Exception e2) {
                    i++;
                }
            }
        }
        return format.getInteger("color-format");
    }

    private void initFrameDataQueue() {
        while (this.mFrameDataQueue.size() > 0) {
            FrameData frameData = (FrameData) this.mFrameDataQueue.poll();
            if (frameData != null) {
                frameData._data = null;
            }
        }
    }

    private void addFrameDataQueue(FrameData frameData) {
        this.mFrameDataQueue.offer(frameData);
    }

    private FrameData takeFrameDataQueue() throws InterruptedException {
        return (FrameData) this.mFrameDataQueue.take();
    }
}
