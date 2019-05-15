package com.hmdglobal.app.camera;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.CameraUtil;

public class TizrShareVideoActivity extends Activity {
    private static final Tag TAG = new Tag("TizrShareVideoActivity");
    private Button mStartBtn;
    private Uri mUri;

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tizr_share_video);
        this.mUri = getIntent().getData();
        this.mStartBtn = (Button) findViewById(R.id.startbtn);
        this.mStartBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(CameraUtil.TIZR_URI));
                    intent.setFlags(67108864);
                    TizrShareVideoActivity.this.startActivity(intent);
                } catch (Exception e) {
                    Log.e(TizrShareVideoActivity.TAG, "Tony tizr share video Exception");
                    e.printStackTrace();
                } catch (Throwable th) {
                    TizrShareVideoActivity.this.finish();
                }
                TizrShareVideoActivity.this.finish();
            }
        });
    }
}
