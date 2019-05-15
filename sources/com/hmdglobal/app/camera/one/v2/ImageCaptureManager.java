package com.hmdglobal.app.camera.one.v2;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.CaptureResult.Key;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Pair;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer;
import com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.PinStateListener;
import com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.Selector;
import com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.SwapTask;
import com.hmdglobal.app.camera.util.Task;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@TargetApi(21)
public class ImageCaptureManager extends CaptureCallback implements OnImageAvailableListener {
    private static final long DEBUG_INTERFRAME_STALL_WARNING = 5;
    private static final long DEBUG_MAX_IMAGE_CALLBACK_DUR = 25;
    private static final boolean DEBUG_PRINT_OPEN_IMAGE_COUNT = false;
    private static final Tag TAG = new Tag("ZSLImageListener");
    private final ConcurrentSharedRingBuffer<CapturedImage> mCapturedImageBuffer;
    private long mDebugLastOnCaptureCompletedMillis = 0;
    private long mDebugStalledFrameCount = 0;
    private final Executor mImageCaptureListenerExecutor;
    private final Handler mListenerHandler;
    private final Map<Key<?>, Pair<Long, Object>> mMetadata = new ConcurrentHashMap();
    private final Map<Key<?>, Set<MetadataChangeListener>> mMetadataChangeListeners = new ConcurrentHashMap();
    private final AtomicInteger mNumOpenImages = new AtomicInteger(0);
    private ImageCaptureListener mPendingImageCaptureCallback;
    private List<CapturedImageConstraint> mPendingImageCaptureConstraints;

    public interface CaptureReadyListener {
        void onReadyStateChange(boolean z);
    }

    private class CapturedImage {
        private Image mImage;
        private TotalCaptureResult mMetadata;

        private CapturedImage() {
            this.mImage = null;
            this.mMetadata = null;
        }

        /* synthetic */ CapturedImage(ImageCaptureManager x0, AnonymousClass1 x1) {
            this();
        }

        public void reset() {
            if (this.mImage != null) {
                this.mImage.close();
                ImageCaptureManager.this.mNumOpenImages.decrementAndGet();
            }
            this.mImage = null;
            this.mMetadata = null;
        }

        public boolean isComplete() {
            return (this.mImage == null || this.mMetadata == null) ? false : true;
        }

        public void addImage(Image image) {
            if (this.mImage == null) {
                this.mImage = image;
                return;
            }
            throw new IllegalArgumentException("Unable to add an Image when one already exists.");
        }

        public Image tryGetImage() {
            return this.mImage;
        }

        public void addMetadata(TotalCaptureResult metadata) {
            if (this.mMetadata == null) {
                this.mMetadata = metadata;
                return;
            }
            throw new IllegalArgumentException("Unable to add a TotalCaptureResult when one already exists.");
        }

        public TotalCaptureResult tryGetMetadata() {
            return this.mMetadata;
        }
    }

    public interface CapturedImageConstraint {
        boolean satisfiesConstraint(TotalCaptureResult totalCaptureResult);
    }

    public interface ImageCaptureListener {
        void onImageCaptured(Image image, TotalCaptureResult totalCaptureResult);
    }

    public interface MetadataChangeListener {
        void onImageMetadataChange(Key<?> key, Object obj, Object obj2, CaptureResult captureResult);
    }

    ImageCaptureManager(int maxImages, Handler listenerHandler, Executor imageCaptureListenerExecutor) {
        this.mCapturedImageBuffer = new ConcurrentSharedRingBuffer(maxImages - 2);
        this.mListenerHandler = listenerHandler;
        this.mImageCaptureListenerExecutor = imageCaptureListenerExecutor;
    }

    public void setCaptureReadyListener(final CaptureReadyListener listener) {
        this.mCapturedImageBuffer.setListener(this.mListenerHandler, new PinStateListener() {
            public void onPinStateChange(boolean pinsAvailable) {
                listener.onReadyStateChange(pinsAvailable);
            }
        });
    }

    public <T> void addMetadataChangeListener(Key<T> key, MetadataChangeListener listener) {
        if (!this.mMetadataChangeListeners.containsKey(key)) {
            this.mMetadataChangeListeners.put(key, Collections.newSetFromMap(new ConcurrentHashMap()));
        }
        ((Set) this.mMetadataChangeListeners.get(key)).add(listener);
    }

    public <T> boolean removeMetadataChangeListener(Key<T> key, MetadataChangeListener listener) {
        if (this.mMetadataChangeListeners.containsKey(key)) {
            return ((Set) this.mMetadataChangeListeners.get(key)).remove(listener);
        }
        return false;
    }

    public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
        CaptureResult captureResult;
        long frameNumber;
        ImageCaptureManager imageCaptureManager = this;
        long frameNumber2 = partialResult.getFrameNumber();
        for (Key<?> key : partialResult.getKeys()) {
            Pair<Long, Object> oldEntry = (Pair) imageCaptureManager.mMetadata.get(key);
            Object oldValue = oldEntry != null ? oldEntry.second : null;
            boolean z = oldEntry != null && frameNumber2 < ((Long) oldEntry.first).longValue();
            if (!z) {
                captureResult = partialResult;
                Object newValue = captureResult.get(key);
                imageCaptureManager.mMetadata.put(key, new Pair(Long.valueOf(frameNumber2), newValue));
                if (oldValue != newValue) {
                    if (imageCaptureManager.mMetadataChangeListeners.containsKey(key)) {
                        Iterator it = ((Set) imageCaptureManager.mMetadataChangeListeners.get(key)).iterator();
                        while (it.hasNext()) {
                            MetadataChangeListener listener = (MetadataChangeListener) it.next();
                            Tag tag = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Dispatching to metadata change listener for key: ");
                            stringBuilder.append(key.toString());
                            Log.v(tag, stringBuilder.toString());
                            Handler handler = imageCaptureManager.mListenerHandler;
                            ImageCaptureManager imageCaptureManager2 = imageCaptureManager;
                            final MetadataChangeListener metadataChangeListener = listener;
                            AnonymousClass2 anonymousClass2 = r0;
                            final Key<?> key2 = key;
                            frameNumber = frameNumber2;
                            Handler handler2 = handler;
                            final Object obj = oldValue;
                            Iterator it2 = it;
                            final Object obj2 = newValue;
                            Object newValue2 = newValue;
                            final CaptureResult newValue3 = captureResult;
                            AnonymousClass2 anonymousClass22 = new Runnable() {
                                public void run() {
                                    metadataChangeListener.onImageMetadataChange(key2, obj, obj2, newValue3);
                                }
                            };
                            handler2.post(anonymousClass2);
                            it = it2;
                            frameNumber2 = frameNumber;
                            newValue = newValue2;
                            imageCaptureManager = this;
                        }
                        imageCaptureManager = this;
                    }
                }
            }
        }
        captureResult = partialResult;
        frameNumber = frameNumber2;
    }

    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, final TotalCaptureResult result) {
        long timestamp = ((Long) result.get(TotalCaptureResult.SENSOR_TIMESTAMP)).longValue();
        long now = SystemClock.uptimeMillis();
        if (now - this.mDebugLastOnCaptureCompletedMillis < 5) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Camera thread has stalled for ");
            long j = this.mDebugStalledFrameCount + 1;
            this.mDebugStalledFrameCount = j;
            stringBuilder.append(j);
            stringBuilder.append(" frames at # ");
            stringBuilder.append(result.getFrameNumber());
            stringBuilder.append(".");
            Log.e(tag, stringBuilder.toString());
        } else {
            this.mDebugStalledFrameCount = 0;
        }
        this.mDebugLastOnCaptureCompletedMillis = now;
        if (!this.mCapturedImageBuffer.swapLeast(timestamp, new SwapTask<CapturedImage>() {
            public CapturedImage create() {
                CapturedImage image = new CapturedImage(ImageCaptureManager.this, null);
                image.addMetadata(result);
                return image;
            }

            public CapturedImage swap(CapturedImage oldElement) {
                oldElement.reset();
                oldElement.addMetadata(result);
                return oldElement;
            }

            public void update(CapturedImage existingElement) {
                existingElement.addMetadata(result);
            }
        })) {
            Log.v(TAG, "Unable to add new image metadata to ring-buffer.");
        }
        tryExecutePendingCaptureRequest(timestamp);
    }

    public void onImageAvailable(ImageReader reader) {
        long startTime = SystemClock.currentThreadTimeMillis();
        final Image img = reader.acquireLatestImage();
        if (img != null) {
            int numOpenImages = this.mNumOpenImages.incrementAndGet();
            if (!this.mCapturedImageBuffer.swapLeast(img.getTimestamp(), new SwapTask<CapturedImage>() {
                public CapturedImage create() {
                    CapturedImage image = new CapturedImage(ImageCaptureManager.this, null);
                    image.addImage(img);
                    return image;
                }

                public CapturedImage swap(CapturedImage oldElement) {
                    oldElement.reset();
                    oldElement.addImage(img);
                    return oldElement;
                }

                public void update(CapturedImage existingElement) {
                    existingElement.addImage(img);
                }
            })) {
                img.close();
                numOpenImages = this.mNumOpenImages.decrementAndGet();
            }
            tryExecutePendingCaptureRequest(img.getTimestamp());
            long totTime = SystemClock.currentThreadTimeMillis() - startTime;
            if (totTime > DEBUG_MAX_IMAGE_CALLBACK_DUR) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onImageAvailable() took ");
                stringBuilder.append(totTime);
                stringBuilder.append("ms");
                Log.v(tag, stringBuilder.toString());
            }
        }
    }

    public void close() {
        try {
            this.mCapturedImageBuffer.close(new Task<CapturedImage>() {
                public void run(CapturedImage e) {
                    e.reset();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void captureNextImage(ImageCaptureListener onImageCaptured, List<CapturedImageConstraint> constraints) {
        this.mPendingImageCaptureCallback = onImageCaptured;
        this.mPendingImageCaptureConstraints = constraints;
    }

    private void tryExecutePendingCaptureRequest(long newImageTimestamp) {
        if (this.mPendingImageCaptureCallback != null) {
            Pair<Long, CapturedImage> pinnedImage = this.mCapturedImageBuffer.tryPin(newImageTimestamp);
            if (pinnedImage != null) {
                CapturedImage image = pinnedImage.second;
                if (image.isComplete()) {
                    TotalCaptureResult captureResult = image.tryGetMetadata();
                    if (this.mPendingImageCaptureConstraints != null) {
                        for (CapturedImageConstraint constraint : this.mPendingImageCaptureConstraints) {
                            if (!constraint.satisfiesConstraint(captureResult)) {
                                this.mCapturedImageBuffer.release(((Long) pinnedImage.first).longValue());
                                return;
                            }
                        }
                    }
                    if (tryExecuteCaptureOrRelease(pinnedImage, this.mPendingImageCaptureCallback)) {
                        this.mPendingImageCaptureCallback = null;
                        this.mPendingImageCaptureConstraints = null;
                    }
                } else {
                    this.mCapturedImageBuffer.release(((Long) pinnedImage.first).longValue());
                }
            }
        }
    }

    public boolean tryCaptureExistingImage(ImageCaptureListener onImageCaptured, final List<CapturedImageConstraint> constraints) {
        Selector<CapturedImage> selector = (constraints == null || constraints.isEmpty()) ? new Selector<CapturedImage>() {
            public boolean select(CapturedImage image) {
                return true;
            }
        } : new Selector<CapturedImage>() {
            public boolean select(CapturedImage e) {
                TotalCaptureResult captureResult = e.tryGetMetadata();
                if (captureResult == null || e.tryGetImage() == null) {
                    return false;
                }
                for (CapturedImageConstraint constraint : constraints) {
                    if (!constraint.satisfiesConstraint(captureResult)) {
                        return false;
                    }
                }
                return true;
            }
        };
        return tryExecuteCaptureOrRelease(this.mCapturedImageBuffer.tryPinGreatestSelected(selector), onImageCaptured);
    }

    private boolean tryExecuteCaptureOrRelease(final Pair<Long, CapturedImage> toCapture, final ImageCaptureListener callback) {
        if (toCapture == null) {
            return false;
        }
        try {
            this.mImageCaptureListenerExecutor.execute(new Runnable() {
                public void run() {
                    try {
                        CapturedImage img = toCapture.second;
                        callback.onImageCaptured(img.tryGetImage(), img.tryGetMetadata());
                    } finally {
                        ImageCaptureManager.this.mCapturedImageBuffer.release(((Long) toCapture.first).longValue());
                    }
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            this.mCapturedImageBuffer.release(((Long) toCapture.first).longValue());
            return false;
        }
    }
}
