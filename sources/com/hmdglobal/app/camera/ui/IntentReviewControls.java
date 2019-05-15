package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import com.hmdglobal.app.camera.DeviceInfo;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class IntentReviewControls extends RelativeLayout implements OnClickListener {
    private final AppController mAppController;
    private ImageButton mCancelButton;
    private Context mContext;
    private ImageButton mDoneButton;
    private boolean mFromVideoUI;
    private int mOrientation = 0;
    private ImageButton mRetakeButton;
    private ImageButton mReviewButton;

    public IntentReviewControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAppController = (AppController) context;
        this.mContext = context;
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mCancelButton = (ImageButton) findViewById(R.id.cancel_button);
        if (this.mCancelButton != null) {
            this.mCancelButton.setOnClickListener(this);
        }
        this.mDoneButton = (ImageButton) findViewById(R.id.done_button);
        if (this.mDoneButton != null) {
            this.mDoneButton.setOnClickListener(this);
        }
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_VDF_REVIEW_UI_CUSTOMIZE, true)) {
            this.mDoneButton.setImageResource(R.drawable.submit_action);
            this.mCancelButton.setImageResource(R.drawable.ic_cancel_vdf);
        }
        this.mRetakeButton = (ImageButton) findViewById(R.id.retake_button);
        if (this.mRetakeButton != null) {
            this.mRetakeButton.setOnClickListener(this);
        }
        this.mReviewButton = (ImageButton) findViewById(R.id.review_button);
        if (this.mReviewButton != null) {
            this.mReviewButton.setOnClickListener(this);
        }
    }

    public void onClick(View v) {
        if (v == this.mDoneButton) {
            this.mAppController.intentReviewDone();
        } else if (v == this.mRetakeButton) {
            this.mAppController.intentReviewRetake();
        } else if (v == this.mReviewButton) {
            this.mAppController.intentReviewPlay();
        } else if (v == this.mCancelButton) {
            this.mAppController.intentReviewCancel();
        }
    }

    public void show(boolean showCancel, boolean showDone, boolean showRetake, boolean showReview) {
        setVisibility(0);
        if (showCancel && this.mCancelButton != null) {
            this.mCancelButton.setVisibility(0);
        }
        if (showDone && this.mDoneButton != null) {
            this.mDoneButton.setVisibility(0);
        }
        if (showRetake && this.mRetakeButton != null) {
            this.mRetakeButton.setVisibility(0);
        }
        if (showReview && this.mReviewButton != null) {
            this.mReviewButton.setVisibility(0);
        }
    }

    public void hide() {
        setVisibility(8);
        if (this.mCancelButton != null) {
            this.mCancelButton.setVisibility(8);
        }
        if (this.mDoneButton != null) {
            this.mDoneButton.setVisibility(8);
        }
        if (this.mRetakeButton != null) {
            this.mRetakeButton.setVisibility(8);
        }
        if (this.mReviewButton != null) {
            this.mReviewButton.setVisibility(8);
        }
    }

    public void setLayoutOrientation(int ori) {
        if (this.mOrientation != ori) {
            this.mOrientation = ori;
            resetLayoutParam();
        }
    }

    private void resetLayoutParam() {
        int navBarHeight = this.mAppController.getCameraAppUI().getNavigationHeight();
        int padding = this.mFromVideoUI ? navBarHeight : ((int) this.mContext.getResources().getDimension(R.dimen.intent_review_cancel_done_button_margin)) + navBarHeight;
        if (padding != 0) {
            int i = this.mOrientation;
            if (i == 0) {
                setPadding(0, 0, 0, 0);
            } else if (i != 90) {
                if (i != MediaProviderUtils.ROTATION_180) {
                    if (i != MediaProviderUtils.ROTATION_270) {
                        setPadding(0, 0, 0, 0);
                    } else if (this.mAppController.isReversibleWorking()) {
                        setPadding(padding, 0, 0, 0);
                    } else {
                        setPadding(0, 0, padding, 0);
                    }
                } else if (DeviceInfo.isReversibleOn(this.mContext.getContentResolver())) {
                    setPadding(0, 0, 0, 0);
                } else {
                    setPadding(0, padding, 0, 0);
                }
            } else if (this.mAppController.isReversibleWorking()) {
                setPadding(0, 0, padding, 0);
            } else {
                setPadding(padding, 0, 0, 0);
            }
            requestLayout();
        }
    }

    public void setFromVideoUI(boolean fromVideoUI) {
        this.mFromVideoUI = fromVideoUI;
    }
}
