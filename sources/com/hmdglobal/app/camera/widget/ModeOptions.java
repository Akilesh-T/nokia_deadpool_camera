package com.hmdglobal.app.camera.widget;

import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import com.hmdglobal.app.camera.CaptureLayoutHelper;
import com.hmdglobal.app.camera.MultiToggleImageButton;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.ui.CameraSelectionMenu;
import com.hmdglobal.app.camera.ui.RadioOptions;
import com.hmdglobal.app.camera.ui.TopRightWeightedLayout;

public class ModeOptions extends FrameLayout {
    public static final int BAR_INVALID = -1;
    public static final int BAR_PANO = 1;
    public static final int BAR_STANDARD = 0;
    private static final int HIDE_ALPHA_ANIMATION_TIME = 200;
    private static final int PADDING_ANIMATION_TIME = 350;
    private static final int RADIUS_ANIMATION_TIME = 250;
    private static final int SHOW_ALPHA_ANIMATION_TIME = 350;
    private ViewGroup mActiveBar;
    private RectF mAnimateFrom = new RectF();
    private int mBackgroundColor;
    private CameraSelectionMenu mCameraSelectionMenu;
    private boolean mDrawCircle;
    private boolean mFill;
    private AnimatorSet mHiddenAnimator;
    private boolean mIsHiddenOrHiding;
    private boolean mIsPortrait;
    private ViewGroup mMainBar;
    private TopRightWeightedLayout mModeOptionsButtons;
    private RadioOptions mModeOptionsExposure;
    private RadioOptions mModeOptionsPano;
    private final Paint mPaint = new Paint();
    private float mRadius = 0.0f;
    private View mViewToShowHide;
    private AnimatorSet mVisibleAnimator;

    public ModeOptions(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setViewToShowHide(View v) {
        this.mViewToShowHide = v;
    }

    public void onFinishInflate() {
        this.mIsHiddenOrHiding = true;
        this.mBackgroundColor = getResources().getColor(R.color.mode_options_background);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setColor(this.mBackgroundColor);
        this.mModeOptionsButtons = (TopRightWeightedLayout) findViewById(R.id.mode_options_buttons);
        this.mModeOptionsPano = (RadioOptions) findViewById(R.id.mode_options_pano);
        this.mModeOptionsExposure = (RadioOptions) findViewById(R.id.mode_options_exposure);
        this.mCameraSelectionMenu = (CameraSelectionMenu) findViewById(R.id.mode_options_flash_selection);
        TopRightWeightedLayout topRightWeightedLayout = this.mModeOptionsButtons;
        this.mActiveBar = topRightWeightedLayout;
        this.mMainBar = topRightWeightedLayout;
        ((ImageButton) findViewById(R.id.exposure_button)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ModeOptions.this.mActiveBar = ModeOptions.this.mModeOptionsExposure;
                ModeOptions.this.mMainBar.setVisibility(4);
                ModeOptions.this.mActiveBar.setVisibility(0);
            }
        });
    }

    public void setMainBar(int b) {
        switch (b) {
            case 0:
                TopRightWeightedLayout topRightWeightedLayout = this.mModeOptionsButtons;
                this.mActiveBar = topRightWeightedLayout;
                this.mMainBar = topRightWeightedLayout;
                this.mModeOptionsButtons.setVisibility(0);
                this.mCameraSelectionMenu.setVisibility(8);
                break;
            case 1:
                RadioOptions radioOptions = this.mModeOptionsPano;
                this.mActiveBar = radioOptions;
                this.mMainBar = radioOptions;
                break;
        }
        this.mMainBar.setVisibility(0);
    }

    public int getMainBar() {
        if (this.mMainBar == this.mModeOptionsButtons) {
            return 0;
        }
        if (this.mMainBar == this.mModeOptionsPano) {
            return 1;
        }
        return -1;
    }

    public void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != 0) {
            boolean z = this.mIsHiddenOrHiding;
        }
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        String str = CaptureLayoutHelper.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ModeOptions.onLayout:changed = ");
        stringBuilder.append(changed);
        stringBuilder.append(";left = ");
        stringBuilder.append(left);
        stringBuilder.append(";top = ");
        stringBuilder.append(top);
        stringBuilder.append(";right = ");
        stringBuilder.append(right);
        stringBuilder.append(";bottom = ");
        stringBuilder.append(bottom);
        Log.d(str, stringBuilder.toString());
        if (changed) {
            float rLeft;
            float rTop;
            boolean z = true;
            if (getResources().getConfiguration().orientation != 1) {
                z = false;
            }
            this.mIsPortrait = z;
            int buttonSize = getResources().getDimensionPixelSize(R.dimen.option_button_circle_size);
            int buttonPadding = getResources().getDimensionPixelSize(R.dimen.mode_options_toggle_padding);
            if (this.mIsPortrait) {
                rLeft = (float) ((getWidth() - buttonPadding) - buttonSize);
                rTop = ((float) (getHeight() - buttonSize)) / 2.0f;
            } else {
                rLeft = (float) buttonPadding;
                rTop = (float) buttonPadding;
            }
            this.mAnimateFrom.set(rLeft, rTop, ((float) buttonSize) + rLeft, ((float) buttonSize) + rTop);
            setupAnimators();
            setupToggleButtonParams();
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    public void onDraw(Canvas canvas) {
        if (this.mDrawCircle) {
            canvas.drawCircle(this.mAnimateFrom.centerX(), this.mAnimateFrom.centerY(), this.mRadius, this.mPaint);
        } else if (this.mFill) {
            canvas.drawPaint(this.mPaint);
        }
        super.onDraw(canvas);
    }

    private void setupToggleButtonParams() {
        int size = this.mIsPortrait ? getHeight() : getWidth();
        for (int i = 0; i < this.mModeOptionsButtons.getChildCount(); i++) {
            View button = this.mModeOptionsButtons.getChildAt(i);
            if (button instanceof MultiToggleImageButton) {
                MultiToggleImageButton toggleButton = (MultiToggleImageButton) button;
                toggleButton.setParentSize(size);
                toggleButton.setAnimDirection(this.mIsPortrait ? 0 : 1);
            }
        }
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x0059, element type: float, insn element type: null */
    private void setupAnimators() {
        /*
        r18 = this;
        r0 = r18;
        r1 = r0.mVisibleAnimator;
        if (r1 == 0) goto L_0x000b;
    L_0x0006:
        r1 = r0.mVisibleAnimator;
        r1.end();
    L_0x000b:
        r1 = r0.mHiddenAnimator;
        if (r1 == 0) goto L_0x0014;
    L_0x000f:
        r1 = r0.mHiddenAnimator;
        r1.end();
    L_0x0014:
        r1 = r0.mIsPortrait;
        if (r1 == 0) goto L_0x001e;
    L_0x0018:
        r1 = r18.getWidth();
    L_0x001c:
        r1 = (float) r1;
        goto L_0x0023;
    L_0x001e:
        r1 = r18.getHeight();
        goto L_0x001c;
    L_0x0023:
        r2 = 2;
        r3 = new float[r2];
        r4 = r0.mAnimateFrom;
        r4 = r4.width();
        r5 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r4 = r4 / r5;
        r6 = 0;
        r3[r6] = r4;
        r4 = r0.mAnimateFrom;
        r4 = r4.width();
        r4 = r4 / r5;
        r4 = r1 - r4;
        r7 = 1;
        r3[r7] = r4;
        r3 = android.animation.ValueAnimator.ofFloat(r3);
        r8 = 250; // 0xfa float:3.5E-43 double:1.235E-321;
        r3.setDuration(r8);
        r4 = new com.hmdglobal.app.camera.widget.ModeOptions$2;
        r4.<init>();
        r3.addUpdateListener(r4);
        r4 = new com.hmdglobal.app.camera.widget.ModeOptions$3;
        r4.<init>();
        r3.addListener(r4);
        r4 = new float[r2];
        r4 = {0, 1065353216};
        r4 = android.animation.ValueAnimator.ofFloat(r4);
        r10 = 350; // 0x15e float:4.9E-43 double:1.73E-321;
        r4.setDuration(r10);
        r12 = new com.hmdglobal.app.camera.widget.ModeOptions$4;
        r12.<init>();
        r4.addUpdateListener(r12);
        r12 = new com.hmdglobal.app.camera.widget.ModeOptions$5;
        r12.<init>();
        r4.addListener(r12);
        r12 = r18.getResources();
        r13 = 2131099973; // 0x7f060145 float:1.7812314E38 double:1.0529032845E-314;
        r12 = r12.getDimensionPixelSize(r13);
        r13 = r0.mActiveBar;
        r13 = r13.getChildCount();
        r14 = new java.util.ArrayList;
        r14.<init>();
        r15 = r6;
    L_0x008c:
        if (r15 >= r13) goto L_0x00c7;
    L_0x008e:
        r8 = r0.mIsPortrait;
        if (r8 == 0) goto L_0x0099;
    L_0x0092:
        r8 = r0.mActiveBar;
        r8 = r8.getChildAt(r15);
        goto L_0x00a2;
    L_0x0099:
        r8 = r0.mActiveBar;
        r9 = r13 + -1;
        r9 = r9 - r15;
        r8 = r8.getChildAt(r9);
    L_0x00a2:
        r9 = new float[r2];
        r16 = r13 - r15;
        r5 = r12 * r16;
        r5 = (float) r5;
        r9[r6] = r5;
        r5 = 0;
        r9[r7] = r5;
        r5 = android.animation.ValueAnimator.ofFloat(r9);
        r5.setDuration(r10);
        r9 = new com.hmdglobal.app.camera.widget.ModeOptions$6;
        r9.<init>(r8);
        r5.addUpdateListener(r9);
        r14.add(r5);
        r15 = r15 + 1;
        r5 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r8 = 250; // 0xfa float:3.5E-43 double:1.235E-321;
        goto L_0x008c;
    L_0x00c7:
        r5 = new android.animation.AnimatorSet;
        r5.<init>();
        r5.playTogether(r14);
        r8 = new android.animation.AnimatorSet;
        r8.<init>();
        r0.mVisibleAnimator = r8;
        r8 = r0.mVisibleAnimator;
        r9 = com.hmdglobal.app.camera.util.Gusterpolator.INSTANCE;
        r8.setInterpolator(r9);
        r8 = r0.mVisibleAnimator;
        r9 = 3;
        r9 = new android.animation.Animator[r9];
        r9[r6] = r3;
        r9[r7] = r4;
        r9[r2] = r5;
        r8.playTogether(r9);
        r3 = new float[r2];
        r4 = r0.mAnimateFrom;
        r4 = r4.width();
        r5 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r4 = r4 / r5;
        r4 = r1 - r4;
        r3[r6] = r4;
        r4 = r0.mAnimateFrom;
        r4 = r4.width();
        r4 = r4 / r5;
        r3[r7] = r4;
        r3 = android.animation.ValueAnimator.ofFloat(r3);
        r4 = 250; // 0xfa float:3.5E-43 double:1.235E-321;
        r3.setDuration(r4);
        r4 = new com.hmdglobal.app.camera.widget.ModeOptions$7;
        r4.<init>();
        r3.addUpdateListener(r4);
        r4 = new com.hmdglobal.app.camera.widget.ModeOptions$8;
        r4.<init>();
        r3.addListener(r4);
        r4 = new float[r2];
        r4 = {1065353216, 0};
        r4 = android.animation.ValueAnimator.ofFloat(r4);
        r8 = 200; // 0xc8 float:2.8E-43 double:9.9E-322;
        r4.setDuration(r8);
        r5 = new com.hmdglobal.app.camera.widget.ModeOptions$9;
        r5.<init>();
        r4.addUpdateListener(r5);
        r5 = new com.hmdglobal.app.camera.widget.ModeOptions$10;
        r5.<init>();
        r4.addListener(r5);
        r5 = new android.animation.AnimatorSet;
        r5.<init>();
        r0.mHiddenAnimator = r5;
        r5 = r0.mHiddenAnimator;
        r8 = com.hmdglobal.app.camera.util.Gusterpolator.INSTANCE;
        r5.setInterpolator(r8);
        r5 = r0.mHiddenAnimator;
        r2 = new android.animation.Animator[r2];
        r2[r6] = r3;
        r2[r7] = r4;
        r5.playTogether(r2);
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.widget.ModeOptions.setupAnimators():void");
    }

    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    public void animateVisible() {
        if (this.mIsHiddenOrHiding) {
            if (this.mViewToShowHide != null) {
                this.mViewToShowHide.setVisibility(4);
            }
            this.mHiddenAnimator.cancel();
            this.mVisibleAnimator.end();
            setVisibility(0);
            this.mVisibleAnimator.start();
        }
        this.mIsHiddenOrHiding = false;
    }

    public void animateHidden() {
    }
}
