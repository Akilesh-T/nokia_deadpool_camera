package com.morphoinc.app.panoramagp3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import com.hmdglobal.app.camera.R;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class SplashApp extends Activity {
    private static final int DISPLAY_LENGTH = 2000;
    private OrientationListener mOrientationListener;
    private int m_orientation;

    private class OrientationListener extends OrientationEventListener {
        private boolean isFirst;
        private final Context mContext;
        private final Point mDisplaySize = new Point();
        private final float mPortraitScale;

        public OrientationListener(Context context) {
            super(context);
            this.mContext = context;
            this.isFirst = true;
            ((WindowManager) SplashApp.this.getSystemService("window")).getDefaultDisplay().getSize(this.mDisplaySize);
            this.mPortraitScale = ((float) this.mDisplaySize.y) / ((float) this.mDisplaySize.x);
        }

        public void onOrientationChanged(int orientation) {
            if (orientation != -1) {
                int view_orientation = invOrientation(roundOrientation(getDisplayRotation(this.mContext) + orientation));
                if (this.isFirst || SplashApp.this.m_orientation != view_orientation) {
                    int cur = SplashApp.this.m_orientation;
                    SplashApp.this.m_orientation = view_orientation;
                    int diff = cur - SplashApp.this.m_orientation;
                    float from = diff >= MediaProviderUtils.ROTATION_270 ? (float) (cur - 360) : (float) cur;
                    float to = (float) (diff <= -270 ? SplashApp.this.m_orientation - 360 : SplashApp.this.m_orientation);
                    View v = SplashApp.this.findViewById(R.id.panoramagp3_ImageView01);
                    if (v.getWidth() > 0) {
                        rotateView(v, from, to, ((float) v.getWidth()) / 2.0f, ((float) v.getHeight()) / 2.0f);
                        this.isFirst = false;
                    }
                }
            }
        }

        private int getDisplayRotation(Context context) {
            switch (((Activity) context).getWindowManager().getDefaultDisplay().getRotation()) {
                case 0:
                    return 0;
                case 1:
                    return 90;
                case 2:
                    return MediaProviderUtils.ROTATION_180;
                case 3:
                    return MediaProviderUtils.ROTATION_270;
                default:
                    return 0;
            }
        }

        private int roundOrientation(int orientation) {
            return (((orientation + 45) / 90) * 90) % 360;
        }

        private int invOrientation(int orientation) {
            return (360 - orientation) % 360;
        }

        private void rotateView(View v, float fromDegrees, float toDegrees, float pivotX, float pivotY) {
            float f = toDegrees;
            float f2 = 1.0f;
            float toScale = (f == 0.0f || f == 180.0f) ? 1.0f : this.mPortraitScale;
            if (f == 0.0f || f == 180.0f) {
                f2 = this.mPortraitScale;
            }
            float fromScale = f2;
            f2 = pivotX;
            float f3 = pivotY;
            RotateAnimation ra = new RotateAnimation(fromDegrees, f, f2, f3);
            ScaleAnimation sa = new ScaleAnimation(fromScale, toScale, fromScale, toScale, f2, f3);
            AnimationSet animation = new AnimationSet(true);
            animation.addAnimation(ra);
            animation.addAnimation(sa);
            animation.setDuration(300);
            animation.setFillAfter(true);
            v.startAnimation(animation);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullscreen();
        new Handler().postDelayed(new Runnable() {
            public void run() {
                SplashApp.this.startActivity(new Intent(SplashApp.this.getApplicationContext(), Camera2App.class));
                SplashApp.this.finish();
            }
        }, 2000);
        this.mOrientationListener = new OrientationListener(this);
    }

    public boolean onKeyDown(int KeyCode, KeyEvent event) {
        return KeyCode != 4;
    }

    private void setFullscreen() {
        getWindow().addFlags(1024);
        getWindow().addFlags(128);
        requestWindowFeature(1);
        setContentView(R.layout.splash);
    }

    /* Access modifiers changed, original: protected */
    public void onResume() {
        super.onResume();
        this.mOrientationListener.enable();
    }

    /* Access modifiers changed, original: protected */
    public void onPause() {
        super.onPause();
        this.mOrientationListener.disable();
    }
}
