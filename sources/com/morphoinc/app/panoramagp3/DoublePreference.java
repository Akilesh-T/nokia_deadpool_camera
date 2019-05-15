package com.morphoinc.app.panoramagp3;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import com.hmdglobal.app.camera.R;

public class DoublePreference extends EditTextPreferenceDialog {
    private final double mDef;
    private final double mMax;
    private final double mMin;

    public /* bridge */ /* synthetic */ void setDefValue(String str) {
        super.setDefValue(str);
    }

    public /* bridge */ /* synthetic */ void setSummary() {
        super.setSummary();
    }

    public /* bridge */ /* synthetic */ void setValue(String str) {
        super.setValue(str);
    }

    /* Access modifiers changed, original: protected */
    public boolean isValidInputText(String text) {
        try {
            double val = Double.valueOf(text).doubleValue();
            if (val < this.mMin || val > this.mMax) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /* Access modifiers changed, original: protected */
    public int getInputType() {
        return 12290;
    }

    public DoublePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DoublePreference);
        this.mMin = (double) a.getFloat(2, 0.0f);
        this.mMax = (double) a.getFloat(1, 0.0f);
        this.mDef = (double) a.getFloat(0, 0.0f);
        a.recycle();
        setDefValue(String.valueOf(this.mDef));
    }
}
