package com.morphoinc.app.panoramagp3;

import android.content.Context;
import android.util.AttributeSet;

public class ShrinkRatioPreference extends EditTextPreferenceDialog {
    public static final double DEFAULT_VALUE = 7.5d;
    private static final double MAX_VALUE = 32.0d;
    private static final double MIN_VALUE = 1.0d;

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
            if (val < MIN_VALUE || val > MAX_VALUE) {
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

    public ShrinkRatioPreference(Context context, AttributeSet attrs) {
        super(context, attrs, String.valueOf(7.5d));
    }

    public static double ShrinkRatioCalculation(int width, int height) {
        double ratio;
        if (width > height) {
            ratio = ((double) width) / 512.0d;
        } else {
            ratio = ((double) height) / 512.0d;
        }
        if (ratio < MIN_VALUE) {
            return MIN_VALUE;
        }
        if (ratio > MAX_VALUE) {
            return MAX_VALUE;
        }
        return ratio;
    }
}
