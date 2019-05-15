package com.hmdglobal.app.camera.widget;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.DimenRes;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.ui.RotateImageView;

public class SoundAction extends ViewGroup {
    private static final int ANIMATION_DURATION = 300;
    private boolean isPortrait = false;
    private RotateImageView mAddButton;
    private int mButtonSpacing = getDimension(R.dimen.sound_icon_spacing);
    private AnimatorSet mCollapseAnimation = new AnimatorSet().setDuration(300);
    private AnimatorSet mExpandAnimation = new AnimatorSet().setDuration(300);
    private boolean mExpanded;

    private class LayoutParams extends android.view.ViewGroup.LayoutParams {
        private ObjectAnimator mCollapseAlpha = new ObjectAnimator();
        private ObjectAnimator mCollapseX = new ObjectAnimator();
        private ObjectAnimator mCollapseY = new ObjectAnimator();
        private ObjectAnimator mExpandAlpha = new ObjectAnimator();
        private ObjectAnimator mExpandX = new ObjectAnimator();
        private ObjectAnimator mExpandY = new ObjectAnimator();
        private Interpolator sAlphaExpandInterpolator = new DecelerateInterpolator();
        private Interpolator sCollapseInterpolator = new DecelerateInterpolator(3.0f);
        private Interpolator sExpandInterpolator = new OvershootInterpolator();

        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
            this.mExpandY.setInterpolator(this.sExpandInterpolator);
            this.mExpandX.setInterpolator(this.sExpandInterpolator);
            this.mExpandAlpha.setInterpolator(this.sAlphaExpandInterpolator);
            this.mCollapseY.setInterpolator(this.sCollapseInterpolator);
            this.mCollapseX.setInterpolator(this.sCollapseInterpolator);
            this.mCollapseAlpha.setInterpolator(this.sCollapseInterpolator);
            this.mCollapseAlpha.setProperty(View.ALPHA);
            this.mCollapseAlpha.setFloatValues(new float[]{1.0f, 0.0f});
            this.mExpandAlpha.setProperty(View.ALPHA);
            this.mExpandAlpha.setFloatValues(new float[]{0.0f, 1.0f});
            this.mCollapseY.setProperty(View.TRANSLATION_Y);
            this.mCollapseX.setProperty(View.TRANSLATION_X);
            this.mExpandY.setProperty(View.TRANSLATION_Y);
            this.mExpandX.setProperty(View.TRANSLATION_X);
            SoundAction.this.mExpandAnimation.play(this.mExpandAlpha);
            if (SoundAction.this.isPortrait) {
                SoundAction.this.mExpandAnimation.play(this.mExpandY);
            } else {
                SoundAction.this.mExpandAnimation.play(this.mExpandX);
            }
            SoundAction.this.mCollapseAnimation.play(this.mCollapseAlpha);
            if (SoundAction.this.isPortrait) {
                SoundAction.this.mCollapseAnimation.play(this.mCollapseY);
            } else {
                SoundAction.this.mCollapseAnimation.play(this.mCollapseX);
            }
        }

        public void setAnimationsTarget(View view) {
            this.mCollapseAlpha.setTarget(view);
            this.mCollapseY.setTarget(view);
            this.mCollapseX.setTarget(view);
            this.mExpandAlpha.setTarget(view);
            this.mExpandY.setTarget(view);
            this.mExpandX.setTarget(view);
        }
    }

    public SoundAction(Context context, AttributeSet attrs) {
        super(context, attrs);
        createAddButton(context);
    }

    private void createAddButton(Context context) {
        this.mAddButton = new RotateImageView(context);
        this.mAddButton.setImageResource(R.drawable.ic_kid_show);
        android.view.ViewGroup.LayoutParams ps = new android.view.ViewGroup.LayoutParams(getDimension(R.dimen.sound_icon_width), getDimension(R.dimen.sound_icon_height));
        this.mAddButton.setLayoutParams(ps);
        this.mAddButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                SoundAction.this.toggle();
            }
        });
        addView(this.mAddButton, ps);
    }

    /* Access modifiers changed, original: 0000 */
    public int getDimension(@DimenRes int id) {
        return (int) getResources().getDimension(id);
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int width = 0;
        int height = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            height = Math.max(height, child.getMeasuredHeight());
            width += child.getMeasuredWidth();
        }
        setMeasuredDimension(((width + (this.mButtonSpacing * (getChildCount() - 1))) * 12) / 10, height);
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int addButtonHeight;
        int marginRight;
        int addButtonWidth = this.mAddButton.getMeasuredWidth();
        int addButtonHeight2 = this.mAddButton.getMeasuredHeight();
        int marginRight2 = getDimension(R.dimen.sound_icon_margin_right);
        int addButtonX = r + marginRight2;
        int addButtonY = (b - t) - addButtonHeight2;
        int bottomX = addButtonX - addButtonWidth;
        int bottomY = addButtonY + addButtonHeight2;
        this.mAddButton.layout(addButtonX - addButtonWidth, addButtonY, addButtonX, bottomY);
        bottomX -= this.mButtonSpacing;
        int i = 1;
        int i2 = getChildCount() - 1;
        while (i2 >= 0) {
            int addButtonWidth2;
            int i3;
            View child = getChildAt(i2);
            if (child == this.mAddButton) {
                addButtonWidth2 = addButtonWidth;
                addButtonHeight = addButtonHeight2;
                marginRight = marginRight2;
                i3 = i;
            } else {
                int childX = bottomX;
                child.layout(childX - child.getMeasuredWidth(), addButtonY, childX, bottomY);
                float collapsedTranslation = (float) (addButtonX - childX);
                child.setTranslationX(this.mExpanded ? 0.0f : collapsedTranslation);
                child.setAlpha(this.mExpanded ? 1.0f : 0.0f);
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                addButtonWidth2 = addButtonWidth;
                addButtonWidth = params.mCollapseX;
                addButtonHeight = addButtonHeight2;
                marginRight = marginRight2;
                float[] fArr = new float[2];
                fArr[0] = 0.0f;
                i3 = 1;
                fArr[1] = collapsedTranslation;
                addButtonWidth.setFloatValues(fArr);
                params.mExpandX.setFloatValues(new float[]{collapsedTranslation, 0.0f});
                params.setAnimationsTarget(child);
                bottomX = (childX - this.mButtonSpacing) - child.getMeasuredHeight();
            }
            i2--;
            i = i3;
            addButtonWidth = addButtonWidth2;
            addButtonHeight2 = addButtonHeight;
            marginRight2 = marginRight;
        }
        addButtonHeight = addButtonHeight2;
        marginRight = marginRight2;
    }

    public android.view.ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(-1, -1);
        params.gravity = 5;
        return new LayoutParams(super.generateLayoutParams(params));
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        bringChildToFront(this.mAddButton);
    }

    public void collapse() {
        if (this.mExpanded) {
            this.mExpanded = false;
            this.mAddButton.setImageResource(R.drawable.ic_kid_show);
            this.mCollapseAnimation.start();
            this.mExpandAnimation.cancel();
        }
    }

    private void toggle() {
        if (this.mExpanded) {
            collapse();
        } else {
            post(new Runnable() {
                public void run() {
                    SoundAction.this.expand();
                }
            });
        }
    }

    private void expand() {
        if (!this.mExpanded) {
            this.mExpanded = true;
            this.mAddButton.setImageResource(R.drawable.ic_kid_close);
            this.mCollapseAnimation.cancel();
            this.mExpandAnimation.start();
        }
    }
}
