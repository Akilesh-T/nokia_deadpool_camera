package com.morphoinc.app.panoramagp3;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import java.util.Locale;

class RenderingAreaPreference extends SeekBarPreferenceDialog {
    public static final int DEFAULT_VALUE = 33;
    private static final int MAX_VALUE = 100;
    private static final int MIN_VALUE = 1;

    public RenderingAreaPreference(Context context, AttributeSet attrs) {
        super(context, attrs, 1, 100, 33);
        this.mValue = PreferenceManager.getDefaultSharedPreferences(this.mContext).getInt(getKey(), 33) - 1;
    }

    /* Access modifiers changed, original: protected */
    public void updateTextView() {
        this.mTextView.setText(String.format(Locale.US, "%d %%", new Object[]{Integer.valueOf(this.mValue + 1)}));
    }

    public void setSummary() {
        super.setSummary(String.format(Locale.US, "%d %%", new Object[]{Integer.valueOf(this.mValue + 1)}));
    }
}
