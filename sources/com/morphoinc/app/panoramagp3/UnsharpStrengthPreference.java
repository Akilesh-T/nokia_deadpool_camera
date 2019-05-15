package com.morphoinc.app.panoramagp3;

import android.content.Context;
import android.util.AttributeSet;

public class UnsharpStrengthPreference extends EditTextPreferenceDialog {
    public static final int DEFAULT_VALUE = 1536;
    private static final int MAX_VALUE = 8192;
    private static final int MIN_VALUE = 0;

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
        boolean z = false;
        try {
            int inputValue = Integer.parseInt(text);
            if (inputValue >= 0 && inputValue <= 8192) {
                z = true;
            }
            return z;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /* Access modifiers changed, original: protected */
    public int getInputType() {
        return 2;
    }

    public UnsharpStrengthPreference(Context context, AttributeSet attrs) {
        super(context, attrs, String.valueOf(DEFAULT_VALUE));
    }
}
