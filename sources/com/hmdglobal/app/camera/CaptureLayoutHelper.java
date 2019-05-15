package com.hmdglobal.app.camera;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraAppUI.NonDecorWindowSizeChangedListener;
import com.hmdglobal.app.camera.app.CameraServices;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAspectRatioChangedListener;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class CaptureLayoutHelper implements NonDecorWindowSizeChangedListener, PreviewAspectRatioChangedListener {
    private static final int BOTTOM_BAR_TOP = 1120;
    private static final float DEFAULT_RATIO = 1.3333334f;
    public static final String TAG = "CaptureLayoutHelper";
    private static final int TOP_BAR_TOP = 80;
    private static float mConfiguredRatio = DEFAULT_RATIO;
    private float mAspectRatio = 0.0f;
    private final int mBottomBarMaxHeight;
    private final int mBottomBarMinHeight;
    private final int mBottomBarOptimalHeight;
    private final AppController mController;
    private int mDisplayHeight;
    private PositionConfiguration mPositionConfiguration = null;
    private int mRotation = 0;
    private CameraServices mService;
    private boolean mShowBottomBar = true;
    private int mWindowHeight = 0;
    private int mWindowWidth = 0;
    private int navigationBarHeight;

    public static final class PositionConfiguration {
        public boolean mBottomBarOverlay = false;
        public final RectF mBottomBarRect = new RectF();
        public final RectF mPreviewRect = new RectF();
    }

    public CaptureLayoutHelper(CameraServices cameraService, AppController controller, int bottomBarMinHeight, int bottomBarMaxHeight, int bottomBarOptimalHeight) {
        this.mService = cameraService;
        this.mBottomBarMinHeight = bottomBarMinHeight;
        this.mBottomBarMaxHeight = bottomBarMaxHeight;
        this.mBottomBarOptimalHeight = bottomBarOptimalHeight;
        this.mController = controller;
    }

    public void onPreviewAspectRatioChanged(float aspectRatio) {
        if (this.mAspectRatio != aspectRatio) {
            this.mAspectRatio = aspectRatio;
            updatePositionConfiguration();
        }
    }

    public void setShowBottomBar(boolean showBottomBar) {
        this.mShowBottomBar = showBottomBar;
    }

    private void updatePositionConfiguration() {
        int currentIndex = this.mController.getCurrentModuleIndex();
        if (this.mWindowWidth != 0 && this.mWindowHeight != 0) {
            if (currentIndex == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_square)) {
                this.mPositionConfiguration = getSquarePositionConfiguration(this.mWindowWidth, this.mWindowHeight, this.mAspectRatio, this.mRotation);
            } else {
                this.mPositionConfiguration = getPositionConfiguration(this.mWindowWidth, this.mWindowHeight, this.mAspectRatio, this.mRotation);
            }
        }
    }

    public RectF getBottomBarRect() {
        if (this.mPositionConfiguration == null) {
            updatePositionConfiguration();
        }
        if (this.mPositionConfiguration == null) {
            return new RectF();
        }
        return new RectF(this.mPositionConfiguration.mBottomBarRect);
    }

    public boolean isScreenReversed() {
        return this.mRotation >= MediaProviderUtils.ROTATION_180;
    }

    public RectF getPreviewRect() {
        if (this.mPositionConfiguration == null) {
            updatePositionConfiguration();
        }
        if (this.mPositionConfiguration == null) {
            return new RectF();
        }
        return new RectF(this.mPositionConfiguration.mPreviewRect);
    }

    public RectF getFullscreenRect() {
        return new RectF(0.0f, 0.0f, (float) this.mWindowWidth, (float) this.mWindowHeight);
    }

    public RectF getUncoveredPreviewRect() {
        if (this.mPositionConfiguration == null) {
            updatePositionConfiguration();
        }
        if (this.mPositionConfiguration == null) {
            return new RectF();
        }
        if (!RectF.intersects(this.mPositionConfiguration.mBottomBarRect, this.mPositionConfiguration.mPreviewRect) || !this.mShowBottomBar) {
            return this.mPositionConfiguration.mPreviewRect;
        }
        if (this.mWindowHeight > this.mWindowWidth) {
            if (this.mRotation < MediaProviderUtils.ROTATION_180 || this.mService.isReversibleEnabled()) {
                return new RectF(this.mPositionConfiguration.mPreviewRect.left, this.mPositionConfiguration.mPreviewRect.top, this.mPositionConfiguration.mPreviewRect.right, this.mPositionConfiguration.mBottomBarRect.top);
            }
            return new RectF(this.mPositionConfiguration.mPreviewRect.left, this.mPositionConfiguration.mBottomBarRect.bottom, this.mPositionConfiguration.mPreviewRect.right, this.mPositionConfiguration.mPreviewRect.bottom);
        } else if (this.mRotation < MediaProviderUtils.ROTATION_180 || this.mService.isReversibleEnabled()) {
            return new RectF(this.mPositionConfiguration.mPreviewRect.left, this.mPositionConfiguration.mPreviewRect.top, this.mPositionConfiguration.mBottomBarRect.left, this.mPositionConfiguration.mPreviewRect.bottom);
        } else {
            return new RectF(this.mPositionConfiguration.mBottomBarRect.right, this.mPositionConfiguration.mPreviewRect.top, this.mPositionConfiguration.mPreviewRect.right, this.mPositionConfiguration.mPreviewRect.bottom);
        }
    }

    public boolean shouldOverlayBottomBar() {
        if (this.mPositionConfiguration == null) {
            updatePositionConfiguration();
        }
        if (this.mPositionConfiguration == null) {
            return false;
        }
        return this.mPositionConfiguration.mBottomBarOverlay;
    }

    public void onNonDecorWindowSizeChanged(int width, int height, int rotation) {
        this.mWindowWidth = width;
        this.mWindowHeight = height;
        this.mRotation = rotation;
        updatePositionConfiguration();
    }

    private PositionConfiguration getPositionConfiguration(int width, int height, float previewAspectRatio, int rotation) {
        String str;
        int i = width;
        int i2 = height;
        boolean landscape = i > i2;
        PositionConfiguration config = new PositionConfiguration();
        int longerEdge = Math.max(width, height);
        int shorterEdge = Math.min(width, height);
        int barStartCoord = (int) (((float) shorterEdge) * mConfiguredRatio);
        if (landscape) {
            config.mBottomBarRect.set((float) barStartCoord, 0.0f, (float) i, (float) i2);
        } else {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("landscape = false, barStartCoord= ");
            stringBuilder.append(barStartCoord);
            stringBuilder.append(" width= ");
            stringBuilder.append(i);
            Log.d(str, stringBuilder.toString());
            config.mBottomBarRect.set(0.0f, 1120.0f, (float) i, (float) i2);
        }
        float f;
        if (previewAspectRatio == 0.0f) {
            config.mPreviewRect.set(0.0f, 0.0f, (float) i, (float) i2);
            config.mBottomBarOverlay = true;
            f = previewAspectRatio;
            int i3 = barStartCoord;
        } else {
            float previewAspectRatio2;
            if (previewAspectRatio < 1.0f) {
                f = 1.0f / previewAspectRatio;
            } else {
                f = previewAspectRatio;
            }
            float spaceNeededAlongLongerEdge = ((float) shorterEdge) * f;
            float remainingSpaceAlongLongerEdge = ((float) longerEdge) - spaceNeededAlongLongerEdge;
            float previewLongerEdge;
            float previewShorterEdge;
            float barSize;
            if (remainingSpaceAlongLongerEdge <= 0.0f) {
                previewLongerEdge = (float) longerEdge;
                previewShorterEdge = ((float) longerEdge) / f;
                barSize = (float) this.mBottomBarOptimalHeight;
                config.mBottomBarOverlay = true;
                Log.d(TAG, "remainingSpaceAlongLongerEdge <= 0");
                if (landscape) {
                    float f2 = barSize;
                    config.mPreviewRect.set(0.0f, ((float) (i2 / 2)) - (previewShorterEdge / 2.0f), previewLongerEdge, ((float) (i2 / 2)) + (previewShorterEdge / 2.0f));
                } else {
                    config.mPreviewRect.set(((float) (i / 2)) - (previewShorterEdge / 2.0f), 0.0f, ((float) (i / 2)) + (previewShorterEdge / 2.0f), previewLongerEdge);
                }
            } else {
                float previewShorterEdge2;
                float right;
                if (f > 1.5555556f) {
                    previewLongerEdge = (float) this.mBottomBarOptimalHeight;
                    previewShorterEdge2 = (float) shorterEdge;
                    barSize = ((float) shorterEdge) * f;
                    config.mBottomBarOverlay = true;
                    float f3;
                    if (landscape) {
                        Log.d(TAG, "previewAspectRatio > 14f / 9f and landscape = true");
                        right = (float) i;
                        previewShorterEdge = right - barSize;
                        if (previewShorterEdge < ((float) this.navigationBarHeight)) {
                            config.mPreviewRect.set(0.0f, 0.0f, right - ((float) this.navigationBarHeight), previewShorterEdge2);
                        } else {
                            f3 = spaceNeededAlongLongerEdge;
                            config.mPreviewRect.set(0.0f, 0.0f, right - previewShorterEdge, previewShorterEdge2);
                        }
                    } else {
                        f3 = spaceNeededAlongLongerEdge;
                        previewLongerEdge = (float) i2;
                        right = previewLongerEdge - barSize;
                        if (right < ((float) this.navigationBarHeight)) {
                            Log.d(TAG, "top < navigationBarHeight");
                            config.mPreviewRect.set(0.0f, 0.0f, previewShorterEdge2, previewLongerEdge);
                        } else {
                            str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("top > navigationBarHeight   bottom");
                            stringBuilder2.append(previewLongerEdge);
                            stringBuilder2.append(" top ");
                            stringBuilder2.append(right);
                            stringBuilder2.append("  previewShorterEdge  ");
                            stringBuilder2.append(previewShorterEdge2);
                            Log.d(str, stringBuilder2.toString());
                            float f4 = previewLongerEdge;
                            config.mPreviewRect.set(0.0f, 150.0f, previewShorterEdge2, (previewLongerEdge - right) + 150.0f);
                        }
                    }
                } else {
                    if (remainingSpaceAlongLongerEdge <= ((float) this.mBottomBarMinHeight)) {
                        right = (float) (longerEdge - this.mBottomBarMinHeight);
                        previewShorterEdge2 = right / f;
                        barSize = (float) this.mBottomBarMinHeight;
                        config.mBottomBarOverlay = false;
                        if (landscape) {
                            config.mPreviewRect.set(0.0f, ((float) (i2 / 2)) - (previewShorterEdge2 / 2.0f), right, ((float) (i2 / 2)) + (previewShorterEdge2 / 2.0f));
                        } else {
                            Log.d(TAG, "remainingSpaceAlongLongerEdge <= mBottomBarMinHeight and landscape = false");
                            config.mPreviewRect.set(((float) (i / 2)) - (previewShorterEdge2 / 2.0f), 0.0f, ((float) (i / 2)) + (previewShorterEdge2 / 2.0f), right);
                        }
                    } else {
                        if (f == 1.2222222f || f == 0.8181818f) {
                        }
                        previewShorterEdge2 = remainingSpaceAlongLongerEdge <= ((float) this.mBottomBarMaxHeight) ? remainingSpaceAlongLongerEdge : (float) this.mBottomBarMaxHeight;
                        barSize = (float) shorterEdge;
                        spaceNeededAlongLongerEdge = ((float) shorterEdge) * f;
                        config.mBottomBarOverlay = false;
                        if (landscape) {
                            previewLongerEdge = ((float) i) - previewShorterEdge2;
                            previewAspectRatio2 = f;
                            config.mPreviewRect.set(previewLongerEdge - spaceNeededAlongLongerEdge, 0.0f, previewLongerEdge, barSize);
                        } else {
                            previewAspectRatio2 = f;
                            f = config.mBottomBarRect.bottom - config.mBottomBarRect.top;
                            previewLongerEdge = ((float) i2) - f;
                            previewShorterEdge2 = previewLongerEdge - spaceNeededAlongLongerEdge;
                            String str2 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("previewShorterEdge   ");
                            stringBuilder3.append(barSize);
                            stringBuilder3.append(" top  ");
                            stringBuilder3.append(previewShorterEdge2);
                            Log.d(str2, stringBuilder3.toString());
                            float f5 = previewLongerEdge;
                            config.mPreviewRect.set(0.0f, 170.0f, barSize, 1130.0f);
                        }
                    }
                }
            }
            previewAspectRatio2 = f;
        }
        if (rotation >= 180 && !this.mService.isReversibleEnabled()) {
            Matrix rotate = new Matrix();
            rotate.setRotate(180.0f, (float) (i / 2), (float) (i2 / 2));
            rotate.mapRect(config.mPreviewRect);
            rotate.mapRect(config.mBottomBarRect);
        }
        round(config.mBottomBarRect);
        round(config.mPreviewRect);
        return config;
    }

    private PositionConfiguration getSquarePositionConfiguration(int width, int height, float previewAspectRatio, int rotation) {
        int i = width;
        int i2 = height;
        boolean landscape = i > i2;
        PositionConfiguration config = new PositionConfiguration();
        int longerEdge = Math.max(width, height);
        int shorterEdge = Math.min(width, height);
        int barStartCoord = (int) (((float) shorterEdge) * mConfiguredRatio);
        int squareTop = (barStartCoord - shorterEdge) / 2;
        int squareHeight = squareTop + shorterEdge;
        if (landscape) {
            config.mBottomBarRect.set((float) barStartCoord, 0.0f, (float) i, (float) i2);
        } else {
            config.mBottomBarRect.set(0.0f, 1120.0f, (float) i, (float) i2);
            squareTop = ((1120 - shorterEdge) / 2) + 80;
            squareHeight = squareTop + shorterEdge;
        }
        config.mPreviewRect.set(0.0f, (float) squareTop, (float) i, (float) squareHeight);
        if (rotation < 180) {
        } else if (!this.mService.isReversibleEnabled()) {
            Matrix rotate = new Matrix();
            rotate.setRotate(180.0f, (float) (i / 2), (float) (i2 / 2));
            rotate.mapRect(config.mPreviewRect);
            rotate.mapRect(config.mBottomBarRect);
        }
        round(config.mBottomBarRect);
        round(config.mPreviewRect);
        return config;
    }

    public static void round(RectF rect) {
        if (rect != null) {
            rect.set((float) Math.round(rect.left), (float) Math.round(rect.top), (float) Math.round(rect.right), (float) Math.round(rect.bottom));
        }
    }

    public void setNavigationBarHeight(int height) {
        this.navigationBarHeight = height;
    }

    public void setDisplayHeight(int height) {
        this.mDisplayHeight = height;
    }
}
