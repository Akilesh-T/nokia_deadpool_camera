package com.hmdglobal.app.camera.ui;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import com.hmdglobal.app.camera.FocusOverlayManager.FaceDetector;
import com.hmdglobal.app.camera.FocusOverlayManager.FocusUI;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.DebugPropertyHelper;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class FocusOverlay extends View implements FocusUI {
    private static final boolean CAPTURE_DEBUG_UI = DebugPropertyHelper.showCaptureDebugUI();
    private static final int FOCUS_ALPHA_GRADIENT_END = 120;
    private static final int FOCUS_ALPHA_GRADIENT_START = 255;
    private static final int FOCUS_DURATION_MS = 200;
    private static final int FOCUS_HIDE_DELAY = 500;
    private static final int FOCUS_STAY_GRADIENT_DELAY = 1000;
    private static final int MSG_HIDE_FOCUS = 285217024;
    private static final Tag TAG = new Tag("FocusOverlay");
    private int mAlpha;
    private final Rect mBounds = new Rect();
    private Paint mDebugCornersPaint;
    private int mDebugFailColor;
    private String mDebugMessage;
    private Paint mDebugSolidPaint;
    private int mDebugStartColor;
    private int mDebugSuccessColor;
    private Paint mDebugTextPaint;
    private FaceDetector mFaceDetector;
    private final ValueAnimator mFocusAnimation = new ValueAnimator();
    private Drawable mFocusBound = this.mFocusSuccessBound;
    private int mFocusBoundRadius;
    private Rect mFocusDebugCornersRect;
    private Rect mFocusDebugSolidRect;
    private final Drawable mFocusFailureBound = getResources().getDrawable(R.drawable.ic_focus_fail);
    private final int mFocusIndicatorSize = getResources().getDimensionPixelSize(R.dimen.focus_inner_ring_size);
    private final int mFocusOuterRingSize = getResources().getDimensionPixelSize(R.dimen.focus_outer_ring_size);
    private final ValueAnimator mFocusRingGradientAnimation;
    private final Drawable mFocusSuccessBound = getResources().getDrawable(R.drawable.ic_focus);
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == FocusOverlay.MSG_HIDE_FOCUS) {
                FocusOverlay.this.setVisibility(4);
            }
        }
    };
    private boolean mIsPassiveScan;
    private int mPositionX;
    private int mPositionY;
    private Rect mPreviewRect;
    private boolean mShowIndicator;

    public FocusOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFocusAnimation.setDuration(200);
        this.mFocusAnimation.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                FocusOverlay.this.mFocusBoundRadius = ((Integer) animation.getAnimatedValue()).intValue();
                FocusOverlay.this.mAlpha = ((FocusOverlay.this.mFocusBoundRadius - FocusOverlay.this.mFocusOuterRingSize) * 255) / (FocusOverlay.this.mFocusIndicatorSize - FocusOverlay.this.mFocusOuterRingSize);
                FocusOverlay.this.invalidate();
            }
        });
        this.mFocusRingGradientAnimation = ValueAnimator.ofInt(new int[]{255, 120});
        this.mFocusRingGradientAnimation.setDuration(200);
        this.mFocusRingGradientAnimation.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                FocusOverlay.this.mAlpha = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                FocusOverlay.this.invalidate();
            }
        });
        if (CAPTURE_DEBUG_UI) {
            Resources res = getResources();
            this.mDebugStartColor = res.getColor(R.color.focus_debug);
            this.mDebugSuccessColor = res.getColor(R.color.focus_debug_success);
            this.mDebugFailColor = res.getColor(R.color.focus_debug_fail);
            this.mDebugTextPaint = new Paint();
            this.mDebugTextPaint.setColor(res.getColor(R.color.focus_debug_text));
            this.mDebugTextPaint.setStyle(Style.FILL);
            this.mDebugSolidPaint = new Paint();
            this.mDebugSolidPaint.setColor(res.getColor(R.color.focus_debug));
            this.mDebugSolidPaint.setAntiAlias(true);
            this.mDebugSolidPaint.setStyle(Style.STROKE);
            this.mDebugSolidPaint.setStrokeWidth(res.getDimension(R.dimen.focus_debug_stroke));
            this.mDebugCornersPaint = new Paint(this.mDebugSolidPaint);
            this.mDebugCornersPaint.setColor(res.getColor(R.color.focus_debug));
            this.mFocusDebugSolidRect = new Rect();
            this.mFocusDebugCornersRect = new Rect();
        }
    }

    public void setFaceDetector(FaceDetector faceDetector) {
        this.mFaceDetector = faceDetector;
    }

    public boolean hasFaces() {
        return this.mFaceDetector != null && this.mFaceDetector.hasFaces();
    }

    public void clearFocus() {
        this.mHandler.removeMessages(MSG_HIDE_FOCUS);
        this.mShowIndicator = false;
        this.mFocusRingGradientAnimation.cancel();
        if (CAPTURE_DEBUG_UI) {
            setVisibility(4);
        }
        invalidate();
    }

    public void keepFocusFrame() {
        this.mHandler.removeMessages(MSG_HIDE_FOCUS);
        this.mFocusRingGradientAnimation.cancel();
        if (this.mAlpha != 255) {
            this.mAlpha = 255;
            invalidate();
        }
    }

    public void setFocusPosition(int x, int y, boolean isPassiveScan) {
        setFocusPosition(x, y, isPassiveScan, 0, 0);
    }

    public void setFocusPosition(int x, int y, boolean isPassiveScan, int aFsize, int aEsize) {
        this.mIsPassiveScan = isPassiveScan;
        this.mPositionX = x;
        this.mPositionY = y;
        this.mFocusBoundRadius = this.mFocusOuterRingSize / 2;
        this.mBounds.set(x - this.mFocusBoundRadius, y - this.mFocusBoundRadius, this.mFocusBoundRadius + x, this.mFocusBoundRadius + y);
        this.mFocusBound.setBounds(this.mBounds);
        if (CAPTURE_DEBUG_UI) {
            if (isPassiveScan) {
                this.mFocusDebugSolidRect.setEmpty();
                int avg = (aFsize + aEsize) / 2;
                this.mFocusDebugCornersRect.set(x - (avg / 2), y - (avg / 2), (avg / 2) + x, (avg / 2) + y);
            } else {
                this.mFocusDebugSolidRect.set(x - (aFsize / 2), y - (aFsize / 2), (aFsize / 2) + x, (aFsize / 2) + y);
                if (aFsize != aEsize) {
                    this.mFocusDebugCornersRect.set(x - (aEsize / 2), y - (aEsize / 2), (aEsize / 2) + x, (aEsize / 2) + y);
                } else {
                    this.mFocusDebugCornersRect.setEmpty();
                }
            }
            this.mDebugSolidPaint.setColor(this.mDebugStartColor);
            this.mDebugCornersPaint.setColor(this.mDebugStartColor);
        }
        if (getVisibility() != 0) {
            setVisibility(0);
        }
        invalidate();
    }

    public void onFocusStarted() {
        this.mHandler.removeMessages(MSG_HIDE_FOCUS);
        this.mFocusBound = this.mFocusSuccessBound;
        this.mShowIndicator = true;
        this.mFocusAnimation.setIntValues(new int[]{this.mFocusOuterRingSize, this.mFocusIndicatorSize});
        this.mFocusRingGradientAnimation.cancel();
        this.mFocusAnimation.start();
        if (CAPTURE_DEBUG_UI) {
            this.mDebugMessage = null;
        }
    }

    public void onFocusSucceeded() {
        this.mHandler.removeMessages(MSG_HIDE_FOCUS);
        this.mFocusAnimation.cancel();
        this.mFocusRingGradientAnimation.cancel();
        this.mFocusBound = this.mFocusSuccessBound;
        this.mShowIndicator = false;
        this.mHandler.sendEmptyMessageDelayed(MSG_HIDE_FOCUS, 500);
        if (CAPTURE_DEBUG_UI && !this.mIsPassiveScan) {
            this.mDebugSolidPaint.setColor(this.mDebugSuccessColor);
        }
        invalidate();
    }

    public void onFocusSucceededAndStay() {
        this.mHandler.removeMessages(MSG_HIDE_FOCUS);
        this.mFocusAnimation.cancel();
        this.mShowIndicator = true;
        this.mFocusBound = this.mFocusSuccessBound;
        this.mFocusRingGradientAnimation.setStartDelay(1000);
        this.mFocusRingGradientAnimation.start();
        invalidate();
    }

    public void onFocusFailed() {
        this.mHandler.removeMessages(MSG_HIDE_FOCUS);
        this.mFocusAnimation.cancel();
        this.mFocusRingGradientAnimation.cancel();
        this.mFocusBound = this.mFocusFailureBound;
        if (CAPTURE_DEBUG_UI && !this.mIsPassiveScan) {
            this.mDebugSolidPaint.setColor(this.mDebugFailColor);
        }
        this.mHandler.sendEmptyMessageDelayed(MSG_HIDE_FOCUS, 500);
        invalidate();
    }

    public void onFocusFailedAndStay() {
        this.mHandler.removeMessages(MSG_HIDE_FOCUS);
        this.mFocusAnimation.cancel();
        this.mShowIndicator = true;
        this.mFocusRingGradientAnimation.setStartDelay(1000);
        this.mFocusRingGradientAnimation.start();
        if (CAPTURE_DEBUG_UI && !this.mIsPassiveScan) {
            this.mDebugSolidPaint.setColor(this.mDebugFailColor);
        }
        invalidate();
    }

    public void setPassiveFocusSuccess(boolean success) {
        this.mFocusAnimation.cancel();
        this.mFocusRingGradientAnimation.cancel();
        this.mShowIndicator = false;
        if (CAPTURE_DEBUG_UI) {
            this.mDebugCornersPaint.setColor(success ? this.mDebugSuccessColor : this.mDebugFailColor);
        }
        invalidate();
    }

    public void showDebugMessage(String message) {
        if (CAPTURE_DEBUG_UI) {
            this.mDebugMessage = message;
        }
    }

    public void pauseFaceDetection() {
        if (this.mFaceDetector != null) {
            this.mFaceDetector.pauseFaceDetection();
        }
    }

    public void resumeFaceDetection() {
        if (this.mFaceDetector != null) {
            this.mFaceDetector.resumeFaceDetection();
        }
    }

    public void onDraw(Canvas canvas) {
        Canvas canvas2 = canvas;
        super.onDraw(canvas);
        if (this.mShowIndicator) {
            this.mBounds.set(this.mPositionX - this.mFocusBoundRadius, this.mPositionY - this.mFocusBoundRadius, this.mPositionX + this.mFocusBoundRadius, this.mPositionY + this.mFocusBoundRadius);
            this.mFocusBound.setBounds(this.mBounds);
            this.mFocusBound.setAlpha(this.mAlpha);
            if (this.mPositionY - this.mFocusBoundRadius < this.mPreviewRect.top) {
                canvas2.clipRect(this.mPositionX - this.mFocusBoundRadius, this.mPreviewRect.top, this.mPositionX + this.mFocusBoundRadius, this.mPositionY + this.mFocusBoundRadius);
            }
            if (this.mPositionY + this.mFocusBoundRadius > this.mPreviewRect.bottom) {
                canvas2.clipRect(this.mPositionX - this.mFocusBoundRadius, this.mPositionY - this.mFocusBoundRadius, this.mPositionX + this.mFocusBoundRadius, this.mPreviewRect.bottom);
            }
            this.mFocusBound.draw(canvas2);
        }
        if (CAPTURE_DEBUG_UI) {
            canvas2.drawRect(this.mFocusDebugSolidRect, this.mDebugSolidPaint);
            float delta = 0.1f * ((float) this.mFocusDebugCornersRect.width());
            float top = (float) this.mFocusDebugCornersRect.top;
            float right = (float) this.mFocusDebugCornersRect.right;
            float bot = (float) this.mFocusDebugCornersRect.bottom;
            canvas2.drawLines(new float[]{left, top + delta, left, top, left, top, ((float) this.mFocusDebugCornersRect.left) + delta, top}, this.mDebugCornersPaint);
            canvas2.drawLines(new float[]{right, top + delta, right, top, right, top, right - delta, top}, this.mDebugCornersPaint);
            canvas2.drawLines(new float[]{left, bot - delta, left, bot, left, bot, left + delta, bot}, this.mDebugCornersPaint);
            canvas2.drawLines(new float[]{right, bot - delta, right, bot, right, bot, right - delta, bot}, this.mDebugCornersPaint);
            if (this.mDebugMessage != null) {
                this.mDebugTextPaint.setTextSize(40.0f);
                canvas2.drawText(this.mDebugMessage, left - 4.0f, 44.0f + bot, this.mDebugTextPaint);
            }
        }
    }

    public void setPreviewRect(Rect previewRect) {
        this.mPreviewRect = previewRect;
    }
}
