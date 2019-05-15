package com.hmdglobal.app.camera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import com.hmdglobal.app.camera.R;

public class AspectRatioSelector extends LinearLayout {
    private AspectRatio mAspectRatio = AspectRatio.ASPECT_RATIO_4x3;
    private View mAspectRatio16x9Button;
    private View mAspectRatio4x3Button;

    public enum AspectRatio {
        ASPECT_RATIO_4x3,
        ASPECT_RATIO_16x9
    }

    public AspectRatioSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onFinishInflate() {
        this.mAspectRatio4x3Button = findViewById(R.id.aspect_ratio_4x3_button);
        this.mAspectRatio4x3Button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AspectRatioSelector.this.setAspectRatio(AspectRatio.ASPECT_RATIO_4x3);
            }
        });
        this.mAspectRatio16x9Button = findViewById(R.id.aspect_ratio_16x9_button);
        this.mAspectRatio16x9Button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AspectRatioSelector.this.setAspectRatio(AspectRatio.ASPECT_RATIO_16x9);
            }
        });
    }

    public void setAspectRatio(AspectRatio aspectRatio) {
        if (aspectRatio == AspectRatio.ASPECT_RATIO_4x3) {
            this.mAspectRatio4x3Button.setSelected(true);
            this.mAspectRatio16x9Button.setSelected(false);
        } else if (aspectRatio == AspectRatio.ASPECT_RATIO_16x9) {
            this.mAspectRatio16x9Button.setSelected(true);
            this.mAspectRatio4x3Button.setSelected(false);
        } else {
            return;
        }
        this.mAspectRatio = aspectRatio;
    }

    public AspectRatio getAspectRatio() {
        return this.mAspectRatio;
    }
}
