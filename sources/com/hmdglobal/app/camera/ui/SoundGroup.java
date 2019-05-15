package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.Rotatable.RotateEntity;
import com.hmdglobal.app.camera.widget.SoundAction;

public class SoundGroup extends FrameLayout implements OnClickListener {
    private static final Tag TAG = new Tag("SoundGroup");
    private CameraActivity mContext;
    private View mCurrentSoundView;
    private RotateImageView mKidCat;
    private RotateImageView mKidHarp;
    private RotateImageView mKidLaser;
    private RotateImageView mKidSheep;
    private RotateImageView mKidTrain;
    private SoundAction mSoundLayout;
    private MediaPlayer player = null;

    public SoundGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = (CameraActivity) context;
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        this.mSoundLayout = (SoundAction) findViewById(R.id.sound_action);
        this.mKidCat = (RotateImageView) findViewById(R.id.kid_cat);
        this.mKidCat.setOnClickListener(this);
        this.mKidHarp = (RotateImageView) findViewById(R.id.kid_harp);
        this.mKidHarp.setOnClickListener(this);
        this.mKidLaser = (RotateImageView) findViewById(R.id.kid_laser);
        this.mKidLaser.setOnClickListener(this);
        this.mKidSheep = (RotateImageView) findViewById(R.id.kid_sheep);
        this.mKidSheep.setOnClickListener(this);
        this.mKidTrain = (RotateImageView) findViewById(R.id.kid_train);
        this.mKidTrain.setOnClickListener(this);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.kid_cat /*2131231010*/:
                playKidSound(v, R.raw.cat);
                return;
            case R.id.kid_harp /*2131231011*/:
                playKidSound(v, R.raw.harp);
                return;
            case R.id.kid_laser /*2131231012*/:
                playKidSound(v, R.raw.laser);
                return;
            case R.id.kid_sheep /*2131231013*/:
                playKidSound(v, R.raw.sheep);
                return;
            case R.id.kid_train /*2131231014*/:
                playKidSound(v, R.raw.train);
                return;
            default:
                return;
        }
    }

    private void playKidSound(View v, int resId) {
        if (this.mCurrentSoundView != null && this.mCurrentSoundView.isSelected()) {
            Log.w(TAG, "stop play current sound");
            toggleViewPlaying(resId, true);
            if (this.mCurrentSoundView.equals(v)) {
                this.mCurrentSoundView = null;
                return;
            }
        }
        Log.w(TAG, "click new sound");
        this.mCurrentSoundView = v;
        toggleViewPlaying(resId, false);
    }

    private void toggleViewPlaying(int resId, boolean isStop) {
        if (this.mCurrentSoundView != null) {
            if (isStop) {
                Log.w(TAG, "tap again, stop playing");
                this.mCurrentSoundView.setSelected(false);
                if (this.player != null && this.player.isPlaying()) {
                    try {
                        this.player.stop();
                        this.player.release();
                    } catch (Exception e) {
                        Log.e(TAG, "stop or release player failed", e);
                    } catch (Throwable th) {
                        this.player = null;
                    }
                    this.player = null;
                }
            } else {
                Log.w(TAG, "start playing");
                this.mCurrentSoundView.setSelected(true);
                this.player = MediaPlayer.create(this.mContext, resId);
                this.player.setLooping(true);
                this.player.start();
            }
        }
    }

    public void hideKidSound() {
        this.mSoundLayout.collapse();
    }

    public void finishKidSound() {
        hideKidSound();
        toggleViewPlaying(0, true);
    }

    public void addRotatableToListenerPool() {
        int childCount = this.mSoundLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            this.mContext.addRotatableToListenerPool(new RotateEntity((RotateImageView) this.mSoundLayout.getChildAt(i), true));
        }
    }

    public void removeRotatableToListenerPool() {
        int childCount = this.mSoundLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            this.mContext.removeRotatableFromListenerPool(((RotateImageView) this.mSoundLayout.getChildAt(i)).hashCode());
        }
    }

    public boolean isSoundPlaying() {
        return this.player != null && this.player.isPlaying();
    }
}
