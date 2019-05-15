package com.morphoinc.app.panoramagp3;

import android.content.ContentResolver;
import android.content.Context;
import android.media.Image;
import android.net.Uri;
import com.bumptech.glide.load.Key;
import com.morphoinc.app.LogFilter;
import com.morphoinc.utils.NativeMemoryAllocator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Locale;

public class InputSaveState extends PanoramaState {
    private ContentResolver mContentResolver;
    private long mDurationSum;
    private final boolean mEnabled;
    private int mFrameCount;
    private boolean mIsRunnable = false;
    private long mMaxDuration;
    private long mMinDuration;
    private MorphoPanoramaGP3 mMorphoPanoramaGP3;
    private AttachImageStack mSaveStack;
    private PerformanceCounter mTimeCounter;

    private class InputSaveRunnable implements Runnable {
        final String folderPath;
        final String imageFormat;
        CaptureImage srcImage;

        public InputSaveRunnable(CaptureImage image, String format, String inputFolderPath) {
            this.imageFormat = format;
            this.folderPath = inputFolderPath;
            this.srcImage = image;
        }

        public void run() {
            while (this.srcImage != null) {
                runMain();
                this.srcImage = InputSaveState.this.mSaveStack.pop();
            }
            InputSaveState.this.mIsRunnable = false;
        }

        private void runMain() {
            byte[] imageBytes;
            Image image = this.srcImage.image();
            int imageWidth = this.srcImage.getWidth();
            int imageHeight = this.srcImage.getHeight();
            if (image == null) {
                imageBytes = this.srcImage.raw();
            } else if ("YUV420_PLANAR".equals(this.imageFormat)) {
                imageBytes = new ConvertFromYuv420Planar().image2bytes(image);
            } else if ("YUV420_SEMIPLANAR".equals(this.imageFormat)) {
                imageBytes = new ConvertFromYuv420SemiPlanar().image2bytes(image);
            } else if ("YVU420_SEMIPLANAR".equals(this.imageFormat)) {
                imageBytes = new ConvertFromYvu420SemiPlanar().image2bytes(image);
            } else {
                LogFilter.e("Camera2App", "Image format error.");
                this.srcImage.close();
                return;
            }
            byte[] imageBytes2 = imageBytes;
            this.srcImage.close();
            InputSaveState.this.saveImage(imageBytes2, imageWidth, imageHeight, this.imageFormat, this.folderPath);
        }
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public InputSaveState(boolean enabled) {
        this.mEnabled = enabled;
        this.mMorphoPanoramaGP3 = null;
        if (enabled) {
            this.mTimeCounter = PerformanceCounter.newInstance(true);
            this.mSaveStack = new AttachImageStack();
        }
    }

    public void setMorphoPanoramaGP3(MorphoPanoramaGP3 MorphoPanoramaGP3) {
        this.mMorphoPanoramaGP3 = MorphoPanoramaGP3;
    }

    public void setContext(Context context) {
        this.mContentResolver = context.getContentResolver();
    }

    public void resetCount() {
        this.mFrameCount = 0;
    }

    public void init() {
        this.mSaveStack.init();
    }

    public boolean onSaveImage(CaptureImage image) {
        if (!this.mEnabled || this.mMorphoPanoramaGP3 == null) {
            throw new IllegalStateException();
        }
        if (this.mIsRunnable) {
            this.mSaveStack.push(image);
        } else {
            this.mIsRunnable = true;
            new Thread(new InputSaveRunnable(image, this.mMorphoPanoramaGP3.getInputImageFormat(), this.mMorphoPanoramaGP3.getInputFolderPath())).start();
        }
        return true;
    }

    private void saveImage(byte[] imageBytes, int width, int height, String format, String inputFolderPath) {
        byte[] bArr = imageBytes;
        updateDuration();
        ByteBuffer byteBuffer = NativeMemoryAllocator.allocateBuffer(bArr.length);
        byteBuffer.put(bArr);
        byteBuffer.clear();
        String outputFileName = String.format(Locale.US, "%06d.jpg", new Object[]{Integer.valueOf(this.mFrameCount)});
        if (Camera2App.addImageAsApplication(this.mContentResolver, outputFileName, System.currentTimeMillis(), inputFolderPath, outputFileName, byteBuffer, width, height, format, null, new IPanoramaSaveListener() {
            public void onSaveUri(Uri uri) {
            }
        }) == null) {
            LogFilter.e("InputSaveState", "Camera2App.addImageAsApplication() error.");
        }
        NativeMemoryAllocator.freeBuffer(byteBuffer);
    }

    private void updateDuration() {
        if (this.mFrameCount == 0) {
            this.mMinDuration = Long.MAX_VALUE;
            this.mMaxDuration = 0;
            this.mDurationSum = 0;
            this.mFrameCount = 1;
            this.mTimeCounter.start();
            return;
        }
        this.mTimeCounter.stop();
        long lap = this.mTimeCounter.get();
        if (lap < this.mMinDuration) {
            this.mMinDuration = lap;
        }
        if (this.mMaxDuration < lap) {
            this.mMaxDuration = lap;
        }
        this.mDurationSum += lap;
        this.mFrameCount++;
        this.mTimeCounter.start();
    }

    public float getFps() {
        if (this.mFrameCount == 0) {
            return 0.0f;
        }
        float spanAve = ((float) this.mDurationSum) / ((float) this.mFrameCount);
        if (spanAve == 0.0f) {
            return 0.0f;
        }
        return 1000.0f / spanAve;
    }

    public void putParamFile(String dirPath, CameraInfo cameraInfo, String imageFormat, int sensorSensitivity, int shutterSpeed, float fps) {
        IOException e;
        String str;
        try {
            PrintWriter writer;
            StringBuilder stringBuilder;
            try {
                writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(dirPath, "params.txt")), Key.STRING_CHARSET_NAME));
                writer.println("## Save data params");
                writer.printf("* IMAGE SIZE:%dx%d", new Object[]{Integer.valueOf(cameraInfo.getCaptureWidth()), Integer.valueOf(cameraInfo.getCaptureHeight())});
                writer.println();
                stringBuilder = new StringBuilder();
                stringBuilder.append("* IMAGE FORMAT:");
            } catch (IOException e2) {
                e = e2;
                str = imageFormat;
                e.printStackTrace();
            }
            try {
                stringBuilder.append(imageFormat);
                writer.println(stringBuilder.toString());
                writer.printf("* ISO:%d", new Object[]{Integer.valueOf(sensorSensitivity)});
                writer.println();
                writer.printf("* SHUTTER SPEED:1/%d", new Object[]{Integer.valueOf(shutterSpeed)});
                writer.println();
                writer.printf("* FPS:%.02ffps", new Object[]{Float.valueOf(fps)});
                writer.println();
                if (this.mEnabled) {
                    writer.printf("    * MIN DURATION:%dmsec", new Object[]{Long.valueOf(this.mMinDuration)});
                    writer.println();
                    writer.printf("    * MAX DURATION:%dmsec", new Object[]{Long.valueOf(this.mMaxDuration)});
                    writer.println();
                    writer.printf("    * AVE DURATION:%dmsec (%dframes)", new Object[]{Long.valueOf(this.mDurationSum / ((long) this.mFrameCount)), Integer.valueOf(this.mFrameCount)});
                    writer.println();
                    writer.printf("    * FPS:%.02ffps", new Object[]{Float.valueOf(getFps())});
                    writer.println();
                }
                writer.println("## Information");
                writer.printf("* SENSOR_ORIENTATION:%d", new Object[]{Integer.valueOf(cameraInfo.getOrientation())});
                writer.println();
                writer.printf("* INFO_SUPPORTED_HARDWARE_LEVEL:%d", new Object[]{Integer.valueOf(cameraInfo.getHardwareLevel())});
                writer.println();
                writer.printf("* SENSOR_INFO_EXPOSURE_TIME_RANGE:%dnsec-%dnsec", new Object[]{cameraInfo.getExposureTimeMin(), cameraInfo.getExposureTimeMax()});
                writer.println();
                writer.printf("* SENSOR_INFO_MAX_FRAME_DURATION:%dnsec", new Object[]{cameraInfo.getMaxFrameDuration()});
                writer.println();
                writer.printf("* SENSOR_INFO_SENSITIVITY_RANGE:%d-%d", new Object[]{Integer.valueOf(cameraInfo.getSensitivityMin()), Integer.valueOf(cameraInfo.getSensitivityMax())});
                writer.println();
                writer.printf("* SENSOR_MAX_ANALOG_SENSITIVITY:%d", new Object[]{Integer.valueOf(cameraInfo.getMaxAnalogSensitivity())});
                writer.println();
                writer.close();
            } catch (IOException e3) {
                e = e3;
                e.printStackTrace();
            }
        } catch (IOException e4) {
            e = e4;
            String str2 = dirPath;
            str = imageFormat;
            e.printStackTrace();
        }
    }
}
