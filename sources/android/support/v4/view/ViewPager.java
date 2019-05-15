package android.support.v4.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.ClassLoaderCreator;
import android.os.Parcelable.Creator;
import android.os.SystemClock;
import android.support.annotation.CallSuper;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;
import android.widget.Scroller;
import com.google.common.primitives.Ints;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ViewPager extends ViewGroup {
    private static final int CLOSE_ENOUGH = 2;
    private static final Comparator<ItemInfo> COMPARATOR = new Comparator<ItemInfo>() {
        public int compare(ItemInfo lhs, ItemInfo rhs) {
            return lhs.position - rhs.position;
        }
    };
    private static final boolean DEBUG = false;
    private static final int DEFAULT_GUTTER_SIZE = 16;
    private static final int DEFAULT_OFFSCREEN_PAGES = 1;
    private static final int DRAW_ORDER_DEFAULT = 0;
    private static final int DRAW_ORDER_FORWARD = 1;
    private static final int DRAW_ORDER_REVERSE = 2;
    private static final int INVALID_POINTER = -1;
    static final int[] LAYOUT_ATTRS = new int[]{16842931};
    private static final int MAX_SETTLE_DURATION = 600;
    private static final int MIN_DISTANCE_FOR_FLING = 25;
    private static final int MIN_FLING_VELOCITY = 400;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_SETTLING = 2;
    private static final String TAG = "ViewPager";
    private static final boolean USE_CACHE = false;
    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return ((((t * t) * t) * t) * t) + 1.0f;
        }
    };
    private static final ViewPositionComparator sPositionComparator = new ViewPositionComparator();
    private int mActivePointerId = -1;
    PagerAdapter mAdapter;
    private List<OnAdapterChangeListener> mAdapterChangeListeners;
    private int mBottomPageBounds;
    private boolean mCalledSuper;
    private int mChildHeightMeasureSpec;
    private int mChildWidthMeasureSpec;
    private int mCloseEnough;
    int mCurItem;
    private int mDecorChildCount;
    private int mDefaultGutterSize;
    private int mDrawingOrder;
    private ArrayList<View> mDrawingOrderedChildren;
    private final Runnable mEndScrollRunnable = new Runnable() {
        public void run() {
            ViewPager.this.setScrollState(0);
            ViewPager.this.populate();
        }
    };
    private int mExpectedAdapterCount;
    private long mFakeDragBeginTime;
    private boolean mFakeDragging;
    private boolean mFirstLayout = true;
    private float mFirstOffset = -3.4028235E38f;
    private int mFlingDistance;
    private int mGutterSize;
    private boolean mInLayout;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private OnPageChangeListener mInternalPageChangeListener;
    private boolean mIsBeingDragged;
    private boolean mIsScrollStarted;
    private boolean mIsUnableToDrag;
    private final ArrayList<ItemInfo> mItems = new ArrayList();
    private float mLastMotionX;
    private float mLastMotionY;
    private float mLastOffset = Float.MAX_VALUE;
    private EdgeEffect mLeftEdge;
    private Drawable mMarginDrawable;
    private int mMaximumVelocity;
    private int mMinimumVelocity;
    private boolean mNeedCalculatePageOffsets = false;
    private PagerObserver mObserver;
    private int mOffscreenPageLimit = 1;
    private OnPageChangeListener mOnPageChangeListener;
    private List<OnPageChangeListener> mOnPageChangeListeners;
    private int mPageMargin;
    private PageTransformer mPageTransformer;
    private int mPageTransformerLayerType;
    private boolean mPopulatePending;
    private Parcelable mRestoredAdapterState = null;
    private ClassLoader mRestoredClassLoader = null;
    private int mRestoredCurItem = -1;
    private EdgeEffect mRightEdge;
    private int mScrollState = 0;
    private Scroller mScroller;
    private boolean mScrollingCacheEnabled;
    private final ItemInfo mTempItem = new ItemInfo();
    private final Rect mTempRect = new Rect();
    private int mTopPageBounds;
    private int mTouchSlop;
    private VelocityTracker mVelocityTracker;

    @Inherited
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DecorView {
    }

    static class ItemInfo {
        Object object;
        float offset;
        int position;
        boolean scrolling;
        float widthFactor;

        ItemInfo() {
        }
    }

    public static class LayoutParams extends android.view.ViewGroup.LayoutParams {
        int childIndex;
        public int gravity;
        public boolean isDecor;
        boolean needsMeasure;
        int position;
        float widthFactor = 0.0f;

        public LayoutParams() {
            super(-1, -1);
        }

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs, ViewPager.LAYOUT_ATTRS);
            this.gravity = a.getInteger(0, 48);
            a.recycle();
        }
    }

    public interface OnAdapterChangeListener {
        void onAdapterChanged(@NonNull ViewPager viewPager, @Nullable PagerAdapter pagerAdapter, @Nullable PagerAdapter pagerAdapter2);
    }

    public interface OnPageChangeListener {
        void onPageScrollStateChanged(int i);

        void onPageScrolled(int i, float f, @Px int i2);

        void onPageSelected(int i);
    }

    public interface PageTransformer {
        void transformPage(@NonNull View view, float f);
    }

    private class PagerObserver extends DataSetObserver {
        PagerObserver() {
        }

        public void onChanged() {
            ViewPager.this.dataSetChanged();
        }

        public void onInvalidated() {
            ViewPager.this.dataSetChanged();
        }
    }

    static class ViewPositionComparator implements Comparator<View> {
        ViewPositionComparator() {
        }

        public int compare(View lhs, View rhs) {
            LayoutParams llp = (LayoutParams) lhs.getLayoutParams();
            LayoutParams rlp = (LayoutParams) rhs.getLayoutParams();
            if (llp.isDecor == rlp.isDecor) {
                return llp.position - rlp.position;
            }
            return llp.isDecor ? 1 : -1;
        }
    }

    class MyAccessibilityDelegate extends AccessibilityDelegateCompat {
        MyAccessibilityDelegate() {
        }

        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);
            event.setClassName(ViewPager.class.getName());
            event.setScrollable(canScroll());
            if (event.getEventType() == 4096 && ViewPager.this.mAdapter != null) {
                event.setItemCount(ViewPager.this.mAdapter.getCount());
                event.setFromIndex(ViewPager.this.mCurItem);
                event.setToIndex(ViewPager.this.mCurItem);
            }
        }

        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(ViewPager.class.getName());
            info.setScrollable(canScroll());
            if (ViewPager.this.canScrollHorizontally(1)) {
                info.addAction(4096);
            }
            if (ViewPager.this.canScrollHorizontally(-1)) {
                info.addAction(8192);
            }
        }

        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            if (super.performAccessibilityAction(host, action, args)) {
                return true;
            }
            if (action != 4096) {
                if (action != 8192 || !ViewPager.this.canScrollHorizontally(-1)) {
                    return false;
                }
                ViewPager.this.setCurrentItem(ViewPager.this.mCurItem - 1);
                return true;
            } else if (!ViewPager.this.canScrollHorizontally(1)) {
                return false;
            } else {
                ViewPager.this.setCurrentItem(ViewPager.this.mCurItem + 1);
                return true;
            }
        }

        private boolean canScroll() {
            return ViewPager.this.mAdapter != null && ViewPager.this.mAdapter.getCount() > 1;
        }
    }

    public static class SavedState extends AbsSavedState {
        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        Parcelable adapterState;
        ClassLoader loader;
        int position;

        public SavedState(@NonNull Parcelable superState) {
            super(superState);
        }

        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.position);
            out.writeParcelable(this.adapterState, flags);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("FragmentPager.SavedState{");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
            stringBuilder.append(" position=");
            stringBuilder.append(this.position);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            if (loader == null) {
                loader = getClass().getClassLoader();
            }
            this.position = in.readInt();
            this.adapterState = in.readParcelable(loader);
            this.loader = loader;
        }
    }

    public static class SimpleOnPageChangeListener implements OnPageChangeListener {
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
        }

        public void onPageScrollStateChanged(int state) {
        }
    }

    public ViewPager(@NonNull Context context) {
        super(context);
        initViewPager();
    }

    public ViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initViewPager();
    }

    /* Access modifiers changed, original: 0000 */
    public void initViewPager() {
        setWillNotDraw(false);
        setDescendantFocusability(262144);
        setFocusable(true);
        Context context = getContext();
        this.mScroller = new Scroller(context, sInterpolator);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        float density = context.getResources().getDisplayMetrics().density;
        this.mTouchSlop = configuration.getScaledPagingTouchSlop();
        this.mMinimumVelocity = (int) (400.0f * density);
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mLeftEdge = new EdgeEffect(context);
        this.mRightEdge = new EdgeEffect(context);
        this.mFlingDistance = (int) (25.0f * density);
        this.mCloseEnough = (int) (2.0f * density);
        this.mDefaultGutterSize = (int) (16.0f * density);
        ViewCompat.setAccessibilityDelegate(this, new MyAccessibilityDelegate());
        if (ViewCompat.getImportantForAccessibility(this) == 0) {
            ViewCompat.setImportantForAccessibility(this, 1);
        }
        ViewCompat.setOnApplyWindowInsetsListener(this, new OnApplyWindowInsetsListener() {
            private final Rect mTempRect = new Rect();

            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat originalInsets) {
                WindowInsetsCompat applied = ViewCompat.onApplyWindowInsets(v, originalInsets);
                if (applied.isConsumed()) {
                    return applied;
                }
                Rect res = this.mTempRect;
                res.left = applied.getSystemWindowInsetLeft();
                res.top = applied.getSystemWindowInsetTop();
                res.right = applied.getSystemWindowInsetRight();
                res.bottom = applied.getSystemWindowInsetBottom();
                int count = ViewPager.this.getChildCount();
                for (int i = 0; i < count; i++) {
                    WindowInsetsCompat childInsets = ViewCompat.dispatchApplyWindowInsets(ViewPager.this.getChildAt(i), applied);
                    res.left = Math.min(childInsets.getSystemWindowInsetLeft(), res.left);
                    res.top = Math.min(childInsets.getSystemWindowInsetTop(), res.top);
                    res.right = Math.min(childInsets.getSystemWindowInsetRight(), res.right);
                    res.bottom = Math.min(childInsets.getSystemWindowInsetBottom(), res.bottom);
                }
                return applied.replaceSystemWindowInsets(res.left, res.top, res.right, res.bottom);
            }
        });
    }

    /* Access modifiers changed, original: protected */
    public void onDetachedFromWindow() {
        removeCallbacks(this.mEndScrollRunnable);
        if (!(this.mScroller == null || this.mScroller.isFinished())) {
            this.mScroller.abortAnimation();
        }
        super.onDetachedFromWindow();
    }

    /* Access modifiers changed, original: 0000 */
    public void setScrollState(int newState) {
        if (this.mScrollState != newState) {
            this.mScrollState = newState;
            if (this.mPageTransformer != null) {
                enableLayers(newState != 0);
            }
            dispatchOnScrollStateChanged(newState);
        }
    }

    public void setAdapter(@Nullable PagerAdapter adapter) {
        if (this.mAdapter != null) {
            this.mAdapter.setViewPagerObserver(null);
            this.mAdapter.startUpdate((ViewGroup) this);
            for (int i = 0; i < this.mItems.size(); i++) {
                ItemInfo ii = (ItemInfo) this.mItems.get(i);
                this.mAdapter.destroyItem((ViewGroup) this, ii.position, ii.object);
            }
            this.mAdapter.finishUpdate((ViewGroup) this);
            this.mItems.clear();
            removeNonDecorViews();
            this.mCurItem = 0;
            scrollTo(0, 0);
        }
        PagerAdapter oldAdapter = this.mAdapter;
        this.mAdapter = adapter;
        this.mExpectedAdapterCount = 0;
        if (this.mAdapter != null) {
            if (this.mObserver == null) {
                this.mObserver = new PagerObserver();
            }
            this.mAdapter.setViewPagerObserver(this.mObserver);
            this.mPopulatePending = false;
            boolean wasFirstLayout = this.mFirstLayout;
            this.mFirstLayout = true;
            this.mExpectedAdapterCount = this.mAdapter.getCount();
            if (this.mRestoredCurItem >= 0) {
                this.mAdapter.restoreState(this.mRestoredAdapterState, this.mRestoredClassLoader);
                setCurrentItemInternal(this.mRestoredCurItem, false, true);
                this.mRestoredCurItem = -1;
                this.mRestoredAdapterState = null;
                this.mRestoredClassLoader = null;
            } else if (wasFirstLayout) {
                requestLayout();
            } else {
                populate();
            }
        }
        if (this.mAdapterChangeListeners != null && !this.mAdapterChangeListeners.isEmpty()) {
            int count = this.mAdapterChangeListeners.size();
            for (int i2 = 0; i2 < count; i2++) {
                ((OnAdapterChangeListener) this.mAdapterChangeListeners.get(i2)).onAdapterChanged(this, oldAdapter, adapter);
            }
        }
    }

    private void removeNonDecorViews() {
        int i = 0;
        while (i < getChildCount()) {
            if (!((LayoutParams) getChildAt(i).getLayoutParams()).isDecor) {
                removeViewAt(i);
                i--;
            }
            i++;
        }
    }

    @Nullable
    public PagerAdapter getAdapter() {
        return this.mAdapter;
    }

    public void addOnAdapterChangeListener(@NonNull OnAdapterChangeListener listener) {
        if (this.mAdapterChangeListeners == null) {
            this.mAdapterChangeListeners = new ArrayList();
        }
        this.mAdapterChangeListeners.add(listener);
    }

    public void removeOnAdapterChangeListener(@NonNull OnAdapterChangeListener listener) {
        if (this.mAdapterChangeListeners != null) {
            this.mAdapterChangeListeners.remove(listener);
        }
    }

    private int getClientWidth() {
        return (getMeasuredWidth() - getPaddingLeft()) - getPaddingRight();
    }

    public void setCurrentItem(int item) {
        this.mPopulatePending = false;
        setCurrentItemInternal(item, this.mFirstLayout ^ 1, false);
    }

    public void setCurrentItem(int item, boolean smoothScroll) {
        this.mPopulatePending = false;
        setCurrentItemInternal(item, smoothScroll, false);
    }

    public int getCurrentItem() {
        return this.mCurItem;
    }

    /* Access modifiers changed, original: 0000 */
    public void setCurrentItemInternal(int item, boolean smoothScroll, boolean always) {
        setCurrentItemInternal(item, smoothScroll, always, 0);
    }

    /* Access modifiers changed, original: 0000 */
    public void setCurrentItemInternal(int item, boolean smoothScroll, boolean always, int velocity) {
        if (this.mAdapter == null || this.mAdapter.getCount() <= 0) {
            setScrollingCacheEnabled(false);
        } else if (always || this.mCurItem != item || this.mItems.size() == 0) {
            boolean dispatchSelected = true;
            if (item < 0) {
                item = 0;
            } else if (item >= this.mAdapter.getCount()) {
                item = this.mAdapter.getCount() - 1;
            }
            int pageLimit = this.mOffscreenPageLimit;
            if (item > this.mCurItem + pageLimit || item < this.mCurItem - pageLimit) {
                for (int i = 0; i < this.mItems.size(); i++) {
                    ((ItemInfo) this.mItems.get(i)).scrolling = true;
                }
            }
            if (this.mCurItem == item) {
                dispatchSelected = false;
            }
            if (this.mFirstLayout) {
                this.mCurItem = item;
                if (dispatchSelected) {
                    dispatchOnPageSelected(item);
                }
                requestLayout();
            } else {
                populate(item);
                scrollToItem(item, smoothScroll, velocity, dispatchSelected);
            }
        } else {
            setScrollingCacheEnabled(false);
        }
    }

    private void scrollToItem(int item, boolean smoothScroll, int velocity, boolean dispatchSelected) {
        ItemInfo curInfo = infoForPosition(item);
        int destX = 0;
        if (curInfo != null) {
            destX = (int) (((float) getClientWidth()) * Math.max(this.mFirstOffset, Math.min(curInfo.offset, this.mLastOffset)));
        }
        if (smoothScroll) {
            smoothScrollTo(destX, 0, velocity);
            if (dispatchSelected) {
                dispatchOnPageSelected(item);
                return;
            }
            return;
        }
        if (dispatchSelected) {
            dispatchOnPageSelected(item);
        }
        completeScroll(false);
        scrollTo(destX, 0);
        pageScrolled(destX);
    }

    @Deprecated
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.mOnPageChangeListener = listener;
    }

    public void addOnPageChangeListener(@NonNull OnPageChangeListener listener) {
        if (this.mOnPageChangeListeners == null) {
            this.mOnPageChangeListeners = new ArrayList();
        }
        this.mOnPageChangeListeners.add(listener);
    }

    public void removeOnPageChangeListener(@NonNull OnPageChangeListener listener) {
        if (this.mOnPageChangeListeners != null) {
            this.mOnPageChangeListeners.remove(listener);
        }
    }

    public void clearOnPageChangeListeners() {
        if (this.mOnPageChangeListeners != null) {
            this.mOnPageChangeListeners.clear();
        }
    }

    public void setPageTransformer(boolean reverseDrawingOrder, @Nullable PageTransformer transformer) {
        setPageTransformer(reverseDrawingOrder, transformer, 2);
    }

    public void setPageTransformer(boolean reverseDrawingOrder, @Nullable PageTransformer transformer, int pageLayerType) {
        int i = 1;
        boolean hasTransformer = transformer != null;
        boolean needsPopulate = hasTransformer != (this.mPageTransformer != null);
        this.mPageTransformer = transformer;
        setChildrenDrawingOrderEnabled(hasTransformer);
        if (hasTransformer) {
            if (reverseDrawingOrder) {
                i = 2;
            }
            this.mDrawingOrder = i;
            this.mPageTransformerLayerType = pageLayerType;
        } else {
            this.mDrawingOrder = 0;
        }
        if (needsPopulate) {
            populate();
        }
    }

    /* Access modifiers changed, original: protected */
    public int getChildDrawingOrder(int childCount, int i) {
        return ((LayoutParams) ((View) this.mDrawingOrderedChildren.get(this.mDrawingOrder == 2 ? (childCount - 1) - i : i)).getLayoutParams()).childIndex;
    }

    /* Access modifiers changed, original: 0000 */
    public OnPageChangeListener setInternalPageChangeListener(OnPageChangeListener listener) {
        OnPageChangeListener oldListener = this.mInternalPageChangeListener;
        this.mInternalPageChangeListener = listener;
        return oldListener;
    }

    public int getOffscreenPageLimit() {
        return this.mOffscreenPageLimit;
    }

    public void setOffscreenPageLimit(int limit) {
        if (limit < 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Requested offscreen page limit ");
            stringBuilder.append(limit);
            stringBuilder.append(" too small; defaulting to ");
            stringBuilder.append(1);
            Log.w(str, stringBuilder.toString());
            limit = 1;
        }
        if (limit != this.mOffscreenPageLimit) {
            this.mOffscreenPageLimit = limit;
            populate();
        }
    }

    public void setPageMargin(int marginPixels) {
        int oldMargin = this.mPageMargin;
        this.mPageMargin = marginPixels;
        int width = getWidth();
        recomputeScrollPosition(width, width, marginPixels, oldMargin);
        requestLayout();
    }

    public int getPageMargin() {
        return this.mPageMargin;
    }

    public void setPageMarginDrawable(@Nullable Drawable d) {
        this.mMarginDrawable = d;
        if (d != null) {
            refreshDrawableState();
        }
        setWillNotDraw(d == null);
        invalidate();
    }

    public void setPageMarginDrawable(@DrawableRes int resId) {
        setPageMarginDrawable(ContextCompat.getDrawable(getContext(), resId));
    }

    /* Access modifiers changed, original: protected */
    public boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mMarginDrawable;
    }

    /* Access modifiers changed, original: protected */
    public void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable d = this.mMarginDrawable;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
    }

    /* Access modifiers changed, original: 0000 */
    public float distanceInfluenceForSnapDuration(float f) {
        return (float) Math.sin((double) ((f - 0.5f) * 0.47123894f));
    }

    /* Access modifiers changed, original: 0000 */
    public void smoothScrollTo(int x, int y) {
        smoothScrollTo(x, y, 0);
    }

    /* Access modifiers changed, original: 0000 */
    public void smoothScrollTo(int x, int y, int velocity) {
        if (getChildCount() == 0) {
            setScrollingCacheEnabled(false);
            return;
        }
        int sx;
        boolean wasScrolling = (this.mScroller == null || this.mScroller.isFinished()) ? false : true;
        if (wasScrolling) {
            sx = this.mIsScrollStarted ? this.mScroller.getCurrX() : this.mScroller.getStartX();
            this.mScroller.abortAnimation();
            setScrollingCacheEnabled(false);
        } else {
            sx = getScrollX();
        }
        int sy = getScrollY();
        int dx = x - sx;
        int dy = y - sy;
        if (dx == 0 && dy == 0) {
            completeScroll(false);
            populate();
            setScrollState(0);
            return;
        }
        int duration;
        setScrollingCacheEnabled(true);
        setScrollState(2);
        int width = getClientWidth();
        int halfWidth = width / 2;
        float distanceRatio = Math.min(1.0f, (((float) Math.abs(dx)) * 1.0f) / ((float) width));
        float distance = ((float) halfWidth) + (((float) halfWidth) * distanceInfluenceForSnapDuration(distanceRatio));
        int velocity2 = Math.abs(velocity);
        if (velocity2 > 0) {
            duration = 4 * Math.round(1000.0f * Math.abs(distance / ((float) velocity2)));
        } else {
            duration = (int) ((1.0f + (((float) Math.abs(dx)) / (((float) this.mPageMargin) + (((float) width) * this.mAdapter.getPageWidth(this.mCurItem))))) * 100.0f);
        }
        int duration2 = Math.min(duration, 600);
        this.mIsScrollStarted = false;
        this.mScroller.startScroll(sx, sy, dx, dy, duration2);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    /* Access modifiers changed, original: 0000 */
    public ItemInfo addNewItem(int position, int index) {
        ItemInfo ii = new ItemInfo();
        ii.position = position;
        ii.object = this.mAdapter.instantiateItem((ViewGroup) this, position);
        ii.widthFactor = this.mAdapter.getPageWidth(position);
        if (index < 0 || index >= this.mItems.size()) {
            this.mItems.add(ii);
        } else {
            this.mItems.add(index, ii);
        }
        return ii;
    }

    /* Access modifiers changed, original: 0000 */
    public void dataSetChanged() {
        int adapterCount = this.mAdapter.getCount();
        this.mExpectedAdapterCount = adapterCount;
        boolean needPopulate = this.mItems.size() < (this.mOffscreenPageLimit * 2) + 1 && this.mItems.size() < adapterCount;
        boolean isUpdating = false;
        int newCurrItem = this.mCurItem;
        boolean needPopulate2 = needPopulate;
        int i = 0;
        while (i < this.mItems.size()) {
            ItemInfo ii = (ItemInfo) this.mItems.get(i);
            int newPos = this.mAdapter.getItemPosition(ii.object);
            if (newPos != -1) {
                if (newPos == -2) {
                    this.mItems.remove(i);
                    i--;
                    if (!isUpdating) {
                        this.mAdapter.startUpdate((ViewGroup) this);
                        isUpdating = true;
                    }
                    this.mAdapter.destroyItem((ViewGroup) this, ii.position, ii.object);
                    needPopulate2 = true;
                    if (this.mCurItem == ii.position) {
                        newCurrItem = Math.max(0, Math.min(this.mCurItem, adapterCount - 1));
                        needPopulate2 = true;
                    }
                } else if (ii.position != newPos) {
                    if (ii.position == this.mCurItem) {
                        newCurrItem = newPos;
                    }
                    ii.position = newPos;
                    needPopulate2 = true;
                }
            }
            i++;
        }
        if (isUpdating) {
            this.mAdapter.finishUpdate((ViewGroup) this);
        }
        Collections.sort(this.mItems, COMPARATOR);
        if (needPopulate2) {
            i = getChildCount();
            for (int i2 = 0; i2 < i; i2++) {
                LayoutParams lp = (LayoutParams) getChildAt(i2).getLayoutParams();
                if (!lp.isDecor) {
                    lp.widthFactor = 0.0f;
                }
            }
            setCurrentItemInternal(newCurrItem, false, true);
            requestLayout();
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void populate() {
        populate(this.mCurItem);
    }

    /* Access modifiers changed, original: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x01d9  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x007b  */
    /* JADX WARNING: Removed duplicated region for block: B:114:0x01e7  */
    /* JADX WARNING: Removed duplicated region for block: B:125:0x021a  */
    public void populate(int r22) {
        /*
        r21 = this;
        r1 = r21;
        r2 = r22;
        r0 = 0;
        r3 = r1.mCurItem;
        if (r3 == r2) goto L_0x0011;
    L_0x0009:
        r3 = r1.mCurItem;
        r0 = r1.infoForPosition(r3);
        r1.mCurItem = r2;
    L_0x0011:
        r3 = r0;
        r0 = r1.mAdapter;
        if (r0 != 0) goto L_0x001a;
    L_0x0016:
        r21.sortChildDrawingOrder();
        return;
    L_0x001a:
        r0 = r1.mPopulatePending;
        if (r0 == 0) goto L_0x0022;
    L_0x001e:
        r21.sortChildDrawingOrder();
        return;
    L_0x0022:
        r0 = r21.getWindowToken();
        if (r0 != 0) goto L_0x0029;
    L_0x0028:
        return;
    L_0x0029:
        r0 = r1.mAdapter;
        r0.startUpdate(r1);
        r4 = r1.mOffscreenPageLimit;
        r0 = r1.mCurItem;
        r0 = r0 - r4;
        r5 = 0;
        r6 = java.lang.Math.max(r5, r0);
        r0 = r1.mAdapter;
        r7 = r0.getCount();
        r0 = r7 + -1;
        r8 = r1.mCurItem;
        r8 = r8 + r4;
        r8 = java.lang.Math.min(r0, r8);
        r0 = r1.mExpectedAdapterCount;
        if (r7 != r0) goto L_0x0259;
    L_0x004b:
        r0 = -1;
        r9 = 0;
        r0 = 0;
    L_0x004e:
        r10 = r1.mItems;
        r10 = r10.size();
        if (r0 >= r10) goto L_0x006f;
    L_0x0056:
        r10 = r1.mItems;
        r10 = r10.get(r0);
        r10 = (android.support.v4.view.ViewPager.ItemInfo) r10;
        r11 = r10.position;
        r12 = r1.mCurItem;
        if (r11 < r12) goto L_0x006c;
    L_0x0064:
        r11 = r10.position;
        r12 = r1.mCurItem;
        if (r11 != r12) goto L_0x006f;
    L_0x006a:
        r9 = r10;
        goto L_0x006f;
    L_0x006c:
        r0 = r0 + 1;
        goto L_0x004e;
    L_0x006f:
        if (r9 != 0) goto L_0x0079;
    L_0x0071:
        if (r7 <= 0) goto L_0x0079;
    L_0x0073:
        r10 = r1.mCurItem;
        r9 = r1.addNewItem(r10, r0);
    L_0x0079:
        if (r9 == 0) goto L_0x01d9;
    L_0x007b:
        r12 = 0;
        r13 = r0 + -1;
        if (r13 < 0) goto L_0x0089;
    L_0x0080:
        r14 = r1.mItems;
        r14 = r14.get(r13);
        r14 = (android.support.v4.view.ViewPager.ItemInfo) r14;
        goto L_0x008a;
    L_0x0089:
        r14 = 0;
    L_0x008a:
        r15 = r21.getClientWidth();
        r16 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r15 > 0) goto L_0x0094;
    L_0x0092:
        r10 = 0;
        goto L_0x00a1;
    L_0x0094:
        r5 = r9.widthFactor;
        r5 = r16 - r5;
        r11 = r21.getPaddingLeft();
        r11 = (float) r11;
        r10 = (float) r15;
        r11 = r11 / r10;
        r10 = r5 + r11;
    L_0x00a1:
        r5 = r10;
        r10 = r1.mCurItem;
        r10 = r10 + -1;
    L_0x00a6:
        if (r10 < 0) goto L_0x0110;
    L_0x00a8:
        r11 = (r12 > r5 ? 1 : (r12 == r5 ? 0 : -1));
        if (r11 < 0) goto L_0x00d8;
    L_0x00ac:
        if (r10 >= r6) goto L_0x00d8;
    L_0x00ae:
        if (r14 != 0) goto L_0x00b1;
    L_0x00b0:
        goto L_0x0110;
    L_0x00b1:
        r11 = r14.position;
        if (r10 != r11) goto L_0x010b;
    L_0x00b5:
        r11 = r14.scrolling;
        if (r11 != 0) goto L_0x010b;
    L_0x00b9:
        r11 = r1.mItems;
        r11.remove(r13);
        r11 = r1.mAdapter;
        r2 = r14.object;
        r11.destroyItem(r1, r10, r2);
        r13 = r13 + -1;
        r0 = r0 + -1;
        if (r13 < 0) goto L_0x00d5;
    L_0x00cb:
        r2 = r1.mItems;
        r2 = r2.get(r13);
        r11 = r2;
        r11 = (android.support.v4.view.ViewPager.ItemInfo) r11;
        goto L_0x00d6;
    L_0x00d5:
        r11 = 0;
    L_0x00d6:
        r2 = r11;
        goto L_0x00f1;
    L_0x00d8:
        if (r14 == 0) goto L_0x00f2;
    L_0x00da:
        r2 = r14.position;
        if (r10 != r2) goto L_0x00f2;
    L_0x00de:
        r2 = r14.widthFactor;
        r12 = r12 + r2;
        r13 = r13 + -1;
        if (r13 < 0) goto L_0x00ef;
    L_0x00e5:
        r2 = r1.mItems;
        r2 = r2.get(r13);
        r11 = r2;
        r11 = (android.support.v4.view.ViewPager.ItemInfo) r11;
        goto L_0x00f0;
    L_0x00ef:
        r11 = 0;
    L_0x00f0:
        r2 = r11;
    L_0x00f1:
        goto L_0x010a;
    L_0x00f2:
        r2 = r13 + 1;
        r2 = r1.addNewItem(r10, r2);
        r11 = r2.widthFactor;
        r12 = r12 + r11;
        r0 = r0 + 1;
        if (r13 < 0) goto L_0x0108;
    L_0x00ff:
        r11 = r1.mItems;
        r11 = r11.get(r13);
        r11 = (android.support.v4.view.ViewPager.ItemInfo) r11;
        goto L_0x0109;
    L_0x0108:
        r11 = 0;
    L_0x0109:
        r2 = r11;
    L_0x010a:
        r14 = r2;
    L_0x010b:
        r10 = r10 + -1;
        r2 = r22;
        goto L_0x00a6;
    L_0x0110:
        r2 = r9.widthFactor;
        r10 = r0 + 1;
        r11 = (r2 > r16 ? 1 : (r2 == r16 ? 0 : -1));
        if (r11 >= 0) goto L_0x01c8;
    L_0x0118:
        r11 = r1.mItems;
        r11 = r11.size();
        if (r10 >= r11) goto L_0x0129;
    L_0x0120:
        r11 = r1.mItems;
        r11 = r11.get(r10);
        r11 = (android.support.v4.view.ViewPager.ItemInfo) r11;
        goto L_0x012a;
    L_0x0129:
        r11 = 0;
    L_0x012a:
        if (r15 > 0) goto L_0x012e;
    L_0x012c:
        r13 = 0;
        goto L_0x0137;
    L_0x012e:
        r13 = r21.getPaddingRight();
        r13 = (float) r13;
        r14 = (float) r15;
        r13 = r13 / r14;
        r13 = r13 + r16;
    L_0x0137:
        r14 = r1.mCurItem;
        r14 = r14 + 1;
    L_0x013b:
        if (r14 >= r7) goto L_0x01c2;
    L_0x013d:
        r16 = (r2 > r13 ? 1 : (r2 == r13 ? 0 : -1));
        if (r16 < 0) goto L_0x017b;
    L_0x0141:
        if (r14 <= r8) goto L_0x017b;
    L_0x0143:
        if (r11 != 0) goto L_0x014c;
        r19 = r4;
        r20 = r5;
        goto L_0x01c6;
    L_0x014c:
        r19 = r4;
        r4 = r11.position;
        if (r14 != r4) goto L_0x0178;
    L_0x0152:
        r4 = r11.scrolling;
        if (r4 != 0) goto L_0x0178;
    L_0x0156:
        r4 = r1.mItems;
        r4.remove(r10);
        r4 = r1.mAdapter;
        r20 = r5;
        r5 = r11.object;
        r4.destroyItem(r1, r14, r5);
        r4 = r1.mItems;
        r4 = r4.size();
        if (r10 >= r4) goto L_0x0175;
    L_0x016c:
        r4 = r1.mItems;
        r4 = r4.get(r10);
        r4 = (android.support.v4.view.ViewPager.ItemInfo) r4;
        goto L_0x0176;
    L_0x0175:
        r4 = 0;
    L_0x0176:
        r11 = r4;
        goto L_0x01ba;
    L_0x0178:
        r20 = r5;
        goto L_0x01ba;
    L_0x017b:
        r19 = r4;
        r20 = r5;
        if (r11 == 0) goto L_0x019e;
    L_0x0181:
        r4 = r11.position;
        if (r14 != r4) goto L_0x019e;
    L_0x0185:
        r4 = r11.widthFactor;
        r2 = r2 + r4;
        r10 = r10 + 1;
        r4 = r1.mItems;
        r4 = r4.size();
        if (r10 >= r4) goto L_0x019b;
    L_0x0192:
        r4 = r1.mItems;
        r4 = r4.get(r10);
        r4 = (android.support.v4.view.ViewPager.ItemInfo) r4;
        goto L_0x019c;
    L_0x019b:
        r4 = 0;
    L_0x019c:
        r11 = r4;
        goto L_0x01ba;
    L_0x019e:
        r4 = r1.addNewItem(r14, r10);
        r10 = r10 + 1;
        r5 = r4.widthFactor;
        r2 = r2 + r5;
        r5 = r1.mItems;
        r5 = r5.size();
        if (r10 >= r5) goto L_0x01b9;
    L_0x01af:
        r5 = r1.mItems;
        r5 = r5.get(r10);
        r11 = r5;
        r11 = (android.support.v4.view.ViewPager.ItemInfo) r11;
        goto L_0x01ba;
    L_0x01b9:
        r11 = 0;
    L_0x01ba:
        r14 = r14 + 1;
        r4 = r19;
        r5 = r20;
        goto L_0x013b;
    L_0x01c2:
        r19 = r4;
        r20 = r5;
    L_0x01c6:
        r14 = r11;
        goto L_0x01cc;
    L_0x01c8:
        r19 = r4;
        r20 = r5;
    L_0x01cc:
        r1.calculatePageOffsets(r9, r0, r3);
        r4 = r1.mAdapter;
        r5 = r1.mCurItem;
        r11 = r9.object;
        r4.setPrimaryItem(r1, r5, r11);
        goto L_0x01db;
    L_0x01d9:
        r19 = r4;
    L_0x01db:
        r2 = r1.mAdapter;
        r2.finishUpdate(r1);
        r2 = r21.getChildCount();
        r4 = 0;
    L_0x01e5:
        if (r4 >= r2) goto L_0x0211;
    L_0x01e7:
        r5 = r1.getChildAt(r4);
        r10 = r5.getLayoutParams();
        r10 = (android.support.v4.view.ViewPager.LayoutParams) r10;
        r10.childIndex = r4;
        r11 = r10.isDecor;
        if (r11 != 0) goto L_0x020d;
    L_0x01f7:
        r11 = r10.widthFactor;
        r12 = 0;
        r11 = (r11 > r12 ? 1 : (r11 == r12 ? 0 : -1));
        if (r11 != 0) goto L_0x020e;
    L_0x01fe:
        r11 = r1.infoForChild(r5);
        if (r11 == 0) goto L_0x020e;
    L_0x0204:
        r13 = r11.widthFactor;
        r10.widthFactor = r13;
        r13 = r11.position;
        r10.position = r13;
        goto L_0x020e;
    L_0x020d:
        r12 = 0;
    L_0x020e:
        r4 = r4 + 1;
        goto L_0x01e5;
    L_0x0211:
        r21.sortChildDrawingOrder();
        r4 = r21.hasFocus();
        if (r4 == 0) goto L_0x0258;
    L_0x021a:
        r4 = r21.findFocus();
        if (r4 == 0) goto L_0x0227;
    L_0x0220:
        r11 = r1.infoForAnyChild(r4);
        r18 = r11;
        goto L_0x0229;
    L_0x0227:
        r18 = 0;
    L_0x0229:
        r5 = r18;
        if (r5 == 0) goto L_0x0233;
    L_0x022d:
        r10 = r5.position;
        r11 = r1.mCurItem;
        if (r10 == r11) goto L_0x0258;
    L_0x0233:
        r17 = 0;
    L_0x0235:
        r10 = r17;
        r11 = r21.getChildCount();
        if (r10 >= r11) goto L_0x0258;
    L_0x023d:
        r11 = r1.getChildAt(r10);
        r5 = r1.infoForChild(r11);
        if (r5 == 0) goto L_0x0255;
    L_0x0247:
        r12 = r5.position;
        r13 = r1.mCurItem;
        if (r12 != r13) goto L_0x0255;
    L_0x024d:
        r12 = 2;
        r12 = r11.requestFocus(r12);
        if (r12 == 0) goto L_0x0255;
    L_0x0254:
        goto L_0x0258;
    L_0x0255:
        r17 = r10 + 1;
        goto L_0x0235;
    L_0x0258:
        return;
    L_0x0259:
        r19 = r4;
        r0 = r21.getResources();	 Catch:{ NotFoundException -> 0x0268 }
        r2 = r21.getId();	 Catch:{ NotFoundException -> 0x0268 }
        r0 = r0.getResourceName(r2);	 Catch:{ NotFoundException -> 0x0268 }
        goto L_0x0271;
    L_0x0268:
        r0 = move-exception;
        r2 = r21.getId();
        r0 = java.lang.Integer.toHexString(r2);
    L_0x0271:
        r2 = new java.lang.IllegalStateException;
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "The application's PagerAdapter changed the adapter's contents without calling PagerAdapter#notifyDataSetChanged! Expected adapter item count: ";
        r4.append(r5);
        r5 = r1.mExpectedAdapterCount;
        r4.append(r5);
        r5 = ", found: ";
        r4.append(r5);
        r4.append(r7);
        r5 = " Pager id: ";
        r4.append(r5);
        r4.append(r0);
        r5 = " Pager class: ";
        r4.append(r5);
        r5 = r21.getClass();
        r4.append(r5);
        r5 = " Problematic adapter: ";
        r4.append(r5);
        r5 = r1.mAdapter;
        r5 = r5.getClass();
        r4.append(r5);
        r4 = r4.toString();
        r2.<init>(r4);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.view.ViewPager.populate(int):void");
    }

    private void sortChildDrawingOrder() {
        if (this.mDrawingOrder != 0) {
            if (this.mDrawingOrderedChildren == null) {
                this.mDrawingOrderedChildren = new ArrayList();
            } else {
                this.mDrawingOrderedChildren.clear();
            }
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                this.mDrawingOrderedChildren.add(getChildAt(i));
            }
            Collections.sort(this.mDrawingOrderedChildren, sPositionComparator);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x0058 A:{LOOP_END, LOOP:2: B:18:0x0054->B:20:0x0058} */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00a3 A:{LOOP_END, LOOP:5: B:33:0x009f->B:35:0x00a3} */
    private void calculatePageOffsets(android.support.v4.view.ViewPager.ItemInfo r12, int r13, android.support.v4.view.ViewPager.ItemInfo r14) {
        /*
        r11 = this;
        r0 = r11.mAdapter;
        r0 = r0.getCount();
        r1 = r11.getClientWidth();
        if (r1 <= 0) goto L_0x0012;
    L_0x000c:
        r2 = r11.mPageMargin;
        r2 = (float) r2;
        r3 = (float) r1;
        r2 = r2 / r3;
        goto L_0x0013;
    L_0x0012:
        r2 = 0;
    L_0x0013:
        if (r14 == 0) goto L_0x00b7;
    L_0x0015:
        r3 = r14.position;
        r4 = r12.position;
        if (r3 >= r4) goto L_0x006d;
    L_0x001b:
        r4 = 0;
        r5 = 0;
        r6 = r14.offset;
        r7 = r14.widthFactor;
        r6 = r6 + r7;
        r6 = r6 + r2;
        r7 = r3 + 1;
    L_0x0025:
        r8 = r12.position;
        if (r7 > r8) goto L_0x006c;
    L_0x0029:
        r8 = r11.mItems;
        r8 = r8.size();
        if (r4 >= r8) goto L_0x006c;
    L_0x0031:
        r8 = r11.mItems;
        r8 = r8.get(r4);
        r5 = r8;
        r5 = (android.support.v4.view.ViewPager.ItemInfo) r5;
    L_0x003a:
        r8 = r5.position;
        if (r7 <= r8) goto L_0x0054;
    L_0x003e:
        r8 = r11.mItems;
        r8 = r8.size();
        r8 = r8 + -1;
        if (r4 >= r8) goto L_0x0054;
    L_0x0048:
        r4 = r4 + 1;
        r8 = r11.mItems;
        r8 = r8.get(r4);
        r5 = r8;
        r5 = (android.support.v4.view.ViewPager.ItemInfo) r5;
        goto L_0x003a;
    L_0x0054:
        r8 = r5.position;
        if (r7 >= r8) goto L_0x0063;
    L_0x0058:
        r8 = r11.mAdapter;
        r8 = r8.getPageWidth(r7);
        r8 = r8 + r2;
        r6 = r6 + r8;
        r7 = r7 + 1;
        goto L_0x0054;
    L_0x0063:
        r5.offset = r6;
        r8 = r5.widthFactor;
        r8 = r8 + r2;
        r6 = r6 + r8;
        r7 = r7 + 1;
        goto L_0x0025;
    L_0x006c:
        goto L_0x00b7;
    L_0x006d:
        r4 = r12.position;
        if (r3 <= r4) goto L_0x00b7;
    L_0x0071:
        r4 = r11.mItems;
        r4 = r4.size();
        r4 = r4 + -1;
        r5 = 0;
        r6 = r14.offset;
        r7 = r3 + -1;
    L_0x007e:
        r8 = r12.position;
        if (r7 < r8) goto L_0x00b7;
    L_0x0082:
        if (r4 < 0) goto L_0x00b7;
    L_0x0084:
        r8 = r11.mItems;
        r8 = r8.get(r4);
        r5 = r8;
        r5 = (android.support.v4.view.ViewPager.ItemInfo) r5;
    L_0x008d:
        r8 = r5.position;
        if (r7 >= r8) goto L_0x009f;
    L_0x0091:
        if (r4 <= 0) goto L_0x009f;
    L_0x0093:
        r4 = r4 + -1;
        r8 = r11.mItems;
        r8 = r8.get(r4);
        r5 = r8;
        r5 = (android.support.v4.view.ViewPager.ItemInfo) r5;
        goto L_0x008d;
    L_0x009f:
        r8 = r5.position;
        if (r7 <= r8) goto L_0x00ae;
    L_0x00a3:
        r8 = r11.mAdapter;
        r8 = r8.getPageWidth(r7);
        r8 = r8 + r2;
        r6 = r6 - r8;
        r7 = r7 + -1;
        goto L_0x009f;
    L_0x00ae:
        r8 = r5.widthFactor;
        r8 = r8 + r2;
        r6 = r6 - r8;
        r5.offset = r6;
        r7 = r7 + -1;
        goto L_0x007e;
    L_0x00b7:
        r3 = r11.mItems;
        r3 = r3.size();
        r4 = r12.offset;
        r5 = r12.position;
        r5 = r5 + -1;
        r6 = r12.position;
        if (r6 != 0) goto L_0x00ca;
    L_0x00c7:
        r6 = r12.offset;
        goto L_0x00cd;
    L_0x00ca:
        r6 = -8388609; // 0xffffffffff7fffff float:-3.4028235E38 double:NaN;
    L_0x00cd:
        r11.mFirstOffset = r6;
        r6 = r12.position;
        r7 = r0 + -1;
        r8 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        if (r6 != r7) goto L_0x00de;
    L_0x00d7:
        r6 = r12.offset;
        r7 = r12.widthFactor;
        r6 = r6 + r7;
        r6 = r6 - r8;
        goto L_0x00e1;
    L_0x00de:
        r6 = 2139095039; // 0x7f7fffff float:3.4028235E38 double:1.056853372E-314;
    L_0x00e1:
        r11.mLastOffset = r6;
        r6 = r13 + -1;
    L_0x00e5:
        if (r6 < 0) goto L_0x0110;
    L_0x00e7:
        r7 = r11.mItems;
        r7 = r7.get(r6);
        r7 = (android.support.v4.view.ViewPager.ItemInfo) r7;
    L_0x00ef:
        r9 = r7.position;
        if (r5 <= r9) goto L_0x00ff;
    L_0x00f3:
        r9 = r11.mAdapter;
        r10 = r5 + -1;
        r5 = r9.getPageWidth(r5);
        r5 = r5 + r2;
        r4 = r4 - r5;
        r5 = r10;
        goto L_0x00ef;
    L_0x00ff:
        r9 = r7.widthFactor;
        r9 = r9 + r2;
        r4 = r4 - r9;
        r7.offset = r4;
        r9 = r7.position;
        if (r9 != 0) goto L_0x010b;
    L_0x0109:
        r11.mFirstOffset = r4;
    L_0x010b:
        r6 = r6 + -1;
        r5 = r5 + -1;
        goto L_0x00e5;
    L_0x0110:
        r6 = r12.offset;
        r7 = r12.widthFactor;
        r6 = r6 + r7;
        r6 = r6 + r2;
        r4 = r12.position;
        r4 = r4 + 1;
        r5 = r13 + 1;
    L_0x011c:
        if (r5 >= r3) goto L_0x014d;
    L_0x011e:
        r7 = r11.mItems;
        r7 = r7.get(r5);
        r7 = (android.support.v4.view.ViewPager.ItemInfo) r7;
    L_0x0126:
        r9 = r7.position;
        if (r4 >= r9) goto L_0x0136;
    L_0x012a:
        r9 = r11.mAdapter;
        r10 = r4 + 1;
        r4 = r9.getPageWidth(r4);
        r4 = r4 + r2;
        r6 = r6 + r4;
        r4 = r10;
        goto L_0x0126;
    L_0x0136:
        r9 = r7.position;
        r10 = r0 + -1;
        if (r9 != r10) goto L_0x0142;
    L_0x013c:
        r9 = r7.widthFactor;
        r9 = r9 + r6;
        r9 = r9 - r8;
        r11.mLastOffset = r9;
    L_0x0142:
        r7.offset = r6;
        r9 = r7.widthFactor;
        r9 = r9 + r2;
        r6 = r6 + r9;
        r5 = r5 + 1;
        r4 = r4 + 1;
        goto L_0x011c;
    L_0x014d:
        r5 = 0;
        r11.mNeedCalculatePageOffsets = r5;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.view.ViewPager.calculatePageOffsets(android.support.v4.view.ViewPager$ItemInfo, int, android.support.v4.view.ViewPager$ItemInfo):void");
    }

    public Parcelable onSaveInstanceState() {
        SavedState ss = new SavedState(super.onSaveInstanceState());
        ss.position = this.mCurItem;
        if (this.mAdapter != null) {
            ss.adapterState = this.mAdapter.saveState();
        }
        return ss;
    }

    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss.getSuperState());
            if (this.mAdapter != null) {
                this.mAdapter.restoreState(ss.adapterState, ss.loader);
                setCurrentItemInternal(ss.position, false, true);
            } else {
                this.mRestoredCurItem = ss.position;
                this.mRestoredAdapterState = ss.adapterState;
                this.mRestoredClassLoader = ss.loader;
            }
            return;
        }
        super.onRestoreInstanceState(state);
    }

    public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
        if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params);
        }
        LayoutParams lp = (LayoutParams) params;
        lp.isDecor |= isDecorView(child);
        if (!this.mInLayout) {
            super.addView(child, index, params);
        } else if (lp == null || !lp.isDecor) {
            lp.needsMeasure = true;
            addViewInLayout(child, index, params);
        } else {
            throw new IllegalStateException("Cannot add pager decor view during layout");
        }
    }

    private static boolean isDecorView(@NonNull View view) {
        return view.getClass().getAnnotation(DecorView.class) != null;
    }

    public void removeView(View view) {
        if (this.mInLayout) {
            removeViewInLayout(view);
        } else {
            super.removeView(view);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public ItemInfo infoForChild(View child) {
        for (int i = 0; i < this.mItems.size(); i++) {
            ItemInfo ii = (ItemInfo) this.mItems.get(i);
            if (this.mAdapter.isViewFromObject(child, ii.object)) {
                return ii;
            }
        }
        return null;
    }

    /* Access modifiers changed, original: 0000 */
    public ItemInfo infoForAnyChild(View child) {
        while (true) {
            Object parent = child.getParent();
            ViewParent parent2 = parent;
            if (parent == this) {
                return infoForChild(child);
            }
            if (parent2 != null && (parent2 instanceof View)) {
                child = (View) parent2;
            }
        }
        return null;
    }

    /* Access modifiers changed, original: 0000 */
    public ItemInfo infoForPosition(int position) {
        for (int i = 0; i < this.mItems.size(); i++) {
            ItemInfo ii = (ItemInfo) this.mItems.get(i);
            if (ii.position == position) {
                return ii;
            }
        }
        return null;
    }

    /* Access modifiers changed, original: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mFirstLayout = true;
    }

    /* Access modifiers changed, original: protected */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x00cd  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00a5  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x00cd  */
    public void onMeasure(int r23, int r24) {
        /*
        r22 = this;
        r0 = r22;
        r1 = 0;
        r2 = r23;
        r3 = getDefaultSize(r1, r2);
        r4 = r24;
        r5 = getDefaultSize(r1, r4);
        r0.setMeasuredDimension(r3, r5);
        r3 = r22.getMeasuredWidth();
        r5 = r3 / 10;
        r6 = r0.mDefaultGutterSize;
        r6 = java.lang.Math.min(r5, r6);
        r0.mGutterSize = r6;
        r6 = r22.getPaddingLeft();
        r6 = r3 - r6;
        r7 = r22.getPaddingRight();
        r6 = r6 - r7;
        r7 = r22.getMeasuredHeight();
        r8 = r22.getPaddingTop();
        r7 = r7 - r8;
        r8 = r22.getPaddingBottom();
        r7 = r7 - r8;
        r8 = r22.getChildCount();
        r9 = r7;
        r7 = r6;
        r6 = r1;
    L_0x0040:
        r10 = 8;
        if (r6 >= r8) goto L_0x00e7;
    L_0x0044:
        r12 = r0.getChildAt(r6);
        r13 = r12.getVisibility();
        if (r13 == r10) goto L_0x00d6;
    L_0x004e:
        r10 = r12.getLayoutParams();
        r10 = (android.support.v4.view.ViewPager.LayoutParams) r10;
        if (r10 == 0) goto L_0x00d6;
    L_0x0056:
        r13 = r10.isDecor;
        if (r13 == 0) goto L_0x00d6;
    L_0x005a:
        r13 = r10.gravity;
        r13 = r13 & 7;
        r14 = r10.gravity;
        r14 = r14 & 112;
        r15 = -2147483648; // 0xffffffff80000000 float:-0.0 double:NaN;
        r16 = -2147483648; // 0xffffffff80000000 float:-0.0 double:NaN;
        r1 = 48;
        if (r14 == r1) goto L_0x0071;
    L_0x006a:
        r1 = 80;
        if (r14 != r1) goto L_0x006f;
    L_0x006e:
        goto L_0x0071;
    L_0x006f:
        r1 = 0;
        goto L_0x0072;
    L_0x0071:
        r1 = 1;
    L_0x0072:
        r11 = 3;
        if (r13 == r11) goto L_0x007c;
    L_0x0075:
        r11 = 5;
        if (r13 != r11) goto L_0x0079;
    L_0x0078:
        goto L_0x007c;
    L_0x0079:
        r17 = 0;
        goto L_0x007e;
    L_0x007c:
        r17 = 1;
    L_0x007e:
        r11 = r17;
        if (r1 == 0) goto L_0x0085;
    L_0x0082:
        r15 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        goto L_0x0089;
    L_0x0085:
        if (r11 == 0) goto L_0x0089;
    L_0x0087:
        r16 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
    L_0x0089:
        r17 = r7;
        r18 = r9;
        r2 = r10.width;
        r19 = r3;
        r3 = -2;
        if (r2 == r3) goto L_0x009e;
    L_0x0094:
        r15 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r2 = r10.width;
        r3 = -1;
        if (r2 == r3) goto L_0x009e;
    L_0x009b:
        r2 = r10.width;
        goto L_0x00a0;
    L_0x009e:
        r2 = r17;
    L_0x00a0:
        r3 = r10.height;
        r4 = -2;
        if (r3 == r4) goto L_0x00b1;
    L_0x00a5:
        r16 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r3 = r10.height;
        r4 = -1;
        if (r3 == r4) goto L_0x00b1;
    L_0x00ac:
        r3 = r10.height;
        r4 = r16;
        goto L_0x00b5;
    L_0x00b1:
        r4 = r16;
        r3 = r18;
    L_0x00b5:
        r20 = r5;
        r5 = android.view.View.MeasureSpec.makeMeasureSpec(r2, r15);
        r21 = r2;
        r2 = android.view.View.MeasureSpec.makeMeasureSpec(r3, r4);
        r12.measure(r5, r2);
        if (r1 == 0) goto L_0x00cd;
    L_0x00c6:
        r16 = r12.getMeasuredHeight();
        r9 = r9 - r16;
        goto L_0x00da;
    L_0x00cd:
        if (r11 == 0) goto L_0x00da;
    L_0x00cf:
        r16 = r12.getMeasuredWidth();
        r7 = r7 - r16;
        goto L_0x00da;
    L_0x00d6:
        r19 = r3;
        r20 = r5;
    L_0x00da:
        r6 = r6 + 1;
        r3 = r19;
        r5 = r20;
        r1 = 0;
        r2 = r23;
        r4 = r24;
        goto L_0x0040;
    L_0x00e7:
        r19 = r3;
        r20 = r5;
        r1 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r2 = android.view.View.MeasureSpec.makeMeasureSpec(r7, r1);
        r0.mChildWidthMeasureSpec = r2;
        r2 = android.view.View.MeasureSpec.makeMeasureSpec(r9, r1);
        r0.mChildHeightMeasureSpec = r2;
        r2 = 1;
        r0.mInLayout = r2;
        r22.populate();
        r2 = 0;
        r0.mInLayout = r2;
        r3 = r22.getChildCount();
    L_0x0107:
        if (r2 >= r3) goto L_0x0130;
    L_0x0109:
        r4 = r0.getChildAt(r2);
        r5 = r4.getVisibility();
        if (r5 == r10) goto L_0x012d;
    L_0x0113:
        r5 = r4.getLayoutParams();
        r5 = (android.support.v4.view.ViewPager.LayoutParams) r5;
        if (r5 == 0) goto L_0x011f;
    L_0x011b:
        r6 = r5.isDecor;
        if (r6 != 0) goto L_0x012d;
    L_0x011f:
        r6 = (float) r7;
        r8 = r5.widthFactor;
        r6 = r6 * r8;
        r6 = (int) r6;
        r6 = android.view.View.MeasureSpec.makeMeasureSpec(r6, r1);
        r8 = r0.mChildHeightMeasureSpec;
        r4.measure(r6, r8);
    L_0x012d:
        r2 = r2 + 1;
        goto L_0x0107;
    L_0x0130:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.view.ViewPager.onMeasure(int, int):void");
    }

    /* Access modifiers changed, original: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw) {
            recomputeScrollPosition(w, oldw, this.mPageMargin, this.mPageMargin);
        }
    }

    private void recomputeScrollPosition(int width, int oldWidth, int margin, int oldMargin) {
        if (oldWidth <= 0 || this.mItems.isEmpty()) {
            ItemInfo ii = infoForPosition(this.mCurItem);
            int scrollPos = (int) (((float) ((width - getPaddingLeft()) - getPaddingRight())) * (ii != null ? Math.min(ii.offset, this.mLastOffset) : 0.0f));
            if (scrollPos != getScrollX()) {
                completeScroll(false);
                scrollTo(scrollPos, getScrollY());
            }
        } else if (this.mScroller.isFinished()) {
            scrollTo((int) (((float) (((width - getPaddingLeft()) - getPaddingRight()) + margin)) * (((float) getScrollX()) / ((float) (((oldWidth - getPaddingLeft()) - getPaddingRight()) + oldMargin)))), getScrollY());
        } else {
            this.mScroller.setFinalX(getCurrentItem() * getClientWidth());
        }
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int hgrav;
        int childLeft;
        int childWidth;
        int width;
        boolean z;
        int count = getChildCount();
        int width2 = r - l;
        int height = b - t;
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        int scrollX = getScrollX();
        int decorCount = 0;
        int paddingBottom2 = paddingBottom;
        paddingBottom = paddingTop;
        paddingTop = paddingLeft;
        for (paddingLeft = 0; paddingLeft < count; paddingLeft++) {
            View child = getChildAt(paddingLeft);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int childLeft2 = 0;
                if (lp.isDecor != 0) {
                    hgrav = lp.gravity & 7;
                    int vgrav = lp.gravity & 112;
                    if (hgrav == 1) {
                        childLeft = Math.max((width2 - child.getMeasuredWidth()) / 2, paddingTop);
                    } else if (hgrav == 3) {
                        childLeft = paddingTop;
                        paddingTop += child.getMeasuredWidth();
                    } else if (hgrav != 5) {
                        childLeft = paddingTop;
                    } else {
                        childLeft = (width2 - paddingRight) - child.getMeasuredWidth();
                        paddingRight += child.getMeasuredWidth();
                    }
                    if (vgrav == 16) {
                        hgrav = Math.max((height - child.getMeasuredHeight()) / 2, paddingBottom);
                    } else if (vgrav == 48) {
                        hgrav = paddingBottom;
                        paddingBottom += child.getMeasuredHeight();
                    } else if (vgrav != 80) {
                        hgrav = paddingBottom;
                    } else {
                        hgrav = (height - paddingBottom2) - child.getMeasuredHeight();
                        paddingBottom2 += child.getMeasuredHeight();
                    }
                    childLeft += scrollX;
                    child.layout(childLeft, hgrav, childLeft + child.getMeasuredWidth(), hgrav + child.getMeasuredHeight());
                    decorCount++;
                }
            }
        }
        childLeft = (width2 - paddingTop) - paddingRight;
        hgrav = 0;
        while (hgrav < count) {
            int count2;
            View child2 = getChildAt(hgrav);
            if (child2.getVisibility() != 8) {
                LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                if (!lp2.isDecor) {
                    ItemInfo infoForChild = infoForChild(child2);
                    ItemInfo ii = infoForChild;
                    if (infoForChild != null) {
                        count2 = count;
                        count = (int) (((float) childLeft) * ii.offset);
                        int childLeft3 = paddingTop + count;
                        int childTop = paddingBottom;
                        int loff = count;
                        if (lp2.needsMeasure != 0) {
                            lp2.needsMeasure = false;
                            childWidth = childLeft;
                            width = width2;
                            child2.measure(MeasureSpec.makeMeasureSpec((int) (((float) childLeft) * lp2.widthFactor), Ints.MAX_POWER_OF_TWO), MeasureSpec.makeMeasureSpec((height - paddingBottom) - paddingBottom2, Ints.MAX_POWER_OF_TWO));
                        } else {
                            childWidth = childLeft;
                            width = width2;
                        }
                        width2 = childTop;
                        child2.layout(childLeft3, width2, child2.getMeasuredWidth() + childLeft3, child2.getMeasuredHeight() + width2);
                        hgrav++;
                        count = count2;
                        childLeft = childWidth;
                        width2 = width;
                    }
                }
            }
            count2 = count;
            childWidth = childLeft;
            width = width2;
            hgrav++;
            count = count2;
            childLeft = childWidth;
            width2 = width;
        }
        childWidth = childLeft;
        width = width2;
        this.mTopPageBounds = paddingBottom;
        this.mBottomPageBounds = height - paddingBottom2;
        this.mDecorChildCount = decorCount;
        if (this.mFirstLayout) {
            z = false;
            scrollToItem(this.mCurItem, false, 0, false);
        } else {
            z = false;
        }
        this.mFirstLayout = z;
    }

    public void computeScroll() {
        this.mIsScrollStarted = true;
        if (this.mScroller.isFinished() || !this.mScroller.computeScrollOffset()) {
            completeScroll(true);
            return;
        }
        int oldX = getScrollX();
        int oldY = getScrollY();
        int x = this.mScroller.getCurrX();
        int y = this.mScroller.getCurrY();
        if (!(oldX == x && oldY == y)) {
            scrollTo(x, y);
            if (!pageScrolled(x)) {
                this.mScroller.abortAnimation();
                scrollTo(0, y);
            }
        }
        ViewCompat.postInvalidateOnAnimation(this);
    }

    private boolean pageScrolled(int xpos) {
        if (this.mItems.size() != 0) {
            ItemInfo ii = infoForCurrentScrollPosition();
            int width = getClientWidth();
            int widthWithMargin = this.mPageMargin + width;
            float marginOffset = ((float) this.mPageMargin) / ((float) width);
            int currentPage = ii.position;
            float pageOffset = ((((float) xpos) / ((float) width)) - ii.offset) / (ii.widthFactor + marginOffset);
            int offsetPixels = (int) (((float) widthWithMargin) * pageOffset);
            this.mCalledSuper = false;
            onPageScrolled(currentPage, pageOffset, offsetPixels);
            if (this.mCalledSuper) {
                return true;
            }
            throw new IllegalStateException("onPageScrolled did not call superclass implementation");
        } else if (this.mFirstLayout) {
            return false;
        } else {
            this.mCalledSuper = false;
            onPageScrolled(0, 0.0f, 0);
            if (this.mCalledSuper) {
                return false;
            }
            throw new IllegalStateException("onPageScrolled did not call superclass implementation");
        }
    }

    /* Access modifiers changed, original: protected */
    @CallSuper
    public void onPageScrolled(int position, float offset, int offsetPixels) {
        int scrollX;
        int paddingLeft;
        int i = 0;
        if (this.mDecorChildCount > 0) {
            scrollX = getScrollX();
            paddingLeft = getPaddingLeft();
            int paddingRight = getPaddingRight();
            int width = getWidth();
            int childCount = getChildCount();
            int paddingRight2 = paddingRight;
            paddingRight = paddingLeft;
            for (paddingLeft = 0; paddingLeft < childCount; paddingLeft++) {
                View child = getChildAt(paddingLeft);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.isDecor) {
                    int childLeft;
                    int hgrav = lp.gravity & 7;
                    if (hgrav == 1) {
                        childLeft = Math.max((width - child.getMeasuredWidth()) / 2, paddingRight);
                    } else if (hgrav == 3) {
                        childLeft = paddingRight;
                        paddingRight += child.getWidth();
                    } else if (hgrav != 5) {
                        childLeft = paddingRight;
                    } else {
                        childLeft = (width - paddingRight2) - child.getMeasuredWidth();
                        paddingRight2 += child.getMeasuredWidth();
                    }
                    int childOffset = (childLeft + scrollX) - child.getLeft();
                    if (childOffset != 0) {
                        child.offsetLeftAndRight(childOffset);
                    }
                }
            }
        }
        dispatchOnPageScrolled(position, offset, offsetPixels);
        if (this.mPageTransformer != null) {
            scrollX = getScrollX();
            paddingLeft = getChildCount();
            while (i < paddingLeft) {
                View child2 = getChildAt(i);
                if (!((LayoutParams) child2.getLayoutParams()).isDecor) {
                    this.mPageTransformer.transformPage(child2, ((float) (child2.getLeft() - scrollX)) / ((float) getClientWidth()));
                }
                i++;
            }
        }
        this.mCalledSuper = true;
    }

    private void dispatchOnPageScrolled(int position, float offset, int offsetPixels) {
        if (this.mOnPageChangeListener != null) {
            this.mOnPageChangeListener.onPageScrolled(position, offset, offsetPixels);
        }
        if (this.mOnPageChangeListeners != null) {
            int z = this.mOnPageChangeListeners.size();
            for (int i = 0; i < z; i++) {
                OnPageChangeListener listener = (OnPageChangeListener) this.mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageScrolled(position, offset, offsetPixels);
                }
            }
        }
        if (this.mInternalPageChangeListener != null) {
            this.mInternalPageChangeListener.onPageScrolled(position, offset, offsetPixels);
        }
    }

    private void dispatchOnPageSelected(int position) {
        if (this.mOnPageChangeListener != null) {
            this.mOnPageChangeListener.onPageSelected(position);
        }
        if (this.mOnPageChangeListeners != null) {
            int z = this.mOnPageChangeListeners.size();
            for (int i = 0; i < z; i++) {
                OnPageChangeListener listener = (OnPageChangeListener) this.mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageSelected(position);
                }
            }
        }
        if (this.mInternalPageChangeListener != null) {
            this.mInternalPageChangeListener.onPageSelected(position);
        }
    }

    private void dispatchOnScrollStateChanged(int state) {
        if (this.mOnPageChangeListener != null) {
            this.mOnPageChangeListener.onPageScrollStateChanged(state);
        }
        if (this.mOnPageChangeListeners != null) {
            int z = this.mOnPageChangeListeners.size();
            for (int i = 0; i < z; i++) {
                OnPageChangeListener listener = (OnPageChangeListener) this.mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageScrollStateChanged(state);
                }
            }
        }
        if (this.mInternalPageChangeListener != null) {
            this.mInternalPageChangeListener.onPageScrollStateChanged(state);
        }
    }

    private void completeScroll(boolean postEvents) {
        boolean needPopulate = this.mScrollState == 2;
        if (needPopulate) {
            setScrollingCacheEnabled(false);
            if (true ^ this.mScroller.isFinished()) {
                this.mScroller.abortAnimation();
                int oldX = getScrollX();
                int oldY = getScrollY();
                int x = this.mScroller.getCurrX();
                int y = this.mScroller.getCurrY();
                if (!(oldX == x && oldY == y)) {
                    scrollTo(x, y);
                    if (x != oldX) {
                        pageScrolled(x);
                    }
                }
            }
        }
        this.mPopulatePending = false;
        boolean needPopulate2 = needPopulate;
        for (int i = 0; i < this.mItems.size(); i++) {
            ItemInfo ii = (ItemInfo) this.mItems.get(i);
            if (ii.scrolling) {
                needPopulate2 = true;
                ii.scrolling = false;
            }
        }
        if (!needPopulate2) {
            return;
        }
        if (postEvents) {
            ViewCompat.postOnAnimation(this, this.mEndScrollRunnable);
        } else {
            this.mEndScrollRunnable.run();
        }
    }

    private boolean isGutterDrag(float x, float dx) {
        return (x < ((float) this.mGutterSize) && dx > 0.0f) || (x > ((float) (getWidth() - this.mGutterSize)) && dx < 0.0f);
    }

    private void enableLayers(boolean enable) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).setLayerType(enable ? this.mPageTransformerLayerType : 0, null);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        MotionEvent motionEvent = ev;
        int action = ev.getAction() & 255;
        if (action == 3 || action == 1) {
            resetTouch();
            return false;
        }
        if (action != 0) {
            if (this.mIsBeingDragged) {
                return true;
            }
            if (this.mIsUnableToDrag) {
                return false;
            }
        }
        if (action == 0) {
            float x = ev.getX();
            this.mInitialMotionX = x;
            this.mLastMotionX = x;
            x = ev.getY();
            this.mInitialMotionY = x;
            this.mLastMotionY = x;
            this.mActivePointerId = motionEvent.getPointerId(0);
            this.mIsUnableToDrag = false;
            this.mIsScrollStarted = true;
            this.mScroller.computeScrollOffset();
            if (this.mScrollState != 2 || Math.abs(this.mScroller.getFinalX() - this.mScroller.getCurrX()) <= this.mCloseEnough) {
                completeScroll(false);
                this.mIsBeingDragged = false;
            } else {
                this.mScroller.abortAnimation();
                this.mPopulatePending = false;
                populate();
                this.mIsBeingDragged = true;
                requestParentDisallowInterceptTouchEvent(true);
                setScrollState(1);
            }
        } else if (action == 2) {
            int activePointerId = this.mActivePointerId;
            if (activePointerId != -1) {
                float y;
                int pointerIndex = motionEvent.findPointerIndex(activePointerId);
                float x2 = motionEvent.getX(pointerIndex);
                float dx = x2 - this.mLastMotionX;
                float xDiff = Math.abs(dx);
                float y2 = motionEvent.getY(pointerIndex);
                float yDiff = Math.abs(y2 - this.mInitialMotionY);
                if (dx == 0.0f || isGutterDrag(this.mLastMotionX, dx)) {
                    y = y2;
                } else {
                    y = y2;
                    if (canScroll(this, false, (int) dx, (int) x2, (int) y2)) {
                        this.mLastMotionX = x2;
                        this.mLastMotionY = y;
                        this.mIsUnableToDrag = true;
                        return false;
                    }
                }
                if (xDiff > ((float) this.mTouchSlop) && 0.5f * xDiff > yDiff) {
                    this.mIsBeingDragged = true;
                    requestParentDisallowInterceptTouchEvent(true);
                    setScrollState(1);
                    this.mLastMotionX = dx > 0.0f ? this.mInitialMotionX + ((float) this.mTouchSlop) : this.mInitialMotionX - ((float) this.mTouchSlop);
                    this.mLastMotionY = y;
                    setScrollingCacheEnabled(true);
                } else if (yDiff > ((float) this.mTouchSlop)) {
                    this.mIsUnableToDrag = true;
                }
                if (this.mIsBeingDragged && performDrag(x2)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            }
        } else if (action == 6) {
            onSecondaryPointerUp(ev);
        }
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
        return this.mIsBeingDragged;
    }

    public boolean onTouchEvent(android.view.MotionEvent r18) {
        /*
        r17 = this;
        r0 = r17;
        r1 = r18;
        r2 = r0.mFakeDragging;
        r3 = 1;
        if (r2 == 0) goto L_0x000a;
    L_0x0009:
        return r3;
    L_0x000a:
        r2 = r18.getAction();
        r4 = 0;
        if (r2 != 0) goto L_0x0018;
    L_0x0011:
        r2 = r18.getEdgeFlags();
        if (r2 == 0) goto L_0x0018;
    L_0x0017:
        return r4;
    L_0x0018:
        r2 = r0.mAdapter;
        if (r2 == 0) goto L_0x0166;
    L_0x001c:
        r2 = r0.mAdapter;
        r2 = r2.getCount();
        if (r2 != 0) goto L_0x0026;
    L_0x0024:
        goto L_0x0166;
    L_0x0026:
        r2 = r0.mVelocityTracker;
        if (r2 != 0) goto L_0x0030;
    L_0x002a:
        r2 = android.view.VelocityTracker.obtain();
        r0.mVelocityTracker = r2;
    L_0x0030:
        r2 = r0.mVelocityTracker;
        r2.addMovement(r1);
        r2 = r18.getAction();
        r5 = 0;
        r6 = r2 & 255;
        switch(r6) {
            case 0: goto L_0x013d;
            case 1: goto L_0x00ec;
            case 2: goto L_0x0075;
            case 3: goto L_0x0064;
            case 4: goto L_0x003f;
            case 5: goto L_0x0053;
            case 6: goto L_0x0043;
            default: goto L_0x003f;
        };
    L_0x003f:
        r16 = r2;
        goto L_0x0160;
    L_0x0043:
        r17.onSecondaryPointerUp(r18);
        r4 = r0.mActivePointerId;
        r4 = r1.findPointerIndex(r4);
        r4 = r1.getX(r4);
        r0.mLastMotionX = r4;
        goto L_0x0071;
    L_0x0053:
        r4 = r18.getActionIndex();
        r6 = r1.getX(r4);
        r0.mLastMotionX = r6;
        r7 = r1.getPointerId(r4);
        r0.mActivePointerId = r7;
        goto L_0x0071;
    L_0x0064:
        r6 = r0.mIsBeingDragged;
        if (r6 == 0) goto L_0x0071;
    L_0x0068:
        r6 = r0.mCurItem;
        r0.scrollToItem(r6, r3, r4, r4);
        r5 = r17.resetTouch();
    L_0x0071:
        r16 = r2;
        goto L_0x0160;
    L_0x0075:
        r4 = r0.mIsBeingDragged;
        if (r4 != 0) goto L_0x00d8;
    L_0x0079:
        r4 = r0.mActivePointerId;
        r4 = r1.findPointerIndex(r4);
        r6 = -1;
        if (r4 != r6) goto L_0x0087;
    L_0x0082:
        r5 = r17.resetTouch();
        goto L_0x0071;
    L_0x0087:
        r6 = r1.getX(r4);
        r7 = r0.mLastMotionX;
        r7 = r6 - r7;
        r7 = java.lang.Math.abs(r7);
        r8 = r1.getY(r4);
        r9 = r0.mLastMotionY;
        r9 = r8 - r9;
        r9 = java.lang.Math.abs(r9);
        r10 = r0.mTouchSlop;
        r10 = (float) r10;
        r10 = (r7 > r10 ? 1 : (r7 == r10 ? 0 : -1));
        if (r10 <= 0) goto L_0x00d8;
    L_0x00a6:
        r10 = (r7 > r9 ? 1 : (r7 == r9 ? 0 : -1));
        if (r10 <= 0) goto L_0x00d8;
    L_0x00aa:
        r0.mIsBeingDragged = r3;
        r0.requestParentDisallowInterceptTouchEvent(r3);
        r10 = r0.mInitialMotionX;
        r10 = r6 - r10;
        r11 = 0;
        r10 = (r10 > r11 ? 1 : (r10 == r11 ? 0 : -1));
        if (r10 <= 0) goto L_0x00bf;
    L_0x00b8:
        r10 = r0.mInitialMotionX;
        r11 = r0.mTouchSlop;
        r11 = (float) r11;
        r10 = r10 + r11;
        goto L_0x00c5;
    L_0x00bf:
        r10 = r0.mInitialMotionX;
        r11 = r0.mTouchSlop;
        r11 = (float) r11;
        r10 = r10 - r11;
    L_0x00c5:
        r0.mLastMotionX = r10;
        r0.mLastMotionY = r8;
        r0.setScrollState(r3);
        r0.setScrollingCacheEnabled(r3);
        r10 = r17.getParent();
        if (r10 == 0) goto L_0x00d8;
    L_0x00d5:
        r10.requestDisallowInterceptTouchEvent(r3);
    L_0x00d8:
        r4 = r0.mIsBeingDragged;
        if (r4 == 0) goto L_0x0071;
    L_0x00dc:
        r4 = r0.mActivePointerId;
        r4 = r1.findPointerIndex(r4);
        r6 = r1.getX(r4);
        r7 = r0.performDrag(r6);
        r5 = r5 | r7;
        goto L_0x0071;
    L_0x00ec:
        r4 = r0.mIsBeingDragged;
        if (r4 == 0) goto L_0x003f;
    L_0x00f0:
        r4 = r0.mVelocityTracker;
        r6 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
        r7 = r0.mMaximumVelocity;
        r7 = (float) r7;
        r4.computeCurrentVelocity(r6, r7);
        r6 = r0.mActivePointerId;
        r6 = r4.getXVelocity(r6);
        r6 = (int) r6;
        r0.mPopulatePending = r3;
        r7 = r17.getClientWidth();
        r8 = r17.getScrollX();
        r9 = r17.infoForCurrentScrollPosition();
        r10 = r0.mPageMargin;
        r10 = (float) r10;
        r11 = (float) r7;
        r10 = r10 / r11;
        r11 = r9.position;
        r12 = (float) r8;
        r13 = (float) r7;
        r12 = r12 / r13;
        r13 = r9.offset;
        r12 = r12 - r13;
        r13 = r9.widthFactor;
        r13 = r13 + r10;
        r12 = r12 / r13;
        r13 = r0.mActivePointerId;
        r13 = r1.findPointerIndex(r13);
        r14 = r1.getX(r13);
        r15 = r0.mInitialMotionX;
        r15 = r14 - r15;
        r15 = (int) r15;
        r16 = r2;
        r2 = r0.determineTargetPage(r11, r12, r6, r15);
        r0.setCurrentItemInternal(r2, r3, r3, r6);
        r5 = r17.resetTouch();
        goto L_0x0160;
    L_0x013d:
        r16 = r2;
        r2 = r0.mScroller;
        r2.abortAnimation();
        r0.mPopulatePending = r4;
        r17.populate();
        r2 = r18.getX();
        r0.mInitialMotionX = r2;
        r0.mLastMotionX = r2;
        r2 = r18.getY();
        r0.mInitialMotionY = r2;
        r0.mLastMotionY = r2;
        r2 = r1.getPointerId(r4);
        r0.mActivePointerId = r2;
    L_0x0160:
        if (r5 == 0) goto L_0x0165;
    L_0x0162:
        android.support.v4.view.ViewCompat.postInvalidateOnAnimation(r17);
    L_0x0165:
        return r3;
    L_0x0166:
        return r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.view.ViewPager.onTouchEvent(android.view.MotionEvent):boolean");
    }

    private boolean resetTouch() {
        this.mActivePointerId = -1;
        endDrag();
        this.mLeftEdge.onRelease();
        this.mRightEdge.onRelease();
        return this.mLeftEdge.isFinished() || this.mRightEdge.isFinished();
    }

    private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    private boolean performDrag(float x) {
        float f = x;
        boolean needsInvalidate = false;
        float deltaX = this.mLastMotionX - f;
        this.mLastMotionX = f;
        float scrollX = ((float) getScrollX()) + deltaX;
        int width = getClientWidth();
        float leftBound = ((float) width) * this.mFirstOffset;
        float rightBound = ((float) width) * this.mLastOffset;
        boolean leftAbsolute = true;
        boolean rightAbsolute = true;
        ItemInfo firstItem = (ItemInfo) this.mItems.get(0);
        ItemInfo lastItem = (ItemInfo) this.mItems.get(this.mItems.size() - 1);
        if (firstItem.position != 0) {
            leftAbsolute = false;
            leftBound = firstItem.offset * ((float) width);
        }
        if (lastItem.position != this.mAdapter.getCount() - 1) {
            rightAbsolute = false;
            rightBound = lastItem.offset * ((float) width);
        }
        if (scrollX < leftBound) {
            if (leftAbsolute) {
                this.mLeftEdge.onPull(Math.abs(leftBound - scrollX) / ((float) width));
                needsInvalidate = true;
            }
            scrollX = leftBound;
        } else if (scrollX > rightBound) {
            if (rightAbsolute) {
                this.mRightEdge.onPull(Math.abs(scrollX - rightBound) / ((float) width));
                needsInvalidate = true;
            }
            scrollX = rightBound;
        }
        this.mLastMotionX += scrollX - ((float) ((int) scrollX));
        scrollTo((int) scrollX, getScrollY());
        pageScrolled((int) scrollX);
        return needsInvalidate;
    }

    private ItemInfo infoForCurrentScrollPosition() {
        int width = getClientWidth();
        float marginOffset = 0.0f;
        float scrollOffset = width > 0 ? ((float) getScrollX()) / ((float) width) : 0.0f;
        if (width > 0) {
            marginOffset = ((float) this.mPageMargin) / ((float) width);
        }
        int lastPos = -1;
        float lastOffset = 0.0f;
        float lastWidth = 0.0f;
        boolean first = true;
        ItemInfo lastItem = null;
        int i = 0;
        while (i < this.mItems.size()) {
            ItemInfo ii = (ItemInfo) this.mItems.get(i);
            if (!(first || ii.position == lastPos + 1)) {
                ii = this.mTempItem;
                ii.offset = (lastOffset + lastWidth) + marginOffset;
                ii.position = lastPos + 1;
                ii.widthFactor = this.mAdapter.getPageWidth(ii.position);
                i--;
            }
            float offset = ii.offset;
            float leftBound = offset;
            float rightBound = (ii.widthFactor + offset) + marginOffset;
            if (!first && scrollOffset < leftBound) {
                return lastItem;
            }
            if (scrollOffset < rightBound || i == this.mItems.size() - 1) {
                return ii;
            }
            first = false;
            lastPos = ii.position;
            lastOffset = offset;
            lastWidth = ii.widthFactor;
            lastItem = ii;
            i++;
        }
        return lastItem;
    }

    private int determineTargetPage(int currentPage, float pageOffset, int velocity, int deltaX) {
        int targetPage;
        if (Math.abs(deltaX) <= this.mFlingDistance || Math.abs(velocity) <= this.mMinimumVelocity) {
            targetPage = currentPage + ((int) (pageOffset + (currentPage >= this.mCurItem ? 0.4f : 0.6f)));
        } else {
            targetPage = velocity > 0 ? currentPage : currentPage + 1;
        }
        if (this.mItems.size() <= 0) {
            return targetPage;
        }
        return Math.max(((ItemInfo) this.mItems.get(0)).position, Math.min(targetPage, ((ItemInfo) this.mItems.get(this.mItems.size() - 1)).position));
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        boolean needsInvalidate = false;
        int overScrollMode = getOverScrollMode();
        if (overScrollMode == 0 || (overScrollMode == 1 && this.mAdapter != null && this.mAdapter.getCount() > 1)) {
            int restoreCount;
            int height;
            int width;
            if (!this.mLeftEdge.isFinished()) {
                restoreCount = canvas.save();
                height = (getHeight() - getPaddingTop()) - getPaddingBottom();
                width = getWidth();
                canvas.rotate(270.0f);
                canvas.translate((float) ((-height) + getPaddingTop()), this.mFirstOffset * ((float) width));
                this.mLeftEdge.setSize(height, width);
                needsInvalidate = false | this.mLeftEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            }
            if (!this.mRightEdge.isFinished()) {
                restoreCount = canvas.save();
                height = getWidth();
                width = (getHeight() - getPaddingTop()) - getPaddingBottom();
                canvas.rotate(90.0f);
                canvas.translate((float) (-getPaddingTop()), (-(this.mLastOffset + 1.0f)) * ((float) height));
                this.mRightEdge.setSize(width, height);
                needsInvalidate |= this.mRightEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            }
        } else {
            this.mLeftEdge.finish();
            this.mRightEdge.finish();
        }
        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        Canvas canvas2;
        super.onDraw(canvas);
        if (this.mPageMargin > 0 && this.mMarginDrawable != null && this.mItems.size() > 0 && this.mAdapter != null) {
            int scrollX = getScrollX();
            int width = getWidth();
            float marginOffset = ((float) this.mPageMargin) / ((float) width);
            ItemInfo ii = (ItemInfo) this.mItems.get(0);
            float offset = ii.offset;
            int itemCount = this.mItems.size();
            int firstPos = ii.position;
            int lastPos = ((ItemInfo) this.mItems.get(itemCount - 1)).position;
            float offset2 = offset;
            int itemIndex = 0;
            int pos = firstPos;
            while (pos < lastPos) {
                float drawAt;
                float marginOffset2;
                while (pos > ii.position && itemIndex < itemCount) {
                    itemIndex++;
                    ii = (ItemInfo) this.mItems.get(itemIndex);
                }
                if (pos == ii.position) {
                    offset2 = (ii.offset + ii.widthFactor) + marginOffset;
                    drawAt = (ii.offset + ii.widthFactor) * ((float) width);
                } else {
                    float widthFactor = this.mAdapter.getPageWidth(pos);
                    drawAt = (offset2 + widthFactor) * ((float) width);
                    offset2 += widthFactor + marginOffset;
                }
                if (((float) this.mPageMargin) + drawAt > ((float) scrollX)) {
                    marginOffset2 = marginOffset;
                    this.mMarginDrawable.setBounds(Math.round(drawAt), this.mTopPageBounds, Math.round(((float) this.mPageMargin) + drawAt), this.mBottomPageBounds);
                    this.mMarginDrawable.draw(canvas);
                } else {
                    canvas2 = canvas;
                    marginOffset2 = marginOffset;
                }
                if (drawAt <= ((float) (scrollX + width))) {
                    pos++;
                    marginOffset = marginOffset2;
                } else {
                    return;
                }
            }
        }
        canvas2 = canvas;
    }

    public boolean beginFakeDrag() {
        if (this.mIsBeingDragged) {
            return false;
        }
        this.mFakeDragging = true;
        setScrollState(1);
        this.mLastMotionX = 0.0f;
        this.mInitialMotionX = 0.0f;
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }
        long time = SystemClock.uptimeMillis();
        MotionEvent ev = MotionEvent.obtain(time, time, 0, 0.0f, 0.0f, 0);
        this.mVelocityTracker.addMovement(ev);
        ev.recycle();
        this.mFakeDragBeginTime = time;
        return true;
    }

    public void endFakeDrag() {
        if (this.mFakeDragging) {
            if (this.mAdapter != null) {
                VelocityTracker velocityTracker = this.mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumVelocity);
                int initialVelocity = (int) velocityTracker.getXVelocity(this.mActivePointerId);
                this.mPopulatePending = true;
                int width = getClientWidth();
                int scrollX = getScrollX();
                ItemInfo ii = infoForCurrentScrollPosition();
                setCurrentItemInternal(determineTargetPage(ii.position, ((((float) scrollX) / ((float) width)) - ii.offset) / ii.widthFactor, initialVelocity, (int) (this.mLastMotionX - this.mInitialMotionX)), true, true, initialVelocity);
            }
            endDrag();
            this.mFakeDragging = false;
            return;
        }
        throw new IllegalStateException("No fake drag in progress. Call beginFakeDrag first.");
    }

    public void fakeDragBy(float xOffset) {
        if (!this.mFakeDragging) {
            throw new IllegalStateException("No fake drag in progress. Call beginFakeDrag first.");
        } else if (this.mAdapter != null) {
            this.mLastMotionX += xOffset;
            float scrollX = ((float) getScrollX()) - xOffset;
            int width = getClientWidth();
            float leftBound = ((float) width) * this.mFirstOffset;
            float rightBound = ((float) width) * this.mLastOffset;
            ItemInfo firstItem = (ItemInfo) this.mItems.get(0);
            ItemInfo lastItem = (ItemInfo) this.mItems.get(this.mItems.size() - 1);
            if (firstItem.position != 0) {
                leftBound = firstItem.offset * ((float) width);
            }
            if (lastItem.position != this.mAdapter.getCount() - 1) {
                rightBound = lastItem.offset * ((float) width);
            }
            if (scrollX < leftBound) {
                scrollX = leftBound;
            } else if (scrollX > rightBound) {
                scrollX = rightBound;
            }
            this.mLastMotionX += scrollX - ((float) ((int) scrollX));
            scrollTo((int) scrollX, getScrollY());
            pageScrolled((int) scrollX);
            MotionEvent ev = MotionEvent.obtain(this.mFakeDragBeginTime, SystemClock.uptimeMillis(), 2, this.mLastMotionX, 0.0f, 0);
            this.mVelocityTracker.addMovement(ev);
            ev.recycle();
        }
    }

    public boolean isFakeDragging() {
        return this.mFakeDragging;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = ev.getActionIndex();
        if (ev.getPointerId(pointerIndex) == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            this.mLastMotionX = ev.getX(newPointerIndex);
            this.mActivePointerId = ev.getPointerId(newPointerIndex);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
        }
    }

    private void endDrag() {
        this.mIsBeingDragged = false;
        this.mIsUnableToDrag = false;
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void setScrollingCacheEnabled(boolean enabled) {
        if (this.mScrollingCacheEnabled != enabled) {
            this.mScrollingCacheEnabled = enabled;
        }
    }

    public boolean canScrollHorizontally(int direction) {
        boolean z = false;
        if (this.mAdapter == null) {
            return false;
        }
        int width = getClientWidth();
        int scrollX = getScrollX();
        if (direction < 0) {
            if (scrollX > ((int) (((float) width) * this.mFirstOffset))) {
                z = true;
            }
            return z;
        } else if (direction <= 0) {
            return false;
        } else {
            if (scrollX < ((int) (((float) width) * this.mLastOffset))) {
                z = true;
            }
            return z;
        }
    }

    /* Access modifiers changed, original: protected */
    /* JADX WARNING: Missing block: B:18:0x0065, code skipped:
            if (r0.canScrollHorizontally(-r17) != false) goto L_0x006b;
     */
    public boolean canScroll(android.view.View r15, boolean r16, int r17, int r18, int r19) {
        /*
        r14 = this;
        r0 = r15;
        r1 = r0 instanceof android.view.ViewGroup;
        r2 = 1;
        if (r1 == 0) goto L_0x005c;
    L_0x0006:
        r1 = r0;
        r1 = (android.view.ViewGroup) r1;
        r3 = r0.getScrollX();
        r4 = r0.getScrollY();
        r5 = r1.getChildCount();
        r6 = r5 + -1;
    L_0x0017:
        if (r6 < 0) goto L_0x005c;
    L_0x0019:
        r13 = r1.getChildAt(r6);
        r7 = r18 + r3;
        r8 = r13.getLeft();
        if (r7 < r8) goto L_0x0059;
    L_0x0025:
        r7 = r18 + r3;
        r8 = r13.getRight();
        if (r7 >= r8) goto L_0x0059;
    L_0x002d:
        r7 = r19 + r4;
        r8 = r13.getTop();
        if (r7 < r8) goto L_0x0059;
    L_0x0035:
        r7 = r19 + r4;
        r8 = r13.getBottom();
        if (r7 >= r8) goto L_0x0059;
    L_0x003d:
        r9 = 1;
        r7 = r18 + r3;
        r8 = r13.getLeft();
        r11 = r7 - r8;
        r7 = r19 + r4;
        r8 = r13.getTop();
        r12 = r7 - r8;
        r7 = r14;
        r8 = r13;
        r10 = r17;
        r7 = r7.canScroll(r8, r9, r10, r11, r12);
        if (r7 == 0) goto L_0x0059;
    L_0x0058:
        return r2;
    L_0x0059:
        r6 = r6 + -1;
        goto L_0x0017;
    L_0x005c:
        if (r16 == 0) goto L_0x0068;
    L_0x005e:
        r3 = r17;
        r4 = -r3;
        r4 = r0.canScrollHorizontally(r4);
        if (r4 == 0) goto L_0x006a;
    L_0x0067:
        goto L_0x006b;
    L_0x0068:
        r3 = r17;
    L_0x006a:
        r2 = 0;
    L_0x006b:
        return r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.view.ViewPager.canScroll(android.view.View, boolean, int, int, int):boolean");
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    public boolean executeKeyEvent(@NonNull KeyEvent event) {
        if (event.getAction() != 0) {
            return false;
        }
        int keyCode = event.getKeyCode();
        if (keyCode != 61) {
            switch (keyCode) {
                case 21:
                    if (event.hasModifiers(2)) {
                        return pageLeft();
                    }
                    return arrowScroll(17);
                case 22:
                    if (event.hasModifiers(2)) {
                        return pageRight();
                    }
                    return arrowScroll(66);
                default:
                    return false;
            }
        } else if (event.hasNoModifiers()) {
            return arrowScroll(2);
        } else {
            if (event.hasModifiers(1)) {
                return arrowScroll(1);
            }
            return false;
        }
    }

    public boolean arrowScroll(int direction) {
        boolean isChild;
        View currentFocused = findFocus();
        if (currentFocused == this) {
            currentFocused = null;
        } else if (currentFocused != null) {
            isChild = false;
            for (ViewPager parent = currentFocused.getParent(); parent instanceof ViewGroup; parent = parent.getParent()) {
                if (parent == this) {
                    isChild = true;
                    break;
                }
            }
            if (!isChild) {
                StringBuilder sb = new StringBuilder();
                sb.append(currentFocused.getClass().getSimpleName());
                for (ViewParent parent2 = currentFocused.getParent(); parent2 instanceof ViewGroup; parent2 = parent2.getParent()) {
                    sb.append(" => ");
                    sb.append(parent2.getClass().getSimpleName());
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("arrowScroll tried to find focus based on non-child current focused view ");
                stringBuilder.append(sb.toString());
                Log.e(str, stringBuilder.toString());
                currentFocused = null;
            }
        }
        isChild = false;
        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);
        if (nextFocused == null || nextFocused == currentFocused) {
            if (direction == 17 || direction == 1) {
                isChild = pageLeft();
            } else if (direction == 66 || direction == 2) {
                isChild = pageRight();
            }
        } else if (direction == 17) {
            isChild = (currentFocused == null || getChildRectInPagerCoordinates(this.mTempRect, nextFocused).left < getChildRectInPagerCoordinates(this.mTempRect, currentFocused).left) ? nextFocused.requestFocus() : pageLeft();
        } else if (direction == 66) {
            isChild = (currentFocused == null || getChildRectInPagerCoordinates(this.mTempRect, nextFocused).left > getChildRectInPagerCoordinates(this.mTempRect, currentFocused).left) ? nextFocused.requestFocus() : pageRight();
        }
        if (isChild) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
        }
        return isChild;
    }

    private Rect getChildRectInPagerCoordinates(Rect outRect, View child) {
        if (outRect == null) {
            outRect = new Rect();
        }
        if (child == null) {
            outRect.set(0, 0, 0, 0);
            return outRect;
        }
        outRect.left = child.getLeft();
        outRect.right = child.getRight();
        outRect.top = child.getTop();
        outRect.bottom = child.getBottom();
        ViewGroup parent = child.getParent();
        while ((parent instanceof ViewGroup) && parent != this) {
            ViewGroup group = parent;
            outRect.left += group.getLeft();
            outRect.right += group.getRight();
            outRect.top += group.getTop();
            outRect.bottom += group.getBottom();
            parent = group.getParent();
        }
        return outRect;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean pageLeft() {
        if (this.mCurItem <= 0) {
            return false;
        }
        setCurrentItem(this.mCurItem - 1, true);
        return true;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean pageRight() {
        if (this.mAdapter == null || this.mCurItem >= this.mAdapter.getCount() - 1) {
            return false;
        }
        setCurrentItem(this.mCurItem + 1, true);
        return true;
    }

    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        int focusableCount = views.size();
        int descendantFocusability = getDescendantFocusability();
        if (descendantFocusability != 393216) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == 0) {
                    ItemInfo ii = infoForChild(child);
                    if (ii != null && ii.position == this.mCurItem) {
                        child.addFocusables(views, direction, focusableMode);
                    }
                }
            }
        }
        if ((descendantFocusability == 262144 && focusableCount != views.size()) || !isFocusable()) {
            return;
        }
        if (!(((focusableMode & 1) == 1 && isInTouchMode() && !isFocusableInTouchMode()) || views == null)) {
            views.add(this);
        }
    }

    public void addTouchables(ArrayList<View> views) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == 0) {
                ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == this.mCurItem) {
                    child.addTouchables(views);
                }
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        int index;
        int increment;
        int end;
        int count = getChildCount();
        if ((direction & 2) != 0) {
            index = 0;
            increment = 1;
            end = count;
        } else {
            index = count - 1;
            increment = -1;
            end = -1;
        }
        for (int i = index; i != end; i += increment) {
            View child = getChildAt(i);
            if (child.getVisibility() == 0) {
                ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == this.mCurItem && child.requestFocus(direction, previouslyFocusedRect)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == 4096) {
            return super.dispatchPopulateAccessibilityEvent(event);
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == 0) {
                ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == this.mCurItem && child.dispatchPopulateAccessibilityEvent(event)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* Access modifiers changed, original: protected */
    public android.view.ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    /* Access modifiers changed, original: protected */
    public android.view.ViewGroup.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return generateDefaultLayoutParams();
    }

    /* Access modifiers changed, original: protected */
    public boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return (p instanceof LayoutParams) && super.checkLayoutParams(p);
    }

    public android.view.ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }
}
