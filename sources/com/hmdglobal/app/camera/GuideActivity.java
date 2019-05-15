package com.hmdglobal.app.camera;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.util.CustomUtil;
import java.util.ArrayList;

public class GuideActivity extends Activity {
    private static final String POINT_WIDTH = "pointWidth";
    private static final String POSITION = "position";
    public static final String PREF_GUIDE = "pref_guide";
    private static final String TAG = "GuideActivity";
    private static final int[] mImagesIds = new int[]{R.drawable.tutor001, R.drawable.tutor002, R.drawable.tutor003, R.drawable.tutor004};
    private static final int[] mStringIds = new int[]{R.string.pro_mode_tutorial_swipe_up_once_hint, R.string.pro_mode_tutorial_swipe_up_twice_hint, R.string.pro_mode_tutorial_swipe_down_hint, R.string.pro_mode_tutorial_swipe_exit_hint};
    private int curPosition = 0;
    private Button mButtonStart;
    private boolean mIsKeyguardLocked = false;
    private KeyguardManager mKeyguardManager = null;
    private LinearLayout mLinearLayoutPointGroup;
    private boolean mNoPermsGranted = false;
    private ArrayList<LinearLayout> mPageViewList = new ArrayList();
    private int mPointWidth;
    private SettingsManager mSettingsManager;
    private SharedPreferences mSharedPreferences;
    private TextView mTextViewSkip;
    private TextView mTextViewTips;
    private ViewPager mViewPageGuide;
    private View mViewWhitePoint;

    class GuideAdapter extends PagerAdapter {
        GuideAdapter() {
        }

        public int getCount() {
            return GuideActivity.mImagesIds.length;
        }

        public boolean isViewFromObject(@NonNull View arg0, @NonNull Object arg1) {
            return arg0 == arg1;
        }

        @NonNull
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            container.addView((View) GuideActivity.this.mPageViewList.get(position));
            return GuideActivity.this.mPageViewList.get(position);
        }

        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }
    }

    class GuidePageListener implements OnPageChangeListener {
        GuidePageListener() {
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            LayoutParams layoutParams = (LayoutParams) GuideActivity.this.mViewWhitePoint.getLayoutParams();
            layoutParams.leftMargin = ((int) (((float) GuideActivity.this.mPointWidth) * positionOffset)) + (GuideActivity.this.mPointWidth * position);
            GuideActivity.this.mViewWhitePoint.setLayoutParams(layoutParams);
        }

        public void onPageSelected(int position) {
            GuideActivity.this.curPosition = position;
            GuideActivity.this.mTextViewTips.setText(GuideActivity.mStringIds[position]);
            if (position == GuideActivity.mImagesIds.length - 1) {
                GuideActivity.this.mButtonStart.setText(R.string.pro_mode_tutorial_btn_finish);
                GuideActivity.this.mTextViewSkip.setVisibility(4);
                return;
            }
            GuideActivity.this.mButtonStart.setText(R.string.startup_dialog_button_next);
            GuideActivity.this.mTextViewSkip.setVisibility(0);
        }

        public void onPageScrollStateChanged(int state) {
        }
    }

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mSharedPreferences = getSharedPreferences(PREF_GUIDE, 0);
        this.mSettingsManager = new SettingsManager(this);
        if (!checkAndJumpNextPage()) {
            getWindow().addFlags(1024);
            setContentView(R.layout.activity_guide);
            if (savedInstanceState != null) {
                this.curPosition = savedInstanceState.getInt(POSITION);
                this.mPointWidth = savedInstanceState.getInt(POINT_WIDTH);
            }
            this.mViewPageGuide = (ViewPager) findViewById(R.id.vp_welcome);
            this.mLinearLayoutPointGroup = (LinearLayout) findViewById(R.id.ll_point_group);
            this.mViewWhitePoint = findViewById(R.id.view_white_point);
            this.mButtonStart = (Button) findViewById(R.id.button_1);
            this.mTextViewSkip = (TextView) findViewById(R.id.tv_skip);
            this.mTextViewTips = (TextView) findViewById(R.id.tv_tips);
            this.mTextViewTips.setText(mStringIds[this.curPosition]);
            this.mViewPageGuide.setAdapter(new GuideAdapter());
            this.mViewPageGuide.addOnPageChangeListener(new GuidePageListener());
            this.mViewPageGuide.setCurrentItem(this.curPosition);
            this.mButtonStart.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    int pos = GuideActivity.this.curPosition + 1;
                    if (pos >= GuideActivity.mImagesIds.length) {
                        GuideActivity.this.mButtonStart.setClickable(false);
                        GuideActivity.this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX, GuideActivity.this.getResources().getInteger(R.integer.camera_mode_photo));
                        GuideActivity.this.mSharedPreferences.edit().putBoolean(Keys.KEY_GUIDE, true).apply();
                        GuideActivity.this.nextActivity();
                        GuideActivity.this.finish();
                        return;
                    }
                    GuideActivity.this.mViewPageGuide.setCurrentItem(pos);
                }
            });
            this.mTextViewSkip.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    GuideActivity.this.mSharedPreferences.edit().putBoolean(Keys.KEY_GUIDE, true).apply();
                    GuideActivity.this.nextActivity();
                    GuideActivity.this.finish();
                }
            });
            initViews();
            this.mIsKeyguardLocked = isKeyguardLocked();
            if (this.mIsKeyguardLocked) {
                this.mNoPermsGranted = true;
                getWindow().addFlags(524288);
            }
        }
    }

    private void sendNotificationForPerms() {
        Intent intent = new Intent();
        String CLASS_NAME = "com.hmdglobal.app.camera.GuideActivity";
        intent.setComponent(new ComponentName(getPackageName(), "com.hmdglobal.app.camera.GuideActivity"));
        intent.setFlags(268468224);
        ((NotificationManager) getSystemService("notification")).notify(1, new Builder(this).setSmallIcon(17301642).setContentTitle(getResources().getString(R.string.permission_title)).setContentText(getResources().getString(R.string.permission_content)).setWhen(System.currentTimeMillis()).setContentIntent(PendingIntent.getActivity(this, 0, intent, 0)).setAutoCancel(true).build());
    }

    /* Access modifiers changed, original: protected */
    public boolean isKeyguardLocked() {
        if (this.mKeyguardManager == null) {
            this.mKeyguardManager = (KeyguardManager) getSystemService("keyguard");
        }
        if (this.mKeyguardManager != null) {
            return this.mKeyguardManager.isKeyguardLocked();
        }
        return false;
    }

    /* Access modifiers changed, original: protected */
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(POSITION, this.mViewPageGuide.getCurrentItem());
        outState.putInt(POINT_WIDTH, this.mPointWidth);
    }

    private void initViews() {
        LayoutParams layoutParams = (LayoutParams) this.mViewWhitePoint.getLayoutParams();
        layoutParams.leftMargin = this.mPointWidth * this.curPosition;
        this.mViewWhitePoint.setLayoutParams(layoutParams);
        int i = 0;
        for (int imageResource : mImagesIds) {
            LinearLayout pageView = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.item_page, null);
            ((ImageView) pageView.findViewById(R.id.iv_guide)).setImageResource(imageResource);
            this.mPageViewList.add(pageView);
        }
        while (i < mImagesIds.length) {
            View point = new View(this);
            point.setBackgroundResource(R.drawable.shape_point_gray);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.guide_point_size), getResources().getDimensionPixelSize(R.dimen.guide_point_size));
            if (i > 0) {
                params.leftMargin = getResources().getDimensionPixelSize(R.dimen.guide_point_margin_left);
            }
            point.setLayoutParams(params);
            this.mLinearLayoutPointGroup.addView(point);
            i++;
        }
        this.mLinearLayoutPointGroup.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                GuideActivity.this.mLinearLayoutPointGroup.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                GuideActivity.this.mPointWidth = GuideActivity.this.mLinearLayoutPointGroup.getChildAt(1).getLeft() - GuideActivity.this.mLinearLayoutPointGroup.getChildAt(0).getLeft();
            }
        });
    }

    public boolean checkAndJumpNextPage() {
        boolean skipGuide;
        if (CustomUtil.getInstance().isPanther()) {
            skipGuide = this.mSharedPreferences.getBoolean(Keys.KEY_GUIDE, false);
        } else {
            skipGuide = true;
        }
        if (skipGuide) {
            nextActivity();
            finish();
        }
        return skipGuide;
    }

    public void nextActivity() {
        Intent it = getIntent();
        it.setClass(this, CameraActivity.class);
        startActivity(it);
        overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_enter);
    }
}
