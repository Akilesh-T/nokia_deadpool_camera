package com.hmdglobal.app.camera;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hmdglobal.app.camera.ui.PeekImageView;

public class TS_PanoramaGPUI extends PhotoUI {
    protected FrameLayout mPanoramaPreview_Frame;
    protected ImageView mPanoramaPreview_ImageView;
    protected ImageView mPanoramaPreview__selectdirection_ui_preview_left;
    protected ImageView mPanoramaPreview__selectdirection_ui_preview_right;
    protected View mPanoramaPreview_imageLeft;
    protected View mPanoramaPreview_imageRight;
    protected View mPanoramaPreview_line;
    protected LinearLayout mPanoramaPreview_selectdirection_ui_left;
    protected LinearLayout mPanoramaPreview_selectdirection_ui_right;
    protected TextView mTipToastText;
    protected PeekImageView peek_thumb;

    public TS_PanoramaGPUI(CameraActivity activity, PhotoController controller, View parent) {
        super(activity, controller, parent);
        this.mPanoramaPreview_Frame = (FrameLayout) parent.findViewById(R.id.PanoramaPreview_Frame);
        this.mPanoramaPreview_ImageView = (ImageView) parent.findViewById(R.id.PanoramaPreview_ImageView);
        this.mPanoramaPreview_line = parent.findViewById(R.id.PanoramaPreview_line);
        this.mPanoramaPreview_selectdirection_ui_right = (LinearLayout) parent.findViewById(R.id.PanoramaPreview_selectdirection_ui_right);
        this.mPanoramaPreview_selectdirection_ui_left = (LinearLayout) parent.findViewById(R.id.PanoramaPreview_selectdirection_ui_left);
        this.mPanoramaPreview__selectdirection_ui_preview_right = (ImageView) parent.findViewById(R.id.PanoramaPreview__selectdirection_ui_preview_right);
        this.mPanoramaPreview__selectdirection_ui_preview_left = (ImageView) parent.findViewById(R.id.PanoramaPreview__selectdirection_ui_preview_left);
        this.peek_thumb = (PeekImageView) parent.findViewById(R.id.peek_thumb);
        this.mTipToastText = (TextView) parent.findViewById(R.id.tip_into_panorama);
    }

    public void setTipTextViewVisiblity(boolean visiblity) {
        this.mTipToastText.setVisibility(visiblity ? 0 : 8);
    }

    public void setTipToastText(CameraActivity activity, int tipResId) {
        this.mTipToastText.setText(activity.getText(tipResId));
    }

    public void showProcessingUI() {
        this.mPanoramaPreview_Frame.setVisibility(0);
        this.mPanoramaPreview_selectdirection_ui_right.setVisibility(4);
        this.mPanoramaPreview_selectdirection_ui_left.setVisibility(4);
        this.mPanoramaPreview_line.setVisibility(0);
        this.mPanoramaPreview_ImageView.setVisibility(0);
    }

    public void showSelectDirectionUI(boolean bshow_left) {
        this.mPanoramaPreview_Frame.setVisibility(0);
        this.mPanoramaPreview_line.setVisibility(4);
        this.mPanoramaPreview_ImageView.setVisibility(4);
        if (bshow_left) {
            this.mPanoramaPreview_selectdirection_ui_right.setVisibility(4);
            this.mPanoramaPreview_selectdirection_ui_left.setVisibility(0);
            return;
        }
        this.mPanoramaPreview_selectdirection_ui_right.setVisibility(0);
        this.mPanoramaPreview_selectdirection_ui_left.setVisibility(4);
    }
}
