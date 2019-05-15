package com.hmdglobal.app.camera;

import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.hmdglobal.app.camera.FocusOverlayManager.FocusUI;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.debug.DebugPropertyHelper;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.EvoSlider;
import com.hmdglobal.app.camera.ui.EvoSlider.EVOChangedListener;
import com.hmdglobal.app.camera.ui.GestureView;
import com.hmdglobal.app.camera.ui.ModuleLayoutWrapper;
import com.hmdglobal.app.camera.ui.ModuleLayoutWrapper.OnAllViewRemovedListener;
import com.hmdglobal.app.camera.ui.PreviewOverlay;
import com.hmdglobal.app.camera.ui.PreviewStatusListener;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAreaChangedListener;
import com.hmdglobal.app.camera.ui.RotateLayout;
import com.hmdglobal.app.camera.ui.ZoomBar;
import com.hmdglobal.app.camera.ui.ZoomBar.ProgressChangeListener;
import com.hmdglobal.app.camera.ui.camera2.Camera2FaceView;
import com.hmdglobal.app.camera.util.CameraUtil;

class MoreUI implements PreviewStatusListener, PreviewAreaChangedListener, OnAllViewRemovedListener {
    private static final Tag TAG = new Tag("MoreModuleUI");
    private static final float UNSET = 0.0f;
    protected final CameraActivity mActivity;
    private float mAspectRatio = 0.0f;
    private final PhotoController mController;
    private final EvoSlider mEvoSlider;
    private final Camera2FaceView mFaceView;
    private final FocusUI mFocusUI;
    private final GestureView mGestureView;
    private ImageView mImage_google_lens;
    private ImageView mImage_square;
    private ImageView mIntentReviewImageView;
    private final PreviewOverlay mPreviewOverlay;
    private final View mRootView;
    private final ZoomBar mZoomBar;
    private final RotateLayout mZoomLayout;
    private final Button mbtn_google_lens;
    private final Button mbtn_panorama;
    private Button mbtn_square;
    private final LinearLayout mll_panorama;

    public void showEvoSlider() {
        this.mEvoSlider.setVisibility(8);
    }

    public boolean isCountingDown() {
        return false;
    }

    public void onPreviewAreaChanged(RectF previewArea) {
        if (this.mFaceView != null) {
            this.mFaceView.onPreviewAreaChanged(previewArea);
        }
    }

    public void onOrientationChanged(int orientation) {
        if (this.mZoomBar != null) {
            this.mZoomBar.setLayoutOrientation(orientation);
        }
    }

    public void onAllViewRemoved(AppController controller) {
    }

    public MoreUI(CameraActivity activity, PhotoController controller, View parent) {
        this.mActivity = activity;
        this.mController = controller;
        this.mRootView = parent;
        ModuleLayoutWrapper moduleRoot = (ModuleLayoutWrapper) this.mRootView.findViewById(R.id.module_layout);
        moduleRoot.setOnAllViewRemovedListener(this, this.mActivity);
        this.mActivity.getLayoutInflater().inflate(R.layout.more_module, moduleRoot, true);
        this.mFocusUI = (FocusUI) this.mRootView.findViewById(R.id.focus_overlay);
        ((View) this.mFocusUI).setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (MoreUI.this.mEvoSlider.getVisibility() == 0) {
                    MoreUI.this.mEvoSlider.dispatchTouchEvent(event);
                }
                return true;
            }
        });
        this.mEvoSlider = (EvoSlider) this.mRootView.findViewById(R.id.evo_slider);
        this.mEvoSlider.setEvoListener(new EVOChangedListener() {
            public void onEVOChanged(int value) {
                MoreUI.this.mController.onEvoChanged(value);
            }
        });
        this.mGestureView = (GestureView) this.mRootView.findViewById(R.id.gesture_view);
        this.mPreviewOverlay = (PreviewOverlay) this.mRootView.findViewById(R.id.preview_overlay);
        this.mZoomLayout = (RotateLayout) this.mRootView.findViewById(R.id.zoom_rotate_layout);
        this.mZoomBar = (ZoomBar) this.mRootView.findViewById(R.id.zoom_bar);
        this.mZoomBar.setProgressChangeListener(new ProgressChangeListener() {
            public void onProgressChanged(float ratio) {
                MoreUI.this.mPreviewOverlay.setRatio(ratio);
                MoreUI.this.mController.onZoomChanged(ratio);
            }
        });
        if (DebugPropertyHelper.showCaptureDebugUI() || DebugPropertyHelper.needShowFaceView()) {
            this.mFaceView = (Camera2FaceView) this.mRootView.findViewById(R.id.face_view);
        } else {
            this.mFaceView = null;
        }
        if (this.mController.isImageCaptureIntent()) {
            initIntentReviewImageView();
        }
        this.mRootView.findViewById(R.id.time_lapsed_container).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                MoreUI.this.mActivity.getCameraAppUI().onModeIdChanged(MoreUI.this.mActivity.getResources().getInteger(R.integer.camera_mode_time_lapse));
            }
        });
        this.mbtn_google_lens = (Button) this.mRootView.findViewById(R.id.btn_google_lens);
        this.mImage_google_lens = (ImageView) this.mRootView.findViewById(R.id.image_lens);
        if (CameraUtil.checkLensAvailability(this.mActivity)) {
            this.mbtn_google_lens.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    CameraUtil.launchLensApk(MoreUI.this.mActivity);
                }
            });
            this.mImage_google_lens.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    CameraUtil.launchLensApk(MoreUI.this.mActivity);
                }
            });
        } else {
            this.mImage_google_lens.setImageResource(R.drawable.lens_disable);
        }
        this.mbtn_panorama = (Button) this.mRootView.findViewById(R.id.btn_panorama);
        this.mbtn_panorama.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d(MoreUI.TAG, "onclick");
            }
        });
        this.mll_panorama = (LinearLayout) this.mRootView.findViewById(R.id.goto_pano);
        this.mll_panorama.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MoreUI.this.mActivity.getCameraAppUI().onModeSelected(MoreUI.this.mActivity.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pano));
            }
        });
        this.mbtn_square = (Button) this.mRootView.findViewById(R.id.btn_square);
        this.mImage_square = (ImageView) this.mRootView.findViewById(R.id.image_square);
        this.mbtn_square.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startSquraeMode();
            }

            private void startSquraeMode() {
                MoreUI.this.mActivity.onModeSelected(MoreUI.this.mActivity.getResources().getInteger(R.integer.camera_mode_square));
            }
        });
        this.mImage_square.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startSquraeMode();
            }

            private void startSquraeMode() {
                MoreUI.this.mActivity.onModeSelected(MoreUI.this.mActivity.getResources().getInteger(R.integer.camera_mode_square));
            }
        });
    }

    private void initIntentReviewImageView() {
        this.mIntentReviewImageView = (ImageView) this.mRootView.findViewById(R.id.intent_review_imageview);
    }

    public OnGestureListener getGestureListener() {
        return null;
    }

    public OnTouchListener getTouchListener() {
        return null;
    }

    public void onPreviewLayoutChanged(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
    }

    public boolean shouldAutoAdjustTransformMatrixOnLayout() {
        return false;
    }

    public boolean shouldAutoAdjustBottomBar() {
        return false;
    }

    public void onPreviewFlipped() {
        this.mController.updateCameraOrientation();
    }

    public void clearEvoPendingUI() {
    }

    public void hideIntentReviewImageView() {
        if (this.mIntentReviewImageView != null) {
            this.mIntentReviewImageView.setVisibility(4);
        }
    }

    public FocusUI getFocusUI() {
        return this.mFocusUI;
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
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
        if (this.mAspectRatio != aspectRatio) {
            this.mAspectRatio = aspectRatio;
            this.mController.updatePreviewAspectRatio(this.mAspectRatio);
        }
    }

    public void initEvoSlider(float x, float y) {
        this.mEvoSlider.setCoord(x, y);
    }

    public boolean onBackPressed() {
        if (this.mController.isImageCaptureIntent()) {
            this.mController.onCaptureCancelled();
            return true;
        } else if (this.mController.canCloseCamera()) {
            return false;
        } else {
            return true;
        }
    }

    /* Access modifiers changed, original: protected */
    public void hidePostCaptureAlert() {
    }

    public void setDisplayOrientation(int orientation) {
        if (this.mFaceView != null) {
            this.mFaceView.setDisplayOrientation(orientation);
        }
    }

    public void setGestureDisplayOrientation(int orientation) {
        if (this.mGestureView != null) {
            this.mGestureView.setDisplayOrientation(orientation);
        }
    }

    public void setPostGestureOrientation(int orientation) {
        if (this.mGestureView != null) {
            this.mGestureView.setPostGestureRotation(orientation);
        }
    }

    public void setSensorOrientation(int orientation) {
        if (this.mGestureView != null) {
            this.mGestureView.setSensorOrientation(orientation);
        }
    }

    public void setGestureMirrored(boolean isMirrored) {
        if (this.mGestureView != null) {
            this.mGestureView.setPreviewMirrored(isMirrored);
        }
    }

    public void setSwipingEnabled(boolean enable) {
        this.mActivity.setSwipingEnabled(enable);
    }

    public void onPause() {
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}
