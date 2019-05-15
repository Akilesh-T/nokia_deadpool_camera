package com.hmdglobal.app.camera;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import com.hmdglobal.app.camera.util.PermissionsUtil;

public class PermsActivity extends Activity {
    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grant_access);
        getWindow().addFlags(4718592);
        ((LinearLayout) findViewById(R.id.exit_layout)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                PermsActivity.this.finish();
            }
        });
        ((LinearLayout) findViewById(R.id.settings_layout)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                PermissionsUtil.gotoSettings(PermsActivity.this);
                PermsActivity.this.finish();
            }
        });
    }
}
