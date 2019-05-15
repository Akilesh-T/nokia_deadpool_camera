package com.morphoinc.app.panoramagp3;

import android.content.Context;
import android.util.AttributeSet;

public class CalcseamPixnumPreference extends EditTextPreferenceDialog {
    public static final int DEFAULT_VALUE = 0;

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
            Integer.valueOf(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /* Access modifiers changed, original: protected */
    public int getInputType() {
        return 2;
    }

    public CalcseamPixnumPreference(Context context, AttributeSet attrs) {
        super(context, attrs, String.valueOf(0));
    }
}
