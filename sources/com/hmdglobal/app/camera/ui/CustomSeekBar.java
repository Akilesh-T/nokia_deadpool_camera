package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.hmdglobal.app.camera.R;

public class CustomSeekBar extends RelativeLayout {
    private EnableStateChangedCallback mCallback;
    private boolean mEnableOnTouch = false;
    private LayoutInflater mInflater;
    private SeekBar mSeekBar;

    public interface EnableStateChangedCallback {
        void onEnableStateChanged(boolean z);
    }

    public CustomSeekBar(Context context) {
        super(context);
    }

    public CustomSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mInflater = LayoutInflater.from(context);
        this.mSeekBar = (SeekBar) this.mInflater.inflate(R.layout.custom_seekbar, this).findViewById(R.id.customseekbar);
    }

    public void setOnSeekbarChangeListener(OnSeekBarChangeListener l) {
        if (this.mSeekBar != null) {
            this.mSeekBar.setOnSeekBarChangeListener(l);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == 0) {
            requestDisallowInterceptTouchEvent(true);
        }
        return false;
    }

    public void setEnableOnTouch(boolean enable, EnableStateChangedCallback callback) {
        this.mEnableOnTouch = enable;
        this.mCallback = callback;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!isShown()) {
            return super.onTouchEvent(event);
        }
        this.mSeekBar.onTouchEvent(event);
        if (!this.mSeekBar.isEnabled() && this.mEnableOnTouch) {
            this.mSeekBar.setEnabled(true);
            if (this.mCallback != null) {
                this.mCallback.onEnableStateChanged(true);
            }
        }
        return true;
    }

    public void setMaxProgress(int progress) {
        this.mSeekBar.setMax(progress);
    }

    public void setProgress(int progress) {
        if (this.mSeekBar != null) {
            this.mSeekBar.setProgress(progress);
        }
    }

    public int getProgress() {
        return this.mSeekBar.getProgress();
    }
}
