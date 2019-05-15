package com.hmdglobal.app.camera.ui;

import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;

public class MT_AnimationController {
    private static final int ANIM_DURATION = 180;
    private static final String TAG = "AnimationController";
    private Runnable mApplyCenterArrowAnim = new Runnable() {
        private int dotCount = 0;

        public void run() {
            if (this.dotCount == 0) {
                this.dotCount = MT_AnimationController.this.mCenterArrow.getChildCount();
            }
            if (this.dotCount <= MT_AnimationController.this.mCenterDotIndex) {
                String str = MT_AnimationController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[run]mApplyCenterArrowAnim return,dotCount = ");
                stringBuilder.append(this.dotCount);
                stringBuilder.append(",mCenterDotIndex =");
                stringBuilder.append(MT_AnimationController.this.mCenterDotIndex);
                Log.w(str, stringBuilder.toString());
                return;
            }
            AlphaAnimation alpha = new AlphaAnimation(1.0f, 0.0f);
            alpha.setDuration(1440);
            alpha.setRepeatCount(-1);
            if (MT_AnimationController.this.mCenterArrow != null) {
                MT_AnimationController.this.mCenterArrow.getChildAt(MT_AnimationController.this.mCenterDotIndex).startAnimation(alpha);
            }
            alpha.startNow();
            MT_AnimationController.this.mCenterDotIndex = MT_AnimationController.this.mCenterDotIndex + 1;
            MT_AnimationController.this.mHanler.postDelayed(this, (long) (360 / this.dotCount));
        }
    };
    private Runnable mApplyDirectionAnim = new Runnable() {
        private int dotCount = 0;

        public void run() {
            for (ViewGroup viewGroup : MT_AnimationController.this.mDirectionIndicators) {
                if (viewGroup == null) {
                    Log.w(MT_AnimationController.TAG, "[run]viewGroup is null,return!");
                    return;
                }
            }
            if (this.dotCount == 0) {
                this.dotCount = MT_AnimationController.this.mDirectionIndicators[0].getChildCount();
            }
            if (this.dotCount <= MT_AnimationController.this.mDirectionDotIndex) {
                String str = MT_AnimationController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[run]mApplyDirectionAnim,return,dotCount = ");
                stringBuilder.append(this.dotCount);
                stringBuilder.append(",mCenterDotIndex =");
                stringBuilder.append(MT_AnimationController.this.mCenterDotIndex);
                Log.i(str, stringBuilder.toString());
                return;
            }
            AlphaAnimation alpha = new AlphaAnimation(1.0f, 0.0f);
            alpha.setDuration((long) (((180 * this.dotCount) * 3) / 2));
            alpha.setRepeatCount(-1);
            MT_AnimationController.this.mDirectionIndicators[0].getChildAt(MT_AnimationController.this.mDirectionDotIndex).startAnimation(alpha);
            MT_AnimationController.this.mDirectionIndicators[1].getChildAt((this.dotCount - MT_AnimationController.this.mDirectionDotIndex) - 1).startAnimation(alpha);
            MT_AnimationController.this.mDirectionIndicators[2].getChildAt((this.dotCount - MT_AnimationController.this.mDirectionDotIndex) - 1).startAnimation(alpha);
            MT_AnimationController.this.mDirectionIndicators[3].getChildAt(MT_AnimationController.this.mDirectionDotIndex).startAnimation(alpha);
            alpha.startNow();
            MT_AnimationController.this.mDirectionDotIndex = MT_AnimationController.this.mDirectionDotIndex + 1;
            MT_AnimationController.this.mHanler.postDelayed(this, 90);
        }
    };
    private ViewGroup mCenterArrow;
    private int mCenterDotIndex = 0;
    private int mDirectionDotIndex = 0;
    private ViewGroup[] mDirectionIndicators;
    private Handler mHanler = new Handler();

    public MT_AnimationController(ViewGroup[] indicators, ViewGroup arrow) {
        this.mDirectionIndicators = indicators;
        this.mCenterArrow = arrow;
    }

    public void startDirectionAnimation() {
        Log.i(TAG, "[startDirectionAnimation]...");
        this.mDirectionDotIndex = 0;
        this.mApplyDirectionAnim.run();
    }

    public void stopDirectionAnimation() {
    }

    public void startCenterAnimation() {
        Log.i(TAG, "[startCenterAnimation]...");
        this.mCenterDotIndex = 0;
        this.mApplyCenterArrowAnim.run();
    }

    public void stopCenterAnimation() {
        Log.i(TAG, "[stopCenterAnimation]...");
        if (this.mCenterArrow != null) {
            for (int i = 0; i < this.mCenterArrow.getChildCount(); i++) {
                this.mCenterArrow.getChildAt(i).clearAnimation();
            }
        }
    }
}
