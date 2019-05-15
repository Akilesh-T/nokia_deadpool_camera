package com.hmdglobal.app.camera.widget;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import java.io.IOException;

public class MicroVideoGuideLayout extends FrameLayout {
    private static final Tag TAG = new Tag("MircoVideoGuideLayout");
    private final OnAudioFocusChangeListener mAudioFocusChangeListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case -2:
                case -1:
                    if (MicroVideoGuideLayout.this.mMediaPlayer != null && MicroVideoGuideLayout.this.mMediaPlayer.isPlaying()) {
                        MicroVideoGuideLayout.this.mMediaPlayer.pause();
                        return;
                    }
                    return;
                case 1:
                case 2:
                case 3:
                    if (MicroVideoGuideLayout.this.mMediaPlayer != null) {
                        MicroVideoGuideLayout.this.mMediaPlayer.start();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private TextView mButton;
    private CheckBox mCheckBox;
    private boolean mCheckBoxChecked;
    private TextView mDescription;
    private GuideSelectionListener mListener;
    private MediaPlayer mMediaPlayer;
    private Callback mSurfaceCallback = new Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(MicroVideoGuideLayout.TAG, "Microvideo guide surface created");
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.i(MicroVideoGuideLayout.TAG, "Microvideo guide surface changed");
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(MicroVideoGuideLayout.TAG, "Microvideo guide surface destoryed");
            if (MicroVideoGuideLayout.this.mMediaPlayer != null) {
                MicroVideoGuideLayout.this.mMediaPlayer.stop();
                MicroVideoGuideLayout.this.mMediaPlayer.release();
                MicroVideoGuideLayout.this.mMediaPlayer = null;
                MicroVideoGuideLayout.this.releaseAudioFocus();
            }
        }
    };
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private TextView mTitle;

    public interface GuideSelectionListener {
        void onGuideSelected(boolean z);
    }

    public void setGuideSelectionListener(GuideSelectionListener listener) {
        this.mListener = listener;
    }

    public MicroVideoGuideLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mSurfaceView = (SurfaceView) findViewById(R.id.micro_video_guide_surfaceview);
        this.mSurfaceHolder = this.mSurfaceView.getHolder();
        this.mSurfaceHolder.setFixedSize((int) getResources().getDimension(R.dimen.micro_guide_video_width), (int) getResources().getDimension(R.dimen.micro_guide_video_height));
        this.mSurfaceHolder.addCallback(this.mSurfaceCallback);
        this.mTitle = (TextView) findViewById(R.id.micro_video_guide_title);
        this.mDescription = (TextView) findViewById(R.id.micro_video_guide_description);
        this.mDescription.setMovementMethod(new ScrollingMovementMethod());
        this.mCheckBox = (CheckBox) findViewById(R.id.micro_video_guide_checkbox);
        this.mCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MicroVideoGuideLayout.this.mCheckBoxChecked = isChecked;
            }
        });
        this.mButton = (TextView) findViewById(R.id.micro_video_guide_button);
        this.mButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MicroVideoGuideLayout.this.mListener.onGuideSelected(MicroVideoGuideLayout.this.mCheckBoxChecked ^ 1);
                MicroVideoGuideLayout.this.mCheckBoxChecked = false;
            }
        });
    }

    private void playMicroVideoGuide() {
        Log.i(TAG, "play MicroVideo guide");
        this.mMediaPlayer.setDisplay(this.mSurfaceHolder);
        try {
            this.mMediaPlayer.prepare();
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
        this.mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                MicroVideoGuideLayout.this.requestAudioFocus();
                MicroVideoGuideLayout.this.mMediaPlayer.start();
            }
        });
    }

    public void changeVisibility(int visible) {
        if (visible == 0) {
            this.mSurfaceView.setZOrderOnTop(true);
        } else {
            this.mSurfaceView.setZOrderOnTop(false);
        }
        this.mSurfaceView.setVisibility(visible);
        setVisibility(visible);
    }

    public void stopPlaying() {
        Log.i(TAG, "stop playing MicroVideo guide");
        if (this.mMediaPlayer != null && this.mMediaPlayer.isPlaying()) {
            this.mMediaPlayer.pause();
            releaseAudioFocus();
        }
    }

    public void startPlaying() {
        Log.i(TAG, "start playing MicroVideo guide");
        if (this.mMediaPlayer != null && !this.mMediaPlayer.isPlaying()) {
            requestAudioFocus();
            this.mMediaPlayer.seekTo(0);
            this.mMediaPlayer.start();
        }
    }

    private void requestAudioFocus() {
        ((AudioManager) getContext().getSystemService("audio")).requestAudioFocus(this.mAudioFocusChangeListener, 3, 2);
    }

    private void releaseAudioFocus() {
        ((AudioManager) getContext().getSystemService("audio")).abandonAudioFocus(this.mAudioFocusChangeListener);
    }
}
