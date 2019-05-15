package com.hmdglobal.app.camera.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import com.google.common.primitives.Ints;
import com.hmdglobal.app.camera.CaptureLayoutHelper;
import com.hmdglobal.app.camera.MultiToggleImageButton;
import com.hmdglobal.app.camera.OnCancelSelectionMenuListener;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.ShutterButton;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.BottomMockViewAnimationDrawable.OnAnimationAsyncListener;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import java.util.HashMap;
import java.util.Map;

public class BottomBar extends FrameLayout {
    private static final int CIRCLE_ANIM_DURATION_MS = 500;
    private static final int DRAWABLE_MAX_LEVEL = 10000;
    private static final int MODE_CANCEL = 3;
    private static final int MODE_CAPTURE = 0;
    private static final int MODE_INTENT = 1;
    private static final int MODE_INTENT_REVIEW = 2;
    private static final Tag TAG = new Tag("BottomBar");
    private final boolean bVdfModeSwitchOn;
    private AnimatedCircleDrawable mAnimatedCircleDrawable;
    private Map<View, Integer> mAnimationViewLockMap = new HashMap();
    private int mBackgroundAlpha = 255;
    private final int mBackgroundAlphaDefault = getResources().getInteger(R.integer.bottom_bar_background_alpha);
    private final int mBackgroundAlphaOverlay = getResources().getInteger(R.integer.bottom_bar_background_alpha_overlay);
    private int mBackgroundColor;
    private int mBackgroundPressedColor;
    private int mCachedColor = 0;
    private RotatableButton mCancelButton;
    private FrameLayout mCancelLayout;
    private RotatableButton mCaptureButton;
    private FrameLayout mCaptureLayout;
    private CaptureLayoutHelper mCaptureLayoutHelper = null;
    private final float mCircleRadius = ((float) (getResources().getDimensionPixelSize(R.dimen.video_capture_circle_diameter) / 2));
    private ColorDrawable mColorDrawable;
    private boolean mDrawCircle;
    private MultiToggleImageButton mEffectsEnterToggleButton;
    private ValueAnimator[] mHidePeekAnimator = new ValueAnimator[1];
    private ValueAnimator[] mHideRemix = new ValueAnimator[1];
    private ValueAnimator[] mHideSegmentRemove = new ValueAnimator[1];
    private ValueAnimator[] mHideShutterButton = new ValueAnimator[1];
    private ValueAnimator[] mHideVideoCaptureAnimator = new ValueAnimator[1];
    private ValueAnimator[] mHideVideoPauseAnimator = new ValueAnimator[1];
    private ValueAnimator[] mHideVideoShutterAnimator = new ValueAnimator[1];
    private TopRightWeightedLayout mIntentReviewLayout;
    private boolean mIsBackgroundTransparent;
    private View mMockBar;
    private BottomMockViewAnimationDrawable mMockViewAnimationDrawable;
    private int mMode;
    private View mModeStrip;
    private MultiToggleImageButton mMoreToggleButton;
    private float mMoveY;
    private boolean mOverLayBottomBar;
    private Integer mOverridenColor = null;
    private RotatableButton mPauseButton;
    private PeekImageView mPeekThumb;
    private RectF mRect = new RectF();
    private View mRemixButton;
    private int mResId = -1;
    private RotatableButton mRotatableButton;
    private MultiToggleImageButton mRotateButton;
    private View mSegmentRemoveButton;
    private ValueAnimator[] mShowPeekAnmator = new ValueAnimator[1];
    private ValueAnimator[] mShowRemix = new ValueAnimator[1];
    private ValueAnimator[] mShowSegmentRemove = new ValueAnimator[1];
    private ValueAnimator[] mShowShutterButton = new ValueAnimator[1];
    private ValueAnimator[] mShowVideoCaptureAnimator = new ValueAnimator[1];
    private ValueAnimator[] mShowVideoPauseAnimator = new ValueAnimator[1];
    private ValueAnimator[] mShowVideoShutterAnimator = new ValueAnimator[1];
    private ShutterButton mShutterButton;
    private final ConstantState[] mShutterButtonBackgroundConstantStates;
    private final int mVideoButtonColor;

    public interface BottomBarSizeListener {
        void onFullSizeReached();
    }

    public interface OnArcSeekBarListener {
        void slipDownShutterButton();

        void slipUpShutterButton();
    }

    public void setOnArcSeekBarListener(OnArcSeekBarListener onArcSeekBarListener) {
        if (this.mShutterButton != null) {
            Log.d(TAG, "[BottomBar] setOnArcSeekBarListener");
            this.mShutterButton.setArcSeekBarListener(onArcSeekBarListener);
        }
    }

    public void setOnCancelSelectionMenu(OnCancelSelectionMenuListener onCancelSelectionMenuListener) {
        if (this.mShutterButton != null) {
            Log.d(TAG, "[BottomBar] mShutterButton setOnArcSeekBarListener");
            this.mShutterButton.setOnCancelSelectionMenu(onCancelSelectionMenuListener);
        }
        if (this.mPeekThumb != null) {
            Log.d(TAG, "[BottomBar] mPeekThumb setOnArcSeekBarListener");
            this.mPeekThumb.setOnCancelSelectionMenu(onCancelSelectionMenuListener);
        }
    }

    public BottomBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mVideoButtonColor = context.getResources().getColor(R.color.video_mode_color);
        TypedArray ar = context.getResources().obtainTypedArray(R.array.shutter_button_backgrounds);
        int len = ar.length();
        this.mShutterButtonBackgroundConstantStates = new ConstantState[len];
        for (int i = 0; i < len; i++) {
            this.mShutterButtonBackgroundConstantStates[i] = context.getResources().getDrawable(ar.getResourceId(i, -1)).getConstantState();
        }
        ar.recycle();
        this.bVdfModeSwitchOn = CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_FIX_MODE_SWITCHING, false);
    }

    private void setPaintColor(int alpha, int color) {
        if (this.mAnimatedCircleDrawable != null) {
            this.mAnimatedCircleDrawable.setColor(color);
            this.mAnimatedCircleDrawable.setAlpha(alpha);
        } else if (this.mColorDrawable != null) {
            this.mColorDrawable.setColor(color);
            this.mColorDrawable.setAlpha(alpha);
        }
        if (this.mIntentReviewLayout != null) {
            ColorDrawable intentBackground = (ColorDrawable) this.mIntentReviewLayout.getBackground();
            intentBackground.setColor(color);
            intentBackground.setAlpha(alpha);
        }
    }

    private void refreshPaintColor() {
        setPaintColor(this.mBackgroundAlpha, this.mBackgroundColor);
    }

    private void setCancelBackgroundColor(int alpha, int color) {
        Drawable d = ((LayerDrawable) this.mCancelButton.getBackground()).getDrawable(null);
        if (d instanceof AnimatedCircleDrawable) {
            AnimatedCircleDrawable animatedCircleDrawable = (AnimatedCircleDrawable) d;
            animatedCircleDrawable.setColor(color);
            animatedCircleDrawable.setAlpha(alpha);
        } else if (d instanceof ColorDrawable) {
            ColorDrawable colorDrawable = (ColorDrawable) d;
            if (!ApiHelper.isLOrHigher()) {
                colorDrawable.setColor(color);
            }
            colorDrawable.setAlpha(alpha);
        }
    }

    private void setCaptureButtonUp() {
        setPaintColor(this.mBackgroundAlpha, this.mBackgroundColor);
    }

    private void setCaptureButtonDown() {
        if (!ApiHelper.isLOrHigher()) {
            setPaintColor(this.mBackgroundAlpha, this.mBackgroundPressedColor);
        }
    }

    private void setCancelButtonUp() {
        setCancelBackgroundColor(this.mBackgroundAlpha, this.mBackgroundColor);
    }

    private void setCancelButtonDown() {
        setCancelBackgroundColor(this.mBackgroundAlpha, this.mBackgroundPressedColor);
    }

    public void onFinishInflate() {
        this.mCaptureLayout = (FrameLayout) findViewById(R.id.bottombar_capture);
        this.mCancelLayout = (FrameLayout) findViewById(R.id.bottombar_cancel);
        this.mCancelLayout.setVisibility(8);
        this.mModeStrip = findViewById(R.id.mode_strip_view);
        boolean z = this.bVdfModeSwitchOn;
        this.mIntentReviewLayout = (TopRightWeightedLayout) findViewById(R.id.bottombar_intent_review);
        this.mRotatableButton = (RotatableButton) findViewById(R.id.video_shutter_button);
        this.mPeekThumb = (PeekImageView) findViewById(R.id.peek_thumb);
        this.mPauseButton = (RotatableButton) findViewById(R.id.video_pause_button);
        this.mPauseButton.setVisibility(8);
        this.mRotateButton = (MultiToggleImageButton) findViewById(R.id.camera_toggle_button);
        this.mRotateButton.setVisibility(8);
        this.mCaptureButton = (RotatableButton) findViewById(R.id.video_snap_button);
        this.mCaptureButton.setVisibility(8);
        this.mSegmentRemoveButton = findViewById(R.id.button_segement_remove);
        this.mMockBar = findViewById(R.id.bottombar_mock);
        this.mRemixButton = findViewById(R.id.button_remix);
        this.mMoreToggleButton = (MultiToggleImageButton) findViewById(R.id.more_enter_toggle_button);
        this.mEffectsEnterToggleButton = (MultiToggleImageButton) findViewById(R.id.effects_enter_toggle_button);
        this.mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
        this.mShutterButton.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == 0) {
                    BottomBar.this.setCaptureButtonDown();
                } else if (1 == event.getActionMasked() || 3 == event.getActionMasked()) {
                    BottomBar.this.setCaptureButtonUp();
                } else if (2 == event.getActionMasked()) {
                    BottomBar.this.mRect.set(0.0f, 0.0f, (float) BottomBar.this.getWidth(), (float) BottomBar.this.getHeight());
                    if (!BottomBar.this.mRect.contains(event.getX(), event.getY())) {
                        BottomBar.this.setCaptureButtonUp();
                    }
                }
                return false;
            }
        });
        this.mShutterButton.setOnCancelSelectionMenu(new OnCancelSelectionMenuListener() {
            public void onCancelSelectionMenu() {
            }
        });
        this.mCancelButton = (RotatableButton) findViewById(R.id.shutter_cancel_button);
        this.mCancelButton.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == 0) {
                    BottomBar.this.setCancelButtonDown();
                } else if (1 == event.getActionMasked() || 3 == event.getActionMasked()) {
                    BottomBar.this.setCancelButtonUp();
                } else if (2 == event.getActionMasked()) {
                    BottomBar.this.mRect.set(0.0f, 0.0f, (float) BottomBar.this.getWidth(), (float) BottomBar.this.getHeight());
                    if (!BottomBar.this.mRect.contains(event.getX(), event.getY())) {
                        BottomBar.this.setCancelButtonUp();
                    }
                }
                return false;
            }
        });
        extendTouchAreaToMatchParent(R.id.done_button);
    }

    private void extendTouchAreaToMatchParent(int id) {
        final View button = findViewById(id);
        final View parent = (View) button.getParent();
        parent.post(new Runnable() {
            public void run() {
                Rect parentRect = new Rect();
                parent.getHitRect(parentRect);
                Rect buttonRect = new Rect();
                button.getHitRect(buttonRect);
                int widthDiff = parentRect.width() - buttonRect.width();
                int heightDiff = parentRect.height() - buttonRect.height();
                buttonRect.left -= widthDiff / 2;
                buttonRect.right += widthDiff / 2;
                buttonRect.top -= heightDiff / 2;
                buttonRect.bottom += heightDiff / 2;
                parent.setTouchDelegate(new TouchDelegate(buttonRect, button));
            }
        });
    }

    public void transitionToCapture() {
        this.mCaptureLayout.setVisibility(0);
        this.mCancelLayout.setVisibility(8);
        this.mIntentReviewLayout.setVisibility(8);
        this.mMode = 0;
    }

    public void transitionToCancel() {
        this.mCaptureLayout.setVisibility(8);
        this.mIntentReviewLayout.setVisibility(8);
        this.mCancelLayout.setVisibility(0);
        this.mMode = 3;
    }

    public void transitionToIntentCaptureLayout() {
        this.mIntentReviewLayout.setVisibility(8);
        this.mCaptureLayout.setVisibility(0);
        this.mCancelLayout.setVisibility(8);
        this.mMode = 1;
    }

    public void transitionToIntentReviewLayout() {
        this.mCaptureLayout.setVisibility(8);
        this.mIntentReviewLayout.setVisibility(0);
        this.mCancelLayout.setVisibility(8);
        this.mMode = 2;
    }

    public boolean isInIntentReview() {
        return this.mMode == 2;
    }

    private void setButtonImageLevels(int level) {
        ((ImageButton) findViewById(R.id.cancel_button)).setImageLevel(level);
        ((ImageButton) findViewById(R.id.done_button)).setImageLevel(level);
        ((ImageButton) findViewById(R.id.retake_button)).setImageLevel(level);
    }

    private void setOverlayBottomBar(boolean overlay) {
        this.mOverLayBottomBar = overlay;
        if (overlay) {
            setBackgroundAlpha(this.mBackgroundAlphaOverlay);
            setButtonImageLevels(1);
            return;
        }
        setBackgroundAlpha(this.mBackgroundAlphaDefault);
        setButtonImageLevels(0);
    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        this.mCaptureLayoutHelper = helper;
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (measureWidth != 0 && measureHeight != 0) {
            if (this.mCaptureLayoutHelper == null) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                Log.e(TAG, "Capture layout helper needs to be set first.");
            } else {
                RectF bottomBarRect = this.mCaptureLayoutHelper.getBottomBarRect();
                super.onMeasure(MeasureSpec.makeMeasureSpec((int) bottomBarRect.width(), Ints.MAX_POWER_OF_TWO), MeasureSpec.makeMeasureSpec(1130, Ints.MAX_POWER_OF_TWO));
                boolean shouldOverlayBottomBar = this.mCaptureLayoutHelper.shouldOverlayBottomBar();
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("heightMeasureSpec = ");
                stringBuilder.append(bottomBarRect.height());
                Log.d(tag, stringBuilder.toString());
                setOverlayBottomBar(shouldOverlayBottomBar);
            }
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    public void setBottomBarColor(int color) {
        this.mCachedColor = color;
        if (this.mOverridenColor != null) {
            super.setBackgroundColor(this.mOverridenColor.intValue());
        } else {
            super.setBackgroundColor(color);
        }
    }

    public void overrideBottomBarColor(Integer color) {
        this.mOverridenColor = color;
        if (this.mOverridenColor == null) {
            super.setBackgroundColor(this.mCachedColor);
        } else {
            super.setBackgroundColor(color.intValue());
        }
    }

    public void setBackgroundColor(int color) {
        this.mCachedColor = color;
        if (this.mMockBar != null) {
            this.mMockBar.setBackgroundColor(color);
        }
        setBackgroundColorInternal(color);
    }

    private void setBackgroundColorInternal(int color) {
        this.mBackgroundColor = color;
        setPaintColor(this.mBackgroundAlpha, this.mBackgroundColor);
        setCancelBackgroundColor(this.mBackgroundAlpha, this.mBackgroundColor);
    }

    private void setBackgroundPressedColor(int color) {
        if (!ApiHelper.isLOrHigher()) {
            this.mBackgroundPressedColor = color;
        }
    }

    private LayerDrawable applyCircleDrawableToShutterBackground(LayerDrawable shutterBackground) {
        if (shutterBackground.findDrawableByLayerId(R.id.circle_item) != null) {
            Drawable animatedCircleDrawable = new AnimatedCircleDrawable((int) this.mCircleRadius);
            animatedCircleDrawable.setLevel(10000);
            this.mAnimatedCircleDrawable = (AnimatedCircleDrawable) animatedCircleDrawable;
        }
        return shutterBackground;
    }

    private LayerDrawable newDrawableFromConstantState(ConstantState constantState) {
        return (LayerDrawable) constantState.newDrawable(getContext().getResources());
    }

    private void setupShutterBackgroundForModeIndex(int index) {
        LayerDrawable shutterBackground = applyCircleDrawableToShutterBackground(newDrawableFromConstantState(this.mShutterButtonBackgroundConstantStates[index]));
        this.mShutterButton.setBackground(shutterBackground);
        this.mCancelButton.setBackground(applyCircleDrawableToShutterBackground(newDrawableFromConstantState(this.mShutterButtonBackgroundConstantStates[index])));
        Drawable d = shutterBackground.getDrawable(null);
        this.mAnimatedCircleDrawable = null;
        this.mColorDrawable = null;
        if (d instanceof AnimatedCircleDrawable) {
            this.mAnimatedCircleDrawable = (AnimatedCircleDrawable) d;
        } else if (d instanceof ColorDrawable) {
            this.mColorDrawable = (ColorDrawable) d;
        }
        setBackgroundPressedColor(getContext().getResources().getColor(CameraUtil.getCameraThemeColorId(index, getContext())));
        refreshPaintColor();
    }

    private LayerDrawable applyBottomMockAnimationToMockView(LayerDrawable mockBackgorund, int targetColor) {
        Drawable d = mockBackgorund.findDrawableByLayerId(R.id.mock_circle);
        if (!(d == null || (d instanceof BottomMockViewAnimationDrawable))) {
            Point shutterCenter = new Point();
            shutterCenter.set(this.mShutterButton.getLeft() + (this.mShutterButton.getMeasuredWidth() / 2), this.mShutterButton.getTop() + (this.mShutterButton.getMeasuredHeight() / 2));
            Drawable mockAnimationDrawable = new BottomMockViewAnimationDrawable(shutterCenter, targetColor, this.mShutterButton.getWidth());
            mockAnimationDrawable.setLevel(10000);
            mockBackgorund.setDrawableByLayerId(R.id.mock_circle, mockAnimationDrawable);
            this.mMockViewAnimationDrawable = (BottomMockViewAnimationDrawable) mockAnimationDrawable;
        }
        return mockBackgorund;
    }

    private void setUpMockViewBackground(int targetColor) {
        this.mMockBar.setBackground(applyBottomMockAnimationToMockView((LayerDrawable) getResources().getDrawable(R.drawable.bottom_mock_background, null).getConstantState().newDrawable(), targetColor));
    }

    public void setColorsForModeIndex(int index) {
        setupShutterBackgroundForModeIndex(index);
    }

    public void setBackgroundAlpha(int alpha) {
        this.mBackgroundAlpha = alpha;
        setPaintColor(this.mBackgroundAlpha, this.mBackgroundColor);
        setCancelBackgroundColor(this.mBackgroundAlpha, this.mBackgroundColor);
    }

    public void setShutterButtonEnabled(final boolean enabled) {
        this.mShutterButton.post(new Runnable() {
            public void run() {
                BottomBar.this.mShutterButton.setEnabled(enabled);
                BottomBar.this.setShutterButtonImportantToA11y(enabled);
            }
        });
    }

    public void setShutterbuttonEnabledWithoutAppearenceChanged(final boolean enabled) {
        this.mShutterButton.post(new Runnable() {
            public void run() {
                BottomBar.this.mShutterButton.enableFilter(false);
                BottomBar.this.mShutterButton.setEnabled(enabled);
                BottomBar.this.setShutterButtonImportantToA11y(enabled);
                BottomBar.this.mShutterButton.enableFilter(true);
            }
        });
    }

    public void setShutterButtonPress(final boolean press) {
        this.mShutterButton.post(new Runnable() {
            public void run() {
                BottomBar.this.mShutterButton.setPressed(press);
            }
        });
    }

    public void setShutterButtonLongClickable(final boolean longClickable) {
        this.mShutterButton.post(new Runnable() {
            public void run() {
                BottomBar.this.mShutterButton.setLongClickable(longClickable);
            }
        });
    }

    public void setShutterButtonImportantToA11y(boolean important) {
        if (important) {
            this.mShutterButton.setImportantForAccessibility(0);
        } else {
            this.mShutterButton.setImportantForAccessibility(2);
        }
    }

    public boolean isShutterButtonEnabled() {
        return this.mShutterButton.isEnabled();
    }

    private TransitionDrawable crossfadeDrawable(Drawable from, Drawable to) {
        TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{from, to});
        transitionDrawable.setCrossFadeEnabled(true);
        return transitionDrawable;
    }

    public void setShutterButtonIcon(int resId) {
        if (this.mResId != resId) {
            this.mResId = resId;
            Drawable iconDrawable = getResources().getDrawable(resId);
            if (iconDrawable != null) {
                iconDrawable = iconDrawable.mutate();
            }
            this.mShutterButton.setImageDrawable(iconDrawable);
        }
    }

    public void setShutterButtonIcon(int resId, boolean animation) {
        if (this.mResId != resId) {
            this.mResId = resId;
            Drawable iconDrawable = getResources().getDrawable(resId);
            if (iconDrawable != null) {
                iconDrawable = iconDrawable.mutate();
            }
            if (animation) {
                this.mShutterButton.setImageDrawable(iconDrawable);
            } else {
                this.mShutterButton.setImageDrawableWithoutRotation(iconDrawable);
            }
        }
    }

    public void animateToVideoStop(int resId) {
        this.mModeStrip.setVisibility(8);
        this.mMockBar.setVisibility(0);
        overrideBottomBarColor(Integer.valueOf(0));
        setUpMockViewBackground(this.mCachedColor);
        if (this.mMockViewAnimationDrawable != null) {
            this.mMockViewAnimationDrawable.animateToCircle(getContext().getResources().getColor(R.color.video_mode_color), getMeasuredWidth(), getMeasuredHeight());
        }
        TransitionDrawable transitionDrawable = crossfadeDrawable(this.mShutterButton.getDrawable(), getResources().getDrawable(resId));
        this.mShutterButton.setImageDrawableWithoutRotation(transitionDrawable);
        transitionDrawable.startTransition(500);
        animateHidePeek();
        animateHideVideoShutter();
    }

    public void animateToFullSize(int resId, final BottomBarSizeListener listener, final boolean isTL) {
        overrideBottomBarColor(Integer.valueOf(0));
        TransitionDrawable transitionDrawable = crossfadeDrawable(this.mShutterButton.getDrawable(), getResources().getDrawable(resId));
        if (this.mMockViewAnimationDrawable != null) {
            Log.v(TAG, "animate to full size");
            this.mMockViewAnimationDrawable.animateToFullSize(new OnAnimationAsyncListener() {
                public void onAnimationFinish() {
                    Log.v(BottomBar.TAG, "fullsize animation end");
                    if (listener != null) {
                        listener.onFullSizeReached();
                    }
                    BottomBar.this.overrideBottomBarColor(null);
                    if (!isTL) {
                        BottomBar.this.showMoreEnterButtonVisible(0);
                        BottomBar.this.showEffectsButtonVisible(4);
                        BottomBar.this.mModeStrip.setVisibility(0);
                    }
                    BottomBar.this.mMockBar.setVisibility(8);
                }
            });
        }
        this.mShutterButton.setImageDrawableWithoutRotation(transitionDrawable);
        transitionDrawable.startTransition(500);
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x0003, element type: float, insn element type: null */
    private android.animation.ValueAnimator buildShowingAnimator(final android.view.View r4) {
        /*
        r3 = this;
        r0 = 2;
        r0 = new float[r0];
        r0 = {0, 1065353216};
        r0 = android.animation.ValueAnimator.ofFloat(r0);
        r1 = 500; // 0x1f4 float:7.0E-43 double:2.47E-321;
        r0.setDuration(r1);
        r1 = com.hmdglobal.app.camera.util.Gusterpolator.INSTANCE;
        r0.setInterpolator(r1);
        r1 = new com.hmdglobal.app.camera.ui.BottomBar$10;
        r1.<init>(r4);
        r0.addUpdateListener(r1);
        r1 = new com.hmdglobal.app.camera.ui.BottomBar$11;
        r1.<init>();
        r0.addListener(r1);
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ui.BottomBar.buildShowingAnimator(android.view.View):android.animation.ValueAnimator");
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x0003, element type: float, insn element type: null */
    private android.animation.ValueAnimator buildHidingAnimator(final android.view.View r4) {
        /*
        r3 = this;
        r0 = 2;
        r0 = new float[r0];
        r0 = {1065353216, 0};
        r0 = android.animation.ValueAnimator.ofFloat(r0);
        r1 = 500; // 0x1f4 float:7.0E-43 double:2.47E-321;
        r0.setDuration(r1);
        r1 = com.hmdglobal.app.camera.util.Gusterpolator.INSTANCE;
        r0.setInterpolator(r1);
        r1 = new com.hmdglobal.app.camera.ui.BottomBar$12;
        r1.<init>(r4);
        r0.addUpdateListener(r1);
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ui.BottomBar.buildHidingAnimator(android.view.View):android.animation.ValueAnimator");
    }

    private void animateHide(ValueAnimator[] showingAnimator, ValueAnimator[] hidingAnimator, View viewToHide) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("animateHide ");
        stringBuilder.append(viewToHide);
        Log.w(tag, stringBuilder.toString());
        if (showingAnimator != null && hidingAnimator != null) {
            if (viewToHide.getVisibility() != 8 || (showingAnimator[0] != null && showingAnimator[0].isRunning())) {
                if (hidingAnimator[0] == null || showingAnimator[0] == null) {
                    showingAnimator[0] = buildShowingAnimator(viewToHide);
                    hidingAnimator[0] = buildHidingAnimator(viewToHide);
                }
                if (showingAnimator[0].isRunning()) {
                    showingAnimator[0].cancel();
                }
                if (!hidingAnimator[0].isRunning()) {
                    hidingAnimator[0].start();
                }
                lockView(viewToHide);
            }
        }
    }

    private void lockView(View viewToLock) {
        if (viewToLock instanceof Lockable) {
            if (this.mAnimationViewLockMap.containsKey(viewToLock)) {
                ((Lockable) viewToLock).unlockWithToken(((Integer) this.mAnimationViewLockMap.get(viewToLock)).intValue());
            }
            this.mAnimationViewLockMap.put(viewToLock, Integer.valueOf(((Lockable) viewToLock).lock()));
        }
    }

    private void unlockView(View viewToUnlock) {
        if ((viewToUnlock instanceof Lockable) && this.mAnimationViewLockMap.containsKey(viewToUnlock)) {
            ((Lockable) viewToUnlock).unlockWithToken(((Integer) this.mAnimationViewLockMap.get(viewToUnlock)).intValue());
        }
    }

    private void hideWithoutAnimation(ValueAnimator[] showingAnimator, ValueAnimator[] hidingAnimator, View viewToHide) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("animateHide ");
        stringBuilder.append(viewToHide);
        Log.w(tag, stringBuilder.toString());
        if (showingAnimator == null || hidingAnimator == null) {
            viewToHide.setVisibility(8);
        } else if (hidingAnimator[0] == null || showingAnimator[0] == null) {
            viewToHide.setVisibility(8);
        } else {
            if (showingAnimator[0].isRunning()) {
                showingAnimator[0].cancel();
            }
            if (hidingAnimator[0].isRunning()) {
                hidingAnimator[0].end();
            }
            viewToHide.setVisibility(8);
        }
    }

    private void showWithoutAnimation(ValueAnimator[] showingAnimator, ValueAnimator[] hidingAnimator, View viewToShow) {
        if (showingAnimator == null || hidingAnimator == null) {
            viewToShow.setVisibility(0);
        } else if (hidingAnimator[0] == null || showingAnimator[0] == null) {
            viewToShow.setAlpha(1.0f);
            viewToShow.setVisibility(0);
        } else {
            if (showingAnimator[0].isRunning()) {
                showingAnimator[0].end();
            }
            if (hidingAnimator[0].isRunning()) {
                hidingAnimator[0].cancel();
            }
            viewToShow.setVisibility(0);
            unlockView(viewToShow);
        }
    }

    private void animateShow(ValueAnimator[] showingAnimator, ValueAnimator[] hidingAnimator, View viewToShow) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("animateShow ");
        stringBuilder.append(viewToShow);
        Log.w(tag, stringBuilder.toString());
        if (showingAnimator != null && hidingAnimator != null) {
            if (viewToShow.getVisibility() != 0 || (hidingAnimator[0] != null && hidingAnimator[0].isRunning())) {
                if (hidingAnimator[0] == null || showingAnimator[0] == null) {
                    showingAnimator[0] = buildShowingAnimator(viewToShow);
                    hidingAnimator[0] = buildHidingAnimator(viewToShow);
                }
                if (hidingAnimator[0].isRunning()) {
                    hidingAnimator[0].cancel();
                }
                if (!showingAnimator[0].isRunning()) {
                    showingAnimator[0].start();
                }
                unlockView(viewToShow);
            }
        }
    }

    public void hideShutterButton() {
        hideWithoutAnimation(this.mShowShutterButton, this.mHideShutterButton, this.mShutterButton);
    }

    public void showShutterButton() {
        showWithoutAnimation(this.mShowShutterButton, this.mHideShutterButton, this.mShutterButton);
    }

    public void showPeek() {
        showWithoutAnimation(this.mShowPeekAnmator, this.mHidePeekAnimator, this.mPeekThumb);
    }

    public void hidePeek() {
        hideWithoutAnimation(this.mShowPeekAnmator, this.mHidePeekAnimator, this.mPeekThumb);
    }

    public void showVideoShutter() {
        showWithoutAnimation(this.mShowVideoShutterAnimator, this.mHideVideoShutterAnimator, this.mRotatableButton);
    }

    public void hideVideoShutter() {
        hideWithoutAnimation(this.mShowVideoShutterAnimator, this.mHideVideoShutterAnimator, this.mRotatableButton);
    }

    public void showSegmentRemove() {
        showWithoutAnimation(this.mShowSegmentRemove, this.mHideSegmentRemove, this.mSegmentRemoveButton);
    }

    public void hideSegementRemove() {
        hideWithoutAnimation(this.mShowSegmentRemove, this.mHideSegmentRemove, this.mSegmentRemoveButton);
    }

    public void showRemix() {
        showWithoutAnimation(this.mShowRemix, this.mHideRemix, this.mRemixButton);
    }

    public void hideRemix() {
        hideWithoutAnimation(this.mShowRemix, this.mHideRemix, this.mRemixButton);
    }

    public void showVideoCapture() {
        showWithoutAnimation(this.mShowVideoCaptureAnimator, this.mHideVideoCaptureAnimator, this.mCaptureButton);
    }

    public void hideVideoCapture() {
        hideWithoutAnimation(this.mShowVideoCaptureAnimator, this.mHideVideoCaptureAnimator, this.mCaptureButton);
    }

    public void showVideoPause() {
        showWithoutAnimation(this.mShowVideoPauseAnimator, this.mHideVideoPauseAnimator, this.mPauseButton);
    }

    public void hideVideoPause() {
        hideWithoutAnimation(this.mShowVideoPauseAnimator, this.mHideVideoPauseAnimator, this.mPauseButton);
    }

    public void animateHidePeek() {
        animateHide(this.mShowPeekAnmator, this.mHidePeekAnimator, this.mPeekThumb);
    }

    public void animateShowPeek() {
        animateShow(this.mShowPeekAnmator, this.mHidePeekAnimator, this.mPeekThumb);
    }

    public void animateHideVideoShutter() {
        animateHide(this.mShowVideoShutterAnimator, this.mHideVideoShutterAnimator, this.mRotatableButton);
    }

    public void animateShowVideoShutter() {
        animateShow(this.mShowVideoShutterAnimator, this.mHideVideoShutterAnimator, this.mRotatableButton);
    }

    public void animateHideSegementRemove() {
        animateHide(this.mShowSegmentRemove, this.mHideSegmentRemove, this.mSegmentRemoveButton);
    }

    public void animateShowSegmentRemove() {
        animateShow(this.mShowSegmentRemove, this.mHideSegmentRemove, this.mSegmentRemoveButton);
    }

    public void animateHideRemix() {
        animateHide(this.mShowRemix, this.mHideRemix, this.mRemixButton);
    }

    public void animateShowRemix() {
        animateShow(this.mShowRemix, this.mHideRemix, this.mRemixButton);
    }

    public void animateHideVideoCapture() {
        animateHide(this.mShowVideoCaptureAnimator, this.mHideVideoCaptureAnimator, this.mCaptureButton);
    }

    public void animateShowVideoCapture() {
        animateShow(this.mShowVideoCaptureAnimator, this.mHideVideoCaptureAnimator, this.mCaptureButton);
    }

    public void animateHideVideoPause() {
        animateHide(this.mShowVideoCaptureAnimator, this.mHideVideoCaptureAnimator, this.mPauseButton);
    }

    public void animateShowVideoPause() {
        animateShow(this.mShowVideoCaptureAnimator, this.mHideVideoCaptureAnimator, this.mPauseButton);
    }

    public boolean isBackgroundTransparent() {
        return this.mIsBackgroundTransparent;
    }

    public void setIsBackgroundTransparent(boolean isBackgroundTransparent) {
        this.mIsBackgroundTransparent = isBackgroundTransparent;
    }

    public void hideRotateButton() {
        if (this.mRotateButton != null) {
            this.mRotateButton.setVisibility(8);
        }
    }

    public void showRotateButton() {
        if (this.mRotateButton != null) {
            this.mRotateButton.setVisibility(0);
        }
    }

    public void showMoreEnterButtonVisible(int visibility) {
        if (this.mMoreToggleButton != null) {
            this.mMoreToggleButton.setVisibility(visibility);
        }
    }

    public void showEffectsButtonVisible(int visibility) {
        if (this.mEffectsEnterToggleButton != null) {
            this.mEffectsEnterToggleButton.setVisibility(visibility);
        }
    }

    public void setModeStripVisible(int visible) {
        if (this.mModeStrip != null) {
            this.mModeStrip.setVisibility(visible);
        }
    }
}
