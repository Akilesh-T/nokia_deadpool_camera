package com.hmdglobal.app.camera.ui;

import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.hmdglobal.app.camera.R;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class MT_PanoProgressIndicator {
    private static final String TAG = "ProgressIndicator";
    private static int sIndicatorMarginLong = 0;
    private static int sIndicatorMarginShort = 0;
    public int mBlockNumber;
    private int mBlockPadding;
    public ImageView mProgressBars;
    public View mProgressView;

    public MT_PanoProgressIndicator(Activity activity, int blockNumber, int[] drawBlockSizes) {
        int i = 0;
        this.mBlockPadding = 0;
        this.mBlockNumber = 9;
        this.mBlockPadding = 4;
        this.mProgressView = activity.findViewById(R.id.progress_indicator);
        this.mProgressView.setVisibility(0);
        this.mProgressBars = (ImageView) activity.findViewById(R.id.progress_bars);
        this.mBlockNumber = blockNumber;
        int[] blockSizes = new int[blockNumber];
        System.arraycopy(drawBlockSizes, 0, blockSizes, 0, blockNumber);
        float scale = activity.getResources().getDisplayMetrics().density;
        if (scale != 1.0f) {
            this.mBlockPadding = (int) ((((float) this.mBlockPadding) * scale) + 0.5f);
            while (i < this.mBlockNumber) {
                blockSizes[i] = (int) ((((float) drawBlockSizes[i]) * scale) + 0.5f);
                i++;
            }
        }
        this.mProgressBars.setImageDrawable(new MT_PanoProgressBarDrawable(activity, this.mProgressBars, blockSizes, this.mBlockPadding));
        getIndicatorMargin();
    }

    public MT_PanoProgressIndicator(Activity activity) {
        this.mBlockPadding = 0;
        this.mBlockNumber = 9;
        this.mProgressView = activity.findViewById(R.id.progress_indicator);
        if (this.mProgressView == null) {
            Log.w(TAG, "mProgressView is null,return!");
            return;
        }
        this.mProgressView.setVisibility(0);
        this.mProgressBars = (ImageView) activity.findViewById(R.id.progress_bars);
    }

    public void setVisibility(int visibility) {
        this.mProgressView.setVisibility(visibility);
    }

    public void setProgress(int progress) {
        this.mProgressBars.setImageLevel(progress);
    }

    private void getIndicatorMargin() {
        if (sIndicatorMarginLong == 0 && sIndicatorMarginShort == 0) {
            Resources res = this.mProgressView.getResources();
            sIndicatorMarginLong = res.getDimensionPixelSize(R.dimen.pano_progress_indicator_bottom_long);
            sIndicatorMarginShort = res.getDimensionPixelSize(R.dimen.pano_progress_indicator_bottom_short);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[getIndicatorMargin]sIndicatorMarginLong = ");
        stringBuilder.append(sIndicatorMarginLong);
        stringBuilder.append(" sIndicatorMarginShort = ");
        stringBuilder.append(sIndicatorMarginShort);
        Log.d(str, stringBuilder.toString());
    }

    public void setOrientation(int orientation) {
        LinearLayout progressViewLayout = this.mProgressView;
        LayoutParams rp = new LayoutParams(progressViewLayout.getLayoutParams());
        int activityOrientation = this.mProgressView.getResources().getConfiguration().orientation;
        if ((2 == activityOrientation && (orientation == 0 || orientation == MediaProviderUtils.ROTATION_180)) || (1 == activityOrientation && (orientation == 90 || orientation == MediaProviderUtils.ROTATION_270))) {
            rp.setMargins(rp.leftMargin, rp.topMargin, rp.rightMargin, sIndicatorMarginShort);
        } else {
            rp.setMargins(rp.leftMargin, rp.topMargin, rp.rightMargin, sIndicatorMarginLong);
        }
        rp.addRule(12);
        rp.addRule(14);
        progressViewLayout.setLayoutParams(rp);
        progressViewLayout.requestLayout();
    }
}
