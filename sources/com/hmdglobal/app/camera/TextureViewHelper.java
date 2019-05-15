package com.hmdglobal.app.camera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import com.hmdglobal.app.camera.app.CameraProvider;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.motion.MotionPictureHelper;
import com.hmdglobal.app.camera.ui.ModeTransitionView;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAreaChangedListener;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAspectRatioChangedListener;
import com.hmdglobal.app.camera.util.BlurUtil;
import com.hmdglobal.app.camera.util.CameraUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TextureViewHelper implements SurfaceTextureListener, OnLayoutChangeListener {
    public static final float MATCH_SCREEN = 0.0f;
    private static final Tag TAG = new Tag("TexViewHelper");
    private static final int UNSET = -1;
    private float mAspectRatio = 0.0f;
    private final ArrayList<PreviewAspectRatioChangedListener> mAspectRatioChangedListeners = new ArrayList();
    private boolean mAutoAdjustTransform = true;
    private final CameraProvider mCameraProvider;
    private CaptureLayoutHelper mCaptureLayoutHelper = null;
    private int mHeight = 0;
    private final ModeTransitionView mModeCover;
    private OnLayoutChangeListener mOnLayoutChangeListener = null;
    private int mOrientation = -1;
    private final TextureView mPreview;
    private RectF mPreviewArea = new RectF();
    private final ArrayList<PreviewAreaChangedListener> mPreviewSizeChangedListeners = new ArrayList();
    private SurfaceTextureListener mSurfaceTextureListener;
    private int mWidth = 0;

    public TextureViewHelper(TextureView preview, ModeTransitionView textureViewCover, CaptureLayoutHelper helper, CameraProvider cameraProvider) {
        this.mPreview = preview;
        this.mModeCover = textureViewCover;
        this.mCameraProvider = cameraProvider;
        this.mPreview.addOnLayoutChangeListener(this);
        this.mPreview.setSurfaceTextureListener(this);
        this.mCaptureLayoutHelper = helper;
    }

    public void setAutoAdjustTransform(boolean enable) {
        this.mAutoAdjustTransform = enable;
    }

    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        Log.v(TAG, "onLayoutChange");
        int width = right - left;
        int height = bottom - top;
        int rotation = CameraUtil.getDisplayRotation(this.mPreview.getContext());
        if (!(this.mWidth == width && this.mHeight == height && this.mOrientation == rotation)) {
            this.mWidth = width;
            this.mHeight = height;
            this.mOrientation = rotation;
            if (!updateTransform()) {
                clearTransform();
            }
        }
        if (this.mOnLayoutChangeListener != null) {
            this.mOnLayoutChangeListener.onLayoutChange(v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom);
            return;
        }
        int i = height;
    }

    public void clearTransform() {
        this.mPreview.setTransform(new Matrix());
        this.mPreviewArea.set(0.0f, 0.0f, (float) this.mWidth, (float) this.mHeight);
        onPreviewAreaChanged(this.mPreviewArea);
        setAspectRatio(0.0f);
    }

    public void updateAspectRatio(float aspectRatio) {
        Log.v(TAG, "updateAspectRatio");
        if (aspectRatio <= 0.0f) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid aspect ratio: ");
            stringBuilder.append(aspectRatio);
            Log.e(tag, stringBuilder.toString());
            return;
        }
        if (aspectRatio < 1.0f) {
            aspectRatio = 1.0f / aspectRatio;
        }
        setAspectRatio(aspectRatio);
        updateTransform();
    }

    private void setAspectRatio(float aspectRatio) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setAspectRatio: ");
        stringBuilder.append(aspectRatio);
        Log.v(tag, stringBuilder.toString());
        if (this.mAspectRatio != aspectRatio) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("aspect ratio changed from: ");
            stringBuilder.append(this.mAspectRatio);
            Log.v(tag, stringBuilder.toString());
            this.mAspectRatio = aspectRatio;
            onAspectRatioChanged();
        }
    }

    private void onAspectRatioChanged() {
        this.mCaptureLayoutHelper.onPreviewAspectRatioChanged(this.mAspectRatio);
        Iterator it = this.mAspectRatioChangedListeners.iterator();
        while (it.hasNext()) {
            ((PreviewAspectRatioChangedListener) it.next()).onPreviewAspectRatioChanged(this.mAspectRatio);
        }
    }

    public void addAspectRatioChangedListener(PreviewAspectRatioChangedListener listener) {
        if (listener != null && !this.mAspectRatioChangedListeners.contains(listener)) {
            this.mAspectRatioChangedListeners.add(listener);
        }
    }

    public RectF getFullscreenRect() {
        return this.mCaptureLayoutHelper.getFullscreenRect();
    }

    public void updateTransformFullScreen(Matrix matrix, float aspectRatio) {
        aspectRatio = aspectRatio < 1.0f ? 1.0f / aspectRatio : aspectRatio;
        if (aspectRatio != this.mAspectRatio) {
            setAspectRatio(aspectRatio);
        }
        this.mPreview.setTransform(matrix);
        this.mPreviewArea = this.mCaptureLayoutHelper.getPreviewRect();
        onPreviewAreaChanged(this.mPreviewArea);
    }

    public void updateTransform(Matrix matrix) {
        RectF previewRect = new RectF(0.0f, 0.0f, (float) this.mWidth, (float) this.mHeight);
        matrix.mapRect(previewRect);
        float previewWidth = previewRect.width();
        float previewHeight = previewRect.height();
        if (previewHeight == 0.0f || previewWidth == 0.0f) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid preview size: ");
            stringBuilder.append(previewWidth);
            stringBuilder.append(" x ");
            stringBuilder.append(previewHeight);
            Log.e(tag, stringBuilder.toString());
            return;
        }
        float aspectRatio = previewWidth / previewHeight;
        aspectRatio = aspectRatio < 1.0f ? 1.0f / aspectRatio : aspectRatio;
        if (aspectRatio != this.mAspectRatio) {
            setAspectRatio(aspectRatio);
        }
        RectF previewAreaBasedOnAspectRatio = this.mCaptureLayoutHelper.getPreviewRect();
        Matrix addtionalTransform = new Matrix();
        addtionalTransform.setRectToRect(previewRect, previewAreaBasedOnAspectRatio, ScaleToFit.CENTER);
        matrix.postConcat(addtionalTransform);
        this.mPreview.setTransform(matrix);
        updatePreviewArea(matrix);
    }

    private void updatePreviewArea(Matrix matrix) {
        this.mPreviewArea.set(0.0f, 0.0f, (float) this.mWidth, (float) this.mHeight);
        matrix.mapRect(this.mPreviewArea);
        onPreviewAreaChanged(this.mPreviewArea);
        if (this.mPreviewArea != null) {
            this.mModeCover.mapPreviewRect(this.mPreviewArea);
        }
    }

    public void setOnLayoutChangeListener(OnLayoutChangeListener listener) {
        this.mOnLayoutChangeListener = listener;
    }

    public void setSurfaceTextureListener(SurfaceTextureListener listener) {
        this.mSurfaceTextureListener = listener;
    }

    private boolean updateTransform() {
        Log.v(TAG, "updateTransform");
        if (!this.mAutoAdjustTransform) {
            return false;
        }
        if (this.mAspectRatio == 0.0f || this.mAspectRatio < 0.0f || this.mWidth == 0 || this.mHeight == 0) {
            return true;
        }
        Matrix matrix;
        int cameraId = this.mCameraProvider.getCurrentCameraId();
        if (cameraId >= 0) {
            matrix = this.mCameraProvider.getCharacteristics(cameraId).getPreviewTransform(this.mOrientation, new RectF(0.0f, 0.0f, (float) this.mWidth, (float) this.mHeight), this.mCaptureLayoutHelper.getPreviewRect());
        } else {
            Log.w(TAG, "Unable to find current camera... defaulting to identity matrix");
            matrix = new Matrix();
        }
        this.mPreview.setTransform(matrix);
        updatePreviewArea(matrix);
        return true;
    }

    private void onPreviewAreaChanged(final RectF previewArea) {
        final List<PreviewAreaChangedListener> listeners = new ArrayList(this.mPreviewSizeChangedListeners);
        this.mPreview.post(new Runnable() {
            public void run() {
                for (PreviewAreaChangedListener listener : listeners) {
                    listener.onPreviewAreaChanged(previewArea);
                }
            }
        });
    }

    public RectF getPreviewArea() {
        return new RectF(this.mPreviewArea);
    }

    public RectF getTextureArea() {
        if (this.mPreview == null) {
            return new RectF();
        }
        Matrix matrix = new Matrix();
        RectF area = new RectF(0.0f, 0.0f, (float) this.mWidth, (float) this.mHeight);
        this.mPreview.getTransform(matrix).mapRect(area);
        return area;
    }

    public Bitmap getPreviewBitmap(int downsample) {
        int height = (int) this.mPreviewArea.height();
        Bitmap preview = this.mPreview.getBitmap((int) this.mPreviewArea.width(), height);
        if (preview == null) {
            return null;
        }
        if (MotionPictureHelper.FRAME_HEIGHT_9 == height) {
            return BlurUtil.getSuitableMap(preview, 280);
        }
        if (960 == height) {
            return BlurUtil.getSuitableMap(preview, 170);
        }
        return BlurUtil.getSuitableMap(preview, HelpTip.HELP_TIP_SHOW_DELAY);
    }

    public void addPreviewAreaSizeChangedListener(PreviewAreaChangedListener listener) {
        if (listener != null && !this.mPreviewSizeChangedListeners.contains(listener)) {
            this.mPreviewSizeChangedListeners.add(listener);
            if (this.mPreviewArea.width() == 0.0f || this.mPreviewArea.height() == 0.0f) {
                listener.onPreviewAreaChanged(new RectF(0.0f, 0.0f, (float) this.mWidth, (float) this.mHeight));
            } else {
                listener.onPreviewAreaChanged(new RectF(this.mPreviewArea));
            }
        }
    }

    public void removePreviewAreaSizeChangedListener(PreviewAreaChangedListener listener) {
        if (listener != null && this.mPreviewSizeChangedListeners.contains(listener)) {
            this.mPreviewSizeChangedListeners.remove(listener);
        }
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (!(this.mWidth == 0 || this.mHeight == 0)) {
            updateTransform();
        }
        if (this.mSurfaceTextureListener != null) {
            this.mSurfaceTextureListener.onSurfaceTextureAvailable(surface, width, height);
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (this.mSurfaceTextureListener != null) {
            this.mSurfaceTextureListener.onSurfaceTextureSizeChanged(surface, width, height);
        }
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (this.mSurfaceTextureListener != null) {
            this.mSurfaceTextureListener.onSurfaceTextureDestroyed(surface);
        }
        return false;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.v(TAG, "on surfaceTexture updated");
        if (this.mSurfaceTextureListener != null) {
            this.mSurfaceTextureListener.onSurfaceTextureUpdated(surface);
        }
    }
}
