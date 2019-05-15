package com.hmdglobal.app.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.SystemClock;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.hmdglobal.app.camera.CaptureLayoutHelper;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.CameraAppUI.CameraModuleScreenShotProvider;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.ModeSelectorItem.VisibleWidthChangedListener;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAreaChangedListener;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.Gusterpolator;
import com.hmdglobal.app.camera.util.UsageStatistics;
import com.hmdglobal.app.camera.widget.AnimationEffects;
import com.hmdglobal.app.camera.widget.SettingsCling;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ModeListView extends FrameLayout implements VisibleWidthChangedListener, PreviewAreaChangedListener {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int BACKGROUND_TRANSPARENTCY = 153;
    private static final int DEFAULT_DURATION_MS = 200;
    private static final int DELAY_MS = 30;
    private static final int FLY_IN_DURATION_MS = 0;
    private static final int FLY_OUT_DURATION_MS = 850;
    private static final int HIDE_SHIMMY_DELAY_MS = 1000;
    private static final int HOLD_DURATION_MS = 0;
    private static final int NO_ITEM_SELECTED = -1;
    private static final int PREVIEW_DOWN_SAMPLE_FACTOR = 4;
    private static final float SCROLL_FACTOR = 0.5f;
    private static final int SCROLL_INTERVAL_MS = 50;
    private static final float SLOW_ZONE_PERCENTAGE = 0.2f;
    private static final float SNAP_BACK_THRESHOLD_RATIO = 0.33f;
    private static final int START_DELAY_MS = 100;
    private static final Tag TAG = new Tag("ModeListView");
    private static final int TOTAL_DURATION_MS = 850;
    private static final float VELOCITY_THRESHOLD = 2.0f;
    private final TimeInterpolator mAccordionInterpolator = new TimeInterpolator() {
        public float getInterpolation(float input) {
            if (input == 0.0f) {
                return 0.0f;
            }
            if (input < 1.0f) {
                return ModeListView.SCROLL_FACTOR * Gusterpolator.INSTANCE.getInterpolation(input / 1.0f);
            } else if (input < 1.0f) {
                return ModeListView.SCROLL_FACTOR;
            } else {
                return ModeListView.SCROLL_FACTOR + (Gusterpolator.INSTANCE.getInterpolation((input - 1.0f) / (1.0f - 1.0f)) * ModeListView.SCROLL_FACTOR);
            }
        }
    };
    private AnimatorSet mAnimatorSet;
    private CaptureLayoutHelper mCaptureLayoutHelper = null;
    private View mChildViewTouched = null;
    private final CurrentStateManager mCurrentStateManager = new CurrentStateManager(this, null);
    private long mCurrentTime;
    private int mFocusItem = -1;
    private final GestureDetector mGestureDetector;
    private int mHeight;
    private int[] mInputPixels;
    private MotionEvent mLastChildTouchEvent = null;
    private long mLastDownTime = 0;
    private long mLastScrollTime;
    private int mListBackgroundColor;
    private LinearLayout mListView;
    private float mModeListOpenFactor = 1.0f;
    private ModeListOpenListener mModeListOpenListener;
    private ModeSelectorItem[] mModeSelectorItems;
    private ModeSwitchListener mModeSwitchListener = null;
    private final OnGestureListener mOnGestureListener = new SimpleOnGestureListener() {
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            ModeListView.this.mCurrentStateManager.getCurrentState().onScroll(e1, e2, distanceX, distanceY);
            ModeListView.this.mLastScrollTime = System.currentTimeMillis();
            return true;
        }

        public boolean onSingleTapUp(MotionEvent ev) {
            ModeListView.this.mCurrentStateManager.getCurrentState().onSingleTapUp(ev);
            return true;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            ModeListView.this.mVelocityX = (velocityX / 1000.0f) * ModeListView.SCROLL_FACTOR;
            ModeListView.this.mCurrentStateManager.getCurrentState().onFling(e1, e2, velocityX, velocityY);
            return true;
        }

        public boolean onDown(MotionEvent ev) {
            ModeListView.this.mVelocityX = 0.0f;
            ModeListView.this.mCurrentStateManager.getCurrentState().onDown(ev);
            return true;
        }
    };
    private int[] mOutputPixels;
    private final LinkedList<TimeBasedPosition> mPositionHistory = new LinkedList();
    private CameraModuleScreenShotProvider mScreenShotProvider = null;
    private float mScrollTrendX = 0.0f;
    private float mScrollTrendY = 0.0f;
    private View mSettingsButton;
    private final int mSettingsButtonMargin;
    private SettingsCling mSettingsCling = null;
    private ArrayList<Integer> mSupportedModes;
    private int mTotalModes;
    private float mVelocityX;
    private ModeListVisibilityChangedListener mVisibilityChangedListener;
    private int mVisibleWidth = 0;
    private int mWidth;

    private class CurrentStateManager {
        private ModeListState mCurrentState;

        private CurrentStateManager() {
        }

        /* synthetic */ CurrentStateManager(ModeListView x0, AnonymousClass1 x1) {
            this();
        }

        /* Access modifiers changed, original: 0000 */
        public ModeListState getCurrentState() {
            return this.mCurrentState;
        }

        /* Access modifiers changed, original: 0000 */
        public void setCurrentState(ModeListState state) {
            this.mCurrentState = state;
            state.onCurrentState();
        }
    }

    public interface ModeListOpenListener {
        void onModeListClosed();

        void onModeListOpenProgress(float f);

        void onOpenFullScreen();
    }

    private abstract class ModeListState implements OnGestureListener {
        protected AnimationEffects mCurrentAnimationEffects;

        private ModeListState() {
            this.mCurrentAnimationEffects = null;
        }

        /* synthetic */ ModeListState(ModeListView x0, AnonymousClass1 x1) {
            this();
        }

        public void onCurrentState() {
            ModeListView.this.showSettingsClingIfEnabled(false);
        }

        public void showSwitcherHint() {
        }

        public AnimationEffects getCurrentAnimationEffects() {
            return this.mCurrentAnimationEffects;
        }

        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            return true;
        }

        public boolean onTouchEvent(MotionEvent ev) {
            return true;
        }

        public void onWindowFocusChanged(boolean hasFocus) {
        }

        public boolean onBackPressed() {
            return false;
        }

        public boolean onMenuPressed() {
            return false;
        }

        public boolean shouldHandleVisibilityChange(int visibility) {
            return true;
        }

        public void onItemSelected(ModeSelectorItem selectedItem) {
        }

        public void startModeSelectionAnimation() {
        }

        public void hide() {
        }

        public void hideAnimated() {
            hide();
        }

        public boolean onDown(MotionEvent e) {
            return false;
        }

        public void onShowPress(MotionEvent e) {
        }

        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        public void onLongPress(MotionEvent e) {
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    public static abstract class ModeListVisibilityChangedListener {
        private Boolean mCurrentVisibility = null;

        public abstract void onVisibilityChanged(boolean z);

        private void onVisibilityEvent(boolean visible) {
            if (this.mCurrentVisibility == null || this.mCurrentVisibility.booleanValue() != visible) {
                this.mCurrentVisibility = Boolean.valueOf(visible);
                onVisibilityChanged(visible);
            }
        }
    }

    public interface ModeSwitchListener {
        int getCurrentModeIndex();

        void onModeSelected(int i);

        void onSettingsSelected();
    }

    private static class TimeBasedPosition {
        private final float mPosition;
        private final long mTimeStamp;

        public TimeBasedPosition(float position, long time) {
            this.mPosition = position;
            this.mTimeStamp = time;
        }

        public float getPosition() {
            return this.mPosition;
        }

        public long getTimeStamp() {
            return this.mTimeStamp;
        }
    }

    private class FullyHiddenState extends ModeListState {
        private Animator mAnimator = null;
        private boolean mShouldBeVisible = false;

        public FullyHiddenState() {
            super(ModeListView.this, null);
            ModeListView.this.reset();
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            this.mShouldBeVisible = true;
            ModeListView.this.resetModeSelectors();
            ModeListView.this.mCurrentStateManager.setCurrentState(new ScrollingState());
            return true;
        }

        public void showSwitcherHint() {
            this.mShouldBeVisible = true;
            ModeListView.this.mCurrentStateManager.setCurrentState(new ShimmyState());
        }

        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            return true;
        }

        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == 0) {
                ModeListView.this.mFocusItem = ModeListView.this.getFocusItem(ev.getX(), ev.getY());
                ModeListView.this.setSwipeMode(true);
            }
            return true;
        }

        public boolean onMenuPressed() {
            if (this.mAnimator != null) {
                return false;
            }
            snapOpenAndShow();
            return true;
        }

        public boolean shouldHandleVisibilityChange(int visibility) {
            if (this.mAnimator != null) {
                return false;
            }
            if (visibility != 0 || this.mShouldBeVisible) {
                return true;
            }
            return false;
        }

        private void snapOpenAndShow() {
            this.mShouldBeVisible = true;
            ModeListView.this.setVisibility(0);
            this.mAnimator = ModeListView.this.snapToFullScreen();
            if (this.mAnimator != null) {
                this.mAnimator.addListener(new AnimatorListener() {
                    public void onAnimationStart(Animator animation) {
                    }

                    public void onAnimationEnd(Animator animation) {
                        FullyHiddenState.this.mAnimator = null;
                        ModeListView.this.mCurrentStateManager.setCurrentState(new FullyShownState(ModeListView.this, null));
                    }

                    public void onAnimationCancel(Animator animation) {
                    }

                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                return;
            }
            ModeListView.this.mCurrentStateManager.setCurrentState(new FullyShownState(ModeListView.this, null));
            UsageStatistics.instance().controlUsed(10000);
        }

        public void onCurrentState() {
            super.onCurrentState();
            ModeListView.this.announceForAccessibility(ModeListView.this.getContext().getResources().getString(R.string.accessibility_mode_list_hidden));
        }
    }

    private class FullyShownState extends ModeListState {
        private Animator mAnimator;

        private FullyShownState() {
            super(ModeListView.this, null);
            this.mAnimator = null;
        }

        /* synthetic */ FullyShownState(ModeListView x0, AnonymousClass1 x1) {
            this();
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (distanceX > 0.0f) {
                ModeListView.this.cancelForwardingTouchEvent();
                ModeListView.this.mCurrentStateManager.setCurrentState(new ScrollingState());
            }
            return true;
        }

        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            if (this.mAnimator == null || !this.mAnimator.isRunning()) {
                return true;
            }
            return false;
        }

        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == 0) {
                ModeListView.this.mFocusItem = -1;
                ModeListView.this.setSwipeMode(false);
                if (ModeListView.this.isTouchInsideList(ev)) {
                    ModeListView.this.mChildViewTouched = ModeListView.this.mModeSelectorItems[ModeListView.this.getFocusItem(ev.getX(), ev.getY())];
                }
            }
            ModeListView.this.forwardTouchEventToChild(ev);
            return true;
        }

        public boolean onSingleTapUp(MotionEvent ev) {
            if (ModeListView.this.isTouchInsideList(ev)) {
                return true;
            }
            snapBackAndHide();
            return false;
        }

        public boolean onBackPressed() {
            snapBackAndHide();
            return true;
        }

        public boolean onMenuPressed() {
            snapBackAndHide();
            return true;
        }

        public void onItemSelected(ModeSelectorItem selectedItem) {
            ModeListView.this.mCurrentStateManager.setCurrentState(new SelectedState(selectedItem));
        }

        private void snapBackAndHide() {
            this.mAnimator = ModeListView.this.snapBack(true);
            if (this.mAnimator != null) {
                this.mAnimator.addListener(new AnimatorListener() {
                    public void onAnimationStart(Animator animation) {
                    }

                    public void onAnimationEnd(Animator animation) {
                        FullyShownState.this.mAnimator = null;
                        ModeListView.this.mCurrentStateManager.setCurrentState(new FullyHiddenState());
                    }

                    public void onAnimationCancel(Animator animation) {
                    }

                    public void onAnimationRepeat(Animator animation) {
                    }
                });
            } else {
                ModeListView.this.mCurrentStateManager.setCurrentState(new FullyHiddenState());
            }
        }

        public void hide() {
            if (this.mAnimator != null) {
                this.mAnimator.cancel();
            } else {
                ModeListView.this.mCurrentStateManager.setCurrentState(new FullyHiddenState());
            }
        }

        public void onCurrentState() {
            ModeListView.this.announceForAccessibility(ModeListView.this.getContext().getResources().getString(R.string.accessibility_mode_list_shown));
            ModeListView.this.showSettingsClingIfEnabled(true);
        }
    }

    private class PeepholeAnimationEffect extends AnimationEffects {
        private static final int PEEP_HOLE_ANIMATION_DURATION_MS = 500;
        private static final int UNSET = -1;
        private Bitmap mBackground;
        private final RectF mBackgroundDrawArea = new RectF();
        private Bitmap mBackgroundOverlay;
        private TouchCircleDrawable mCircleDrawable;
        private Paint mCirclePaint = new Paint();
        private Paint mCoverPaint = new Paint();
        private ValueAnimator mFadeOutAlphaAnimator;
        private final Paint mMaskPaint = new Paint();
        private ValueAnimator mPeepHoleAnimator;
        private int mPeepHoleCenterX = -1;
        private int mPeepHoleCenterY = -1;
        private float mRadius = 0.0f;
        private ValueAnimator mRevealAlphaAnimator;

        public PeepholeAnimationEffect() {
            this.mMaskPaint.setAlpha(0);
            this.mMaskPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
            this.mCirclePaint.setColor(0);
            this.mCirclePaint.setAlpha(0);
            this.mCoverPaint.setColor(0);
            this.mCoverPaint.setAlpha(0);
            setupAnimators();
        }

        private void setupAnimators() {
            this.mFadeOutAlphaAnimator = ValueAnimator.ofInt(new int[]{0, 255});
            this.mFadeOutAlphaAnimator.setDuration(100);
            this.mFadeOutAlphaAnimator.setInterpolator(Gusterpolator.INSTANCE);
            this.mFadeOutAlphaAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    PeepholeAnimationEffect.this.mCoverPaint.setAlpha(((Integer) animation.getAnimatedValue()).intValue());
                    ModeListView.this.invalidate();
                }
            });
            this.mFadeOutAlphaAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    ModeListView.this.setLayerType(2, null);
                }

                public void onAnimationEnd(Animator animation) {
                    ModeListView.this.setLayerType(0, null);
                }
            });
            this.mRevealAlphaAnimator = ValueAnimator.ofInt(new int[]{255, 0});
            this.mRevealAlphaAnimator.setDuration(500);
            this.mRevealAlphaAnimator.setInterpolator(Gusterpolator.INSTANCE);
            this.mRevealAlphaAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    int alpha = ((Integer) animation.getAnimatedValue()).intValue();
                    PeepholeAnimationEffect.this.mCirclePaint.setAlpha(alpha);
                    PeepholeAnimationEffect.this.mCoverPaint.setAlpha(alpha);
                }
            });
            this.mRevealAlphaAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    ModeListView.this.setLayerType(2, null);
                }

                public void onAnimationEnd(Animator animation) {
                    ModeListView.this.setLayerType(0, null);
                }
            });
            int horizontalDistanceToFarEdge = Math.max(this.mPeepHoleCenterX, ModeListView.this.mWidth - this.mPeepHoleCenterX);
            int verticalDistanceToFarEdge = Math.max(this.mPeepHoleCenterY, ModeListView.this.mHeight - this.mPeepHoleCenterY);
            int endRadius = (int) Math.sqrt((double) ((horizontalDistanceToFarEdge * horizontalDistanceToFarEdge) + (verticalDistanceToFarEdge * verticalDistanceToFarEdge)));
            this.mPeepHoleAnimator = ValueAnimator.ofFloat(new float[]{(float) (ModeListView.this.getResources().getDimensionPixelSize(R.dimen.mode_selector_icon_block_width) / 2), (float) endRadius});
            this.mPeepHoleAnimator.setDuration(500);
            this.mPeepHoleAnimator.setInterpolator(Gusterpolator.INSTANCE);
            this.mPeepHoleAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    PeepholeAnimationEffect.this.mRadius = ((Float) PeepholeAnimationEffect.this.mPeepHoleAnimator.getAnimatedValue()).floatValue();
                    ModeListView.this.invalidate();
                }
            });
            this.mPeepHoleAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    ModeListView.this.setLayerType(2, null);
                }

                public void onAnimationEnd(Animator animation) {
                    ModeListView.this.setLayerType(0, null);
                }
            });
            int size = ModeListView.this.getContext().getResources().getDimensionPixelSize(R.dimen.mode_selector_icon_block_width);
            this.mCircleDrawable = new TouchCircleDrawable(ModeListView.this.getContext().getResources());
            this.mCircleDrawable.setSize(size, size);
            this.mCircleDrawable.setUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    ModeListView.this.invalidate();
                }
            });
        }

        public void setSize(int width, int height) {
            ModeListView.this.mWidth = width;
            ModeListView.this.mHeight = height;
        }

        public boolean onTouchEvent(MotionEvent event) {
            return true;
        }

        public void drawForeground(Canvas canvas) {
            if (this.mPeepHoleAnimator != null) {
                canvas.drawCircle((float) this.mPeepHoleCenterX, (float) this.mPeepHoleCenterY, this.mRadius, this.mMaskPaint);
                canvas.drawCircle((float) this.mPeepHoleCenterX, (float) this.mPeepHoleCenterY, this.mRadius, this.mCirclePaint);
            }
        }

        public void setAnimationStartingPosition(int x, int y) {
            this.mPeepHoleCenterX = x;
            this.mPeepHoleCenterY = y;
        }

        public void setModeSpecificColor(int color) {
            this.mCirclePaint.setColor(ViewCompat.MEASURED_SIZE_MASK & color);
        }

        public void setBackground(Bitmap background, RectF drawArea) {
            this.mBackground = background;
            this.mBackgroundDrawArea.set(drawArea);
        }

        public void setBackgroundOverlay(Bitmap overlay) {
            this.mBackgroundOverlay = overlay;
        }

        public void drawBackground(Canvas canvas) {
            if (this.mBackground != null && this.mBackgroundOverlay != null) {
                canvas.drawBitmap(this.mBackground, null, this.mBackgroundDrawArea, null);
                canvas.drawPaint(this.mCoverPaint);
                canvas.drawBitmap(this.mBackgroundOverlay, 0.0f, 0.0f, null);
                if (this.mCircleDrawable != null) {
                    this.mCircleDrawable.draw(canvas);
                }
            }
        }

        public boolean shouldDrawSuper() {
            return this.mBackground == null || this.mBackgroundOverlay == null;
        }

        public void startFadeoutAnimation(AnimatorListener listener, final ModeSelectorItem selectedItem, int x, int y, final int modeId) {
            this.mCoverPaint.setColor(0);
            this.mCoverPaint.setAlpha(0);
            this.mCircleDrawable.setIconDrawable(selectedItem.getIcon().getIconDrawableClone(), selectedItem.getIcon().getIconDrawableSize());
            this.mCircleDrawable.setCenter(new Point(x, y));
            this.mCircleDrawable.setColor(selectedItem.getHighlightColor());
            this.mCircleDrawable.setAnimatorListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    ModeListView.this.post(new Runnable() {
                        public void run() {
                            selectedItem.setSelected(true);
                            ModeListView.this.onModeSelected(modeId);
                        }
                    });
                }
            });
            AnimatorSet s = new AnimatorSet();
            s.play(this.mFadeOutAlphaAnimator);
            if (listener != null) {
                s.addListener(listener);
            }
            this.mCircleDrawable.animate();
            s.start();
        }

        public void startAnimation(AnimatorListener listener) {
            if (this.mPeepHoleAnimator == null || !this.mPeepHoleAnimator.isRunning()) {
                if (this.mPeepHoleCenterY == -1 || this.mPeepHoleCenterX == -1) {
                    this.mPeepHoleCenterX = ModeListView.this.mWidth / 2;
                    this.mPeepHoleCenterY = ModeListView.this.mHeight / 2;
                }
                this.mCirclePaint.setAlpha(255);
                this.mCoverPaint.setAlpha(255);
                AnimatorSet s = new AnimatorSet();
                s.play(this.mPeepHoleAnimator).with(this.mRevealAlphaAnimator);
                if (listener != null) {
                    s.addListener(listener);
                }
                s.start();
            }
        }

        public void endAnimation() {
        }

        public boolean cancelAnimation() {
            if (this.mPeepHoleAnimator == null || !this.mPeepHoleAnimator.isRunning()) {
                return false;
            }
            this.mPeepHoleAnimator.cancel();
            return true;
        }
    }

    private class ScrollingState extends ModeListState {
        private Animator mAnimator = null;

        public ScrollingState() {
            super(ModeListView.this, null);
            ModeListView.this.setVisibility(0);
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            ModeListView.this.scroll(ModeListView.this.mFocusItem, distanceX * ModeListView.SCROLL_FACTOR, ModeListView.SCROLL_FACTOR * distanceY);
            return true;
        }

        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            if (this.mAnimator != null) {
                return false;
            }
            return true;
        }

        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == 1 || ev.getActionMasked() == 3) {
                final boolean shouldSnapBack = ModeListView.this.shouldSnapBack();
                if (shouldSnapBack) {
                    this.mAnimator = ModeListView.this.snapBack();
                } else {
                    this.mAnimator = ModeListView.this.snapToFullScreen();
                }
                this.mAnimator.addListener(new AnimatorListener() {
                    public void onAnimationStart(Animator animation) {
                    }

                    public void onAnimationEnd(Animator animation) {
                        ScrollingState.this.mAnimator = null;
                        ModeListView.this.mFocusItem = -1;
                        if (shouldSnapBack) {
                            ModeListView.this.mCurrentStateManager.setCurrentState(new FullyHiddenState());
                            return;
                        }
                        ModeListView.this.mCurrentStateManager.setCurrentState(new FullyShownState(ModeListView.this, null));
                        UsageStatistics.instance().controlUsed(10000);
                    }

                    public void onAnimationCancel(Animator animation) {
                    }

                    public void onAnimationRepeat(Animator animation) {
                    }
                });
            }
            return true;
        }
    }

    private class SelectedState extends ModeListState {
        public SelectedState(ModeSelectorItem selectedItem) {
            super(ModeListView.this, null);
            int modeId = selectedItem.getModeId();
            for (ModeSelectorItem selected : ModeListView.this.mModeSelectorItems) {
                selected.setSelected(false);
            }
            PeepholeAnimationEffect effect = new PeepholeAnimationEffect();
            effect.setSize(ModeListView.this.mWidth, ModeListView.this.mHeight);
            int[] location = new int[2];
            selectedItem.getIconCenterLocationInWindow(location);
            int i = location[0];
            int iconY = location[1];
            ModeListView.this.getLocationInWindow(location);
            int iconX = i - location[0];
            int iconY2 = iconY - location[1];
            effect.setAnimationStartingPosition(iconX, iconY2);
            effect.setModeSpecificColor(selectedItem.getHighlightColor());
            if (ModeListView.this.mScreenShotProvider != null) {
                effect.setBackground(ModeListView.this.mScreenShotProvider.getPreviewFrame(4), ModeListView.this.mCaptureLayoutHelper.getPreviewRect());
                effect.setBackgroundOverlay(ModeListView.this.mScreenShotProvider.getPreviewOverlayAndControls());
            }
            this.mCurrentAnimationEffects = effect;
            effect.startFadeoutAnimation(null, selectedItem, iconX, iconY2, modeId);
            ModeListView.this.invalidate();
        }

        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            return false;
        }

        public void startModeSelectionAnimation() {
            this.mCurrentAnimationEffects.startAnimation(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    SelectedState.this.mCurrentAnimationEffects = null;
                    ModeListView.this.mCurrentStateManager.setCurrentState(new FullyHiddenState());
                }
            });
        }

        public void hide() {
            if (!this.mCurrentAnimationEffects.cancelAnimation()) {
                this.mCurrentAnimationEffects = null;
                ModeListView.this.mCurrentStateManager.setCurrentState(new FullyHiddenState());
            }
        }
    }

    private class ShimmyState extends ModeListState {
        private Animator mAnimator = null;
        private final Runnable mHideShimmy = new Runnable() {
            public void run() {
                ShimmyState.this.startHidingShimmy();
            }
        };
        private boolean mStartHidingShimmyWhenWindowGainsFocus = false;

        public ShimmyState() {
            super(ModeListView.this, null);
            int i = 0;
            ModeListView.this.setVisibility(0);
            ModeListView.this.mSettingsButton.setVisibility(4);
            ModeListView.this.mModeListOpenFactor = 0.0f;
            ModeListView.this.onModeListOpenRatioUpdate(0.0f);
            int maxVisibleWidth = ModeListView.this.mModeSelectorItems[0].getMaxVisibleWidth();
            while (i < ModeListView.this.mModeSelectorItems.length) {
                ModeListView.this.mModeSelectorItems[i].setVisibleWidth(maxVisibleWidth);
                i++;
            }
            if (ModeListView.this.hasWindowFocus()) {
                hideShimmyWithDelay();
            } else {
                this.mStartHidingShimmyWhenWindowGainsFocus = true;
            }
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            cancelAnimation();
            ModeListView.this.cancelForwardingTouchEvent();
            ModeListView.this.mCurrentStateManager.setCurrentState(new ScrollingState());
            UsageStatistics.instance().controlUsed(10000);
            return true;
        }

        public boolean shouldHandleTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == 0) {
                if (ModeListView.this.isTouchInsideList(ev) && ev.getX() <= ((float) ModeListView.this.mModeSelectorItems[0].getMaxVisibleWidth())) {
                    ModeListView.this.mChildViewTouched = ModeListView.this.mModeSelectorItems[ModeListView.this.getFocusItem(ev.getX(), ev.getY())];
                    return true;
                } else if (ModeListView.this.mLastDownTime != ev.getDownTime()) {
                    ModeListView.this.mLastDownTime = ev.getDownTime();
                    return false;
                }
            }
            return true;
        }

        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == 0 && ev.getActionMasked() == 0) {
                ModeListView.this.mFocusItem = ModeListView.this.getFocusItem(ev.getX(), ev.getY());
                ModeListView.this.setSwipeMode(true);
            }
            ModeListView.this.forwardTouchEventToChild(ev);
            return true;
        }

        public void onItemSelected(ModeSelectorItem selectedItem) {
            cancelAnimation();
            ModeListView.this.mCurrentStateManager.setCurrentState(new SelectedState(selectedItem));
        }

        private void hideShimmyWithDelay() {
            ModeListView.this.postDelayed(this.mHideShimmy, 1000);
        }

        public void onWindowFocusChanged(boolean hasFocus) {
            if (this.mStartHidingShimmyWhenWindowGainsFocus && hasFocus) {
                this.mStartHidingShimmyWhenWindowGainsFocus = false;
                hideShimmyWithDelay();
            }
        }

        private void startHidingShimmy() {
            if (this.mAnimator == null) {
                int maxVisibleWidth = ModeListView.this.mModeSelectorItems[0].getMaxVisibleWidth();
                this.mAnimator = ModeListView.this.animateListToWidth(-100, 850, Gusterpolator.INSTANCE, maxVisibleWidth, 0);
                this.mAnimator.addListener(new AnimatorListener() {
                    private boolean mSuccess = true;

                    public void onAnimationStart(Animator animation) {
                    }

                    public void onAnimationEnd(Animator animation) {
                        ShimmyState.this.mAnimator = null;
                        ShimmyState.this.onAnimationEnd(this.mSuccess);
                    }

                    public void onAnimationCancel(Animator animation) {
                        this.mSuccess = false;
                    }

                    public void onAnimationRepeat(Animator animation) {
                    }
                });
            }
        }

        private void cancelAnimation() {
            ModeListView.this.removeCallbacks(this.mHideShimmy);
            if (this.mAnimator == null || !this.mAnimator.isRunning()) {
                this.mAnimator = null;
                onAnimationEnd(false);
                return;
            }
            this.mAnimator.cancel();
        }

        public void onCurrentState() {
            super.onCurrentState();
            ModeListView.this.disableA11yOnModeSelectorItems();
        }

        private void onAnimationEnd(boolean success) {
            ModeListView.this.mSettingsButton.setVisibility(0);
            if (success) {
                ModeListView.this.enableA11yOnModeSelectorItems();
                ModeListView.this.mModeListOpenFactor = 1.0f;
                ModeListView.this.mCurrentStateManager.setCurrentState(new FullyHiddenState());
                return;
            }
            final ValueAnimator openFactorAnimator = ValueAnimator.ofFloat(new float[]{ModeListView.this.mModeListOpenFactor, 1.0f});
            openFactorAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    ModeListView.this.mModeListOpenFactor = ((Float) openFactorAnimator.getAnimatedValue()).floatValue();
                    ModeListView.this.onVisibleWidthChanged(ModeListView.this.mVisibleWidth);
                }
            });
            openFactorAnimator.addListener(new AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    ModeListView.this.mModeListOpenFactor = 1.0f;
                }

                public void onAnimationCancel(Animator animation) {
                }

                public void onAnimationRepeat(Animator animation) {
                }
            });
            openFactorAnimator.start();
        }

        public void hide() {
            cancelAnimation();
            ModeListView.this.mCurrentStateManager.setCurrentState(new FullyHiddenState());
        }

        public void hideAnimated() {
            cancelAnimation();
            ModeListView.this.animateListToWidth(0).addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    ModeListView.this.mCurrentStateManager.setCurrentState(new FullyHiddenState());
                }
            });
        }
    }

    private void onItemSelected(ModeSelectorItem selectedItem) {
        this.mCurrentStateManager.getCurrentState().onItemSelected(selectedItem);
    }

    private boolean isTouchInsideList(MotionEvent ev) {
        float x = ev.getX() - this.mListView.getX();
        float y = ev.getY() - this.mListView.getY();
        if (x < 0.0f || x > ((float) this.mListView.getWidth()) || y < 0.0f || y > ((float) this.mListView.getHeight())) {
            return false;
        }
        return true;
    }

    public ModeListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mGestureDetector = new GestureDetector(context, this.mOnGestureListener);
        this.mListBackgroundColor = getResources().getColor(R.color.mode_list_background);
        this.mSettingsButtonMargin = getResources().getDimensionPixelSize(R.dimen.mode_list_settings_icon_margin);
    }

    private void disableA11yOnModeSelectorItems() {
        for (View selectorItem : this.mModeSelectorItems) {
            selectorItem.setImportantForAccessibility(2);
        }
    }

    private void enableA11yOnModeSelectorItems() {
        for (View selectorItem : this.mModeSelectorItems) {
            selectorItem.setImportantForAccessibility(0);
        }
    }

    private void setBackgroundAlpha(int alpha) {
        alpha &= 255;
        this.mListBackgroundColor &= ViewCompat.MEASURED_SIZE_MASK;
        this.mListBackgroundColor |= alpha << 24;
        setBackgroundColor(this.mListBackgroundColor);
    }

    public void init(List<Integer> modeIndexList) {
        int i;
        int[] modeSequence = getResources().getIntArray(R.array.camera_modes_in_nav_drawer_if_supported);
        int[] visibleModes = getResources().getIntArray(R.array.camera_modes_always_visible);
        SparseBooleanArray modeIsSupported = new SparseBooleanArray();
        for (i = 0; i < modeIndexList.size(); i++) {
            modeIsSupported.put(((Integer) modeIndexList.get(i)).intValue(), true);
        }
        for (int mode : visibleModes) {
            modeIsSupported.put(mode, true);
        }
        this.mSupportedModes = new ArrayList();
        for (int mode2 : modeSequence) {
            if (modeIsSupported.get(mode2, false)) {
                this.mSupportedModes.add(Integer.valueOf(mode2));
            }
        }
        this.mTotalModes = this.mSupportedModes.size();
        initializeModeSelectorItems();
        this.mSettingsButton = findViewById(R.id.settings_button);
        this.mSettingsButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ModeListView.this.post(new Runnable() {
                    public void run() {
                        ModeListView.this.mModeSwitchListener.onSettingsSelected();
                    }
                });
            }
        });
        onModeListOpenRatioUpdate(0.0f);
        if (this.mCurrentStateManager.getCurrentState() == null) {
            this.mCurrentStateManager.setCurrentState(new FullyHiddenState());
        }
    }

    public void setCameraModuleScreenShotProvider(CameraModuleScreenShotProvider provider) {
        this.mScreenShotProvider = provider;
    }

    private void initializeModeSelectorItems() {
        this.mModeSelectorItems = new ModeSelectorItem[this.mTotalModes];
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        this.mListView = (LinearLayout) findViewById(R.id.mode_list);
        for (int i = 0; i < this.mTotalModes; i++) {
            final ModeSelectorItem selectorItem = (ModeSelectorItem) inflater.inflate(R.layout.mode_selector, null);
            this.mListView.addView(selectorItem);
            if (i == 0) {
                selectorItem.setPadding(selectorItem.getPaddingLeft(), 0, selectorItem.getPaddingRight(), selectorItem.getPaddingBottom());
            }
            if (i == this.mTotalModes - 1) {
                selectorItem.setPadding(selectorItem.getPaddingLeft(), selectorItem.getPaddingTop(), selectorItem.getPaddingRight(), 0);
            }
            int modeId = getModeIndex(i);
            selectorItem.setHighlightColor(getResources().getColor(CameraUtil.getCameraThemeColorId(modeId, getContext())));
            selectorItem.setImageResource(CameraUtil.getCameraModeIconResId(modeId, getContext()));
            selectorItem.setText(CameraUtil.getCameraModeText(modeId, getContext()));
            selectorItem.setContentDescription(CameraUtil.getCameraModeContentDescription(modeId, getContext()));
            selectorItem.setModeId(modeId);
            selectorItem.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    ModeListView.this.onItemSelected(selectorItem);
                }
            });
            this.mModeSelectorItems[i] = selectorItem;
        }
        this.mModeSelectorItems[this.mTotalModes - 1].setVisibleWidthChangedListener(this);
        resetModeSelectors();
    }

    private int getModeIndex(int modeSelectorIndex) {
        if (modeSelectorIndex < this.mTotalModes && modeSelectorIndex >= 0) {
            return ((Integer) this.mSupportedModes.get(modeSelectorIndex)).intValue();
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid mode selector index: ");
        stringBuilder.append(modeSelectorIndex);
        stringBuilder.append(", total modes: ");
        stringBuilder.append(this.mTotalModes);
        Log.e(tag, stringBuilder.toString());
        return getResources().getInteger(R.integer.camera_mode_photo);
    }

    private void onModeSelected(int modeIndex) {
        if (this.mModeSwitchListener != null) {
            this.mModeSwitchListener.onModeSelected(modeIndex);
        }
    }

    public void setModeSwitchListener(ModeSwitchListener listener) {
        this.mModeSwitchListener = listener;
    }

    public void setModeListOpenListener(ModeListOpenListener listener) {
        this.mModeListOpenListener = listener;
    }

    public void setVisibilityChangedListener(ModeListVisibilityChangedListener listener) {
        this.mVisibilityChangedListener = listener;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == 0) {
            this.mChildViewTouched = null;
        }
        if (!this.mCurrentStateManager.getCurrentState().shouldHandleTouchEvent(ev)) {
            return false;
        }
        getParent().requestDisallowInterceptTouchEvent(true);
        super.onTouchEvent(ev);
        this.mGestureDetector.onTouchEvent(ev);
        this.mCurrentStateManager.getCurrentState().onTouchEvent(ev);
        return true;
    }

    private void forwardTouchEventToChild(MotionEvent ev) {
        if (this.mChildViewTouched != null) {
            float x = (ev.getX() - this.mListView.getX()) - ((float) this.mChildViewTouched.getLeft());
            float y = (ev.getY() - this.mListView.getY()) - ((float) this.mChildViewTouched.getTop());
            this.mLastChildTouchEvent = MotionEvent.obtain(ev);
            this.mLastChildTouchEvent.setLocation(x, y);
            this.mChildViewTouched.onTouchEvent(this.mLastChildTouchEvent);
        }
    }

    private void setSwipeMode(boolean swipeIn) {
        for (ModeSelectorItem onSwipeModeChanged : this.mModeSelectorItems) {
            onSwipeModeChanged.onSwipeModeChanged(swipeIn);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mWidth = right - left;
        this.mHeight = ((bottom - top) - getPaddingTop()) - getPaddingBottom();
        updateModeListLayout();
        if (this.mCurrentStateManager.getCurrentState().getCurrentAnimationEffects() != null) {
            this.mCurrentStateManager.getCurrentState().getCurrentAnimationEffects().setSize(this.mWidth, this.mHeight);
        }
    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        this.mCaptureLayoutHelper = helper;
    }

    public void onPreviewAreaChanged(RectF previewArea) {
        if (getVisibility() == 0 && !hasWindowFocus()) {
            updateModeListLayout();
        }
    }

    private void updateModeListLayout() {
        if (this.mCaptureLayoutHelper == null) {
            Log.e(TAG, "Capture layout helper needs to be set first.");
            return;
        }
        RectF uncoveredPreviewArea = this.mCaptureLayoutHelper.getUncoveredPreviewRect();
        this.mListView.setTranslationX(uncoveredPreviewArea.left);
        this.mListView.setTranslationY(uncoveredPreviewArea.centerY() - ((float) (this.mListView.getMeasuredHeight() / 2)));
        updateSettingsButtonLayout(uncoveredPreviewArea);
    }

    private void updateSettingsButtonLayout(RectF uncoveredPreviewArea) {
        if (this.mWidth > this.mHeight) {
            this.mSettingsButton.setTranslationX((uncoveredPreviewArea.right - ((float) this.mSettingsButtonMargin)) - ((float) this.mSettingsButton.getMeasuredWidth()));
            this.mSettingsButton.setTranslationY(uncoveredPreviewArea.top + ((float) this.mSettingsButtonMargin));
        } else {
            this.mSettingsButton.setTranslationX((uncoveredPreviewArea.right - ((float) this.mSettingsButtonMargin)) - ((float) this.mSettingsButton.getMeasuredWidth()));
            this.mSettingsButton.setTranslationY((uncoveredPreviewArea.bottom - ((float) this.mSettingsButtonMargin)) - ((float) this.mSettingsButton.getMeasuredHeight()));
        }
        if (this.mSettingsCling != null) {
            this.mSettingsCling.updatePosition(this.mSettingsButton);
        }
    }

    public void draw(Canvas canvas) {
        AnimationEffects currentEffects = this.mCurrentStateManager.getCurrentState().getCurrentAnimationEffects();
        if (currentEffects != null) {
            currentEffects.drawBackground(canvas);
            if (currentEffects.shouldDrawSuper()) {
                super.draw(canvas);
            }
            currentEffects.drawForeground(canvas);
            return;
        }
        super.draw(canvas);
    }

    public void setShouldShowSettingsCling(boolean show) {
        if (show) {
            if (this.mSettingsCling == null) {
                inflate(getContext(), R.layout.settings_cling, this);
                this.mSettingsCling = (SettingsCling) findViewById(R.id.settings_cling);
            }
        } else if (this.mSettingsCling != null) {
            removeView(this.mSettingsCling);
            this.mSettingsCling = null;
        }
    }

    private void showSettingsClingIfEnabled(boolean show) {
        if (this.mSettingsCling != null) {
            this.mSettingsCling.setVisibility(show ? 0 : 4);
        }
    }

    public void showModeSwitcherHint() {
        this.mCurrentStateManager.getCurrentState().showSwitcherHint();
    }

    public void hide() {
        this.mCurrentStateManager.getCurrentState().hide();
    }

    public void hideAnimated() {
        this.mCurrentStateManager.getCurrentState().hideAnimated();
    }

    private void resetModeSelectors() {
        for (ModeSelectorItem visibleWidth : this.mModeSelectorItems) {
            visibleWidth.setVisibleWidth(0);
        }
    }

    private boolean isRunningAccordionAnimation() {
        return this.mAnimatorSet != null && this.mAnimatorSet.isRunning();
    }

    private int getFocusItem(float x, float y) {
        x -= this.mListView.getX();
        y -= this.mListView.getY();
        for (int i = 0; i < this.mModeSelectorItems.length; i++) {
            if (y <= ((float) this.mModeSelectorItems[i].getBottom())) {
                return i;
            }
        }
        return this.mModeSelectorItems.length - 1;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        this.mCurrentStateManager.getCurrentState().onWindowFocusChanged(hasFocus);
    }

    public void onVisibilityChanged(View v, int visibility) {
        super.onVisibilityChanged(v, visibility);
        boolean z = true;
        if (visibility == 0) {
            if (this.mModeSwitchListener != null) {
                int parentMode = CameraUtil.getCameraModeParentModeId(this.mModeSwitchListener.getCurrentModeIndex(), getContext());
                for (int i = 0; i < this.mSupportedModes.size(); i++) {
                    if (((Integer) this.mSupportedModes.get(i)).intValue() == parentMode) {
                        this.mModeSelectorItems[i].setSelected(true);
                    }
                }
            }
            updateModeListLayout();
        } else {
            if (this.mModeSelectorItems != null) {
                for (ModeSelectorItem selected : this.mModeSelectorItems) {
                    selected.setSelected(false);
                }
            }
            if (this.mModeListOpenListener != null) {
                this.mModeListOpenListener.onModeListClosed();
            }
        }
        if (this.mVisibilityChangedListener != null) {
            ModeListVisibilityChangedListener modeListVisibilityChangedListener = this.mVisibilityChangedListener;
            if (getVisibility() != 0) {
                z = false;
            }
            modeListVisibilityChangedListener.onVisibilityEvent(z);
        }
    }

    public void setVisibility(int visibility) {
        ModeListState currentState = this.mCurrentStateManager.getCurrentState();
        if (currentState == null || currentState.shouldHandleVisibilityChange(visibility)) {
            super.setVisibility(visibility);
        }
    }

    private void scroll(int itemId, float deltaX, float deltaY) {
        float longestWidth;
        this.mScrollTrendX = (this.mScrollTrendX * 0.3f) + (deltaX * 0.7f);
        this.mScrollTrendY = (this.mScrollTrendY * 0.3f) + (0.7f * deltaY);
        this.mCurrentTime = SystemClock.uptimeMillis();
        int i = 0;
        if (itemId != -1) {
            longestWidth = (float) this.mModeSelectorItems[itemId].getVisibleWidth();
        } else {
            longestWidth = (float) this.mModeSelectorItems[0].getVisibleWidth();
        }
        float newPosition = Math.max(Math.min(longestWidth - deltaX, getMaxMovementBasedOnPosition((int) longestWidth, this.mModeSelectorItems[0].getMaxVisibleWidth())), 0.0f);
        insertNewPosition(newPosition, this.mCurrentTime);
        while (i < this.mModeSelectorItems.length) {
            this.mModeSelectorItems[i].setVisibleWidth(calculateVisibleWidthForItem(i, (int) newPosition));
            i++;
        }
    }

    private int calculateVisibleWidthForItem(int itemId, int longestWidth) {
        if (itemId == this.mFocusItem || this.mFocusItem == -1) {
            return longestWidth;
        }
        return (int) getPosition(this.mCurrentTime - ((long) (Math.abs(itemId - this.mFocusItem) * 30)), (float) this.mModeSelectorItems[itemId].getVisibleWidth());
    }

    private void insertNewPosition(float position, long time) {
        this.mPositionHistory.add(new TimeBasedPosition(position, time));
        long timeCutoff = time - ((long) ((this.mTotalModes - 1) * 30));
        while (this.mPositionHistory.size() > 0 && ((TimeBasedPosition) this.mPositionHistory.getFirst()).getTimeStamp() < timeCutoff) {
            this.mPositionHistory.removeFirst();
        }
    }

    private float getPosition(long time, float currentPosition) {
        int i = 0;
        while (i < this.mPositionHistory.size()) {
            TimeBasedPosition historyPosition = (TimeBasedPosition) this.mPositionHistory.get(i);
            if (historyPosition.getTimeStamp() <= time) {
                i++;
            } else if (i == 0) {
                return (historyPosition.getPosition() * 0.2f) + ((1.0f - 0.2f) * currentPosition);
            } else {
                TimeBasedPosition prevTimeBasedPosition = (TimeBasedPosition) this.mPositionHistory.get(i - 1);
                return ((historyPosition.getPosition() - prevTimeBasedPosition.getPosition()) * (((float) (time - prevTimeBasedPosition.getTimeStamp())) / ((float) (historyPosition.getTimeStamp() - prevTimeBasedPosition.getTimeStamp())))) + prevTimeBasedPosition.getPosition();
            }
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid time input for getPosition(). time: ");
        stringBuilder.append(time);
        Log.e(tag, stringBuilder.toString());
        if (this.mPositionHistory.size() == 0) {
            Log.e(TAG, "TimeBasedPosition history size is 0");
        } else {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("First position recorded at ");
            stringBuilder.append(((TimeBasedPosition) this.mPositionHistory.getFirst()).getTimeStamp());
            stringBuilder.append(" , last position recorded at ");
            stringBuilder.append(((TimeBasedPosition) this.mPositionHistory.getLast()).getTimeStamp());
            Log.e(tag, stringBuilder.toString());
        }
        return (float) i;
    }

    private void reset() {
        resetModeSelectors();
        this.mScrollTrendX = 0.0f;
        this.mScrollTrendY = 0.0f;
        setVisibility(4);
    }

    public void onVisibleWidthChanged(int visibleWidth) {
        this.mVisibleWidth = visibleWidth;
        int maxVisibleWidth = this.mModeSelectorItems[0].getMaxVisibleWidth();
        visibleWidth = Math.min(maxVisibleWidth, visibleWidth);
        if (visibleWidth != maxVisibleWidth) {
            cancelForwardingTouchEvent();
        }
        onModeListOpenRatioUpdate(this.mModeListOpenFactor * (((float) visibleWidth) / ((float) maxVisibleWidth)));
    }

    private void onModeListOpenRatioUpdate(float openRatio) {
        for (ModeSelectorItem textAlpha : this.mModeSelectorItems) {
            textAlpha.setTextAlpha(openRatio);
        }
        setBackgroundAlpha((int) (153.0f * openRatio));
        if (this.mModeListOpenListener != null) {
            this.mModeListOpenListener.onModeListOpenProgress(openRatio);
        }
        if (this.mSettingsButton != null) {
            this.mSettingsButton.setAlpha(openRatio);
        }
    }

    private void cancelForwardingTouchEvent() {
        if (this.mChildViewTouched != null) {
            this.mLastChildTouchEvent.setAction(3);
            this.mChildViewTouched.onTouchEvent(this.mLastChildTouchEvent);
            this.mChildViewTouched = null;
        }
    }

    public void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != 0) {
            this.mCurrentStateManager.getCurrentState().hide();
        }
    }

    public boolean onMenuPressed() {
        return this.mCurrentStateManager.getCurrentState().onMenuPressed();
    }

    private void snap() {
        if (shouldSnapBack()) {
            snapBack();
        } else {
            snapToFullScreen();
        }
    }

    private boolean shouldSnapBack() {
        boolean z = false;
        int itemId = Math.max(0, this.mFocusItem);
        if (Math.abs(this.mVelocityX) > 2.0f) {
            if (this.mVelocityX < 0.0f) {
                z = true;
            }
            return z;
        } else if (((float) this.mModeSelectorItems[itemId].getVisibleWidth()) < ((float) this.mModeSelectorItems[itemId].getMaxVisibleWidth()) * SNAP_BACK_THRESHOLD_RATIO) {
            return true;
        } else {
            if (Math.abs(this.mScrollTrendX) <= Math.abs(this.mScrollTrendY) || this.mScrollTrendX <= 0.0f) {
                return false;
            }
            return true;
        }
    }

    public Animator snapBack(boolean withAnimation) {
        if (!withAnimation) {
            setVisibility(4);
            resetModeSelectors();
            return null;
        } else if (this.mVelocityX <= -1.0f) {
            return animateListToWidthAtVelocity(this.mVelocityX, 0);
        } else {
            return animateListToWidth(0);
        }
    }

    private Animator snapBack() {
        return snapBack(true);
    }

    private Animator snapToFullScreen() {
        Animator animator;
        int fullWidth = this.mModeSelectorItems[this.mFocusItem == -1 ? 0 : this.mFocusItem].getMaxVisibleWidth();
        if (this.mVelocityX <= 2.0f) {
            animator = animateListToWidth(fullWidth);
        } else {
            animator = animateListToWidthAtVelocity(2.0f, fullWidth);
        }
        if (this.mModeListOpenListener != null) {
            this.mModeListOpenListener.onOpenFullScreen();
        }
        return animator;
    }

    private Animator animateListToWidth(int... width) {
        return animateListToWidth(0, 200, null, width);
    }

    private Animator animateListToWidth(int delay, int duration, TimeInterpolator interpolator, int... width) {
        if (this.mAnimatorSet != null && this.mAnimatorSet.isRunning()) {
            this.mAnimatorSet.end();
        }
        ArrayList<Animator> animators = new ArrayList();
        boolean animateModeItemsInOrder = true;
        if (delay < 0) {
            animateModeItemsInOrder = false;
            delay *= -1;
        }
        for (int i = 0; i < this.mTotalModes; i++) {
            ObjectAnimator animator;
            if (animateModeItemsInOrder) {
                animator = ObjectAnimator.ofInt(this.mModeSelectorItems[i], "visibleWidth", width);
            } else {
                animator = ObjectAnimator.ofInt(this.mModeSelectorItems[(this.mTotalModes - 1) - i], "visibleWidth", width);
            }
            animator.setDuration((long) duration);
            animator.setStartDelay((long) (i * delay));
            animators.add(animator);
        }
        this.mAnimatorSet = new AnimatorSet();
        this.mAnimatorSet.playTogether(animators);
        this.mAnimatorSet.setInterpolator(interpolator);
        this.mAnimatorSet.start();
        return this.mAnimatorSet;
    }

    private Animator animateListToWidthAtVelocity(float velocity, int width) {
        if (this.mAnimatorSet != null && this.mAnimatorSet.isRunning()) {
            this.mAnimatorSet.end();
        }
        ArrayList<Animator> animators = new ArrayList();
        int i;
        if (this.mFocusItem == -1) {
            i = 0;
        } else {
            i = this.mFocusItem;
        }
        for (int i2 = 0; i2 < this.mTotalModes; i2++) {
            ObjectAnimator animator = ObjectAnimator.ofInt(this.mModeSelectorItems[i2], "visibleWidth", new int[]{width});
            animator.setDuration((long) ((int) (((float) width) / velocity)));
            animators.add(animator);
        }
        this.mAnimatorSet = new AnimatorSet();
        this.mAnimatorSet.playTogether(animators);
        this.mAnimatorSet.setInterpolator(null);
        this.mAnimatorSet.start();
        return this.mAnimatorSet;
    }

    public boolean onBackPressed() {
        return this.mCurrentStateManager.getCurrentState().onBackPressed();
    }

    public void startModeSelectionAnimation() {
        this.mCurrentStateManager.getCurrentState().startModeSelectionAnimation();
    }

    public float getMaxMovementBasedOnPosition(int lastVisibleWidth, int maxWidth) {
        float position;
        int timeElapsed = (int) (System.currentTimeMillis() - this.mLastScrollTime);
        if (timeElapsed > 50) {
            timeElapsed = 50;
        }
        int slowZone = (int) (((float) maxWidth) * 1045220557);
        if (lastVisibleWidth < maxWidth - slowZone) {
            position = (2.0f * ((float) timeElapsed)) + ((float) lastVisibleWidth);
        } else {
            position = (((float) timeElapsed) * ((1.0f - ((float) ((lastVisibleWidth - (maxWidth - slowZone)) / slowZone))) * 2.0f)) + ((float) lastVisibleWidth);
        }
        return Math.min((float) maxWidth, position);
    }
}
