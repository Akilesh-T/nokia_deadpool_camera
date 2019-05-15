package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.hmdglobal.app.camera.R;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class RecordTimeLayout extends RotateLayout {
    private static final int[] mMarginBottom = new int[1];
    private static final int[] mMarginLeft = new int[1];
    private static final int[] mMarginRight = new int[1];
    private static final int[] mMarginTop = new int[4];
    private TextView mRecordingTime;

    public RecordTimeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = getResources();
        mMarginTop[0] = res.getDimensionPixelSize(R.dimen.video_recordtime_margin_top_ori_0);
        mMarginTop[1] = res.getDimensionPixelSize(R.dimen.video_recordtime_margin_top_ori_90);
        mMarginTop[2] = (getResources().getDisplayMetrics().heightPixels - res.getDimensionPixelSize(R.dimen.video_recordtime_margin_top_ori_180)) - 20;
        mMarginTop[3] = res.getDimensionPixelSize(R.dimen.video_recordtime_margin_top_ori_270);
        mMarginLeft[0] = res.getDimensionPixelSize(R.dimen.video_recordtime_margin_left_ori_0);
        mMarginBottom[0] = res.getDimensionPixelSize(R.dimen.video_recordtime_margin_bottom_ori_0);
        mMarginRight[0] = res.getDimensionPixelSize(R.dimen.video_recordtime_margin_right_ori_0);
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mRecordingTime = (TextView) this.mChild.findViewById(R.id.recording_time);
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthSpec, int heightSpec) {
        setRecordingTimeLocation(getOrientation());
        super.onMeasure(widthSpec, heightSpec);
    }

    private void setRecordingTimeLocation(int orientation) {
        if (this.mRecordingTime != null) {
            LayoutParams params = new LayoutParams(-2, -2);
            params.gravity = 17;
            if (orientation == 0) {
                params.topMargin = mMarginTop[0];
            } else if (orientation == 90) {
                params.topMargin = mMarginTop[1];
            } else if (orientation == MediaProviderUtils.ROTATION_180) {
                params.topMargin = mMarginTop[2];
            } else if (orientation == MediaProviderUtils.ROTATION_270) {
                params.topMargin = mMarginTop[3];
            }
            params.leftMargin = mMarginLeft[0];
            params.rightMargin = mMarginRight[0];
            params.bottomMargin = mMarginBottom[0];
            this.mRecordingTime.setLayoutParams(params);
        }
    }
}
