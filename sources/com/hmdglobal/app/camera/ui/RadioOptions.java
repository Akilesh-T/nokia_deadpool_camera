package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import com.hmdglobal.app.camera.R;

public class RadioOptions extends TopRightWeightedLayout {
    private Drawable mBackground;
    private OnOptionClickListener mOnOptionClickListener;

    public interface OnOptionClickListener {
        void onOptionClicked(View view);
    }

    public void setOnOptionClickListener(OnOptionClickListener listener) {
        this.mOnOptionClickListener = listener;
    }

    public RadioOptions(Context context, AttributeSet attrs) {
        super(context, attrs);
        int drawableId = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RadioOptions, 0, 0).getResourceId(0, 0);
        if (drawableId > 0) {
            this.mBackground = context.getResources().getDrawable(drawableId);
        }
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        updateListeners();
    }

    public void updateListeners() {
        OnClickListener onClickListener = new OnClickListener() {
            public void onClick(View button) {
                RadioOptions.this.setSelectedOptionByView(button);
            }
        };
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setOnClickListener(onClickListener);
        }
    }

    public void setSelectedOptionByTag(Object tag) {
        setSelectedOptionByView(findViewWithTag(tag));
    }

    public void setSeletedOptionById(int id) {
        setSelectedOptionByView(findViewById(id));
    }

    private void setSelectedOptionByView(View view) {
        if (view != null) {
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).setBackground(null);
            }
            view.setBackground(this.mBackground);
            if (this.mOnOptionClickListener != null) {
                this.mOnOptionClickListener.onOptionClicked(view);
            }
        }
    }
}
