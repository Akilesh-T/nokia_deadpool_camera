package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class CountDownView extends RotateLayout {
    private static final long ANIMATION_DURATION_MS = 800;
    private static final int SET_TIMER_TEXT = 1;
    private static final Tag TAG = new Tag("CountDownView");
    private Animation mCountDownAnim;
    private final Handler mHandler = new MainHandler();
    private OnCountDownStatusListener mListener;
    private final RectF mPreviewArea = new RectF();
    private TextView mRemainingSecondsView;
    private int mRemainingSecs = 0;

    private class MainHandler extends Handler {
        public MainHandler() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message message) {
            if (message.what == 1) {
                CountDownView.this.remainingSecondsChanged(CountDownView.this.mRemainingSecs - 1);
            }
        }
    }

    public interface OnCountDownStatusListener {
        void onCountDownFinished();

        void onRemainingSecondsChanged(int i);
    }

    public CountDownView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isCountingDown() {
        return this.mRemainingSecs > 0;
    }

    public void onPreviewAreaChanged(RectF previewArea) {
        this.mPreviewArea.set(previewArea);
    }

    private void remainingSecondsChanged(int newVal) {
        this.mRemainingSecs = newVal;
        if (this.mListener != null) {
            this.mListener.onRemainingSecondsChanged(this.mRemainingSecs);
        }
        if (newVal == 0) {
            setVisibility(4);
            if (this.mListener != null) {
                this.mListener.onCountDownFinished();
                return;
            }
            return;
        }
        this.mRemainingSecondsView.setText(String.format(getResources().getConfiguration().locale, "%d", new Object[]{Integer.valueOf(newVal)}));
        startFadeOutAnimation();
        this.mHandler.sendEmptyMessageDelayed(1, 1000);
    }

    private void startFadeOutAnimation() {
        if (this.mCountDownAnim == null) {
            this.mCountDownAnim = AnimationUtils.loadAnimation(getContext(), R.anim.count_down_exit);
        }
        this.mCountDownAnim.reset();
        int textWidth = getMeasuredWidth();
        int textHeight = getMeasuredHeight();
        setTranslationX(this.mPreviewArea.centerX() - ((float) (textWidth / 2)));
        setTranslationY((this.mPreviewArea.centerY() - ((float) (textHeight / 2))) / 2.0f);
        clearAnimation();
        startAnimation(this.mCountDownAnim);
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mRemainingSecondsView = (TextView) findViewById(R.id.remaining_seconds);
    }

    public void setCountDownStatusListener(OnCountDownStatusListener listener) {
        this.mListener = listener;
    }

    public void startCountDown(int sec) {
        if (sec <= 0) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid input for countdown timer: ");
            stringBuilder.append(sec);
            stringBuilder.append(" seconds");
            Log.w(tag, stringBuilder.toString());
            return;
        }
        if (isCountingDown()) {
            cancelCountDown();
        }
        setVisibility(0);
        remainingSecondsChanged(sec);
    }

    public void cancelCountDown() {
        if (this.mRemainingSecs > 0) {
            this.mRemainingSecs = 0;
            this.mHandler.removeMessages(1);
            setVisibility(4);
        }
    }
}
