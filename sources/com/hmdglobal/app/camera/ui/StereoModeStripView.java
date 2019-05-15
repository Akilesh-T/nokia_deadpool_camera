package com.hmdglobal.app.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.OnCancelSelectionMenuListener;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.ModuleManager;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.ModeStrip.OnModeIdListener;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.LockUtils;
import com.hmdglobal.app.camera.util.LockUtils.Lock;
import com.hmdglobal.app.camera.util.LockUtils.LockType;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class StereoModeStripView extends FrameLayout implements ModeStrip {
    private static final int DIRECTION_LEFT = 1;
    private static final int DIRECTION_NONE = -1;
    private static final int DIRECTION_RIGHT = 2;
    private static final int FINAL_ANGLE = 55;
    private static final int FIRST_MODE = 0;
    private static final int MODE_CHANGE_ENSURE_DELAY = 100;
    private static int SCROLL_DELTA_X_THRESHOLD = 20;
    private static final int SCROLL_DURATION_LOWER = 150;
    private static final int SCROLL_DURATION_UPPER = 300;
    private static final float SCROLL_FACTOR = 2.5f;
    private static final float SCROLL_IMPACT_RATIO = 1.1f;
    private static final Tag TAG = new Tag("StereoModeStripView");
    private ModeStripViewAdapter mAdapter;
    private final AnimatorListener mAnimatorLifecycleListener = new AnimatorListener() {
        public void onAnimationStart(Animator animation) {
            Log.v(StereoModeStripView.TAG, "Start scrolling");
        }

        public void onAnimationRepeat(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
            Log.w(StereoModeStripView.TAG, "scroll animation end");
            if (StereoModeStripView.this.mInternalLock != -1) {
                StereoModeStripView.this.unLockView(Integer.valueOf(StereoModeStripView.this.mInternalLock));
                StereoModeStripView.this.mInternalLock = -1;
            }
            StereoModeStripView.this.mScrollAnimator.removeListener(StereoModeStripView.this.mAnimatorLifecycleListener);
            StereoModeStripView.this.mScrollAnimator.removeAllUpdateListeners();
            if (!StereoModeStripView.this.updateModesInHistory()) {
                StereoModeStripView.this.mScrollThreshold = StereoModeStripView.SCROLL_DELTA_X_THRESHOLD;
                if (StereoModeStripView.this.mListener != null) {
                    Log.w(StereoModeStripView.TAG, "posting scroll not effective, change mode");
                    StereoModeStripView.this.mModeIdChangedRunnable.setTargetModeId(StereoModeStripView.this.mCurrentModeIndex);
                    StereoModeStripView.this.mModeIdChangedRunnable.run();
                }
            }
        }

        public void onAnimationCancel(Animator animation) {
            if (StereoModeStripView.this.mInternalLock != -1) {
                StereoModeStripView.this.unLockView(Integer.valueOf(StereoModeStripView.this.mInternalLock));
                StereoModeStripView.this.mInternalLock = -1;
            }
            if (StereoModeStripView.this.mListener != null) {
                Log.w(StereoModeStripView.TAG, "posting scroll not effective, change mode");
                StereoModeStripView.this.mModeIdChangedRunnable.setTargetModeId(StereoModeStripView.this.mCurrentModeIndex);
                StereoModeStripView.this.mModeIdChangedRunnable.run();
            }
            StereoModeStripView.this.mScrollAnimator.removeListener(this);
            StereoModeStripView.this.mScrollAnimator.removeAllUpdateListeners();
        }
    };
    private AnimatorUpdateListener mAnimatorUpdateListener = new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            if (!StereoModeStripView.this.mNeedFreezeModeScroll) {
                StereoModeStripView.this.mTransX = ((Integer) animation.getAnimatedValue()).intValue();
            }
            StereoModeStripView.this.invalidate();
        }
    };
    private OnCancelSelectionMenuListener mCancelSelectionMenuListener;
    private List<Integer> mChildrenWidths = new ArrayList();
    private final int mColorNormal;
    private final int mColorSelected;
    private int mCurrentModeIndex = 0;
    private double mDeltaXInSingleTouch = Camera2ParamsFragment.TARGET_EV;
    private int mFutureIndexModule = -1;
    private int mIndicatorTransX;
    private int mInitialTransX;
    private int mInternalLock = -1;
    private boolean mIsFistMove = true;
    private boolean mIsSlideable = true;
    private boolean mIsWaitingForScroll = true;
    private OnModeIdListener mListener;
    private final ModeIdChangeRunnable mModeIdChangedRunnable = new ModeIdChangeRunnable() {
        public void run() {
            if (StereoModeStripView.this.mListener != null) {
                StereoModeStripView.this.mListener.onModeIdChanged((int) StereoModeStripView.this.mAdapter.getItemId(this.mTargetModeId));
            }
        }
    };
    private final int mModeStartPostion;
    private Lock mMultiLock;
    private final boolean mNeedFreezeModeScroll = CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_FIX_MODE_SWITCHING, false);
    private List<PointF> mOperationHistory = new ArrayList();
    private boolean mPaused = false;
    private List<ScrollHistory> mPendingIndexUpdateHistory = new LinkedList();
    private final int mScreenWidth;
    private ValueAnimator mScrollAnimator;
    private ScrollIndicator mScrollIndicator;
    private int mScrollThreshold = SCROLL_DELTA_X_THRESHOLD;
    private int mScrolllingDirection = -1;
    private RelativeLayout mStereoGroup;
    private final ConditionalRunnable mSwitchingRunnable = new ConditionalRunnable() {
        public void run() {
        }

        public void run(double deltaX, int duration) {
            if (Math.abs(deltaX) >= ((double) StereoModeStripView.this.mScrollThreshold) && StereoModeStripView.this.mIsSlideable) {
                Tag access$200 = StereoModeStripView.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("start scroll , scrollThreshold is ");
                stringBuilder.append(StereoModeStripView.this.mScrollThreshold);
                stringBuilder.append(" duration is ");
                stringBuilder.append(duration);
                stringBuilder.append(" deltaX is ");
                stringBuilder.append(deltaX);
                stringBuilder.append(" isLocked ?");
                stringBuilder.append(StereoModeStripView.this.isLocked());
                Log.w(access$200, stringBuilder.toString());
                StereoModeStripView.this.mScrollThreshold = 0;
                int currentIndex = StereoModeStripView.this.mCurrentModeIndex;
                Tag access$2002 = StereoModeStripView.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("currentIndex is ");
                stringBuilder2.append(currentIndex);
                Log.w(access$2002, stringBuilder2.toString());
                if (StereoModeStripView.this.mScrolllingDirection == 1 && StereoModeStripView.this.mCurrentModeIndex > 0) {
                    StereoModeStripView.this.mCurrentModeIndex = StereoModeStripView.this.mCurrentModeIndex - 1;
                }
                if (StereoModeStripView.this.mScrolllingDirection == 2 && StereoModeStripView.this.mCurrentModeIndex < StereoModeStripView.this.mAdapter.getCount() - 1) {
                    StereoModeStripView.this.mCurrentModeIndex = StereoModeStripView.this.mCurrentModeIndex + 1;
                }
                int targetIndex = StereoModeStripView.this.mCurrentModeIndex;
                Tag access$2003 = StereoModeStripView.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("targetIndex is ");
                stringBuilder3.append(targetIndex);
                Log.w(access$2003, stringBuilder3.toString());
                StereoModeStripView.this.mFutureIndexModule = targetIndex;
                if (currentIndex != targetIndex && StereoModeStripView.this.mAdapter != null && targetIndex >= 0 && targetIndex < StereoModeStripView.this.mAdapter.getCount()) {
                    StereoModeStripView.this.mListener.onModeIdChanging();
                    StereoModeStripView.this.switchToMode(currentIndex, targetIndex, duration);
                }
                StereoModeStripView.this.mScrolllingDirection = -1;
            }
        }

        public void run(double deltaX, int duration, int indexChangeset) {
            if (Math.abs(deltaX) >= ((double) StereoModeStripView.this.mScrollThreshold)) {
                int currentIndex = StereoModeStripView.this.mCurrentModeIndex;
                Tag access$200 = StereoModeStripView.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("currentIndex is ");
                stringBuilder.append(currentIndex);
                Log.w(access$200, stringBuilder.toString());
                StereoModeStripView.access$312(StereoModeStripView.this, indexChangeset);
                if (StereoModeStripView.this.mCurrentModeIndex < 0) {
                    StereoModeStripView.this.mCurrentModeIndex = 0;
                }
                if (StereoModeStripView.this.mCurrentModeIndex > StereoModeStripView.this.mAdapter.getCount() - 1) {
                    StereoModeStripView.this.mCurrentModeIndex = StereoModeStripView.this.mAdapter.getCount() - 1;
                }
                int targetIndex = StereoModeStripView.this.mCurrentModeIndex;
                Tag access$2002 = StereoModeStripView.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("targetIndex is ");
                stringBuilder2.append(targetIndex);
                Log.w(access$2002, stringBuilder2.toString());
                StereoModeStripView.this.mFutureIndexModule = targetIndex;
                if (currentIndex != targetIndex && StereoModeStripView.this.mAdapter != null && targetIndex >= 0 && targetIndex < StereoModeStripView.this.mAdapter.getCount()) {
                    StereoModeStripView.this.mListener.onModeIdChanging();
                    StereoModeStripView.this.switchToMode(currentIndex, targetIndex, duration);
                }
                StereoModeStripView.this.mScrolllingDirection = -1;
            }
        }
    };
    private int mTransX;
    private VelocityObserver mVelocityObserver = new VelocityObserver();
    private int mWidth;

    private interface ConditionalRunnable extends Runnable {
        void run(double d, int i);

        void run(double d, int i, int i2);
    }

    private abstract class ModeIdChangeRunnable implements Runnable {
        protected int mTargetModeId;

        private ModeIdChangeRunnable() {
        }

        /* synthetic */ ModeIdChangeRunnable(StereoModeStripView x0, AnonymousClass1 x1) {
            this();
        }

        public void setTargetModeId(int targetId) {
            this.mTargetModeId = targetId;
        }
    }

    private class ModeStripViewAdapter extends BaseAdapter {
        private int mDefaultModeIndex;
        List<ModuleHolder> mModules = new ArrayList();

        private class ModuleHolder {
            public int MODULE_INDEX;
            public String MODULE_NAME;

            /* synthetic */ ModuleHolder(ModeStripViewAdapter x0, int x1, AnonymousClass1 x2) {
                this(x1);
            }

            private ModuleHolder(int index) {
                this.MODULE_INDEX = index;
                this.MODULE_NAME = CameraUtil.getCameraModeText(index, StereoModeStripView.this.getContext());
                getModuleView();
            }

            public View getModuleView() {
                StereoRotateTextView view = (StereoRotateTextView) LayoutInflater.from(StereoModeStripView.this.getContext()).inflate(R.layout.stereo_text, null);
                view.setText(this.MODULE_NAME);
                return view;
            }
        }

        private class VdfModuleHolder extends ModuleHolder {
            public View mItem;
            public ImageView mItemImg;
            public TextView mItemName;
            public int width;

            /* synthetic */ VdfModuleHolder(ModeStripViewAdapter x0, int x1, AnonymousClass1 x2) {
                this(x1);
            }

            private VdfModuleHolder(int index) {
                super(ModeStripViewAdapter.this, index, null);
            }

            public View getModuleView() {
                this.mItem = LayoutInflater.from(StereoModeStripView.this.getContext()).inflate(R.layout.mode_item, null);
                this.mItemImg = (ImageView) this.mItem.findViewById(R.id.mode_item_img);
                this.mItemName = (TextView) this.mItem.findViewById(R.id.mode_item_name);
                this.mItemName.setText(this.MODULE_NAME);
                this.mItem.setTag(Integer.valueOf(this.MODULE_INDEX));
                return this.mItem;
            }
        }

        public ModeStripViewAdapter(ModuleManager manager) {
            int index;
            for (Integer index2 : manager.getSupportedModeIndexList()) {
                index = index2.intValue();
                if (manager.getModuleAgent(index).needAddToStrip()) {
                    if (StereoModeStripView.this.mNeedFreezeModeScroll) {
                        this.mModules.add(new VdfModuleHolder(this, index, null));
                    } else {
                        this.mModules.add(new ModuleHolder(this, index, null));
                    }
                    if (manager.getDefaultModuleIndex() == index) {
                        this.mDefaultModeIndex = this.mModules.size() - 1;
                        Tag access$200 = StereoModeStripView.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("default index is ");
                        stringBuilder.append(this.mDefaultModeIndex);
                        Log.w(access$200, stringBuilder.toString());
                    }
                }
            }
            if (StereoModeStripView.this.mNeedFreezeModeScroll) {
                int mModeWidth;
                index = (int) StereoModeStripView.this.getContext().getResources().getDimension(R.dimen.mode_item_divition);
                if (this.mModules.size() > 0) {
                    mModeWidth = (StereoModeStripView.this.mScreenWidth - ((this.mModules.size() - 1) * index)) / this.mModules.size();
                    for (int mModeWidth2 = 0; mModeWidth2 < this.mModules.size(); mModeWidth2++) {
                        ModuleHolder moduleHolder = (ModuleHolder) this.mModules.get(mModeWidth2);
                        if (moduleHolder != null && (moduleHolder instanceof VdfModuleHolder)) {
                            if (mModeWidth2 == 0) {
                                ((VdfModuleHolder) moduleHolder).mItemImg.setVisibility(4);
                                ((VdfModuleHolder) moduleHolder).width = mModeWidth;
                            } else {
                                ((VdfModuleHolder) moduleHolder).width = mModeWidth + index;
                            }
                        }
                    }
                } else {
                    mModeWidth = StereoModeStripView.this.mScreenWidth;
                }
                Tag access$2002 = StereoModeStripView.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mScreenWidth=");
                stringBuilder2.append(StereoModeStripView.this.mScreenWidth);
                stringBuilder2.append(", mModeWidth=");
                stringBuilder2.append(mModeWidth);
                stringBuilder2.append(", mModules.size()=");
                stringBuilder2.append(this.mModules.size());
                Log.d(access$2002, stringBuilder2.toString());
            }
        }

        public void setIndex(int index) {
            for (int i = 0; i < this.mModules.size(); i++) {
                ModuleHolder moduleHolder = (ModuleHolder) this.mModules.get(i);
                if (moduleHolder != null && (moduleHolder instanceof VdfModuleHolder)) {
                    if (index == i) {
                        ((VdfModuleHolder) moduleHolder).mItemName.setTextColor(StereoModeStripView.this.mColorSelected);
                    } else {
                        ((VdfModuleHolder) moduleHolder).mItemName.setTextColor(StereoModeStripView.this.mColorNormal);
                    }
                }
            }
        }

        public int getDefaultModeIndex() {
            return this.mDefaultModeIndex;
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

    private class ScrollHistory {
        double deltaX;
        int direction;
        float velocityX;

        private ScrollHistory() {
        }

        /* synthetic */ ScrollHistory(StereoModeStripView x0, AnonymousClass1 x1) {
            this();
        }
    }

    static /* synthetic */ int access$312(StereoModeStripView x0, int x1) {
        int i = x0.mCurrentModeIndex + x1;
        x0.mCurrentModeIndex = i;
        return i;
    }

    public void setOnCancelSelectionMenu(OnCancelSelectionMenuListener onCancelSelectionMenuListener) {
        this.mCancelSelectionMenuListener = onCancelSelectionMenuListener;
    }

    public StereoModeStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mStereoGroup = (RelativeLayout) ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.stereo_group_layout, null);
        this.mMultiLock = LockUtils.getInstance().generateMultiLock(LockType.MULTILOCK);
        SCROLL_DELTA_X_THRESHOLD = context.getResources().getDimensionPixelSize(R.dimen.modestrip_touch_delta_threshold);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("threshold is ");
        stringBuilder.append(SCROLL_DELTA_X_THRESHOLD);
        Log.w(tag, stringBuilder.toString());
        addView(this.mStereoGroup);
        setBackgroundColor(0);
        this.mColorNormal = context.getResources().getColor(R.color.mode_name_normal);
        this.mColorSelected = context.getResources().getColor(R.color.mode_name_selected);
        this.mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
        this.mModeStartPostion = (int) getResources().getDimension(R.dimen.mode_start_postion);
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mScrollAnimator != null && this.mScrollAnimator.isRunning()) {
            this.mScrollAnimator.end();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mAdapter != null) {
            this.mWidth = getMeasuredWidth();
            measureChildren(widthMeasureSpec, heightMeasureSpec);
            this.mChildrenWidths.clear();
            for (int i = 0; i < this.mStereoGroup.getChildCount(); i++) {
                this.mChildrenWidths.add(Integer.valueOf(this.mStereoGroup.getChildAt(i).getMeasuredWidth()));
            }
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stereoGroup size is ");
            stringBuilder.append(this.mStereoGroup.getChildCount());
            Log.w(tag, stringBuilder.toString());
            this.mInitialTransX = this.mWidth / 2;
            if (this.mNeedFreezeModeScroll) {
                this.mTransX = this.mModeStartPostion;
                this.mIndicatorTransX = getPositionX(this.mTransX, this.mCurrentModeIndex) - (this.mWidth / 2);
            } else {
                this.mTransX = getTargetTranslationX(this.mCurrentModeIndex);
            }
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mTransX is ");
            stringBuilder.append(this.mTransX);
            Log.w(tag, stringBuilder.toString());
            if (this.mScrollIndicator != null) {
                attachScrollIndicator(this.mScrollIndicator);
                if (this.mNeedFreezeModeScroll) {
                    this.mScrollIndicator.animateTrans(0, this.mStereoGroup.getChildAt(this.mCurrentModeIndex).getMeasuredWidth(), 0, this.mIndicatorTransX, -1);
                }
            }
            invalidate();
        }
    }

    private int getPositionX(int offset, int index) {
        int i = 0;
        if (this.mChildrenWidths.size() == 0) {
            return 0;
        }
        int sumPrevWidth = 0;
        while (i < index) {
            sumPrevWidth += ((Integer) this.mChildrenWidths.get(i)).intValue();
            i++;
        }
        return (offset + sumPrevWidth) + (((Integer) this.mChildrenWidths.get(index)).intValue() / 2);
    }

    private int mapPostionDegree(int position, int origin, int border) {
        if (position < origin) {
            return (55 * (origin - position)) / origin;
        }
        return (-55 * (position - origin)) / (border - origin);
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        if (this.mWidth == 0 || this.mInitialTransX == 0) {
            super.onDraw(canvas);
            return;
        }
        int indexOfChildClosestToCenter = 0;
        int indicatorTransX = 0;
        if (this.mScrollIndicator != null) {
            indicatorTransX = ((StereoScrollIndicatorView) this.mScrollIndicator).getIndicatorTransX();
        }
        float minAbsFromCenter = -1.0f;
        for (int i = 0; i < this.mStereoGroup.getChildCount(); i++) {
            View child = this.mStereoGroup.getChildAt(i);
            if (child != null) {
                int childPosition = getPositionX(this.mTransX, i);
                if (!this.mNeedFreezeModeScroll) {
                    ((StereoRotateTextView) child).rotateY(mapPostionDegree(childPosition, this.mInitialTransX, this.mWidth), childPosition - this.mInitialTransX < 0);
                }
                child.setTranslationX((float) (childPosition - (child.getMeasuredWidth() / 2)));
                float childFromCenter = (float) Math.abs((this.mWidth / 2) - childPosition);
                if (this.mNeedFreezeModeScroll) {
                    int gapFromIndicator = Math.abs(indicatorTransX - (childPosition - (this.mWidth / 2)));
                    if (minAbsFromCenter < 0.0f) {
                        minAbsFromCenter = (float) gapFromIndicator;
                    } else if (((float) gapFromIndicator) < minAbsFromCenter) {
                        minAbsFromCenter = (float) gapFromIndicator;
                        indexOfChildClosestToCenter = i;
                    }
                } else if (minAbsFromCenter < 0.0f) {
                    minAbsFromCenter = childFromCenter;
                } else if (childFromCenter < minAbsFromCenter) {
                    minAbsFromCenter = childFromCenter;
                    indexOfChildClosestToCenter = i;
                }
                if (!this.mNeedFreezeModeScroll) {
                    ((StereoRotateTextView) child).setTextColor(getResources().getColor(R.color.mode_name_text_color_unselected));
                }
            }
        }
        if (!this.mNeedFreezeModeScroll) {
            ((StereoRotateTextView) this.mStereoGroup.getChildAt(indexOfChildClosestToCenter)).setTextColor(getResources().getColor(R.color.mode_name_text_color_selected));
        } else if (this.mAdapter != null) {
            this.mAdapter.setIndex(this.mCurrentModeIndex);
        }
        super.onDraw(canvas);
    }

    public int getLastModuleId() {
        return this.mCurrentModeIndex;
    }

    public int getFutureModuel() {
        return this.mFutureIndexModule;
    }

    public void isSlideable(boolean slideable) {
        this.mIsSlideable = slideable;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[StereoModeStripView] onTouchEvent isLocked = ");
        stringBuilder.append(isLocked());
        stringBuilder.append(" action = ");
        stringBuilder.append(ev.getAction());
        stringBuilder.append(" isIgnoreLock = ");
        stringBuilder.append(isIgnoreLock());
        Log.d(tag, stringBuilder.toString());
        if (isLocked() && isIgnoreLock() && ev.getAction() == 1 && this.mCancelSelectionMenuListener != null) {
            this.mCancelSelectionMenuListener.onCancelSelectionMenu();
        }
        boolean isHorizontalMove = false;
        if (this.mPaused) {
            return false;
        }
        PointF lastCoord;
        double dx;
        double dy;
        Tag tag2;
        StringBuilder stringBuilder2;
        if (ev.getAction() == 0) {
            this.mOperationHistory.clear();
            this.mOperationHistory.add(new PointF(ev.getX(), ev.getY()));
        } else if (ev.getAction() == 2) {
            tag = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("ACTION_MOVE slideable = ");
            stringBuilder3.append(this.mIsSlideable);
            Log.d(tag, stringBuilder3.toString());
            this.mOperationHistory.add(new PointF(ev.getX(), ev.getY()));
            if (this.mOperationHistory.size() < 2) {
                this.mScrolllingDirection = -1;
                return true;
            }
            lastCoord = (PointF) this.mOperationHistory.get(this.mOperationHistory.size() - 2);
            dx = (double) (ev.getX() - lastCoord.x);
            dy = (double) (ev.getY() - lastCoord.y);
            tag2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("scroll orientation ratio is ");
            stringBuilder2.append(Math.abs(dx / dy));
            Log.v(tag2, stringBuilder2.toString());
            if ((Math.abs(dx / dy) >= 1.100000023841858d) && this.mIsFistMove) {
                this.mIsFistMove = false;
                if (!checkMovementDuringTouchEvent(dx, false)) {
                    return false;
                }
            }
            this.mScrolllingDirection = -1;
            Tag tag3 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("ScrollingDirection is ");
            stringBuilder4.append(this.mScrolllingDirection);
            Log.w(tag3, stringBuilder4.toString());
        } else if (ev.getAction() == 1) {
            Log.v(TAG, "on action up");
            this.mIsFistMove = true;
            profileHistoryOperation(this.mOperationHistory);
            if (this.mOperationHistory.size() == 1) {
                lastCoord = (PointF) this.mOperationHistory.get(0);
                dx = (double) (ev.getX() - lastCoord.x);
                dy = (double) (ev.getY() - lastCoord.y);
                tag2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("scroll orientation ratio is ");
                stringBuilder2.append(Math.abs(dx / dy));
                Log.v(tag2, stringBuilder2.toString());
                if (Math.abs(dx / dy) >= 1.100000023841858d) {
                    isHorizontalMove = true;
                }
                if (isHorizontalMove) {
                    checkMovementDuringTouchEvent(dx, true);
                }
            }
            this.mOperationHistory.clear();
            this.mScrolllingDirection = -1;
            this.mIsWaitingForScroll = true;
            this.mDeltaXInSingleTouch = Camera2ParamsFragment.TARGET_EV;
        } else if (ev.getAction() == 3) {
            Log.v(TAG, "on action cancle");
            this.mIsFistMove = true;
        }
        return true;
    }

    private boolean checkMovementDuringTouchEvent(double dx, boolean injectVelocity) {
        ConditionalRunnable switchRunnable = null;
        if (this.mDeltaXInSingleTouch == Camera2ParamsFragment.TARGET_EV) {
            this.mVelocityObserver.start();
        }
        this.mDeltaXInSingleTouch += dx;
        this.mVelocityObserver.record();
        int i = 2;
        if (this.mNeedFreezeModeScroll) {
            if (dx <= Camera2ParamsFragment.TARGET_EV) {
                i = 1;
            }
            this.mScrolllingDirection = i;
        } else {
            if (dx > Camera2ParamsFragment.TARGET_EV) {
                i = 1;
            }
            this.mScrolllingDirection = i;
        }
        if (this.mIsWaitingForScroll) {
            Log.v(TAG, "start one single scroll");
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deltaX is ");
            stringBuilder.append(this.mDeltaXInSingleTouch);
            Log.w(tag, stringBuilder.toString());
            postScrolling(this.mScrolllingDirection, injectVelocity ? 0.0f : this.mVelocityObserver.getVelocityX((float) Math.abs(this.mDeltaXInSingleTouch)), this.mDeltaXInSingleTouch);
        }
        if (this.mListener != null) {
            switchRunnable = this.mSwitchingRunnable;
        }
        if (isLocked()) {
            return false;
        }
        if (switchRunnable != null) {
            Log.w(TAG, "clear directions");
            this.mPendingIndexUpdateHistory.clear();
            int duration = 300;
            float velocityX = this.mVelocityObserver.getVelocityX((float) Math.abs(this.mDeltaXInSingleTouch));
            if (velocityX != 0.0f) {
                duration = trimDuration((int) ((((float) ((Integer) this.mChildrenWidths.get(this.mCurrentModeIndex)).intValue()) / velocityX) * SCROLL_FACTOR));
            }
            switchRunnable.run(this.mDeltaXInSingleTouch, duration);
        }
        return true;
    }

    private void profileHistoryOperation(List<PointF> operationHistory) {
        StringBuilder stringBuilder = new StringBuilder("operation history : ");
        for (PointF p : operationHistory) {
            stringBuilder.append(p.x);
            stringBuilder.append(Size.DELIMITER);
            stringBuilder.append(p.y);
            stringBuilder.append("; ");
        }
        Log.v(TAG, stringBuilder.toString());
    }

    private int calculateUpdateIndex() {
        if (this.mPendingIndexUpdateHistory.size() <= 1) {
            return 0;
        }
        int index = 0;
        for (ScrollHistory history : this.mPendingIndexUpdateHistory) {
            if (history.direction == 1) {
                index--;
            } else if (history.direction == 2) {
                index++;
            }
        }
        if (index != 0) {
            Log.w(TAG, "clear directions to boost swiping");
            this.mPendingIndexUpdateHistory.clear();
        }
        return index;
    }

    public void attachScrollIndicator(ScrollIndicator scrollIndicator) {
        if (scrollIndicator == null) {
            Log.w(TAG, "attachScrollIndicator = null");
            return;
        }
        this.mScrollIndicator = scrollIndicator;
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("currentModeIndex is ");
        stringBuilder.append(this.mCurrentModeIndex);
        Log.w(tag, stringBuilder.toString());
        if (!(this.mWidth == 0 || this.mStereoGroup.getChildCount() == 0)) {
            int index = this.mCurrentModeIndex;
            if (this.mCurrentModeIndex < 0 || this.mCurrentModeIndex >= this.mStereoGroup.getChildCount()) {
                index = 0;
            }
            this.mScrollIndicator.initializeWidth(this.mStereoGroup.getChildAt(index).getMeasuredWidth());
        }
    }

    public void setCurrentModeWithModeIndex(int modeId) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set ");
        stringBuilder.append(modeId);
        stringBuilder.append(" to be current mode");
        Log.w(tag, stringBuilder.toString());
        if (this.mAdapter != null) {
            int targetIndex = -1;
            for (int i = 0; i < this.mAdapter.getCount(); i++) {
                if (this.mAdapter.getItemId(i) == ((long) modeId)) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex != -1) {
                switchToMode(this.mCurrentModeIndex, targetIndex, 150);
            }
            this.mCurrentModeIndex = targetIndex;
        }
    }

    public void initDefaultMode(int modeId) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set ");
        stringBuilder.append(modeId);
        stringBuilder.append(" to be current mode");
        Log.w(tag, stringBuilder.toString());
        if (this.mAdapter != null) {
            int targetIndex = -1;
            for (int i = 0; i < this.mAdapter.getCount(); i++) {
                if (this.mAdapter.getItemId(i) == ((long) modeId)) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex != -1) {
                switchToMode(this.mCurrentModeIndex, targetIndex, 0);
            }
            this.mCurrentModeIndex = targetIndex;
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0054, code skipped:
            return;
     */
    public void postScrolling(int r5, float r6, double r7) {
        /*
        r4 = this;
        r0 = -1;
        if (r5 < r0) goto L_0x0054;
    L_0x0003:
        r1 = 2;
        if (r5 <= r1) goto L_0x0007;
    L_0x0006:
        goto L_0x0054;
    L_0x0007:
        if (r5 == r0) goto L_0x0053;
    L_0x0009:
        r0 = 0;
        r4.mIsWaitingForScroll = r0;
        r0 = TAG;
        r1 = "cancel wait for scroll";
        com.hmdglobal.app.camera.debug.Log.v(r0, r1);
        r0 = TAG;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "add direction ";
        r1.append(r2);
        r1.append(r5);
        r1 = r1.toString();
        com.hmdglobal.app.camera.debug.Log.w(r0, r1);
        r0 = new com.hmdglobal.app.camera.ui.StereoModeStripView$ScrollHistory;
        r1 = 0;
        r0.<init>(r4, r1);
        r0.direction = r5;
        r0.velocityX = r6;
        r0.deltaX = r7;
        r1 = TAG;
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "velocity is ";
        r2.append(r3);
        r3 = 1148846080; // 0x447a0000 float:1000.0 double:5.676053805E-315;
        r3 = r3 * r6;
        r2.append(r3);
        r2 = r2.toString();
        com.hmdglobal.app.camera.debug.Log.w(r1, r2);
        r1 = r4.mPendingIndexUpdateHistory;
        r1.add(r0);
    L_0x0053:
        return;
    L_0x0054:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ui.StereoModeStripView.postScrolling(int, float, double):void");
    }

    /* JADX WARNING: Missing block: B:27:0x011b, code skipped:
            return;
     */
    private void switchToMode(int r20, int r21, int r22) {
        /*
        r19 = this;
        r0 = r19;
        r1 = r20;
        r2 = r21;
        r9 = r22;
        r3 = TAG;
        r4 = "from %d to %d";
        r10 = 2;
        r5 = new java.lang.Object[r10];
        r6 = java.lang.Integer.valueOf(r20);
        r11 = 0;
        r5[r11] = r6;
        r6 = java.lang.Integer.valueOf(r21);
        r12 = 1;
        r5[r12] = r6;
        r4 = java.lang.String.format(r4, r5);
        com.hmdglobal.app.camera.debug.Log.w(r3, r4);
        if (r1 == r2) goto L_0x011b;
    L_0x0026:
        r3 = r0.mAdapter;
        if (r3 != 0) goto L_0x002c;
    L_0x002a:
        goto L_0x011b;
    L_0x002c:
        if (r2 < 0) goto L_0x011a;
    L_0x002e:
        r3 = r0.mAdapter;
        r3 = r3.getCount();
        if (r2 >= r3) goto L_0x011a;
    L_0x0036:
        r3 = r0.mWidth;
        if (r3 != 0) goto L_0x0064;
    L_0x003a:
        r0.mCurrentModeIndex = r2;
        r3 = TAG;
        r4 = "width ==0 , start modeChanged";
        com.hmdglobal.app.camera.debug.Log.w(r3, r4);
        r3 = r0.mScrollIndicator;
        if (r3 == 0) goto L_0x004c;
    L_0x0047:
        r3 = r0.mScrollIndicator;
        r0.attachScrollIndicator(r3);
    L_0x004c:
        r3 = r0.mListener;
        if (r3 == 0) goto L_0x0063;
    L_0x0050:
        r3 = TAG;
        r4 = "posting scroll not effective, change mode";
        com.hmdglobal.app.camera.debug.Log.w(r3, r4);
        r3 = r0.mModeIdChangedRunnable;
        r4 = r0.mCurrentModeIndex;
        r3.setTargetModeId(r4);
        r3 = r0.mModeIdChangedRunnable;
        r3.run();
    L_0x0063:
        return;
    L_0x0064:
        r13 = r19.getTargetTranslationX(r20);
        r3 = TAG;
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = " start scroll animation ,currentTransX is ";
        r4.append(r5);
        r4.append(r13);
        r4 = r4.toString();
        com.hmdglobal.app.camera.debug.Log.w(r3, r4);
        r14 = r0.getTargetTranslationX(r2);
        r3 = r0.mScrollIndicator;
        if (r3 == 0) goto L_0x00ce;
    L_0x0086:
        r3 = r0.mStereoGroup;
        r15 = r3.getChildAt(r1);
        r3 = r0.mStereoGroup;
        r8 = r3.getChildAt(r2);
        r7 = r15.getMeasuredWidth();
        r6 = r8.getMeasuredWidth();
        r3 = r0.mNeedFreezeModeScroll;
        if (r3 == 0) goto L_0x00c5;
    L_0x009e:
        r3 = r0.mTransX;
        r3 = r0.getPositionX(r3, r1);
        r4 = r0.mWidth;
        r4 = r4 / r10;
        r16 = r3 - r4;
        r3 = r0.mTransX;
        r3 = r0.getPositionX(r3, r2);
        r4 = r0.mWidth;
        r4 = r4 / r10;
        r17 = r3 - r4;
        r3 = r0.mScrollIndicator;
        r4 = r7;
        r5 = r6;
        r12 = r6;
        r6 = r16;
        r11 = r7;
        r7 = r17;
        r18 = r8;
        r8 = r9;
        r3.animateTrans(r4, r5, r6, r7, r8);
        goto L_0x00ce;
    L_0x00c5:
        r12 = r6;
        r11 = r7;
        r18 = r8;
        r3 = r0.mScrollIndicator;
        r3.animateWidth(r11, r12, r9);
    L_0x00ce:
        r3 = new int[r10];
        r4 = 0;
        r3[r4] = r13;
        r4 = 1;
        r3[r4] = r14;
        r3 = android.animation.ValueAnimator.ofInt(r3);
        r0.mScrollAnimator = r3;
        r3 = r0.mScrollAnimator;
        r4 = (long) r9;
        r3.setDuration(r4);
        r3 = TAG;
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "duration is ";
        r4.append(r5);
        r4.append(r9);
        r4 = r4.toString();
        com.hmdglobal.app.camera.debug.Log.w(r3, r4);
        r3 = r0.mScrollAnimator;
        r4 = r0.mAnimatorUpdateListener;
        r3.addUpdateListener(r4);
        r3 = r0.mScrollAnimator;
        r4 = r0.mAnimatorLifecycleListener;
        r3.addListener(r4);
        r3 = r0.mInternalLock;
        r4 = -1;
        if (r3 != r4) goto L_0x0115;
    L_0x010b:
        r3 = r19.lockView();
        r3 = r3.intValue();
        r0.mInternalLock = r3;
    L_0x0115:
        r3 = r0.mScrollAnimator;
        r3.start();
    L_0x011a:
        return;
    L_0x011b:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ui.StereoModeStripView.switchToMode(int, int, int):void");
    }

    private void switchToMode(int fromIndex, int toIndex) {
        switchToMode(fromIndex, toIndex, 300);
    }

    private int getTargetTranslationX(int currentModeIndex) {
        int i = 0;
        if (currentModeIndex >= this.mChildrenWidths.size() || currentModeIndex < 0) {
            return 0;
        }
        int transX = (getMeasuredWidth() / 2) - (((Integer) this.mChildrenWidths.get(currentModeIndex)).intValue() / 2);
        while (i < currentModeIndex) {
            transX -= ((Integer) this.mChildrenWidths.get(i)).intValue();
            i++;
        }
        return transX;
    }

    public void setAdapter(Adapter adapter) {
        this.mAdapter = (ModeStripViewAdapter) adapter;
        notifyDatasetChanged();
    }

    public void notifyDatasetChanged() {
        if (this.mAdapter != null) {
            int i = 0;
            int i2;
            if (this.mNeedFreezeModeScroll) {
                while (true) {
                    i2 = i;
                    if (i2 >= this.mAdapter.getCount()) {
                        break;
                    }
                    VdfModuleHolder holder = (VdfModuleHolder) this.mAdapter.getItem(i2);
                    if (holder != null && (holder instanceof VdfModuleHolder)) {
                        holder.mItem.setOnClickListener(new OnClickListener() {
                            public void onClick(View view) {
                                Tag access$200 = StereoModeStripView.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("[StereoModeStripView] holder.mItem.onCLick isLocked = ");
                                stringBuilder.append(StereoModeStripView.this.isLocked());
                                stringBuilder.append(" isIgnoreLock = ");
                                stringBuilder.append(StereoModeStripView.this.isIgnoreLock());
                                Log.d(access$200, stringBuilder.toString());
                                if (StereoModeStripView.this.isLocked()) {
                                    if (StereoModeStripView.this.isIgnoreLock() && StereoModeStripView.this.mCancelSelectionMenuListener != null) {
                                        StereoModeStripView.this.mCancelSelectionMenuListener.onCancelSelectionMenu();
                                    }
                                } else if (!StereoModeStripView.this.mPaused) {
                                    if (StereoModeStripView.this.mListener != null) {
                                        int currentMode = (int) StereoModeStripView.this.mAdapter.getItemId(StereoModeStripView.this.mCurrentModeIndex);
                                        int selectingMode = ((Integer) view.getTag()).intValue();
                                        if (currentMode != selectingMode) {
                                            StereoModeStripView.this.mFutureIndexModule = selectingMode;
                                            StereoModeStripView.this.mListener.onModeIdChanging();
                                        } else {
                                            return;
                                        }
                                    }
                                    StereoModeStripView.this.setCurrentModeWithModeIndex(((Integer) view.getTag()).intValue());
                                }
                            }
                        });
                        LayoutParams lp = new LayoutParams(holder.width, -2);
                        if (i2 == 0) {
                            int gap = (int) getContext().getResources().getDimension(R.dimen.mode_item_divition);
                            int marginLeft = (int) getContext().getResources().getDimension(R.dimen.first_mode_margin_left);
                            TextView tx = holder.mItemName;
                            LinearLayout.LayoutParams txlp = (LinearLayout.LayoutParams) tx.getLayoutParams();
                            txlp.setMargins(marginLeft, txlp.topMargin, gap, txlp.bottomMargin);
                            tx.setLayoutParams(txlp);
                        }
                        this.mStereoGroup.addView(holder.mItem, lp);
                        this.mAdapter.setIndex(this.mCurrentModeIndex);
                    }
                    i = i2 + 1;
                }
            } else {
                while (true) {
                    i2 = i;
                    if (i2 >= this.mAdapter.getCount()) {
                        break;
                    }
                    if (this.mAdapter.getView(i2, null, null) instanceof StereoRotateTextView) {
                        StereoRotateTextView stereoText = (StereoRotateTextView) this.mAdapter.getView(i2, null, null);
                        stereoText.setMaxRotation(55);
                        stereoText.setTag(Integer.valueOf((int) this.mAdapter.getItemId(i2)));
                        stereoText.setOnClickListener(new OnClickListener() {
                            public void onClick(View view) {
                                Tag access$200 = StereoModeStripView.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("[StereoModeStripView] stereoText.onClick isLocked = ");
                                stringBuilder.append(StereoModeStripView.this.isLocked());
                                stringBuilder.append(" isIgnoreLock = ");
                                stringBuilder.append(StereoModeStripView.this.isIgnoreLock());
                                Log.d(access$200, stringBuilder.toString());
                                if (StereoModeStripView.this.isLocked()) {
                                    if (StereoModeStripView.this.isIgnoreLock() && StereoModeStripView.this.mCancelSelectionMenuListener != null) {
                                        StereoModeStripView.this.mCancelSelectionMenuListener.onCancelSelectionMenu();
                                    }
                                    return;
                                }
                                if (StereoModeStripView.this.mListener != null) {
                                    int currentMode = (int) StereoModeStripView.this.mAdapter.getItemId(StereoModeStripView.this.mCurrentModeIndex);
                                    int selectingMode = ((Integer) view.getTag()).intValue();
                                    if (currentMode != selectingMode) {
                                        StereoModeStripView.this.mFutureIndexModule = selectingMode;
                                        StereoModeStripView.this.mListener.onModeIdChanging();
                                    } else {
                                        return;
                                    }
                                }
                                StereoModeStripView.this.setCurrentModeWithModeIndex(((Integer) view.getTag()).intValue());
                            }
                        });
                        this.mStereoGroup.addView(stereoText);
                    }
                    i = i2 + 1;
                }
                invalidate();
            }
        }
    }

    public void setModeIndexChangeListener(OnModeIdListener listener) {
        this.mListener = listener;
    }

    public Integer lockView() {
        return this.mMultiLock.aquireLock();
    }

    public boolean unLockView(Integer token) {
        boolean result = this.mMultiLock.unlockWithToken(token);
        onLockStateUpdate();
        return result;
    }

    public void init(ModuleManager moduleManager) {
        setAdapter(new ModeStripViewAdapter(moduleManager));
    }

    private void onLockStateUpdate() {
        if (!isLocked()) {
            post(new Runnable() {
                public void run() {
                    if (!StereoModeStripView.this.isLocked() && StereoModeStripView.this.mPendingIndexUpdateHistory.size() > 0 && !StereoModeStripView.this.mPaused) {
                        StereoModeStripView.this.mListener.onModeIdChanging();
                        StereoModeStripView.this.updateModesInHistory();
                    }
                }
            });
        }
    }

    private boolean updateModesInHistory() {
        Tag tag;
        int direction = -1;
        int animatorDuration = 300;
        int indexChangeset = 0;
        double deltaX = Camera2ParamsFragment.TARGET_EV;
        if (this.mPendingIndexUpdateHistory.size() > 0 && !this.mPaused) {
            indexChangeset = calculateUpdateIndex();
            if (indexChangeset == 0) {
                ScrollHistory scrollHistory = (ScrollHistory) this.mPendingIndexUpdateHistory.remove(0);
                direction = scrollHistory.direction;
                int width = ((Integer) this.mChildrenWidths.get(this.mCurrentModeIndex)).intValue();
                if (scrollHistory.velocityX == 0.0f) {
                    animatorDuration = 150;
                } else {
                    animatorDuration = trimDuration((int) ((((float) width) * SCROLL_FACTOR) / scrollHistory.velocityX));
                }
                Tag tag2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("animatorDuration is ");
                stringBuilder.append(animatorDuration);
                Log.w(tag2, stringBuilder.toString());
                deltaX = scrollHistory.deltaX;
            } else {
                if (this.mNeedFreezeModeScroll) {
                    direction = indexChangeset > 0 ? true : true;
                } else {
                    direction = indexChangeset > 0 ? true : true;
                }
                animatorDuration = 300;
                deltaX = (double) SCROLL_DELTA_X_THRESHOLD;
            }
            tag = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("pending direction is ");
            stringBuilder2.append(direction);
            Log.w(tag, stringBuilder2.toString());
        }
        if (direction == -1) {
            return false;
        }
        this.mScrolllingDirection = direction;
        if (this.mScrolllingDirection == 1 && this.mCurrentModeIndex <= 0) {
            this.mScrolllingDirection = -1;
            if (this.mListener != null) {
                Log.w(TAG, "posting scroll not effective, change mode");
                this.mModeIdChangedRunnable.setTargetModeId(this.mCurrentModeIndex);
                this.mModeIdChangedRunnable.run();
            }
            return true;
        } else if (this.mScrolllingDirection != 2 || this.mCurrentModeIndex < this.mAdapter.getCount() - 1) {
            tag = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("continue to scroll , direction is ");
            stringBuilder3.append(this.mScrolllingDirection);
            stringBuilder3.append(" currentIndex is ");
            stringBuilder3.append(this.mCurrentModeIndex);
            Log.w(tag, stringBuilder3.toString());
            this.mScrollThreshold = 0;
            if (indexChangeset != 0) {
                this.mSwitchingRunnable.run(deltaX, animatorDuration, indexChangeset);
            } else {
                this.mSwitchingRunnable.run(deltaX, animatorDuration);
            }
            this.mScrollThreshold = SCROLL_DELTA_X_THRESHOLD;
            return true;
        } else {
            this.mScrolllingDirection = -1;
            if (this.mListener != null) {
                Log.w(TAG, "posting scroll not effective, change mode");
                this.mModeIdChangedRunnable.setTargetModeId(this.mCurrentModeIndex);
                this.mModeIdChangedRunnable.run();
            }
            return true;
        }
    }

    public boolean isLocked() {
        return this.mMultiLock.isLocked();
    }

    public void lockSelf() {
        this.mMultiLock.aquireLock(hashCode());
    }

    public void unLockSelf() {
        this.mMultiLock.unlockWithToken(Integer.valueOf(hashCode()));
        onLockStateUpdate();
    }

    public int lock() {
        return this.mMultiLock.aquireLock().intValue();
    }

    public boolean unlockWithToken(int token) {
        return this.mMultiLock.unlockWithToken(Integer.valueOf(token));
    }

    public boolean isIgnoreLock() {
        return this.mMultiLock.isIgnoreLock();
    }

    public void pause() {
        Log.w(TAG, "mode strip pause");
        this.mPaused = true;
        this.mScrollThreshold = SCROLL_DELTA_X_THRESHOLD;
        this.mPendingIndexUpdateHistory.clear();
    }

    public void resume() {
        Log.w(TAG, "mode strip resume");
        this.mPaused = false;
        this.mScrollThreshold = SCROLL_DELTA_X_THRESHOLD;
    }

    private int trimDuration(int duration) {
        if (duration > 300) {
            return 300;
        }
        return duration < 150 ? 150 : duration;
    }

    public boolean isPaused() {
        return this.mPaused;
    }
}
