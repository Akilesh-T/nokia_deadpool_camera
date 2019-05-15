package com.morphoinc.app.panoramagp3;

import android.content.Context;
import android.util.AttributeSet;

public class AovxPreference extends EditTextPreferenceDialog {
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
            Double.valueOf(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /* Access modifiers changed, original: protected */
    public int getInputType() {
        return 8194;
    }

    public AovxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
