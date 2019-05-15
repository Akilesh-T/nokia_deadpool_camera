package com.hmdglobal.app.camera.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RadialGradient;
import android.graphics.Shader.TileMode;
import android.os.Parcelable;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.ModuleManager;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.ModeScrollBar.onBarStatueChangedListener;
import com.hmdglobal.app.camera.ui.ModeStrip.OnModeIdListener;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.LockUtils;
import com.hmdglobal.app.camera.util.LockUtils.Lock;
import com.hmdglobal.app.camera.util.LockUtils.LockType;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class ModeStripView extends HorizontalScrollView implements ModeStrip {
    private static final int DIRECTION_LEFT = 1;
    private static final int DIRECTION_NONE = -1;
    private static final int DIRECTION_RIGHT = 2;
    private static final int GAP_HEIGHT = 10;
    private static final Tag TAG = new Tag("ModeStripView");
    private final int ANIM_RESET_ALPHA = 1;
    private final int ANIM_RESET_RADIUS = 0;
    private final int ENTER_MODE_ANIM_DURATION = 350;
    private final int LEAVE_MODE_ANIM_DURATION = Callback.DEFAULT_SWIPE_ANIMATION_DURATION;
    private final float RIPPLE_COLOR_ALPHA = 0.2f;
    private int initModeIndex = -1;
    private Adapter mAdapter;
    private final float mAlphaFactor;
    private ViewGroup mChildWrapper;
    private int mClickIndex = -1;
    private List<Integer> mCoordsX = new ArrayList();
    private int mCurrentIndex = -1;
    private float mDownX;
    private float mDownY;
    private View[] mGapHolder = new View[2];
    private boolean mIsEnterModeAnimating = false;
    private boolean mIsLeaveModeAnimating = false;
    List<Integer> mItemWidthList = new ArrayList();
    private OnModeIdListener mListener;
    private float mMaxRadius;
    private ModeScrollBar mModeScrollBar;
    private Integer mModeSelectionLockToken;
    private Lock mMultiLock;
    private Paint mPaint;
    private final Integer mPaintAlpha;
    private Path mPath = new Path();
    private int mPendingIndex = 0;
    private RadialGradient mRadialGradient;
    private float mRadius;
    private ObjectAnimator mRadiusAnimator;
    private final Integer mRadiusIncrement;
    private final int mRippleColor;
    private int mScrolllingDirection = -1;
    private int mSlideToModeIndex = -1;
    private int mStartCoord = 0;
    private int mVisualWidth = 0;

    private class ModeStripViewAdapter extends BaseAdapter {
        List<ModuleHolder> mModules = new ArrayList();

        private class ModuleHolder {
            public int MODULE_INDEX;
            public String MODULE_NAME;

            /* synthetic */ ModuleHolder(ModeStripViewAdapter x0, int x1, AnonymousClass1 x2) {
                this(x1);
            }

            private ModuleHolder(int index) {
                this.MODULE_INDEX = index;
                this.MODULE_NAME = CameraUtil.getCameraModeText(index, ModeStripView.this.getContext());
            }

            public View getModuleView() {
                View view = LayoutInflater.from(ModeStripView.this.getContext()).inflate(R.layout.mode_strip_item, null);
                ((TextView) view.findViewById(R.id.mode_title)).setText(this.MODULE_NAME);
                return view;
            }
        }

        public ModeStripViewAdapter(ModuleManager manager) {
            for (Integer index : manager.getSupportedModeIndexList()) {
                int index2 = index.intValue();
                if (manager.getModuleAgent(index2).needAddToStrip()) {
                    this.mModules.add(new ModuleHolder(this, index2, null));
                }
            }
        }

        public int getCount() {
            return this.mModules.size();
        }

        public Object getItem(int position) {
            return this.mModules.get(position);
        }

        public long getItemId(int position) {
            return (long) ((ModuleHolder) this.mModules.get(position)).MODULE_INDEX;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return ((ModuleHolder) this.mModules.get(position)).getModuleView();
        }
    }

    public ModeStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mChildWrapper = (ViewGroup) ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.strip_wrapper, null);
        addView(this.mChildWrapper);
        this.mMultiLock = LockUtils.getInstance().generateMultiLock(LockType.MULTILOCK);
        this.mPaintAlpha = Integer.valueOf(getResources().getInteger(R.integer.mode_paint_alpha));
        this.mRadiusIncrement = Integer.valueOf(getResources().getInteger(R.integer.mode_ripple_radius_increment));
        this.mPaint = new Paint(1);
        this.mPaint.setAlpha(this.mPaintAlpha.intValue());
        this.mRippleColor = -1;
        this.mAlphaFactor = 0.2f;
    }

    public Integer lockView() {
        return this.mMultiLock.aquireLock();
    }

    public boolean unLockView(Integer token) {
        return this.mMultiLock.unlockWithToken(token);
    }

    public void setModeIndexChangeListener(OnModeIdListener listener) {
        this.mListener = listener;
    }

    public void setAdapter(Adapter adapter) {
        this.mAdapter = adapter;
        notifyDatasetChanged();
    }

    public synchronized void notifyDatasetChanged() {
        int count = this.mAdapter.getCount();
        this.mChildWrapper.removeAllViews();
        int i = 0;
        if (this.mGapHolder[0] != null) {
            this.mChildWrapper.addView(this.mGapHolder[0]);
        }
        while (true) {
            int i2 = i;
            if (i2 >= count) {
                break;
            }
            View view = this.mAdapter.getView(i2, null, null);
            view.setTag(Integer.valueOf(i2));
            view.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (ModeStripView.this.mMultiLock.isLocked()) {
                        Log.e(ModeStripView.TAG, "Mode can't to switch. mode locked");
                    } else if (ModeStripView.this.mIsEnterModeAnimating || ModeStripView.this.mIsLeaveModeAnimating) {
                        Log.e(ModeStripView.TAG, "Mode can't to switch.Leaving or entering animation is running");
                    } else {
                        ModeStripView.this.mClickIndex = ((Integer) v.getTag()).intValue();
                        if (ModeStripView.this.mCurrentIndex == ModeStripView.this.mClickIndex) {
                            Tag access$100 = ModeStripView.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("No need to change. mCurrentIndex equals to clickindex : ");
                            stringBuilder.append(ModeStripView.this.mCurrentIndex);
                            Log.e(access$100, stringBuilder.toString());
                            return;
                        }
                        ModeStripView.this.requestLockModeStripView(true);
                        if (ModeStripView.this.mModeScrollBar != null) {
                            final int target = ((Integer) v.getTag()).intValue();
                            int[] pos = new int[2];
                            v.getLocationInWindow(pos);
                            ModeStripView.this.mModeScrollBar.scrollToItem(ModeStripView.this.mCurrentIndex, target, pos[0] + (((Integer) ModeStripView.this.mItemWidthList.get(target)).intValue() / 2), new onBarStatueChangedListener() {
                                public void onScrollStarted() {
                                    ModeStripView.this.getHandler().post(new Runnable() {
                                        public void run() {
                                        }
                                    });
                                }

                                public void onItemReached() {
                                    ModeStripView.this.getHandler().post(new Runnable() {
                                        public void run() {
                                            ModeStripView.this.updateTextColor(target);
                                        }
                                    });
                                }

                                public void onEndArrived() {
                                    ModeStripView.this.getHandler().post(new Runnable() {
                                        public void run() {
                                            ModeStripView.this.setCurrentItem(target, true);
                                        }
                                    });
                                }

                                public void onScrollFinished() {
                                    ModeStripView.this.getHandler().post(new Runnable() {
                                        public void run() {
                                            ModeStripView.this.requestLockModeStripView(false);
                                        }
                                    });
                                }
                            });
                        }
                    }
                }
            });
            this.mChildWrapper.addView(view);
            i = i2 + 1;
        }
        if (this.mGapHolder[1] != null) {
            this.mChildWrapper.addView(this.mGapHolder[1]);
        }
        if (count == 1) {
            setVisibility(8);
        }
        if (this.initModeIndex != -1) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Tony initModeIndex = ");
            stringBuilder.append(this.initModeIndex);
            Log.d(tag, stringBuilder.toString());
            updateTextColor(this.initModeIndex);
        }
    }

    private void enterModeAnimation(int enterModeIndex) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Tony enterModeAnimation index = ");
        stringBuilder.append(enterModeIndex);
        Log.d(tag, stringBuilder.toString());
        View v = this.mChildWrapper.getChildAt(enterModeIndex + 1);
        this.mDownX = (float) (v.getLeft() + (v.getWidth() / 2));
        this.mDownY = (float) v.getTop();
        this.mRadiusAnimator = ObjectAnimator.ofFloat(this, "radius", new float[]{(float) CameraUtil.dpToPixel(0), this.mMaxRadius + ((float) CameraUtil.dpToPixel(this.mRadiusIncrement.intValue()))}).setDuration(350);
        this.mRadiusAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        this.mRadiusAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                ModeStripView.this.mIsEnterModeAnimating = true;
            }

            public void onAnimationEnd(Animator animation) {
                ModeStripView.this.setRadius(0.0f);
                ModeStripView.this.setAlpha(1.0f);
                ModeStripView.this.mIsEnterModeAnimating = false;
                ModeStripView.this.mDownX = -1.0f;
                ModeStripView.this.mDownY = -1.0f;
            }
        });
        this.mRadiusAnimator.start();
    }

    private void leaveModeAnimation(int leaveModeIndex) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Tony leaveModeAnimation index = ");
        stringBuilder.append(leaveModeIndex);
        Log.d(tag, stringBuilder.toString());
        View v = this.mChildWrapper.getChildAt(leaveModeIndex + 1);
        this.mDownX = (float) (v.getLeft() + (v.getWidth() / 2));
        this.mDownY = (float) v.getTop();
        this.mRadiusAnimator = ObjectAnimator.ofFloat(this, "radius", new float[]{this.mMaxRadius + ((float) CameraUtil.dpToPixel(this.mRadiusIncrement.intValue())), (float) CameraUtil.dpToPixel(0)}).setDuration(250);
        this.mRadiusAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        this.mRadiusAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                ModeStripView.this.mIsLeaveModeAnimating = true;
            }

            public void onAnimationEnd(Animator animator) {
                ModeStripView.this.setRadius(0.0f);
                ModeStripView.this.setAlpha(1.0f);
                ModeStripView.this.mIsLeaveModeAnimating = false;
                ModeStripView.this.mDownX = -1.0f;
                ModeStripView.this.mDownY = -1.0f;
            }
        });
        this.mRadiusAnimator.start();
    }

    private void updateTextColor(int index) {
        if (this.mChildWrapper != null && this.mChildWrapper.getChildCount() > 1) {
            for (int i = 1; i < this.mChildWrapper.getChildCount() - 1; i++) {
                TextView mTx = (TextView) this.mChildWrapper.getChildAt(i).findViewById(R.id.mode_title);
                if (i == index + 1) {
                    mTx.setTextColor(getResources().getColor(R.color.mode_title_select));
                } else {
                    mTx.setTextColor(getResources().getColor(R.color.mode_title_unselect));
                }
            }
        }
    }

    public void setRadius(float radius) {
        this.mRadius = radius;
        if (this.mRadius > 0.0f) {
            this.mRadialGradient = new RadialGradient(this.mDownX, this.mDownY, this.mRadius, adjustAlpha(this.mRippleColor, this.mAlphaFactor), this.mRippleColor, TileMode.MIRROR);
            this.mPaint.setShader(this.mRadialGradient);
        }
        invalidate();
    }

    public void requestLockModeStripView(boolean enable) {
        if (enable) {
            if (this.mModeSelectionLockToken == null) {
                this.mModeSelectionLockToken = lockView();
            }
        } else if (this.mModeSelectionLockToken != null) {
            unLockView(this.mModeSelectionLockToken);
            this.mModeSelectionLockToken = null;
        }
    }

    public int adjustAlpha(int color, float factor) {
        return Color.argb(Math.round(((float) Color.alpha(color)) * factor), Color.red(color), Color.green(color), Color.blue(color));
    }

    /* JADX WARNING: Missing block: B:26:0x0063, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:28:0x0065, code skipped:
            return;
     */
    public synchronized void setCurrentItem(int r5, boolean r6) {
        /*
        r4 = this;
        monitor-enter(r4);
        r0 = r4.mAdapter;	 Catch:{ all -> 0x0066 }
        if (r0 == 0) goto L_0x0064;
    L_0x0005:
        r0 = r4.mAdapter;	 Catch:{ all -> 0x0066 }
        r0 = r0.getCount();	 Catch:{ all -> 0x0066 }
        if (r5 < r0) goto L_0x000e;
    L_0x000d:
        goto L_0x0064;
    L_0x000e:
        r0 = r4.mCoordsX;	 Catch:{ all -> 0x0066 }
        r0 = r0.size();	 Catch:{ all -> 0x0066 }
        if (r0 != 0) goto L_0x0034;
    L_0x0016:
        r0 = TAG;	 Catch:{ all -> 0x0066 }
        r1 = "View not ready or not initialized";
        com.hmdglobal.app.camera.debug.Log.e(r0, r1);	 Catch:{ all -> 0x0066 }
        r0 = r4.mPendingIndex;	 Catch:{ all -> 0x0066 }
        if (r0 == r5) goto L_0x0031;
    L_0x0021:
        r0 = r4.mListener;	 Catch:{ all -> 0x0066 }
        if (r0 == 0) goto L_0x0031;
    L_0x0025:
        r0 = r4.mListener;	 Catch:{ all -> 0x0066 }
        r1 = r4.mAdapter;	 Catch:{ all -> 0x0066 }
        r1 = r1.getItemId(r5);	 Catch:{ all -> 0x0066 }
        r1 = (int) r1;	 Catch:{ all -> 0x0066 }
        r0.onModeIdChanged(r1);	 Catch:{ all -> 0x0066 }
    L_0x0031:
        r4.mPendingIndex = r5;	 Catch:{ all -> 0x0066 }
        goto L_0x0062;
    L_0x0034:
        r0 = r4.mCoordsX;	 Catch:{ all -> 0x0066 }
        r0 = r0.get(r5);	 Catch:{ all -> 0x0066 }
        r0 = (java.lang.Integer) r0;	 Catch:{ all -> 0x0066 }
        r0 = r0.intValue();	 Catch:{ all -> 0x0066 }
        r1 = 0;
        if (r6 != 0) goto L_0x0047;
    L_0x0043:
        r4.scrollTo(r0, r1);	 Catch:{ all -> 0x0066 }
        goto L_0x004a;
    L_0x0047:
        r4.smoothScrollTo(r0, r1);	 Catch:{ all -> 0x0066 }
    L_0x004a:
        r1 = r4.mPendingIndex;	 Catch:{ all -> 0x0066 }
        if (r1 == r5) goto L_0x005e;
    L_0x004e:
        r1 = r4.mListener;	 Catch:{ all -> 0x0066 }
        if (r1 == 0) goto L_0x005e;
    L_0x0052:
        r1 = r4.mListener;	 Catch:{ all -> 0x0066 }
        r2 = r4.mAdapter;	 Catch:{ all -> 0x0066 }
        r2 = r2.getItemId(r5);	 Catch:{ all -> 0x0066 }
        r2 = (int) r2;	 Catch:{ all -> 0x0066 }
        r1.onModeIdChanged(r2);	 Catch:{ all -> 0x0066 }
    L_0x005e:
        r4.mPendingIndex = r5;	 Catch:{ all -> 0x0066 }
        r4.mCurrentIndex = r5;	 Catch:{ all -> 0x0066 }
    L_0x0062:
        monitor-exit(r4);
        return;
    L_0x0064:
        monitor-exit(r4);
        return;
    L_0x0066:
        r5 = move-exception;
        monitor-exit(r4);
        throw r5;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ui.ModeStripView.setCurrentItem(int, boolean):void");
    }

    public void setCurrentItem(int index) {
        setCurrentItem(index, false);
    }

    public void setCurrentModeWithModeIndex(int modeIndex) {
        if (this.mAdapter != null) {
            for (int i = 0; i < this.mAdapter.getCount(); i++) {
                if (((ModuleHolder) this.mAdapter.getItem(i)).MODULE_INDEX == modeIndex) {
                    this.initModeIndex = i;
                    updateTextColor(this.initModeIndex);
                    setCurrentItem(i);
                    return;
                }
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
    }

    public void fling(int velocityX) {
        super.fling(0);
    }

    public void lockSelf() {
        this.mMultiLock.aquireLock(hashCode());
    }

    public void unLockSelf() {
        this.mMultiLock.unlockWithToken(Integer.valueOf(hashCode()));
    }

    public boolean isLocked() {
        return this.mMultiLock.isLocked();
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == 3 || action == 1) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    /* JADX WARNING: Missing block: B:23:0x004e, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:44:0x0098, code skipped:
            return false;
     */
    public synchronized boolean onTouchEvent(android.view.MotionEvent r14) {
        /*
        r13 = this;
        monitor-enter(r13);
        r0 = r13.mMultiLock;	 Catch:{ all -> 0x0099 }
        r0 = r0.isLocked();	 Catch:{ all -> 0x0099 }
        r1 = 0;
        if (r0 == 0) goto L_0x000c;
    L_0x000a:
        monitor-exit(r13);
        return r1;
    L_0x000c:
        r0 = r14.getAction();	 Catch:{ all -> 0x0099 }
        r2 = -1;
        r3 = 2;
        r4 = 1;
        if (r0 != r3) goto L_0x004f;
    L_0x0015:
        r0 = r14.getHistorySize();	 Catch:{ all -> 0x0099 }
        if (r0 == 0) goto L_0x004d;
    L_0x001b:
        r0 = r14.getX();	 Catch:{ all -> 0x0099 }
        r5 = r14.getHistoricalX(r1);	 Catch:{ all -> 0x0099 }
        r0 = r0 - r5;
        r5 = (double) r0;	 Catch:{ all -> 0x0099 }
        r0 = r14.getY();	 Catch:{ all -> 0x0099 }
        r7 = r14.getHistoricalY(r1);	 Catch:{ all -> 0x0099 }
        r0 = r0 - r7;
        r7 = (double) r0;	 Catch:{ all -> 0x0099 }
        r9 = r5 / r7;
        r9 = java.lang.Math.abs(r9);	 Catch:{ all -> 0x0099 }
        r11 = 4607182418800017408; // 0x3ff0000000000000 float:0.0 double:1.0;
        r0 = (r9 > r11 ? 1 : (r9 == r11 ? 0 : -1));
        if (r0 < 0) goto L_0x003d;
    L_0x003b:
        r0 = r4;
        goto L_0x003e;
    L_0x003d:
        r0 = r1;
    L_0x003e:
        if (r0 == 0) goto L_0x004b;
    L_0x0040:
        r9 = 0;
        r2 = (r5 > r9 ? 1 : (r5 == r9 ? 0 : -1));
        if (r2 <= 0) goto L_0x0048;
    L_0x0046:
        r3 = r4;
    L_0x0048:
        r13.mScrolllingDirection = r3;	 Catch:{ all -> 0x0099 }
        goto L_0x004d;
    L_0x004b:
        r13.mScrolllingDirection = r2;	 Catch:{ all -> 0x0099 }
    L_0x004d:
        monitor-exit(r13);
        return r1;
    L_0x004f:
        r0 = r14.getAction();	 Catch:{ all -> 0x0099 }
        if (r4 != r0) goto L_0x0097;
    L_0x0055:
        r0 = r13.mCurrentIndex;	 Catch:{ all -> 0x0099 }
        r5 = r13.mScrolllingDirection;	 Catch:{ all -> 0x0099 }
        if (r4 != r5) goto L_0x0072;
    L_0x005b:
        if (r0 <= 0) goto L_0x0095;
    L_0x005d:
        r3 = r13.mModeScrollBar;	 Catch:{ all -> 0x0099 }
        if (r3 == 0) goto L_0x0095;
    L_0x0061:
        r3 = r0 + -1;
        r13.mSlideToModeIndex = r3;	 Catch:{ all -> 0x0099 }
        r3 = r13.mModeScrollBar;	 Catch:{ all -> 0x0099 }
        r4 = r13.mCurrentIndex;	 Catch:{ all -> 0x0099 }
        r5 = new com.hmdglobal.app.camera.ui.ModeStripView$4;	 Catch:{ all -> 0x0099 }
        r5.<init>();	 Catch:{ all -> 0x0099 }
        r3.scrollToLeft(r4, r5);	 Catch:{ all -> 0x0099 }
        goto L_0x0095;
    L_0x0072:
        r5 = r13.mScrolllingDirection;	 Catch:{ all -> 0x0099 }
        if (r3 != r5) goto L_0x0095;
    L_0x0076:
        r3 = r13.mCoordsX;	 Catch:{ all -> 0x0099 }
        r3 = r3.size();	 Catch:{ all -> 0x0099 }
        r3 = r3 - r4;
        if (r0 >= r3) goto L_0x0095;
    L_0x007f:
        r3 = r13.mModeScrollBar;	 Catch:{ all -> 0x0099 }
        if (r3 == 0) goto L_0x0095;
    L_0x0083:
        if (r0 < 0) goto L_0x0089;
    L_0x0085:
        r3 = r0 + 1;
        r13.mSlideToModeIndex = r3;	 Catch:{ all -> 0x0099 }
    L_0x0089:
        r3 = r13.mModeScrollBar;	 Catch:{ all -> 0x0099 }
        r4 = r13.mCurrentIndex;	 Catch:{ all -> 0x0099 }
        r5 = new com.hmdglobal.app.camera.ui.ModeStripView$5;	 Catch:{ all -> 0x0099 }
        r5.<init>();	 Catch:{ all -> 0x0099 }
        r3.scrollToRight(r4, r5);	 Catch:{ all -> 0x0099 }
    L_0x0095:
        r13.mScrolllingDirection = r2;	 Catch:{ all -> 0x0099 }
    L_0x0097:
        monitor-exit(r13);
        return r1;
    L_0x0099:
        r14 = move-exception;
        monitor-exit(r13);
        throw r14;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ui.ModeStripView.onTouchEvent(android.view.MotionEvent):boolean");
    }

    /* Access modifiers changed, original: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mMaxRadius = (float) Math.sqrt((double) ((((w / 2) * w) / 2) + (((h / 2) * h) / 2)));
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isInEditMode()) {
            canvas.save();
            this.mPath.reset();
            this.mPath.addCircle(this.mDownX, this.mDownY, this.mRadius, Direction.CW);
            canvas.clipPath(this.mPath);
            canvas.restore();
            canvas.drawCircle(this.mDownX, this.mDownY, this.mRadius, this.mPaint);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(onSaveInstanceState());
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mAdapter != null && this.mChildWrapper.getChildCount() != 0) {
            synchronized (this) {
                int coordsX;
                int width;
                int count = this.mAdapter.getCount();
                this.mCoordsX.clear();
                this.mVisualWidth = getMeasuredWidth();
                int i = 1;
                if (this.mGapHolder[0] != null) {
                    if (this.mGapHolder[1] != null) {
                        coordsX = this.mStartCoord;
                        while (true) {
                            int i2 = i;
                            if (i2 < count + 1) {
                                width = this.mChildWrapper.getChildAt(i2).getWidth();
                                if (width == 0) {
                                    this.mCoordsX.clear();
                                    return;
                                }
                                this.mCoordsX.add(Integer.valueOf(((width / 2) + coordsX) - (this.mVisualWidth / 2)));
                                coordsX += width;
                                i = i2 + 1;
                            } else {
                                if (this.mPendingIndex != this.mCurrentIndex) {
                                    setCurrentItem(this.mPendingIndex);
                                }
                                setupScrollBar();
                                return;
                            }
                        }
                    }
                }
                coordsX = this.mVisualWidth / 2;
                width = coordsX;
                int childCount = this.mChildWrapper.getChildCount();
                if (childCount != 0) {
                    coordsX -= this.mChildWrapper.getChildAt(0).getMeasuredWidth() / 2;
                    this.mStartCoord = coordsX;
                    width -= this.mChildWrapper.getChildAt(childCount - 1).getMeasuredWidth() / 2;
                }
                LayoutParams lpLeft = new LayoutParams(coordsX, 10);
                LayoutParams lpRight = new LayoutParams(width, 10);
                View gap_left = new View(getContext());
                View gap_right = new View(getContext());
                gap_left.setLayoutParams(lpLeft);
                gap_right.setLayoutParams(lpRight);
                this.mGapHolder[0] = gap_left;
                this.mGapHolder[1] = gap_right;
                notifyDatasetChanged();
                measure(widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    public void attachScrollIndicator(ScrollIndicator scrollIndicator) {
    }

    public void addScrollBar(ModeScrollBar bar) {
        this.mModeScrollBar = bar;
    }

    public void setupScrollBar() {
        if (this.mModeScrollBar != null && this.mChildWrapper != null) {
            int count = this.mChildWrapper.getChildCount();
            int i = 1;
            while (i < count - 1) {
                View v = this.mChildWrapper.getChildAt(i);
                if (v.getWidth() == 0) {
                    this.mItemWidthList.clear();
                    return;
                } else {
                    this.mItemWidthList.add(Integer.valueOf(v.getWidth()));
                    i++;
                }
            }
            this.mModeScrollBar.setItemWidth(this.mItemWidthList);
            if (this.mCurrentIndex != -1) {
                this.mModeScrollBar.setOriIndex(this.mCurrentIndex);
            }
        }
    }

    public void init(ModuleManager moduleManager) {
        setAdapter(new ModeStripViewAdapter(moduleManager));
    }

    public void pause() {
    }

    public void resume() {
    }

    public int lock() {
        return this.mMultiLock.aquireLock().intValue();
    }

    public boolean unlockWithToken(int token) {
        return this.mMultiLock.unlockWithToken(Integer.valueOf(token));
    }
}
