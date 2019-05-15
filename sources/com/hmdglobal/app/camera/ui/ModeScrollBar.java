package com.hmdglobal.app.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.Gusterpolator;
import java.util.List;

public class ModeScrollBar extends View {
    private static final int ANIM_PHASE_ONE_DURATION = 300;
    private static final int ANIM_PHASE_TWO_DURATION = 250;
    private final int INDEX_OUT_OF_BOUNDS = -2;
    private final int ITEM_LIST_NOT_INIT = -1;
    private final Tag TAG = new Tag("ModeScrollBar");
    private int barCenter;
    private int barLeft;
    private int barRight;
    private int barWidth;
    ColorDrawable leftBar = new ColorDrawable(getResources().getColor(R.color.mode_scroll_bar_color_background));
    AnimatorSet mAnimator;
    private List<Integer> mItemWidthList;
    private int mScreenWidth;
    private AnimatorUpdateListener mWidthAnimatorListener = new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            ModeScrollBar.this.barWidth = (int) ((Float) valueAnimator.getAnimatedValue()).floatValue();
        }
    };
    ColorDrawable rightBar = new ColorDrawable(getResources().getColor(R.color.mode_scroll_bar_color_background));
    ColorDrawable selectBar = new ColorDrawable(getResources().getColor(R.color.mode_scroll_bar_color_select));

    public interface onBarStatueChangedListener {
        void onEndArrived();

        void onItemReached();

        void onScrollFinished();

        void onScrollStarted();
    }

    public ModeScrollBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setItemWidth(List list) {
        this.mItemWidthList = list;
    }

    private int getItemWidth(int index) {
        if (this.mItemWidthList == null) {
            return -1;
        }
        if (index < 0 || index >= this.mItemWidthList.size()) {
            return -2;
        }
        return ((Integer) this.mItemWidthList.get(index)).intValue();
    }

    private boolean checkValue(int sp, int ep, int sw, int ew) {
        return sp > 0 && ep > 0 && sw > 0 && ew > 0;
    }

    public void setOriIndex(int index) {
        if (this.mScreenWidth == 0) {
            this.mScreenWidth = getMeasuredWidth();
        }
        updateWidth(index);
    }

    public void updateWidth(int index) {
        int width = getItemWidth(index);
        if (width > 0) {
            this.barLeft = (this.mScreenWidth / 2) - (width / 2);
            this.barRight = (this.mScreenWidth / 2) + (width / 2);
            invalidate();
        }
    }

    public void scrollToLeft(int index, onBarStatueChangedListener listener) {
        if (index >= 1) {
            int startPos = this.mScreenWidth / 2;
            int startWidth = getItemWidth(index);
            int endWidth = getItemWidth(index - 1);
            int endPos = startPos - ((startWidth + endWidth) / 2);
            if (checkValue(startPos, endPos, startWidth, endWidth)) {
                startScrollAnimation((float) startPos, (float) endPos, (float) startWidth, (float) endWidth, listener);
            }
        }
    }

    public void scrollToRight(int index, onBarStatueChangedListener listener) {
        if (this.mItemWidthList != null && index <= this.mItemWidthList.size() - 2) {
            int startPos = this.mScreenWidth / 2;
            int startWidth = getItemWidth(index);
            int endWidth = getItemWidth(index + 1);
            int endPos = ((startWidth + endWidth) / 2) + startPos;
            if (checkValue(startPos, endPos, startWidth, endWidth)) {
                startScrollAnimation((float) startPos, (float) endPos, (float) startWidth, (float) endWidth, listener);
            }
        }
    }

    public void scrollToItem(int current, int target, int pos, onBarStatueChangedListener listener) {
        int startPos = this.mScreenWidth / 2;
        int startWidth = getItemWidth(current);
        int endWidth = getItemWidth(target);
        int endPos = pos;
        if (checkValue(startPos, endPos, startWidth, endWidth)) {
            startScrollAnimation((float) startPos, (float) endPos, (float) startWidth, (float) endWidth, listener);
        }
    }

    private void startScrollAnimation(float startPosition, float targetPosition, float startWidth, float targetWidth, onBarStatueChangedListener listener) {
        final onBarStatueChangedListener onbarstatuechangedlistener = listener;
        if (this.mAnimator == null || !this.mAnimator.isRunning()) {
            this.mAnimator = new AnimatorSet();
            ValueAnimator widthAnimator = ValueAnimator.ofFloat(new float[]{startWidth, targetWidth});
            widthAnimator.setDuration(300);
            ValueAnimator posStartAnimator = ValueAnimator.ofFloat(new float[]{startPosition, targetPosition});
            posStartAnimator.setDuration(300);
            ValueAnimator posEndAnimator = ValueAnimator.ofFloat(new float[]{targetPosition, startPosition});
            posEndAnimator.setDuration(250);
            widthAnimator.setInterpolator(Gusterpolator.INSTANCE);
            posStartAnimator.setInterpolator(Gusterpolator.INSTANCE);
            posEndAnimator.setInterpolator(Gusterpolator.INSTANCE);
            final boolean headingLeft = targetPosition - startPosition < 0.0f;
            widthAnimator.addUpdateListener(this.mWidthAnimatorListener);
            final float f = targetPosition;
            ValueAnimator posEndAnimator2 = posEndAnimator;
            final float f2 = targetWidth;
            ValueAnimator posStartAnimator2 = posStartAnimator;
            final onBarStatueChangedListener posStartAnimator3 = onbarstatuechangedlistener;
            AnonymousClass1 positionAnimatorListener = new AnimatorUpdateListener() {
                private boolean isItemReachCallbacked = false;

                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    ModeScrollBar.this.barCenter = (int) ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    ModeScrollBar.this.barLeft = ModeScrollBar.this.barCenter - (ModeScrollBar.this.barWidth / 2);
                    ModeScrollBar.this.barRight = ModeScrollBar.this.barCenter + (ModeScrollBar.this.barWidth / 2);
                    if (!this.isItemReachCallbacked) {
                        if (headingLeft && ((float) ModeScrollBar.this.barLeft) < f + (f2 / 2.0f)) {
                            posStartAnimator3.onItemReached();
                            this.isItemReachCallbacked = true;
                        } else if (!headingLeft && ((float) ModeScrollBar.this.barRight) > f - (f2 / 2.0f)) {
                            posStartAnimator3.onItemReached();
                            this.isItemReachCallbacked = true;
                        }
                    }
                    ModeScrollBar.this.invalidate();
                }
            };
            posStartAnimator2.addUpdateListener(positionAnimatorListener);
            posEndAnimator2.addUpdateListener(positionAnimatorListener);
            posEndAnimator2.addListener(new AnimatorListener() {
                public void onAnimationStart(Animator animator) {
                }

                public void onAnimationEnd(Animator animator) {
                    if (onbarstatuechangedlistener != null) {
                        onbarstatuechangedlistener.onScrollFinished();
                    }
                }

                public void onAnimationCancel(Animator animator) {
                }

                public void onAnimationRepeat(Animator animator) {
                }
            });
            AnimatorSet phaseStartAnimation = new AnimatorSet();
            phaseStartAnimation.playTogether(new Animator[]{widthAnimator, posStartAnimator2});
            phaseStartAnimation.addListener(new AnimatorListener() {
                public void onAnimationStart(Animator animator) {
                    if (onbarstatuechangedlistener != null) {
                        onbarstatuechangedlistener.onScrollStarted();
                    }
                }

                public void onAnimationEnd(Animator animator) {
                    if (onbarstatuechangedlistener != null) {
                        onbarstatuechangedlistener.onEndArrived();
                    }
                }

                public void onAnimationCancel(Animator animator) {
                }

                public void onAnimationRepeat(Animator animator) {
                }
            });
            this.mAnimator.playSequentially(new Animator[]{phaseStartAnimation, posEndAnimator2});
            this.mAnimator.start();
        }
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        canvas.save();
        int height = canvas.getHeight();
        this.selectBar.setBounds(this.barLeft, 0, this.barRight, height);
        this.selectBar.draw(canvas);
        this.leftBar.setBounds(0, 0, this.barLeft, height);
        this.leftBar.draw(canvas);
        this.rightBar.setBounds(this.barRight, 0, this.mScreenWidth, height);
        this.rightBar.draw(canvas);
        canvas.restore();
        super.onDraw(canvas);
    }

    public void hide() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            setVisibility(4);
        } else {
            getHandler().post(new Runnable() {
                public void run() {
                    ModeScrollBar.this.setVisibility(4);
                }
            });
        }
    }

    public void show() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            setVisibility(0);
        } else {
            getHandler().post(new Runnable() {
                public void run() {
                    ModeScrollBar.this.setVisibility(0);
                }
            });
        }
    }
}
