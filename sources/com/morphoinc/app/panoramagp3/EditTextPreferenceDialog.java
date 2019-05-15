package com.morphoinc.app.panoramagp3;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.hmdglobal.app.camera.R;

abstract class EditTextPreferenceDialog extends DialogPreference {
    private final Context mContext;
    private String mDefValue;
    private EditText mEditText;
    private String mValue;

    public abstract int getInputType();

    public abstract boolean isValidInputText(String str);

    EditTextPreferenceDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.mValue = "";
        this.mDefValue = "";
    }

    EditTextPreferenceDialog(Context context, AttributeSet attrs, String def) {
        this(context, attrs);
        setDefValue(def);
    }

    public void setDefValue(String val) {
        this.mDefValue = val;
        this.mValue = PreferenceManager.getDefaultSharedPreferences(this.mContext).getString(getKey(), this.mDefValue);
    }

    public void setSummary() {
        super.setSummary(this.mValue);
    }

    /* Access modifiers changed, original: protected */
    public View onCreateDialogView() {
        View layout = View.inflate(this.mContext, R.layout.edittext_preference_dialog, null);
        this.mValue = PreferenceManager.getDefaultSharedPreferences(this.mContext).getString(getKey(), this.mDefValue);
        this.mEditText = (EditText) layout.findViewById(R.id.edittext_dialog);
        this.mEditText.setText(this.mValue);
        this.mEditText.setInputType(getInputType());
        this.mEditText.selectAll();
        Toast.makeText(this.mContext, "Clearing the text, return to the initial value.", 1).show();
        return layout;
    }

    /* Access modifiers changed, original: protected */
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String text = this.mEditText.getText().toString();
            if (text.isEmpty()) {
                this.mValue = this.mDefValue;
            } else if (isValidInputText(text)) {
                this.mValue = text;
            } else {
                return;
            }
            save();
        }
    }

    public void setValue(String val) {
        this.mValue = val;
        save();
    }

    private void save() {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        editor.putString(getKey(), this.mValue);
        editor.apply();
    }
}
