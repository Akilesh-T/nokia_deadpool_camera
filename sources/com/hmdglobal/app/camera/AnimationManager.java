package com.hmdglobal.app.camera;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;

public class AnimationManager {
    private static final int CIRCLE_ANIM_DURATION_MS = 500;
    public static final float FLASH_ALPHA_END = 0.0f;
    public static final float FLASH_ALPHA_START = 0.3f;
    public static final int FLASH_DURATION = 300;
    public static final int HOLD_DURATION = 2500;
    public static final int SHRINK_DURATION = 400;
    public static final int SLIDE_DURATION = 1100;
    private AnimatorSet mCaptureAnimator;
    private ObjectAnimator mFlashAnim;

    public void startCaptureAnimation(View view) {
        final View view2 = view;
        if (this.mCaptureAnimator != null && this.mCaptureAnimator.isStarted()) {
            this.mCaptureAnimator.cancel();
        }
        View parentView = (View) view.getParent();
        float slideDistance = (float) (parentView.getWidth() - view.getLeft());
        float scaleX = ((float) parentView.getWidth()) / ((float) view.getWidth());
        float scaleY = ((float) parentView.getHeight()) / ((float) view.getHeight());
        float scale = scaleX > scaleY ? scaleX : scaleY;
        int centerX = view.getLeft() + (view.getWidth() / 2);
        int centerY = view.getTop() + (view.getHeight() / 2);
        ObjectAnimator slide = ObjectAnimator.ofFloat(view2, "translationX", new float[]{0.0f, slideDistance}).setDuration(1100);
        slide.setStartDelay(2900);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(view2, "translationY", new float[]{(float) ((parentView.getHeight() / 2) - centerY), 0.0f}).setDuration(400);
        translateY.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
                view2.setClickable(true);
            }

            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mCaptureAnimator = new AnimatorSet();
        AnimatorSet animatorSet = this.mCaptureAnimator;
        Animator[] animatorArr = new Animator[5];
        ObjectAnimator slide2 = slide;
        animatorArr[0] = ObjectAnimator.ofFloat(view2, "scaleX", new float[]{scale, 1.0f}).setDuration(400);
        animatorArr[1] = ObjectAnimator.ofFloat(view2, "scaleY", new float[]{scale, 1.0f}).setDuration(400);
        animatorArr[2] = ObjectAnimator.ofFloat(view2, "translationX", new float[]{(float) ((parentView.getWidth() / 2) - centerX), 0.0f}).setDuration(400);
        animatorArr[3] = translateY;
        animatorArr[4] = slide2;
        animatorSet.playTogether(animatorArr);
        this.mCaptureAnimator.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator animator) {
                view2.setClickable(false);
                view2.setVisibility(0);
            }

            public void onAnimationEnd(Animator animator) {
                view2.setScaleX(1.0f);
                view2.setScaleX(1.0f);
                view2.setTranslationX(0.0f);
                view2.setTranslationY(0.0f);
                view2.setVisibility(4);
                AnimationManager.this.mCaptureAnimator.removeAllListeners();
                AnimationManager.this.mCaptureAnimator = null;
            }

            public void onAnimationCancel(Animator animator) {
                view2.setVisibility(4);
            }

            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mCaptureAnimator.start();
    }

    public void startFlashAnimation(final View flashOverlay) {
        if (this.mFlashAnim != null && this.mFlashAnim.isRunning()) {
            this.mFlashAnim.cancel();
        }
        this.mFlashAnim = ObjectAnimator.ofFloat(flashOverlay, "alpha", new float[]{0.3f, 0.0f});
        this.mFlashAnim.setDuration(300);
        this.mFlashAnim.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator animator) {
                flashOverlay.setVisibility(0);
            }

            public void onAnimationEnd(Animator animator) {
                flashOverlay.setAlpha(0.0f);
                flashOverlay.setVisibility(8);
                AnimationManager.this.mFlashAnim.removeAllListeners();
                AnimationManager.this.mFlashAnim = null;
            }

            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mFlashAnim.start();
    }

    public void cancelAnimations() {
        if (this.mFlashAnim != null && this.mFlashAnim.isRunning()) {
            this.mFlashAnim.cancel();
        }
        if (this.mCaptureAnimator != null && this.mCaptureAnimator.isStarted()) {
            this.mCaptureAnimator.cancel();
        }
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x0003, element type: float, insn element type: null */
    public static android.animation.ValueAnimator buildShowingAnimator(final android.view.View r3) {
        /*
        r0 = 2;
        r0 = new float[r0];
        r0 = {0, 1065353216};
        r0 = android.animation.ValueAnimator.ofFloat(r0);
        r1 = 500; // 0x1f4 float:7.0E-43 double:2.47E-321;
        r0.setDuration(r1);
        r1 = com.hmdglobal.app.camera.util.Gusterpolator.INSTANCE;
        r0.setInterpolator(r1);
        r1 = new com.hmdglobal.app.camera.AnimationManager$4;
        r1.<init>(r3);
        r0.addUpdateListener(r1);
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.AnimationManager.buildShowingAnimator(android.view.View):android.animation.ValueAnimator");
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x0003, element type: float, insn element type: null */
    public static android.animation.ValueAnimator buildHidingAnimator(final android.view.View r3) {
        /*
        r0 = 2;
        r0 = new float[r0];
        r0 = {1065353216, 0};
        r0 = android.animation.ValueAnimator.ofFloat(r0);
        r1 = 500; // 0x1f4 float:7.0E-43 double:2.47E-321;
        r0.setDuration(r1);
        r1 = com.hmdglobal.app.camera.util.Gusterpolator.INSTANCE;
        r0.setInterpolator(r1);
        r1 = new com.hmdglobal.app.camera.AnimationManager$5;
        r1.<init>(r3);
        r0.addUpdateListener(r1);
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.AnimationManager.buildHidingAnimator(android.view.View):android.animation.ValueAnimator");
    }
}
