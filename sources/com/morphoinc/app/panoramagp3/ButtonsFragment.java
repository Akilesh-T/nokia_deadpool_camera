package com.morphoinc.app.panoramagp3;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.ui.PeekImageView;

public class ButtonsFragment extends Fragment {
    public static final String ACTION_END = "end";
    public static final String ACTION_EXTRA = "action_extra";
    public static final String ACTION_START = "start";
    private static final String LOG_TAG = "Camera2App";
    public static final String SLIP_UP_ACTION = "com.hmdcamera.action.slipup.shutter";
    private View mCameraSettingButton;
    private ImageView mGpsIcon;
    private float mMoveY;
    private View mSettingButton;
    private ImageButton mTakePictureButton;
    private PeekImageView mThumbnailButton;
    private int mThumbnailOrientation = 0;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.buttons_fragment, container);
        this.mThumbnailButton = (PeekImageView) view.findViewById(R.id.panoramagp3_thumbnail);
        this.mGpsIcon = (ImageView) view.findViewById(R.id.panoramagp3_gps_icon);
        this.mTakePictureButton = (ImageButton) view.findViewById(R.id.take_picture_button);
        this.mSettingButton = view.findViewById(R.id.setting_button);
        this.mCameraSettingButton = view.findViewById(R.id.show_camera_setting_button);
        return view;
    }

    private void translateShutter(boolean isUp) {
        if (isUp) {
            ObjectAnimator.ofFloat(this.mTakePictureButton, "translationY", new float[]{10.0f, 30.0f, 10.0f}).setDuration(200).start();
        } else {
            ObjectAnimator.ofFloat(this.mTakePictureButton, "translationY", new float[]{10.0f, -40.0f, 10.0f}).setDuration(200).start();
        }
    }

    public void onDestroy() {
        clearThumbnail();
        super.onDestroy();
    }

    public void clearThumbnail() {
        this.mThumbnailButton.setViewThumbBitmap(null);
    }

    public void setThumbnailBitmap(Bitmap bmp, int orientation) {
        this.mThumbnailButton.setViewThumbBitmap(bmp);
        this.mThumbnailOrientation = orientation;
    }

    public void setThumbnailBitmap(Bitmap bmp) {
        setThumbnailBitmap(bmp, 0);
    }

    public ImageView getmThumbnailButton() {
        return this.mThumbnailButton;
    }

    public Size getThumbnailMaxSize() {
        View view = getView();
        if (view == null) {
            return new Size(0, 0);
        }
        ImageButton baseImage = (ImageButton) view.findViewById(R.id.take_picture_button);
        return new Size((baseImage.getWidth() + 1) & -2, (baseImage.getHeight() + 1) & -2);
    }

    public void setGpsIconVisibility(boolean gpsSetting) {
        if (gpsSetting) {
            this.mGpsIcon.setVisibility(0);
        } else {
            this.mGpsIcon.setVisibility(4);
        }
    }

    public void setEnabledCamera2Setting(boolean enabled) {
        View view = getView();
        if (view != null) {
            view.findViewById(R.id.show_camera_setting_button).setEnabled(enabled);
        }
    }

    public void setShutterIcon() {
        this.mTakePictureButton.setImageResource(R.drawable.icon_shutter);
    }

    public void setPauseIcon() {
        this.mTakePictureButton.setImageResource(R.drawable.pano_shutter);
    }

    public void rotateView(int rotate) {
        this.mGpsIcon.setRotation((float) rotate);
        this.mThumbnailButton.setRotation((float) (this.mThumbnailOrientation + rotate));
        this.mTakePictureButton.setRotation((float) rotate);
        this.mSettingButton.setRotation((float) rotate);
        this.mCameraSettingButton.setRotation((float) rotate);
    }
}
