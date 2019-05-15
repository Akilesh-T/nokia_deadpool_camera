package com.hmdglobal.app.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ProxyLauncher extends Activity {
    public static final int RESULT_USER_CANCELED = -2;

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            startActivityForResult((Intent) getIntent().getParcelableExtra("android.intent.extra.INTENT"), 0);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 0) {
            resultCode = -2;
        }
        setResult(resultCode, data);
        finish();
    }
}
