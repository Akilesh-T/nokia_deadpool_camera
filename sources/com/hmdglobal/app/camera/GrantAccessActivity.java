package com.hmdglobal.app.camera;

import android.app.Activity;
import android.os.Bundle;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.PermissionsUtil;

public class GrantAccessActivity extends Activity {
    private static final Tag TAG = new Tag("GrantAccessActivity");

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showDialog();
    }

    private void showDialog() {
        Log.e(TAG, "showDialog");
        PermissionsUtil.showDialogGrantAccess(this, true);
    }
}
