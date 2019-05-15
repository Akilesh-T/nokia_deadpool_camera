package com.morphoinc.app.panoramagp3;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.hmdglobal.app.camera.R;
import java.util.Locale;

class SeekBarPreferenceDialog extends DialogPreference implements OnSeekBarChangeListener {
    final Context mContext;
    private final int mDefValue;
    private final int mMaxValue;
    private final int mMinValue;
    private SeekBar mSeekBar;
    TextView mTextView;
    int mValue;

    public SeekBarPreferenceDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 1, 100, 1);
    }

    SeekBarPreferenceDialog(Context context, AttributeSet attrs, int min, int max, int def) {
        super(context, attrs);
        this.mContext = context;
        this.mMinValue = min;
        this.mMaxValue = max;
        this.mDefValue = def;
    }

    /* Access modifiers changed, original: protected */
    public View onCreateDialogView() {
        View layout = View.inflate(this.mContext, R.layout.seekbar_preference_dialog, null);
        this.mValue = PreferenceManager.getDefaultSharedPreferences(this.mContext).getInt(getKey(), this.mDefValue) - this.mMinValue;
        this.mSeekBar = (SeekBar) layout.findViewById(R.id.seekbar_dialog);
        this.mSeekBar.setMax(this.mMaxValue - this.mMinValue);
        this.mSeekBar.setOnSeekBarChangeListener(this);
        this.mSeekBar.setProgress(this.mValue);
        this.mTextView = (TextView) layout.findViewById(R.id.seekbar_dialog_value);
        updateTextView();
        return layout;
    }

    /* Access modifiers changed, original: protected */
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
            editor.putInt(getKey(), this.mValue + this.mMinValue);
            editor.apply();
        }
    }

    public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
    }

    public void onStartTrackingTouch(SeekBar arg0) {
    }

    public void onStopTrackingTouch(SeekBar arg0) {
        this.mValue = this.mSeekBar.getProgress();
        updateTextView();
    }

    /* Access modifiers changed, original: 0000 */
    public void updateTextView() {
        this.mTextView.setText(String.format(Locale.US, "%d", new Object[]{Integer.valueOf(this.mValue + this.mMinValue)}));
    }
}
