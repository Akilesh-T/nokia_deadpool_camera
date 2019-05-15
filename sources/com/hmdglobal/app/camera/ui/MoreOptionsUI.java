package com.hmdglobal.app.camera.ui;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.CameraUtil;

public class MoreOptionsUI {
    private static final int MIN_CLICK_DELAY_TIME = 800;
    private static final Tag TAG = new Tag("MoreOptionsUI");
    private static long lastClickTime = 0;
    private Button btn;
    private int[] btns = new int[]{R.id.btn_square, R.id.btn_time_lapse, R.id.btn_panorama};
    private ImageButton imgbtn;
    private int[] imgbtns = new int[]{R.id.imgbtn_square, R.id.imgbtn_timelapse, R.id.imgbtn_panorama};
    protected final CameraActivity mActivity;
    private Button mLensButton;
    private ImageButton mLensImageButton;
    private final View mRootView;

    private class ClickListener implements OnClickListener {
        private ClickListener() {
        }

        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_google_lens /*2131230839*/:
                case R.id.imgbtn_lens /*2131230982*/:
                    if (MoreOptionsUI.isFastClick()) {
                        CameraUtil.launchLensApk(MoreOptionsUI.this.mActivity);
                    }
                    MoreOptionsUI.this.hideMoreOptionsWrapper();
                    return;
                case R.id.btn_panorama /*2131230840*/:
                case R.id.imgbtn_panorama /*2131230983*/:
                    if (MoreOptionsUI.isFastClick()) {
                        MoreOptionsUI.this.mActivity.getCameraAppUI().onModeIdChanging();
                        MoreOptionsUI.this.mActivity.getCameraAppUI().onModeIdChanged(MoreOptionsUI.this.mActivity.getResources().getInteger(R.integer.camera_mode_pano));
                    }
                    MoreOptionsUI.this.hideMoreOptionsWrapper();
                    return;
                case R.id.btn_square /*2131230843*/:
                case R.id.imgbtn_square /*2131230984*/:
                    MoreOptionsUI.this.hideMoreOptionsWrapper();
                    if (MoreOptionsUI.isFastClick()) {
                        MoreOptionsUI.this.mActivity.getCameraAppUI().onModeIdChanging();
                        MoreOptionsUI.this.mActivity.getCameraAppUI().onModeIdChanged(MoreOptionsUI.this.mActivity.getResources().getInteger(R.integer.camera_mode_square));
                        return;
                    }
                    return;
                case R.id.btn_time_lapse /*2131230844*/:
                case R.id.imgbtn_timelapse /*2131230985*/:
                    MoreOptionsUI.this.hideMoreOptionsWrapper();
                    if (MoreOptionsUI.isFastClick()) {
                        MoreOptionsUI.this.mActivity.getCameraAppUI().onModeIdChanging();
                        MoreOptionsUI.this.mActivity.getCameraAppUI().onModeIdChanged(MoreOptionsUI.this.mActivity.getResources().getInteger(R.integer.camera_mode_time_lapse));
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public MoreOptionsUI(CameraActivity activity, View parent) {
        this.mActivity = activity;
        this.mRootView = parent;
        this.mLensImageButton = (ImageButton) this.mRootView.findViewById(R.id.imgbtn_lens);
        this.mLensButton = (Button) this.mRootView.findViewById(R.id.btn_google_lens);
        if (CameraUtil.checkLensAvailability(this.mActivity)) {
            this.mLensImageButton.setOnClickListener(new ClickListener());
            this.mLensButton.setOnClickListener(new ClickListener());
        } else {
            this.mLensImageButton.setImageResource(R.drawable.lens_disable);
        }
        int i = 0;
        for (int findViewById : this.btns) {
            this.btn = (Button) this.mRootView.findViewById(findViewById);
            this.btn.setOnClickListener(new ClickListener());
        }
        while (i < this.imgbtns.length) {
            this.imgbtn = (ImageButton) this.mRootView.findViewById(this.imgbtns[i]);
            this.imgbtn.setOnClickListener(new ClickListener());
            i++;
        }
    }

    private void hideMoreOptionsWrapper() {
        this.mActivity.getButtonManager().hideMoreOptionsWrapper();
    }

    public static boolean isFastClick() {
        boolean flag = false;
        long curClickTime = System.currentTimeMillis();
        if (curClickTime - lastClickTime >= 800) {
            flag = true;
        }
        lastClickTime = curClickTime;
        return flag;
    }
}
