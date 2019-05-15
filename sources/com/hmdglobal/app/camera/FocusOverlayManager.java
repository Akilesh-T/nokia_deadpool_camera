package com.hmdglobal.app.camera;

import android.annotation.TargetApi;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Area;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraCapabilities.Feature;
import com.android.ex.camera2.portability.CameraCapabilities.FocusMode;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.MotionManager.MotionListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.one.Settings3A;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAreaChangedListener;
import com.hmdglobal.app.camera.ui.TouchCoordinate;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.UsageStatistics;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class FocusOverlayManager implements PreviewAreaChangedListener, MotionListener {
    public static int ACTION_KEEP_FOCUS_FRAME = 4;
    public static int ACTION_NONE = 0;
    public static int ACTION_PRESS_SHUTTER = 1;
    public static int ACTION_RESTORE_CAF_LATER = 2;
    public static final float AE_REGION_BOX = Settings3A.getMeteringRegionWidth();
    public static final float AF_REGION_BOX = Settings3A.getAutoFocusRegionWidth();
    private static final int RESET_TOUCH_FOCUS = 0;
    private static final int RESET_TOUCH_FOCUS_DELAY_MILLIS = Settings3A.getFocusHoldMillis();
    private static final int STATE_FAIL = 5;
    private static final int STATE_FOCUSING = 1;
    private static final int STATE_FOCUSING_SNAP_ON_FINISH = 2;
    private static final int STATE_IDLE = 0;
    private static final int STATE_LOCKED = 6;
    private static final int STATE_SUCCESS = 3;
    private static final int STATE_SUCCESS_AND_STAY = 4;
    private static final Tag TAG = new Tag("FocusOverlayMgr");
    private boolean mAeAwbLock;
    private final AppController mAppController;
    private CameraCapabilities mCapabilities;
    private FocusArea mCaptureFocusArea;
    private final List<FocusMode> mDefaultFocusModes;
    private int mDisplayOrientation;
    private boolean mEightMiao;
    private List<Area> mFocusArea;
    private boolean mFocusAreaSupported;
    private boolean mFocusLocked;
    private FocusMode mFocusMode;
    private final Handler mHandler;
    private boolean mInitialized;
    Listener mListener;
    private boolean mLockAeAwbNeeded;
    private final Matrix mMatrix;
    private List<Area> mMeteringArea;
    private boolean mMeteringAreaSupported;
    private boolean mMirror;
    private FocusMode mOverrideFocusMode;
    private final Rect mPreviewRect = new Rect(0, 0, 0, 0);
    private boolean mPreviousMoving;
    private final SettingsManager mSettingsManager;
    private int mState = 0;
    private TouchCoordinate mTouchCoordinate;
    private long mTouchTime;
    private final FocusUI mUI;

    public interface FaceDetector {
        boolean hasFaces();

        void pauseFaceDetection();

        void resumeFaceDetection();
    }

    public class FocusArea {
        public int x;
        public int y;

        public FocusArea(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public interface Listener {
        void autoFocus();

        boolean cancelAutoFocus();

        boolean capture();

        void setFocusParameters();

        void startFaceDetection();

        void stopFaceDetection();
    }

    private static class MainHandler extends Handler {
        final WeakReference<FocusOverlayManager> mManager;

        public MainHandler(FocusOverlayManager manager, Looper looper) {
            super(looper);
            this.mManager = new WeakReference(manager);
        }

        public void handleMessage(Message msg) {
            FocusOverlayManager manager = (FocusOverlayManager) this.mManager.get();
            if (manager != null && msg.what == 0) {
                manager.cancelAutoFocus();
                manager.mListener.startFaceDetection();
            }
        }
    }

    public interface FocusUI extends FaceDetector {
        void clearFocus();

        void keepFocusFrame();

        void onFocusFailed();

        void onFocusFailedAndStay();

        void onFocusStarted();

        void onFocusSucceeded();

        void onFocusSucceededAndStay();

        void setFaceDetector(FaceDetector faceDetector);

        void setFocusPosition(int i, int i2, boolean z);

        void setFocusPosition(int i, int i2, boolean z, int i3, int i4);

        void setPassiveFocusSuccess(boolean z);

        void setPreviewRect(Rect rect);

        void showDebugMessage(String str);
    }

    public FocusOverlayManager(AppController appController, List<FocusMode> defaultFocusModes, CameraCapabilities capabilities, Listener listener, boolean mirror, Looper looper, FocusUI ui) {
        this.mAppController = appController;
        this.mSettingsManager = appController.getSettingsManager();
        this.mHandler = new MainHandler(this, looper);
        this.mMatrix = new Matrix();
        this.mDefaultFocusModes = new ArrayList(defaultFocusModes);
        updateCapabilities(capabilities);
        this.mListener = listener;
        setMirror(mirror);
        this.mUI = ui;
        this.mFocusLocked = false;
    }

    public void updateCapabilities(CameraCapabilities capabilities) {
        if (capabilities != null) {
            this.mCapabilities = capabilities;
            this.mFocusAreaSupported = this.mCapabilities.supports(Feature.FOCUS_AREA);
            this.mMeteringAreaSupported = this.mCapabilities.supports(Feature.METERING_AREA);
            boolean z = this.mCapabilities.supports(Feature.AUTO_EXPOSURE_LOCK) || this.mCapabilities.supports(Feature.AUTO_WHITE_BALANCE_LOCK);
            this.mLockAeAwbNeeded = z;
        }
    }

    public void setPreviewRect(Rect previewRect) {
        if (!this.mPreviewRect.equals(previewRect)) {
            this.mPreviewRect.set(previewRect);
            setMatrix();
        }
    }

    public void redirectToExposureSlider(float x, float y) {
    }

    public void keepFocusFrame() {
        this.mUI.keepFocusFrame();
        this.mHandler.removeMessages(0);
    }

    public void onPreviewAreaChanged(RectF previewArea) {
        setPreviewRect(CameraUtil.rectFToRect(previewArea));
        this.mCaptureFocusArea = new FocusArea(this.mPreviewRect.centerX(), this.mPreviewRect.centerY());
        this.mUI.setPreviewRect(this.mPreviewRect);
    }

    public Rect getPreviewRect() {
        return new Rect(this.mPreviewRect);
    }

    public void setMirror(boolean mirror) {
        this.mMirror = mirror;
        setMatrix();
    }

    public void setDisplayOrientation(int displayOrientation) {
        this.mDisplayOrientation = displayOrientation;
        setMatrix();
    }

    private void setMatrix() {
        if (this.mPreviewRect.width() != 0 && this.mPreviewRect.height() != 0) {
            Matrix matrix = new Matrix();
            CameraUtil.prepareMatrix(matrix, this.mMirror, this.mDisplayOrientation, getPreviewRect());
            matrix.invert(this.mMatrix);
            this.mInitialized = true;
            this.mPreviousMoving = false;
        }
    }

    private void lockAeAwbIfNeeded() {
        if (this.mLockAeAwbNeeded && !this.mAeAwbLock) {
            this.mAeAwbLock = true;
            this.mListener.setFocusParameters();
        }
    }

    private void unlockAeAwbIfNeeded() {
        if (this.mLockAeAwbNeeded && this.mAeAwbLock && this.mState != 2) {
            this.mAeAwbLock = false;
            this.mListener.setFocusParameters();
        }
    }

    public void onShutterUp(FocusMode currentFocusMode) {
        if (this.mInitialized) {
            if (needAutoFocusCall(currentFocusMode) && (this.mState == 1 || this.mState == 3 || this.mState == 4 || this.mState == 5)) {
                cancelAutoFocus();
            }
            unlockAeAwbIfNeeded();
        }
    }

    public void focusAndCapture(FocusMode currentFocusMode) {
        if (this.mInitialized) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mState is ");
            stringBuilder.append(this.mState);
            Log.w(tag, stringBuilder.toString());
            if (needAutoFocusCall(currentFocusMode)) {
                if (this.mState == 3 || this.mState == 4 || this.mState == 5 || this.mState == 6) {
                    capture();
                } else if (this.mState == 1) {
                    this.mState = 2;
                } else if (this.mState == 0) {
                    autoFocusAndCapture();
                }
            } else if (currentFocusMode == FocusMode.FIXED) {
                autoFocusAndCapture();
            } else {
                capture();
            }
        }
    }

    public void onAutoFocus(boolean focused, int action) {
        boolean shutterButtonPressed = (ACTION_PRESS_SHUTTER & action) != 0;
        boolean needResetFocusLater = (ACTION_RESTORE_CAF_LATER & action) != 0;
        boolean needKeepFocusFrame = (ACTION_KEEP_FOCUS_FRAME & action) != 0;
        int i = 3;
        if (this.mState == 2) {
            if (focused) {
                if (needKeepFocusFrame) {
                    i = 4;
                }
                this.mState = i;
            } else {
                this.mState = 5;
            }
            updateFocusUI();
            capture();
        } else if (this.mState == 1) {
            if (focused) {
                if (needKeepFocusFrame) {
                    i = 4;
                }
                this.mState = i;
            } else {
                this.mState = 5;
            }
            updateFocusUI();
            if (this.mFocusArea != null) {
                this.mFocusLocked = true;
                if (!needResetFocusLater || this.mEightMiao) {
                    this.mHandler.sendEmptyMessageDelayed(0, 8000);
                } else {
                    this.mHandler.sendEmptyMessageDelayed(0, (long) RESET_TOUCH_FOCUS_DELAY_MILLIS);
                }
            }
            if (shutterButtonPressed) {
                lockAeAwbIfNeeded();
            }
        } else {
            int i2 = this.mState;
        }
    }

    public void onAutoFocusMoving(boolean moving) {
        if (!this.mInitialized) {
            return;
        }
        if (this.mUI.hasFaces()) {
            this.mUI.clearFocus();
        } else if (this.mState == 0) {
            if (moving && !this.mPreviousMoving) {
                this.mUI.setFocusPosition(this.mPreviewRect.centerX(), this.mPreviewRect.centerY(), true, getAFRegionEdge(), getAERegionEdge());
                this.mUI.onFocusStarted();
            } else if (!moving) {
                this.mUI.onFocusSucceeded();
            }
            this.mPreviousMoving = moving;
        }
    }

    private int getAFRegionEdge() {
        return (int) (((float) Math.min(this.mPreviewRect.width(), this.mPreviewRect.height())) * AF_REGION_BOX);
    }

    private int getAERegionEdge() {
        return (int) (((float) Math.min(this.mPreviewRect.width(), this.mPreviewRect.height())) * AE_REGION_BOX);
    }

    @TargetApi(14)
    private void initializeFocusAreas(int x, int y) {
        if (this.mFocusArea == null) {
            this.mFocusArea = new ArrayList();
            this.mFocusArea.add(new Area(new Rect(), 1));
        }
        calculateTapArea(x, y, getAFRegionEdge(), ((Area) this.mFocusArea.get(0)).rect);
    }

    @TargetApi(14)
    private void initializeMeteringAreas(int x, int y) {
        if (this.mMeteringArea == null) {
            this.mMeteringArea = new ArrayList();
            this.mMeteringArea.add(new Area(new Rect(), 1));
        }
        calculateTapArea(x, y, getAERegionEdge(), ((Area) this.mMeteringArea.get(0)).rect);
    }

    /* JADX WARNING: Missing block: B:45:0x00c2, code skipped:
            return;
     */
    public void onSingleTapUp(int r8, int r9) {
        /*
        r7 = this;
        r0 = r7.mInitialized;
        if (r0 == 0) goto L_0x00c2;
    L_0x0004:
        r0 = r7.mState;
        r1 = 2;
        if (r0 != r1) goto L_0x000b;
    L_0x0009:
        goto L_0x00c2;
    L_0x000b:
        r0 = r7.mPreviewRect;
        r0 = r0.left;
        if (r8 < r0) goto L_0x00c1;
    L_0x0011:
        r0 = r7.mPreviewRect;
        r0 = r0.right;
        if (r8 > r0) goto L_0x00c1;
    L_0x0017:
        r0 = r7.mPreviewRect;
        r0 = r0.top;
        if (r9 < r0) goto L_0x00c1;
    L_0x001d:
        r0 = r7.mPreviewRect;
        r0 = r0.bottom;
        if (r9 <= r0) goto L_0x0025;
    L_0x0023:
        goto L_0x00c1;
    L_0x0025:
        r0 = r7.mFocusArea;
        if (r0 == 0) goto L_0x0040;
    L_0x0029:
        r0 = r7.mState;
        r1 = 1;
        if (r0 == r1) goto L_0x003d;
    L_0x002e:
        r0 = r7.mState;
        r1 = 3;
        if (r0 == r1) goto L_0x003d;
    L_0x0033:
        r0 = r7.mState;
        r1 = 4;
        if (r0 == r1) goto L_0x003d;
    L_0x0038:
        r0 = r7.mState;
        r1 = 5;
        if (r0 != r1) goto L_0x0040;
    L_0x003d:
        r7.cancelAutoFocus();
    L_0x0040:
        r0 = r7.mPreviewRect;
        r0 = r0.width();
        if (r0 == 0) goto L_0x00c0;
    L_0x0048:
        r0 = r7.mPreviewRect;
        r0 = r0.height();
        if (r0 != 0) goto L_0x0051;
    L_0x0050:
        goto L_0x00c0;
    L_0x0051:
        r0 = r7.mFocusAreaSupported;
        if (r0 != 0) goto L_0x0059;
    L_0x0055:
        r0 = r7.mMeteringAreaSupported;
        if (r0 == 0) goto L_0x005c;
    L_0x0059:
        r7.initializeFocusAreas(r8, r9);
    L_0x005c:
        r0 = r7.mMeteringAreaSupported;
        if (r0 == 0) goto L_0x0063;
    L_0x0060:
        r7.initializeMeteringAreas(r8, r9);
    L_0x0063:
        r1 = r7.mUI;
        r4 = 0;
        r5 = r7.getAFRegionEdge();
        r6 = r7.getAERegionEdge();
        r2 = r8;
        r3 = r9;
        r1.setFocusPosition(r2, r3, r4, r5, r6);
        r0 = new com.hmdglobal.app.camera.ui.TouchCoordinate;
        r1 = (float) r8;
        r2 = (float) r9;
        r3 = r7.mPreviewRect;
        r3 = r3.width();
        r3 = (float) r3;
        r4 = r7.mPreviewRect;
        r4 = r4.height();
        r4 = (float) r4;
        r0.<init>(r1, r2, r3, r4);
        r7.mTouchCoordinate = r0;
        r0 = java.lang.System.currentTimeMillis();
        r7.mTouchTime = r0;
        r0 = r7.mListener;
        r0.stopFaceDetection();
        r0 = r7.mListener;
        r0.setFocusParameters();
        r0 = r7.mFocusAreaSupported;
        if (r0 != 0) goto L_0x00b5;
    L_0x009e:
        r0 = r7.mMeteringAreaSupported;
        if (r0 == 0) goto L_0x00a3;
    L_0x00a2:
        goto L_0x00b5;
    L_0x00a3:
        r7.updateFocusUI();
        r0 = r7.mHandler;
        r1 = 0;
        r0.removeMessages(r1);
        r0 = r7.mHandler;
        r2 = RESET_TOUCH_FOCUS_DELAY_MILLIS;
        r2 = (long) r2;
        r0.sendEmptyMessageDelayed(r1, r2);
        goto L_0x00b8;
    L_0x00b5:
        r7.autoFocus();
    L_0x00b8:
        r0 = new com.hmdglobal.app.camera.FocusOverlayManager$FocusArea;
        r0.<init>(r8, r9);
        r7.mCaptureFocusArea = r0;
        return;
    L_0x00c0:
        return;
    L_0x00c1:
        return;
    L_0x00c2:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.FocusOverlayManager.onSingleTapUp(int, int):void");
    }

    public void onPreviewStarted() {
        this.mState = 0;
        resetTouchFocus();
    }

    public void onPreviewStopped() {
        this.mState = 0;
        updateFocusUI();
    }

    public void onCameraReleased() {
        onPreviewStopped();
    }

    public void onMoving() {
        if (this.mFocusLocked) {
            Log.d(TAG, "onMoving: Early focus unlock.");
            cancelAutoFocus();
        }
    }

    private void autoFocus(int focusingState) {
        this.mState = focusingState;
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update focus state to ");
        stringBuilder.append(this.mState);
        Log.w(tag, stringBuilder.toString());
        this.mListener.autoFocus();
        updateFocusUI();
        this.mHandler.removeMessages(0);
    }

    private void autoFocus() {
        autoFocus(1);
    }

    private void autoFocusAndCapture() {
        autoFocus(2);
    }

    public void cancelAutoFocus() {
        Log.v(TAG, "Cancel autofocus.");
        resetTouchFocus();
        if (this.mListener.cancelAutoFocus()) {
            this.mUI.resumeFaceDetection();
            this.mState = 0;
            this.mFocusLocked = false;
            updateFocusUI();
        } else {
            this.mState = 6;
        }
        this.mHandler.removeMessages(0);
    }

    private void capture() {
        if (this.mListener.capture()) {
            if (this.mState != 6) {
                this.mState = 0;
            }
            this.mHandler.removeMessages(0);
        }
    }

    public FocusMode getFocusMode(FocusMode currentFocusMode) {
        Tag tag;
        StringBuilder stringBuilder;
        if (this.mOverrideFocusMode != null) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("returning override focus: ");
            stringBuilder.append(this.mOverrideFocusMode);
            Log.v(tag, stringBuilder.toString());
            return this.mOverrideFocusMode;
        } else if (this.mCapabilities == null) {
            Log.v(TAG, "no capabilities, returning default AUTO focus mode");
            return FocusMode.AUTO;
        } else {
            if (!this.mFocusAreaSupported || this.mFocusArea == null) {
                String focusSetting = this.mSettingsManager.getString(this.mAppController.getCameraScope(), Keys.KEY_FOCUS_MODE);
                Tag tag2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("stored focus setting for camera: ");
                stringBuilder2.append(focusSetting);
                Log.v(tag2, stringBuilder2.toString());
                this.mFocusMode = this.mCapabilities.getStringifier().focusModeFromString(focusSetting);
                tag2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("focus mode resolved from setting: ");
                stringBuilder2.append(this.mFocusMode);
                Log.v(tag2, stringBuilder2.toString());
                if (this.mFocusMode == null) {
                    for (FocusMode mode : this.mDefaultFocusModes) {
                        if (this.mCapabilities.supports(mode)) {
                            this.mFocusMode = mode;
                            tag2 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("selected supported focus mode from default list");
                            stringBuilder3.append(mode);
                            Log.v(tag2, stringBuilder3.toString());
                            break;
                        }
                    }
                }
            } else {
                Log.v(TAG, "in tap to focus, returning AUTO focus mode");
                this.mFocusMode = FocusMode.AUTO;
            }
            if (!this.mCapabilities.supports(this.mFocusMode)) {
                if (this.mCapabilities.supports(FocusMode.AUTO)) {
                    Log.v(TAG, "no supported focus mode, falling back to AUTO");
                    this.mFocusMode = FocusMode.AUTO;
                } else {
                    tag = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("no supported focus mode, falling back to current: ");
                    stringBuilder.append(currentFocusMode);
                    Log.v(tag, stringBuilder.toString());
                    this.mFocusMode = currentFocusMode;
                }
            }
            return this.mFocusMode;
        }
    }

    public FocusArea getCaptureFocusArea() {
        return this.mCaptureFocusArea;
    }

    public List<Area> getFocusAreas() {
        return this.mFocusArea;
    }

    public List<Area> getMeteringAreas() {
        return this.mMeteringArea;
    }

    public void updateFocusUI() {
        if (this.mInitialized) {
            if (this.mState == 0) {
                if (this.mFocusArea == null) {
                    this.mUI.clearFocus();
                } else {
                    this.mUI.onFocusStarted();
                }
            } else if (this.mState == 1) {
                this.mUI.onFocusStarted();
            } else if (this.mState == 6) {
                this.mUI.keepFocusFrame();
            } else if (this.mFocusMode == FocusMode.CONTINUOUS_PICTURE) {
                this.mUI.onFocusSucceeded();
            } else if (this.mState == 4) {
                this.mUI.onFocusSucceededAndStay();
            } else if (this.mState == 3) {
                this.mUI.onFocusSucceeded();
            } else if (this.mState == 5) {
                this.mUI.onFocusFailed();
            }
        }
    }

    public void resetTouchFocus() {
        if (this.mInitialized) {
            this.mUI.clearFocus();
            this.mFocusArea = null;
            this.mMeteringArea = null;
            this.mCaptureFocusArea = new FocusArea(this.mPreviewRect.centerX(), this.mPreviewRect.centerY());
            this.mListener.setFocusParameters();
            if (this.mTouchCoordinate != null) {
                UsageStatistics.instance().tapToFocus(this.mTouchCoordinate, Float.valueOf(0.001f * ((float) (System.currentTimeMillis() - this.mTouchTime))));
                this.mTouchCoordinate = null;
            }
        }
    }

    private void calculateTapArea(int x, int y, int size, Rect rect) {
        int left = CameraUtil.clamp(x - (size / 2), this.mPreviewRect.left, this.mPreviewRect.right - size);
        int top = CameraUtil.clamp(y - (size / 2), this.mPreviewRect.top, this.mPreviewRect.bottom - size);
        RectF rectF = new RectF((float) left, (float) top, (float) (left + size), (float) (top + size));
        this.mMatrix.mapRect(rectF);
        CameraUtil.rectFToRect(rectF, rect);
    }

    /* Access modifiers changed, original: 0000 */
    public int getFocusState() {
        return this.mState;
    }

    public boolean isFocusCompleted() {
        return this.mState == 3 || this.mState == 4 || this.mState == 5;
    }

    public boolean isFocusingSnapOnFinish() {
        return this.mState == 2;
    }

    public void removeMessages() {
        this.mPreviousMoving = false;
        this.mHandler.removeMessages(0);
    }

    public void removeFoucsMessages() {
        this.mHandler.removeMessages(0);
    }

    public void isEight() {
        this.mEightMiao = true;
    }

    public void noEight() {
        this.mEightMiao = false;
    }

    public void sendMessage() {
        this.mHandler.sendEmptyMessageDelayed(0, 0);
    }

    public void overrideFocusMode(FocusMode focusMode) {
        this.mOverrideFocusMode = focusMode;
    }

    public void setAeAwbLock(boolean lock) {
        this.mAeAwbLock = lock;
    }

    public boolean getAeAwbLock() {
        return this.mAeAwbLock;
    }

    private boolean needAutoFocusCall(FocusMode focusMode) {
        return (focusMode == FocusMode.INFINITY || focusMode == FocusMode.FIXED || focusMode == FocusMode.EXTENDED_DOF || focusMode == FocusMode.MANUAL) ? false : true;
    }
}
