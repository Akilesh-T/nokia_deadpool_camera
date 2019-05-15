package android.support.v7.widget.helper;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.recyclerview.R;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ChildDrawingOrderCallback;
import android.support.v7.widget.RecyclerView.ItemAnimator;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.RecyclerView.OnChildAttachStateChangeListener;
import android.support.v7.widget.RecyclerView.OnItemTouchListener;
import android.support.v7.widget.RecyclerView.State;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import java.util.ArrayList;
import java.util.List;

public class ItemTouchHelper extends ItemDecoration implements OnChildAttachStateChangeListener {
    static final int ACTION_MODE_DRAG_MASK = 16711680;
    private static final int ACTION_MODE_IDLE_MASK = 255;
    static final int ACTION_MODE_SWIPE_MASK = 65280;
    public static final int ACTION_STATE_DRAG = 2;
    public static final int ACTION_STATE_IDLE = 0;
    public static final int ACTION_STATE_SWIPE = 1;
    private static final int ACTIVE_POINTER_ID_NONE = -1;
    public static final int ANIMATION_TYPE_DRAG = 8;
    public static final int ANIMATION_TYPE_SWIPE_CANCEL = 4;
    public static final int ANIMATION_TYPE_SWIPE_SUCCESS = 2;
    private static final boolean DEBUG = false;
    static final int DIRECTION_FLAG_COUNT = 8;
    public static final int DOWN = 2;
    public static final int END = 32;
    public static final int LEFT = 4;
    private static final int PIXELS_PER_SECOND = 1000;
    public static final int RIGHT = 8;
    public static final int START = 16;
    private static final String TAG = "ItemTouchHelper";
    public static final int UP = 1;
    private int mActionState = 0;
    private int mActivePointerId = -1;
    @NonNull
    private Callback mCallback;
    private ChildDrawingOrderCallback mChildDrawingOrderCallback = null;
    private List<Integer> mDistances;
    private long mDragScrollStartTimeInMs;
    private float mDx;
    private float mDy;
    private GestureDetectorCompat mGestureDetector;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private ItemTouchHelperGestureListener mItemTouchHelperGestureListener;
    private float mMaxSwipeVelocity;
    private final OnItemTouchListener mOnItemTouchListener = new OnItemTouchListener() {
        public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent event) {
            ItemTouchHelper.this.mGestureDetector.onTouchEvent(event);
            int action = event.getActionMasked();
            if (action == 0) {
                ItemTouchHelper.this.mActivePointerId = event.getPointerId(0);
                ItemTouchHelper.this.mInitialTouchX = event.getX();
                ItemTouchHelper.this.mInitialTouchY = event.getY();
                ItemTouchHelper.this.obtainVelocityTracker();
                if (ItemTouchHelper.this.mSelected == null) {
                    RecoverAnimation animation = ItemTouchHelper.this.findAnimation(event);
                    if (animation != null) {
                        ItemTouchHelper.this.mInitialTouchX = ItemTouchHelper.this.mInitialTouchX - animation.mX;
                        ItemTouchHelper.this.mInitialTouchY = ItemTouchHelper.this.mInitialTouchY - animation.mY;
                        ItemTouchHelper.this.endRecoverAnimation(animation.mViewHolder, true);
                        if (ItemTouchHelper.this.mPendingCleanup.remove(animation.mViewHolder.itemView)) {
                            ItemTouchHelper.this.mCallback.clearView(ItemTouchHelper.this.mRecyclerView, animation.mViewHolder);
                        }
                        ItemTouchHelper.this.select(animation.mViewHolder, animation.mActionState);
                        ItemTouchHelper.this.updateDxDy(event, ItemTouchHelper.this.mSelectedFlags, 0);
                    }
                }
            } else if (action == 3 || action == 1) {
                ItemTouchHelper.this.mActivePointerId = -1;
                ItemTouchHelper.this.select(null, 0);
            } else if (ItemTouchHelper.this.mActivePointerId != -1) {
                int index = event.findPointerIndex(ItemTouchHelper.this.mActivePointerId);
                if (index >= 0) {
                    ItemTouchHelper.this.checkSelectForSwipe(action, event, index);
                }
            }
            if (ItemTouchHelper.this.mVelocityTracker != null) {
                ItemTouchHelper.this.mVelocityTracker.addMovement(event);
            }
            if (ItemTouchHelper.this.mSelected != null) {
                return true;
            }
            return false;
        }

        public void onTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent event) {
            ItemTouchHelper.this.mGestureDetector.onTouchEvent(event);
            if (ItemTouchHelper.this.mVelocityTracker != null) {
                ItemTouchHelper.this.mVelocityTracker.addMovement(event);
            }
            if (ItemTouchHelper.this.mActivePointerId != -1) {
                int action = event.getActionMasked();
                int activePointerIndex = event.findPointerIndex(ItemTouchHelper.this.mActivePointerId);
                if (activePointerIndex >= 0) {
                    ItemTouchHelper.this.checkSelectForSwipe(action, event, activePointerIndex);
                }
                ViewHolder viewHolder = ItemTouchHelper.this.mSelected;
                if (viewHolder != null) {
                    int newPointerIndex = 0;
                    if (action != 6) {
                        switch (action) {
                            case 1:
                                break;
                            case 2:
                                if (activePointerIndex >= 0) {
                                    ItemTouchHelper.this.updateDxDy(event, ItemTouchHelper.this.mSelectedFlags, activePointerIndex);
                                    ItemTouchHelper.this.moveIfNecessary(viewHolder);
                                    ItemTouchHelper.this.mRecyclerView.removeCallbacks(ItemTouchHelper.this.mScrollRunnable);
                                    ItemTouchHelper.this.mScrollRunnable.run();
                                    ItemTouchHelper.this.mRecyclerView.invalidate();
                                    break;
                                }
                                break;
                            case 3:
                                if (ItemTouchHelper.this.mVelocityTracker != null) {
                                    ItemTouchHelper.this.mVelocityTracker.clear();
                                    break;
                                }
                                break;
                        }
                        ItemTouchHelper.this.select(null, 0);
                        ItemTouchHelper.this.mActivePointerId = -1;
                    } else {
                        int pointerIndex = event.getActionIndex();
                        if (event.getPointerId(pointerIndex) == ItemTouchHelper.this.mActivePointerId) {
                            if (pointerIndex == 0) {
                                newPointerIndex = 1;
                            }
                            ItemTouchHelper.this.mActivePointerId = event.getPointerId(newPointerIndex);
                            ItemTouchHelper.this.updateDxDy(event, ItemTouchHelper.this.mSelectedFlags, pointerIndex);
                        }
                    }
                }
            }
        }

        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            if (disallowIntercept) {
                ItemTouchHelper.this.select(null, 0);
            }
        }
    };
    private View mOverdrawChild = null;
    private int mOverdrawChildPosition = -1;
    final List<View> mPendingCleanup = new ArrayList();
    List<RecoverAnimation> mRecoverAnimations = new ArrayList();
    private RecyclerView mRecyclerView;
    private final Runnable mScrollRunnable = new Runnable() {
        public void run() {
            if (ItemTouchHelper.this.mSelected != null && ItemTouchHelper.this.scrollIfNecessary()) {
                if (ItemTouchHelper.this.mSelected != null) {
                    ItemTouchHelper.this.moveIfNecessary(ItemTouchHelper.this.mSelected);
                }
                ItemTouchHelper.this.mRecyclerView.removeCallbacks(ItemTouchHelper.this.mScrollRunnable);
                ViewCompat.postOnAnimation(ItemTouchHelper.this.mRecyclerView, this);
            }
        }
    };
    private ViewHolder mSelected = null;
    private int mSelectedFlags;
    private float mSelectedStartX;
    private float mSelectedStartY;
    private int mSlop;
    private List<ViewHolder> mSwapTargets;
    private float mSwipeEscapeVelocity;
    private final float[] mTmpPosition = new float[2];
    private Rect mTmpRect;
    private VelocityTracker mVelocityTracker;

    public static abstract class Callback {
        private static final int ABS_HORIZONTAL_DIR_FLAGS = 789516;
        public static final int DEFAULT_DRAG_ANIMATION_DURATION = 200;
        public static final int DEFAULT_SWIPE_ANIMATION_DURATION = 250;
        private static final long DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS = 2000;
        static final int RELATIVE_DIR_FLAGS = 3158064;
        private static final Interpolator sDragScrollInterpolator = new Interpolator() {
            public float getInterpolation(float t) {
                return (((t * t) * t) * t) * t;
            }
        };
        private static final Interpolator sDragViewScrollCapInterpolator = new Interpolator() {
            public float getInterpolation(float t) {
                t -= 1.0f;
                return ((((t * t) * t) * t) * t) + 1.0f;
            }
        };
        private int mCachedMaxScrollSpeed = -1;

        public abstract int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder);

        public abstract boolean onMove(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder, @NonNull ViewHolder viewHolder2);

        public abstract void onSwiped(@NonNull ViewHolder viewHolder, int i);

        @NonNull
        public static ItemTouchUIUtil getDefaultUIUtil() {
            return ItemTouchUIUtilImpl.INSTANCE;
        }

        public static int convertToRelativeDirection(int flags, int layoutDirection) {
            int masked = flags & ABS_HORIZONTAL_DIR_FLAGS;
            if (masked == 0) {
                return flags;
            }
            flags &= ~masked;
            if (layoutDirection == 0) {
                return flags | (masked << 2);
            }
            return (flags | ((masked << 1) & -789517)) | ((ABS_HORIZONTAL_DIR_FLAGS & (masked << 1)) << 2);
        }

        public static int makeMovementFlags(int dragFlags, int swipeFlags) {
            return (makeFlag(0, swipeFlags | dragFlags) | makeFlag(1, swipeFlags)) | makeFlag(2, dragFlags);
        }

        public static int makeFlag(int actionState, int directions) {
            return directions << (actionState * 8);
        }

        public int convertToAbsoluteDirection(int flags, int layoutDirection) {
            int masked = flags & RELATIVE_DIR_FLAGS;
            if (masked == 0) {
                return flags;
            }
            flags &= ~masked;
            if (layoutDirection == 0) {
                return flags | (masked >> 2);
            }
            return (flags | ((masked >> 1) & -3158065)) | ((RELATIVE_DIR_FLAGS & (masked >> 1)) >> 2);
        }

        /* Access modifiers changed, original: final */
        public final int getAbsoluteMovementFlags(RecyclerView recyclerView, ViewHolder viewHolder) {
            return convertToAbsoluteDirection(getMovementFlags(recyclerView, viewHolder), ViewCompat.getLayoutDirection(recyclerView));
        }

        /* Access modifiers changed, original: 0000 */
        public boolean hasDragFlag(RecyclerView recyclerView, ViewHolder viewHolder) {
            return (ItemTouchHelper.ACTION_MODE_DRAG_MASK & getAbsoluteMovementFlags(recyclerView, viewHolder)) != 0;
        }

        /* Access modifiers changed, original: 0000 */
        public boolean hasSwipeFlag(RecyclerView recyclerView, ViewHolder viewHolder) {
            return (65280 & getAbsoluteMovementFlags(recyclerView, viewHolder)) != 0;
        }

        public boolean canDropOver(@NonNull RecyclerView recyclerView, @NonNull ViewHolder current, @NonNull ViewHolder target) {
            return true;
        }

        public boolean isLongPressDragEnabled() {
            return true;
        }

        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        public int getBoundingBoxMargin() {
            return 0;
        }

        public float getSwipeThreshold(@NonNull ViewHolder viewHolder) {
            return 0.5f;
        }

        public float getMoveThreshold(@NonNull ViewHolder viewHolder) {
            return 0.5f;
        }

        public float getSwipeEscapeVelocity(float defaultValue) {
            return defaultValue;
        }

        public float getSwipeVelocityThreshold(float defaultValue) {
            return defaultValue;
        }

        public ViewHolder chooseDropTarget(@NonNull ViewHolder selected, @NonNull List<ViewHolder> dropTargets, int curX, int curY) {
            ViewHolder viewHolder = selected;
            int right = curX + viewHolder.itemView.getWidth();
            int bottom = curY + viewHolder.itemView.getHeight();
            ViewHolder winner = null;
            int winnerScore = -1;
            int dx = curX - viewHolder.itemView.getLeft();
            int dy = curY - viewHolder.itemView.getTop();
            int targetsSize = dropTargets.size();
            for (int i = 0; i < targetsSize; i++) {
                int diff;
                int score;
                ViewHolder target = (ViewHolder) dropTargets.get(i);
                if (dx > 0) {
                    diff = target.itemView.getRight() - right;
                    if (diff < 0 && target.itemView.getRight() > viewHolder.itemView.getRight()) {
                        score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
                if (dx < 0) {
                    diff = target.itemView.getLeft() - curX;
                    if (diff > 0 && target.itemView.getLeft() < viewHolder.itemView.getLeft()) {
                        score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
                if (dy < 0) {
                    diff = target.itemView.getTop() - curY;
                    if (diff > 0 && target.itemView.getTop() < viewHolder.itemView.getTop()) {
                        score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
                if (dy > 0) {
                    diff = target.itemView.getBottom() - bottom;
                    if (diff < 0 && target.itemView.getBottom() > viewHolder.itemView.getBottom()) {
                        score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
            }
            List<ViewHolder> list = dropTargets;
            return winner;
        }

        public void onSelectedChanged(@Nullable ViewHolder viewHolder, int actionState) {
            if (viewHolder != null) {
                ItemTouchUIUtilImpl.INSTANCE.onSelected(viewHolder.itemView);
            }
        }

        private int getMaxDragScroll(RecyclerView recyclerView) {
            if (this.mCachedMaxScrollSpeed == -1) {
                this.mCachedMaxScrollSpeed = recyclerView.getResources().getDimensionPixelSize(R.dimen.item_touch_helper_max_drag_scroll_per_frame);
            }
            return this.mCachedMaxScrollSpeed;
        }

        public void onMoved(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder, int fromPos, @NonNull ViewHolder target, int toPos, int x, int y) {
            LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof ViewDropHandler) {
                ((ViewDropHandler) layoutManager).prepareForDrop(viewHolder.itemView, target.itemView, x, y);
                return;
            }
            if (layoutManager.canScrollHorizontally()) {
                if (layoutManager.getDecoratedLeft(target.itemView) <= recyclerView.getPaddingLeft()) {
                    recyclerView.scrollToPosition(toPos);
                }
                if (layoutManager.getDecoratedRight(target.itemView) >= recyclerView.getWidth() - recyclerView.getPaddingRight()) {
                    recyclerView.scrollToPosition(toPos);
                }
            }
            if (layoutManager.canScrollVertically()) {
                if (layoutManager.getDecoratedTop(target.itemView) <= recyclerView.getPaddingTop()) {
                    recyclerView.scrollToPosition(toPos);
                }
                if (layoutManager.getDecoratedBottom(target.itemView) >= recyclerView.getHeight() - recyclerView.getPaddingBottom()) {
                    recyclerView.scrollToPosition(toPos);
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void onDraw(Canvas c, RecyclerView parent, ViewHolder selected, List<RecoverAnimation> recoverAnimationList, int actionState, float dX, float dY) {
            Canvas canvas = c;
            int recoverAnimSize = recoverAnimationList.size();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= recoverAnimSize) {
                    break;
                }
                RecoverAnimation anim = (RecoverAnimation) recoverAnimationList.get(i2);
                anim.update();
                int count = canvas.save();
                onChildDraw(canvas, parent, anim.mViewHolder, anim.mX, anim.mY, anim.mActionState, false);
                canvas.restoreToCount(count);
                i = i2 + 1;
            }
            List<RecoverAnimation> list = recoverAnimationList;
            if (selected != null) {
                int count2 = canvas.save();
                onChildDraw(canvas, parent, selected, dX, dY, actionState, true);
                canvas.restoreToCount(count2);
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void onDrawOver(Canvas c, RecyclerView parent, ViewHolder selected, List<RecoverAnimation> recoverAnimationList, int actionState, float dX, float dY) {
            Canvas canvas = c;
            List<RecoverAnimation> list = recoverAnimationList;
            int recoverAnimSize = recoverAnimationList.size();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= recoverAnimSize) {
                    break;
                }
                RecoverAnimation anim = (RecoverAnimation) list.get(i2);
                int count = canvas.save();
                onChildDrawOver(canvas, parent, anim.mViewHolder, anim.mX, anim.mY, anim.mActionState, false);
                canvas.restoreToCount(count);
                i = i2 + 1;
            }
            if (selected != null) {
                int count2 = canvas.save();
                onChildDrawOver(canvas, parent, selected, dX, dY, actionState, true);
                canvas.restoreToCount(count2);
            }
            boolean hasRunningAnimation = false;
            for (int i3 = recoverAnimSize - 1; i3 >= 0; i3--) {
                RecoverAnimation anim2 = (RecoverAnimation) list.get(i3);
                if (anim2.mEnded && !anim2.mIsPendingCleanup) {
                    list.remove(i3);
                } else if (!anim2.mEnded) {
                    hasRunningAnimation = true;
                }
            }
            if (hasRunningAnimation) {
                parent.invalidate();
            }
        }

        public void clearView(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder) {
            ItemTouchUIUtilImpl.INSTANCE.clearView(viewHolder.itemView);
        }

        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            ItemTouchUIUtilImpl.INSTANCE.onDraw(c, recyclerView, viewHolder.itemView, dX, dY, actionState, isCurrentlyActive);
        }

        public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView, ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            ItemTouchUIUtilImpl.INSTANCE.onDrawOver(c, recyclerView, viewHolder.itemView, dX, dY, actionState, isCurrentlyActive);
        }

        public long getAnimationDuration(@NonNull RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
            ItemAnimator itemAnimator = recyclerView.getItemAnimator();
            if (itemAnimator == null) {
                return animationType == 8 ? 200 : 250;
            }
            long moveDuration;
            if (animationType == 8) {
                moveDuration = itemAnimator.getMoveDuration();
            } else {
                moveDuration = itemAnimator.getRemoveDuration();
            }
            return moveDuration;
        }

        public int interpolateOutOfBoundsScroll(@NonNull RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
            float timeRatio;
            int cappedScroll = (int) (((float) (((int) Math.signum((float) viewSizeOutOfBounds)) * getMaxDragScroll(recyclerView))) * sDragViewScrollCapInterpolator.getInterpolation(Math.min(1.0f, (((float) Math.abs(viewSizeOutOfBounds)) * 1.0f) / ((float) viewSize))));
            if (msSinceStartScroll > DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS) {
                timeRatio = 1.0f;
            } else {
                timeRatio = ((float) msSinceStartScroll) / 2000.0f;
            }
            int value = (int) (((float) cappedScroll) * sDragScrollInterpolator.getInterpolation(timeRatio));
            if (value != 0) {
                return value;
            }
            return viewSizeOutOfBounds > 0 ? 1 : -1;
        }
    }

    private class ItemTouchHelperGestureListener extends SimpleOnGestureListener {
        private boolean mShouldReactToLongPress = true;

        ItemTouchHelperGestureListener() {
        }

        /* Access modifiers changed, original: 0000 */
        public void doNotReactToLongPress() {
            this.mShouldReactToLongPress = false;
        }

        public boolean onDown(MotionEvent e) {
            return true;
        }

        public void onLongPress(MotionEvent e) {
            if (this.mShouldReactToLongPress) {
                View child = ItemTouchHelper.this.findChildView(e);
                if (child != null) {
                    ViewHolder vh = ItemTouchHelper.this.mRecyclerView.getChildViewHolder(child);
                    if (vh != null && ItemTouchHelper.this.mCallback.hasDragFlag(ItemTouchHelper.this.mRecyclerView, vh) && e.getPointerId(0) == ItemTouchHelper.this.mActivePointerId) {
                        int index = e.findPointerIndex(ItemTouchHelper.this.mActivePointerId);
                        float x = e.getX(index);
                        float y = e.getY(index);
                        ItemTouchHelper.this.mInitialTouchX = x;
                        ItemTouchHelper.this.mInitialTouchY = y;
                        ItemTouchHelper.this.mDx = ItemTouchHelper.this.mDy = 0.0f;
                        if (ItemTouchHelper.this.mCallback.isLongPressDragEnabled()) {
                            ItemTouchHelper.this.select(vh, 2);
                        }
                    }
                }
            }
        }
    }

    private static class RecoverAnimation implements AnimatorListener {
        final int mActionState;
        final int mAnimationType;
        boolean mEnded = false;
        private float mFraction;
        boolean mIsPendingCleanup;
        boolean mOverridden = false;
        final float mStartDx;
        final float mStartDy;
        final float mTargetX;
        final float mTargetY;
        private final ValueAnimator mValueAnimator;
        final ViewHolder mViewHolder;
        float mX;
        float mY;

        RecoverAnimation(ViewHolder viewHolder, int animationType, int actionState, float startDx, float startDy, float targetX, float targetY) {
            this.mActionState = actionState;
            this.mAnimationType = animationType;
            this.mViewHolder = viewHolder;
            this.mStartDx = startDx;
            this.mStartDy = startDy;
            this.mTargetX = targetX;
            this.mTargetY = targetY;
            this.mValueAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
            this.mValueAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    RecoverAnimation.this.setFraction(animation.getAnimatedFraction());
                }
            });
            this.mValueAnimator.setTarget(viewHolder.itemView);
            this.mValueAnimator.addListener(this);
            setFraction(0.0f);
        }

        public void setDuration(long duration) {
            this.mValueAnimator.setDuration(duration);
        }

        public void start() {
            this.mViewHolder.setIsRecyclable(false);
            this.mValueAnimator.start();
        }

        public void cancel() {
            this.mValueAnimator.cancel();
        }

        public void setFraction(float fraction) {
            this.mFraction = fraction;
        }

        public void update() {
            if (this.mStartDx == this.mTargetX) {
                this.mX = this.mViewHolder.itemView.getTranslationX();
            } else {
                this.mX = this.mStartDx + (this.mFraction * (this.mTargetX - this.mStartDx));
            }
            if (this.mStartDy == this.mTargetY) {
                this.mY = this.mViewHolder.itemView.getTranslationY();
            } else {
                this.mY = this.mStartDy + (this.mFraction * (this.mTargetY - this.mStartDy));
            }
        }

        public void onAnimationStart(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
            if (!this.mEnded) {
                this.mViewHolder.setIsRecyclable(true);
            }
            this.mEnded = true;
        }

        public void onAnimationCancel(Animator animation) {
            setFraction(1.0f);
        }

        public void onAnimationRepeat(Animator animation) {
        }
    }

    public interface ViewDropHandler {
        void prepareForDrop(@NonNull View view, @NonNull View view2, int i, int i2);
    }

    public static abstract class SimpleCallback extends Callback {
        private int mDefaultDragDirs;
        private int mDefaultSwipeDirs;

        public SimpleCallback(int dragDirs, int swipeDirs) {
            this.mDefaultSwipeDirs = swipeDirs;
            this.mDefaultDragDirs = dragDirs;
        }

        public void setDefaultSwipeDirs(int defaultSwipeDirs) {
            this.mDefaultSwipeDirs = defaultSwipeDirs;
        }

        public void setDefaultDragDirs(int defaultDragDirs) {
            this.mDefaultDragDirs = defaultDragDirs;
        }

        public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder) {
            return this.mDefaultSwipeDirs;
        }

        public int getDragDirs(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder) {
            return this.mDefaultDragDirs;
        }

        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder) {
            return Callback.makeMovementFlags(getDragDirs(recyclerView, viewHolder), getSwipeDirs(recyclerView, viewHolder));
        }
    }

    public ItemTouchHelper(@NonNull Callback callback) {
        this.mCallback = callback;
    }

    private static boolean hitTest(View child, float x, float y, float left, float top) {
        return x >= left && x <= ((float) child.getWidth()) + left && y >= top && y <= ((float) child.getHeight()) + top;
    }

    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) {
        if (this.mRecyclerView != recyclerView) {
            if (this.mRecyclerView != null) {
                destroyCallbacks();
            }
            this.mRecyclerView = recyclerView;
            if (recyclerView != null) {
                Resources resources = recyclerView.getResources();
                this.mSwipeEscapeVelocity = resources.getDimension(R.dimen.item_touch_helper_swipe_escape_velocity);
                this.mMaxSwipeVelocity = resources.getDimension(R.dimen.item_touch_helper_swipe_escape_max_velocity);
                setupCallbacks();
            }
        }
    }

    private void setupCallbacks() {
        this.mSlop = ViewConfiguration.get(this.mRecyclerView.getContext()).getScaledTouchSlop();
        this.mRecyclerView.addItemDecoration(this);
        this.mRecyclerView.addOnItemTouchListener(this.mOnItemTouchListener);
        this.mRecyclerView.addOnChildAttachStateChangeListener(this);
        startGestureDetection();
    }

    private void destroyCallbacks() {
        this.mRecyclerView.removeItemDecoration(this);
        this.mRecyclerView.removeOnItemTouchListener(this.mOnItemTouchListener);
        this.mRecyclerView.removeOnChildAttachStateChangeListener(this);
        for (int i = this.mRecoverAnimations.size() - 1; i >= 0; i--) {
            this.mCallback.clearView(this.mRecyclerView, ((RecoverAnimation) this.mRecoverAnimations.get(0)).mViewHolder);
        }
        this.mRecoverAnimations.clear();
        this.mOverdrawChild = null;
        this.mOverdrawChildPosition = -1;
        releaseVelocityTracker();
        stopGestureDetection();
    }

    private void startGestureDetection() {
        this.mItemTouchHelperGestureListener = new ItemTouchHelperGestureListener();
        this.mGestureDetector = new GestureDetectorCompat(this.mRecyclerView.getContext(), this.mItemTouchHelperGestureListener);
    }

    private void stopGestureDetection() {
        if (this.mItemTouchHelperGestureListener != null) {
            this.mItemTouchHelperGestureListener.doNotReactToLongPress();
            this.mItemTouchHelperGestureListener = null;
        }
        if (this.mGestureDetector != null) {
            this.mGestureDetector = null;
        }
    }

    private void getSelectedDxDy(float[] outPosition) {
        if ((this.mSelectedFlags & 12) != 0) {
            outPosition[0] = (this.mSelectedStartX + this.mDx) - ((float) this.mSelected.itemView.getLeft());
        } else {
            outPosition[0] = this.mSelected.itemView.getTranslationX();
        }
        if ((this.mSelectedFlags & 3) != 0) {
            outPosition[1] = (this.mSelectedStartY + this.mDy) - ((float) this.mSelected.itemView.getTop());
        } else {
            outPosition[1] = this.mSelected.itemView.getTranslationY();
        }
    }

    public void onDrawOver(Canvas c, RecyclerView parent, State state) {
        float dx = 0.0f;
        float dy = 0.0f;
        if (this.mSelected != null) {
            getSelectedDxDy(this.mTmpPosition);
            dx = this.mTmpPosition[0];
            dy = this.mTmpPosition[1];
        }
        this.mCallback.onDrawOver(c, parent, this.mSelected, this.mRecoverAnimations, this.mActionState, dx, dy);
    }

    public void onDraw(Canvas c, RecyclerView parent, State state) {
        this.mOverdrawChildPosition = -1;
        float dx = 0.0f;
        float dy = 0.0f;
        if (this.mSelected != null) {
            getSelectedDxDy(this.mTmpPosition);
            dx = this.mTmpPosition[0];
            dy = this.mTmpPosition[1];
        }
        this.mCallback.onDraw(c, parent, this.mSelected, this.mRecoverAnimations, this.mActionState, dx, dy);
    }

    /* JADX WARNING: Removed duplicated region for block: B:46:0x013e  */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x0149  */
    private void select(@android.support.annotation.Nullable android.support.v7.widget.RecyclerView.ViewHolder r29, int r30) {
        /*
        r28 = this;
        r11 = r28;
        r12 = r29;
        r13 = r30;
        r0 = r11.mSelected;
        if (r12 != r0) goto L_0x000f;
    L_0x000a:
        r0 = r11.mActionState;
        if (r13 != r0) goto L_0x000f;
    L_0x000e:
        return;
    L_0x000f:
        r0 = -9223372036854775808;
        r11.mDragScrollStartTimeInMs = r0;
        r14 = r11.mActionState;
        r15 = 1;
        r11.endRecoverAnimation(r12, r15);
        r11.mActionState = r13;
        r10 = 2;
        if (r13 != r10) goto L_0x0030;
    L_0x001e:
        if (r12 == 0) goto L_0x0028;
    L_0x0020:
        r0 = r12.itemView;
        r11.mOverdrawChild = r0;
        r28.addChildDrawingOrderCallback();
        goto L_0x0030;
    L_0x0028:
        r0 = new java.lang.IllegalArgumentException;
        r1 = "Must pass a ViewHolder when dragging";
        r0.<init>(r1);
        throw r0;
    L_0x0030:
        r9 = 8;
        r0 = r9 * r13;
        r0 = r0 + r9;
        r0 = r15 << r0;
        r16 = r0 + -1;
        r17 = 0;
        r0 = r11.mSelected;
        r8 = 0;
        if (r0 == 0) goto L_0x00fe;
    L_0x0040:
        r7 = r11.mSelected;
        r0 = r7.itemView;
        r0 = r0.getParent();
        if (r0 == 0) goto L_0x00e5;
    L_0x004a:
        if (r14 != r10) goto L_0x004e;
    L_0x004c:
        r0 = r8;
        goto L_0x0052;
    L_0x004e:
        r0 = r11.swipeIfNecessary(r7);
    L_0x0052:
        r6 = r0;
        r28.releaseVelocityTracker();
        r0 = 4;
        if (r6 == r0) goto L_0x007b;
    L_0x0059:
        if (r6 == r9) goto L_0x007b;
    L_0x005b:
        r1 = 16;
        if (r6 == r1) goto L_0x007b;
    L_0x005f:
        r1 = 32;
        if (r6 == r1) goto L_0x007b;
    L_0x0063:
        switch(r6) {
            case 1: goto L_0x006b;
            case 2: goto L_0x006b;
            default: goto L_0x0066;
        };
    L_0x0066:
        r1 = 0;
        r2 = 0;
    L_0x0068:
        r18 = r1;
        goto L_0x008b;
    L_0x006b:
        r1 = 0;
        r2 = r11.mDy;
        r2 = java.lang.Math.signum(r2);
        r3 = r11.mRecyclerView;
        r3 = r3.getHeight();
        r3 = (float) r3;
        r2 = r2 * r3;
        goto L_0x0068;
    L_0x007b:
        r2 = 0;
        r1 = r11.mDx;
        r1 = java.lang.Math.signum(r1);
        r3 = r11.mRecyclerView;
        r3 = r3.getWidth();
        r3 = (float) r3;
        r1 = r1 * r3;
        goto L_0x0068;
    L_0x008b:
        r19 = r2;
        if (r14 != r10) goto L_0x0092;
    L_0x008f:
        r0 = 8;
    L_0x0091:
        goto L_0x0097;
    L_0x0092:
        if (r6 <= 0) goto L_0x0096;
    L_0x0094:
        r0 = 2;
        goto L_0x0091;
    L_0x0097:
        r5 = r0;
        r0 = r11.mTmpPosition;
        r11.getSelectedDxDy(r0);
        r0 = r11.mTmpPosition;
        r20 = r0[r8];
        r0 = r11.mTmpPosition;
        r21 = r0[r15];
        r22 = new android.support.v7.widget.helper.ItemTouchHelper$3;
        r0 = r22;
        r1 = r11;
        r2 = r7;
        r3 = r5;
        r4 = r14;
        r15 = r5;
        r5 = r20;
        r23 = r6;
        r6 = r21;
        r24 = r7;
        r7 = r18;
        r25 = r14;
        r14 = r8;
        r8 = r19;
        r26 = r9;
        r9 = r23;
        r14 = r10;
        r10 = r24;
        r0.<init>(r1, r2, r3, r4, r5, r6, r7, r8, r9, r10);
        r1 = r11.mCallback;
        r2 = r11.mRecyclerView;
        r3 = r18 - r20;
        r4 = r19 - r21;
        r1 = r1.getAnimationDuration(r2, r15, r3, r4);
        r0.setDuration(r1);
        r3 = r11.mRecoverAnimations;
        r3.add(r0);
        r0.start();
        r0 = 1;
        r17 = r0;
        r0 = r24;
        goto L_0x00fa;
    L_0x00e5:
        r24 = r7;
        r26 = r9;
        r25 = r14;
        r14 = r10;
        r0 = r24;
        r1 = r0.itemView;
        r11.removeChildDrawingOrderCallbackIfNecessary(r1);
        r1 = r11.mCallback;
        r2 = r11.mRecyclerView;
        r1.clearView(r2, r0);
    L_0x00fa:
        r1 = 0;
        r11.mSelected = r1;
        goto L_0x0103;
    L_0x00fe:
        r26 = r9;
        r25 = r14;
        r14 = r10;
    L_0x0103:
        if (r12 == 0) goto L_0x0135;
    L_0x0105:
        r0 = r11.mCallback;
        r1 = r11.mRecyclerView;
        r0 = r0.getAbsoluteMovementFlags(r1, r12);
        r0 = r0 & r16;
        r1 = r11.mActionState;
        r1 = r1 * 8;
        r0 = r0 >> r1;
        r11.mSelectedFlags = r0;
        r0 = r12.itemView;
        r0 = r0.getLeft();
        r0 = (float) r0;
        r11.mSelectedStartX = r0;
        r0 = r12.itemView;
        r0 = r0.getTop();
        r0 = (float) r0;
        r11.mSelectedStartY = r0;
        r11.mSelected = r12;
        if (r13 != r14) goto L_0x0135;
    L_0x012c:
        r0 = r11.mSelected;
        r0 = r0.itemView;
        r1 = 0;
        r0.performHapticFeedback(r1);
        goto L_0x0136;
    L_0x0135:
        r1 = 0;
    L_0x0136:
        r0 = r11.mRecyclerView;
        r0 = r0.getParent();
        if (r0 == 0) goto L_0x0147;
    L_0x013e:
        r2 = r11.mSelected;
        if (r2 == 0) goto L_0x0144;
    L_0x0142:
        r1 = 1;
    L_0x0144:
        r0.requestDisallowInterceptTouchEvent(r1);
    L_0x0147:
        if (r17 != 0) goto L_0x0152;
    L_0x0149:
        r1 = r11.mRecyclerView;
        r1 = r1.getLayoutManager();
        r1.requestSimpleAnimationsInNextLayout();
    L_0x0152:
        r1 = r11.mCallback;
        r2 = r11.mSelected;
        r3 = r11.mActionState;
        r1.onSelectedChanged(r2, r3);
        r1 = r11.mRecyclerView;
        r1.invalidate();
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.widget.helper.ItemTouchHelper.select(android.support.v7.widget.RecyclerView$ViewHolder, int):void");
    }

    private void postDispatchSwipe(final RecoverAnimation anim, final int swipeDir) {
        this.mRecyclerView.post(new Runnable() {
            public void run() {
                if (ItemTouchHelper.this.mRecyclerView != null && ItemTouchHelper.this.mRecyclerView.isAttachedToWindow() && !anim.mOverridden && anim.mViewHolder.getAdapterPosition() != -1) {
                    ItemAnimator animator = ItemTouchHelper.this.mRecyclerView.getItemAnimator();
                    if ((animator == null || !animator.isRunning(null)) && !ItemTouchHelper.this.hasRunningRecoverAnim()) {
                        ItemTouchHelper.this.mCallback.onSwiped(anim.mViewHolder, swipeDir);
                    } else {
                        ItemTouchHelper.this.mRecyclerView.post(this);
                    }
                }
            }
        });
    }

    private boolean hasRunningRecoverAnim() {
        int size = this.mRecoverAnimations.size();
        for (int i = 0; i < size; i++) {
            if (!((RecoverAnimation) this.mRecoverAnimations.get(i)).mEnded) {
                return true;
            }
        }
        return false;
    }

    private boolean scrollIfNecessary() {
        if (this.mSelected == null) {
            this.mDragScrollStartTimeInMs = Long.MIN_VALUE;
            return false;
        }
        int curX;
        long now = System.currentTimeMillis();
        long scrollDuration = this.mDragScrollStartTimeInMs == Long.MIN_VALUE ? 0 : now - this.mDragScrollStartTimeInMs;
        LayoutManager lm = this.mRecyclerView.getLayoutManager();
        if (this.mTmpRect == null) {
            this.mTmpRect = new Rect();
        }
        int scrollX = 0;
        int scrollY = 0;
        lm.calculateItemDecorationsForChild(this.mSelected.itemView, this.mTmpRect);
        if (lm.canScrollHorizontally()) {
            curX = (int) (this.mSelectedStartX + this.mDx);
            int leftDiff = (curX - this.mTmpRect.left) - this.mRecyclerView.getPaddingLeft();
            if (this.mDx < 0.0f && leftDiff < 0) {
                scrollX = leftDiff;
            } else if (this.mDx > 0.0f) {
                int rightDiff = ((this.mSelected.itemView.getWidth() + curX) + this.mTmpRect.right) - (this.mRecyclerView.getWidth() - this.mRecyclerView.getPaddingRight());
                if (rightDiff > 0) {
                    scrollX = rightDiff;
                }
            }
        }
        if (lm.canScrollVertically()) {
            int curY = (int) (this.mSelectedStartY + this.mDy);
            curX = (curY - this.mTmpRect.top) - this.mRecyclerView.getPaddingTop();
            if (this.mDy < 0.0f && curX < 0) {
                scrollY = curX;
            } else if (this.mDy > 0.0f) {
                int bottomDiff = ((this.mSelected.itemView.getHeight() + curY) + this.mTmpRect.bottom) - (this.mRecyclerView.getHeight() - this.mRecyclerView.getPaddingBottom());
                if (bottomDiff > 0) {
                    scrollY = bottomDiff;
                }
            }
        }
        if (scrollX != 0) {
            scrollX = this.mCallback.interpolateOutOfBoundsScroll(this.mRecyclerView, this.mSelected.itemView.getWidth(), scrollX, this.mRecyclerView.getWidth(), scrollDuration);
        }
        if (scrollY != 0) {
            scrollY = this.mCallback.interpolateOutOfBoundsScroll(this.mRecyclerView, this.mSelected.itemView.getHeight(), scrollY, this.mRecyclerView.getHeight(), scrollDuration);
        }
        if (scrollX == 0 && scrollY == 0) {
            this.mDragScrollStartTimeInMs = Long.MIN_VALUE;
            return false;
        }
        if (this.mDragScrollStartTimeInMs == Long.MIN_VALUE) {
            this.mDragScrollStartTimeInMs = now;
        }
        this.mRecyclerView.scrollBy(scrollX, scrollY);
        return true;
    }

    private List<ViewHolder> findSwapTargets(ViewHolder viewHolder) {
        int left;
        ViewHolder viewHolder2 = viewHolder;
        if (this.mSwapTargets == null) {
            this.mSwapTargets = new ArrayList();
            this.mDistances = new ArrayList();
        } else {
            this.mSwapTargets.clear();
            this.mDistances.clear();
        }
        int margin = this.mCallback.getBoundingBoxMargin();
        int left2 = Math.round(this.mSelectedStartX + this.mDx) - margin;
        int top = Math.round(this.mSelectedStartY + this.mDy) - margin;
        int right = (viewHolder2.itemView.getWidth() + left2) + (2 * margin);
        int bottom = (viewHolder2.itemView.getHeight() + top) + (2 * margin);
        int centerX = (left2 + right) / 2;
        int centerY = (top + bottom) / 2;
        LayoutManager lm = this.mRecyclerView.getLayoutManager();
        int childCount = lm.getChildCount();
        int i = 0;
        while (i < childCount) {
            int margin2;
            View other = lm.getChildAt(i);
            if (other != viewHolder2.itemView) {
                if (other.getBottom() >= top && other.getTop() <= bottom && other.getRight() >= left2) {
                    if (other.getLeft() <= right) {
                        ViewHolder otherVh = this.mRecyclerView.getChildViewHolder(other);
                        if (this.mCallback.canDropOver(this.mRecyclerView, this.mSelected, otherVh)) {
                            int dx = Math.abs(centerX - ((other.getLeft() + other.getRight()) / 2));
                            int dy = Math.abs(centerY - ((other.getTop() + other.getBottom()) / 2));
                            int dist = (dx * dx) + (dy * dy);
                            dx = this.mSwapTargets.size();
                            margin2 = margin;
                            margin = 0;
                            int j = 0;
                            while (true) {
                                left = left2;
                                left2 = j;
                                if (left2 >= dx) {
                                    break;
                                }
                                int cnt = dx;
                                if (dist <= ((Integer) this.mDistances.get(left2)).intValue()) {
                                    break;
                                }
                                margin++;
                                j = left2 + 1;
                                left2 = left;
                                dx = cnt;
                            }
                            this.mSwapTargets.add(margin, otherVh);
                            this.mDistances.add(margin, Integer.valueOf(dist));
                            i++;
                            margin = margin2;
                            left2 = left;
                            viewHolder2 = viewHolder;
                        }
                    }
                }
                margin2 = margin;
                left = left2;
                i++;
                margin = margin2;
                left2 = left;
                viewHolder2 = viewHolder;
            }
            margin2 = margin;
            left = left2;
            i++;
            margin = margin2;
            left2 = left;
            viewHolder2 = viewHolder;
        }
        left = left2;
        return this.mSwapTargets;
    }

    private void moveIfNecessary(ViewHolder viewHolder) {
        ViewHolder viewHolder2 = viewHolder;
        if (!this.mRecyclerView.isLayoutRequested() && this.mActionState == 2) {
            float threshold = this.mCallback.getMoveThreshold(viewHolder2);
            int x = (int) (this.mSelectedStartX + this.mDx);
            int y = (int) (this.mSelectedStartY + this.mDy);
            if (((float) Math.abs(y - viewHolder2.itemView.getTop())) >= ((float) viewHolder2.itemView.getHeight()) * threshold || ((float) Math.abs(x - viewHolder2.itemView.getLeft())) >= ((float) viewHolder2.itemView.getWidth()) * threshold) {
                List<ViewHolder> swapTargets = findSwapTargets(viewHolder);
                if (swapTargets.size() != 0) {
                    ViewHolder target = this.mCallback.chooseDropTarget(viewHolder2, swapTargets, x, y);
                    if (target == null) {
                        this.mSwapTargets.clear();
                        this.mDistances.clear();
                        return;
                    }
                    int toPosition = target.getAdapterPosition();
                    int fromPosition = viewHolder.getAdapterPosition();
                    if (this.mCallback.onMove(this.mRecyclerView, viewHolder2, target)) {
                        this.mCallback.onMoved(this.mRecyclerView, viewHolder2, fromPosition, target, toPosition, x, y);
                    }
                }
            }
        }
    }

    public void onChildViewAttachedToWindow(@NonNull View view) {
    }

    public void onChildViewDetachedFromWindow(@NonNull View view) {
        removeChildDrawingOrderCallbackIfNecessary(view);
        ViewHolder holder = this.mRecyclerView.getChildViewHolder(view);
        if (holder != null) {
            if (this.mSelected == null || holder != this.mSelected) {
                endRecoverAnimation(holder, false);
                if (this.mPendingCleanup.remove(holder.itemView)) {
                    this.mCallback.clearView(this.mRecyclerView, holder);
                }
            } else {
                select(null, 0);
            }
        }
    }

    private void endRecoverAnimation(ViewHolder viewHolder, boolean override) {
        for (int i = this.mRecoverAnimations.size() - 1; i >= 0; i--) {
            RecoverAnimation anim = (RecoverAnimation) this.mRecoverAnimations.get(i);
            if (anim.mViewHolder == viewHolder) {
                anim.mOverridden |= override;
                if (!anim.mEnded) {
                    anim.cancel();
                }
                this.mRecoverAnimations.remove(i);
                return;
            }
        }
    }

    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
        outRect.setEmpty();
    }

    private void obtainVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
        }
        this.mVelocityTracker = VelocityTracker.obtain();
    }

    private void releaseVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private ViewHolder findSwipedView(MotionEvent motionEvent) {
        LayoutManager lm = this.mRecyclerView.getLayoutManager();
        if (this.mActivePointerId == -1) {
            return null;
        }
        int pointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
        float dy = motionEvent.getY(pointerIndex) - this.mInitialTouchY;
        float absDx = Math.abs(motionEvent.getX(pointerIndex) - this.mInitialTouchX);
        float absDy = Math.abs(dy);
        if (absDx < ((float) this.mSlop) && absDy < ((float) this.mSlop)) {
            return null;
        }
        if (absDx > absDy && lm.canScrollHorizontally()) {
            return null;
        }
        if (absDy > absDx && lm.canScrollVertically()) {
            return null;
        }
        View child = findChildView(motionEvent);
        if (child == null) {
            return null;
        }
        return this.mRecyclerView.getChildViewHolder(child);
    }

    /* JADX WARNING: Missing block: B:46:0x0096, code skipped:
            return;
     */
    private void checkSelectForSwipe(int r13, android.view.MotionEvent r14, int r15) {
        /*
        r12 = this;
        r0 = r12.mSelected;
        if (r0 != 0) goto L_0x0096;
    L_0x0004:
        r0 = 2;
        if (r13 != r0) goto L_0x0096;
    L_0x0007:
        r1 = r12.mActionState;
        if (r1 == r0) goto L_0x0096;
    L_0x000b:
        r0 = r12.mCallback;
        r0 = r0.isItemViewSwipeEnabled();
        if (r0 != 0) goto L_0x0015;
    L_0x0013:
        goto L_0x0096;
    L_0x0015:
        r0 = r12.mRecyclerView;
        r0 = r0.getScrollState();
        r1 = 1;
        if (r0 != r1) goto L_0x001f;
    L_0x001e:
        return;
    L_0x001f:
        r0 = r12.findSwipedView(r14);
        if (r0 != 0) goto L_0x0026;
    L_0x0025:
        return;
    L_0x0026:
        r2 = r12.mCallback;
        r3 = r12.mRecyclerView;
        r2 = r2.getAbsoluteMovementFlags(r3, r0);
        r3 = 65280; // 0xff00 float:9.1477E-41 double:3.22526E-319;
        r3 = r3 & r2;
        r3 = r3 >> 8;
        if (r3 != 0) goto L_0x0037;
    L_0x0036:
        return;
    L_0x0037:
        r4 = r14.getX(r15);
        r5 = r14.getY(r15);
        r6 = r12.mInitialTouchX;
        r6 = r4 - r6;
        r7 = r12.mInitialTouchY;
        r7 = r5 - r7;
        r8 = java.lang.Math.abs(r6);
        r9 = java.lang.Math.abs(r7);
        r10 = r12.mSlop;
        r10 = (float) r10;
        r10 = (r8 > r10 ? 1 : (r8 == r10 ? 0 : -1));
        if (r10 >= 0) goto L_0x005e;
    L_0x0056:
        r10 = r12.mSlop;
        r10 = (float) r10;
        r10 = (r9 > r10 ? 1 : (r9 == r10 ? 0 : -1));
        if (r10 >= 0) goto L_0x005e;
    L_0x005d:
        return;
    L_0x005e:
        r10 = (r8 > r9 ? 1 : (r8 == r9 ? 0 : -1));
        r11 = 0;
        if (r10 <= 0) goto L_0x0075;
    L_0x0063:
        r10 = (r6 > r11 ? 1 : (r6 == r11 ? 0 : -1));
        if (r10 >= 0) goto L_0x006c;
    L_0x0067:
        r10 = r3 & 4;
        if (r10 != 0) goto L_0x006c;
    L_0x006b:
        return;
    L_0x006c:
        r10 = (r6 > r11 ? 1 : (r6 == r11 ? 0 : -1));
        if (r10 <= 0) goto L_0x0087;
    L_0x0070:
        r10 = r3 & 8;
        if (r10 != 0) goto L_0x0087;
    L_0x0074:
        return;
    L_0x0075:
        r10 = (r7 > r11 ? 1 : (r7 == r11 ? 0 : -1));
        if (r10 >= 0) goto L_0x007e;
    L_0x0079:
        r10 = r3 & 1;
        if (r10 != 0) goto L_0x007e;
    L_0x007d:
        return;
    L_0x007e:
        r10 = (r7 > r11 ? 1 : (r7 == r11 ? 0 : -1));
        if (r10 <= 0) goto L_0x0087;
    L_0x0082:
        r10 = r3 & 2;
        if (r10 != 0) goto L_0x0087;
    L_0x0086:
        return;
    L_0x0087:
        r12.mDy = r11;
        r12.mDx = r11;
        r10 = 0;
        r10 = r14.getPointerId(r10);
        r12.mActivePointerId = r10;
        r12.select(r0, r1);
        return;
    L_0x0096:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.widget.helper.ItemTouchHelper.checkSelectForSwipe(int, android.view.MotionEvent, int):void");
    }

    private View findChildView(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (this.mSelected != null) {
            View selectedView = this.mSelected.itemView;
            if (hitTest(selectedView, x, y, this.mSelectedStartX + this.mDx, this.mSelectedStartY + this.mDy)) {
                return selectedView;
            }
        }
        for (int i = this.mRecoverAnimations.size() - 1; i >= 0; i--) {
            RecoverAnimation anim = (RecoverAnimation) this.mRecoverAnimations.get(i);
            View view = anim.mViewHolder.itemView;
            if (hitTest(view, x, y, anim.mX, anim.mY)) {
                return view;
            }
        }
        return this.mRecyclerView.findChildViewUnder(x, y);
    }

    public void startDrag(@NonNull ViewHolder viewHolder) {
        if (!this.mCallback.hasDragFlag(this.mRecyclerView, viewHolder)) {
            Log.e(TAG, "Start drag has been called but dragging is not enabled");
        } else if (viewHolder.itemView.getParent() != this.mRecyclerView) {
            Log.e(TAG, "Start drag has been called with a view holder which is not a child of the RecyclerView which is controlled by this ItemTouchHelper.");
        } else {
            obtainVelocityTracker();
            this.mDy = 0.0f;
            this.mDx = 0.0f;
            select(viewHolder, 2);
        }
    }

    public void startSwipe(@NonNull ViewHolder viewHolder) {
        if (!this.mCallback.hasSwipeFlag(this.mRecyclerView, viewHolder)) {
            Log.e(TAG, "Start swipe has been called but swiping is not enabled");
        } else if (viewHolder.itemView.getParent() != this.mRecyclerView) {
            Log.e(TAG, "Start swipe has been called with a view holder which is not a child of the RecyclerView controlled by this ItemTouchHelper.");
        } else {
            obtainVelocityTracker();
            this.mDy = 0.0f;
            this.mDx = 0.0f;
            select(viewHolder, 1);
        }
    }

    private RecoverAnimation findAnimation(MotionEvent event) {
        if (this.mRecoverAnimations.isEmpty()) {
            return null;
        }
        View target = findChildView(event);
        for (int i = this.mRecoverAnimations.size() - 1; i >= 0; i--) {
            RecoverAnimation anim = (RecoverAnimation) this.mRecoverAnimations.get(i);
            if (anim.mViewHolder.itemView == target) {
                return anim;
            }
        }
        return null;
    }

    private void updateDxDy(MotionEvent ev, int directionFlags, int pointerIndex) {
        float x = ev.getX(pointerIndex);
        float y = ev.getY(pointerIndex);
        this.mDx = x - this.mInitialTouchX;
        this.mDy = y - this.mInitialTouchY;
        if ((directionFlags & 4) == 0) {
            this.mDx = Math.max(0.0f, this.mDx);
        }
        if ((directionFlags & 8) == 0) {
            this.mDx = Math.min(0.0f, this.mDx);
        }
        if ((directionFlags & 1) == 0) {
            this.mDy = Math.max(0.0f, this.mDy);
        }
        if ((directionFlags & 2) == 0) {
            this.mDy = Math.min(0.0f, this.mDy);
        }
    }

    private int swipeIfNecessary(ViewHolder viewHolder) {
        if (this.mActionState == 2) {
            return 0;
        }
        int originalMovementFlags = this.mCallback.getMovementFlags(this.mRecyclerView, viewHolder);
        int flags = (this.mCallback.convertToAbsoluteDirection(originalMovementFlags, ViewCompat.getLayoutDirection(this.mRecyclerView)) & 65280) >> 8;
        if (flags == 0) {
            return 0;
        }
        int originalFlags = (65280 & originalMovementFlags) >> 8;
        int checkHorizontalSwipe;
        int swipeDir;
        if (Math.abs(this.mDx) > Math.abs(this.mDy)) {
            checkHorizontalSwipe = checkHorizontalSwipe(viewHolder, flags);
            swipeDir = checkHorizontalSwipe;
            if (checkHorizontalSwipe <= 0) {
                checkHorizontalSwipe = checkVerticalSwipe(viewHolder, flags);
                swipeDir = checkHorizontalSwipe;
                if (checkHorizontalSwipe > 0) {
                    return swipeDir;
                }
            } else if ((originalFlags & swipeDir) == 0) {
                return Callback.convertToRelativeDirection(swipeDir, ViewCompat.getLayoutDirection(this.mRecyclerView));
            } else {
                return swipeDir;
            }
        }
        checkHorizontalSwipe = checkVerticalSwipe(viewHolder, flags);
        swipeDir = checkHorizontalSwipe;
        if (checkHorizontalSwipe > 0) {
            return swipeDir;
        }
        checkHorizontalSwipe = checkHorizontalSwipe(viewHolder, flags);
        swipeDir = checkHorizontalSwipe;
        if (checkHorizontalSwipe > 0) {
            if ((originalFlags & swipeDir) == 0) {
                return Callback.convertToRelativeDirection(swipeDir, ViewCompat.getLayoutDirection(this.mRecyclerView));
            }
            return swipeDir;
        }
        return 0;
    }

    private int checkHorizontalSwipe(ViewHolder viewHolder, int flags) {
        if ((flags & 12) != 0) {
            int i = 4;
            int dirFlag = this.mDx > 0.0f ? 8 : 4;
            if (this.mVelocityTracker != null && this.mActivePointerId > -1) {
                this.mVelocityTracker.computeCurrentVelocity(1000, this.mCallback.getSwipeVelocityThreshold(this.mMaxSwipeVelocity));
                float xVelocity = this.mVelocityTracker.getXVelocity(this.mActivePointerId);
                float yVelocity = this.mVelocityTracker.getYVelocity(this.mActivePointerId);
                if (xVelocity > 0.0f) {
                    i = 8;
                }
                int velDirFlag = i;
                float absXVelocity = Math.abs(xVelocity);
                if ((velDirFlag & flags) != 0 && dirFlag == velDirFlag && absXVelocity >= this.mCallback.getSwipeEscapeVelocity(this.mSwipeEscapeVelocity) && absXVelocity > Math.abs(yVelocity)) {
                    return velDirFlag;
                }
            }
            float threshold = ((float) this.mRecyclerView.getWidth()) * this.mCallback.getSwipeThreshold(viewHolder);
            if ((flags & dirFlag) != 0 && Math.abs(this.mDx) > threshold) {
                return dirFlag;
            }
        }
        return 0;
    }

    private int checkVerticalSwipe(ViewHolder viewHolder, int flags) {
        if ((flags & 3) != 0) {
            int i = 1;
            int dirFlag = this.mDy > 0.0f ? 2 : 1;
            if (this.mVelocityTracker != null && this.mActivePointerId > -1) {
                this.mVelocityTracker.computeCurrentVelocity(1000, this.mCallback.getSwipeVelocityThreshold(this.mMaxSwipeVelocity));
                float xVelocity = this.mVelocityTracker.getXVelocity(this.mActivePointerId);
                float yVelocity = this.mVelocityTracker.getYVelocity(this.mActivePointerId);
                if (yVelocity > 0.0f) {
                    i = 2;
                }
                int velDirFlag = i;
                float absYVelocity = Math.abs(yVelocity);
                if ((velDirFlag & flags) != 0 && velDirFlag == dirFlag && absYVelocity >= this.mCallback.getSwipeEscapeVelocity(this.mSwipeEscapeVelocity) && absYVelocity > Math.abs(xVelocity)) {
                    return velDirFlag;
                }
            }
            float threshold = ((float) this.mRecyclerView.getHeight()) * this.mCallback.getSwipeThreshold(viewHolder);
            if ((flags & dirFlag) != 0 && Math.abs(this.mDy) > threshold) {
                return dirFlag;
            }
        }
        return 0;
    }

    private void addChildDrawingOrderCallback() {
        if (VERSION.SDK_INT < 21) {
            if (this.mChildDrawingOrderCallback == null) {
                this.mChildDrawingOrderCallback = new ChildDrawingOrderCallback() {
                    public int onGetChildDrawingOrder(int childCount, int i) {
                        if (ItemTouchHelper.this.mOverdrawChild == null) {
                            return i;
                        }
                        int childPosition = ItemTouchHelper.this.mOverdrawChildPosition;
                        if (childPosition == -1) {
                            childPosition = ItemTouchHelper.this.mRecyclerView.indexOfChild(ItemTouchHelper.this.mOverdrawChild);
                            ItemTouchHelper.this.mOverdrawChildPosition = childPosition;
                        }
                        if (i == childCount - 1) {
                            return childPosition;
                        }
                        return i < childPosition ? i : i + 1;
                    }
                };
            }
            this.mRecyclerView.setChildDrawingOrderCallback(this.mChildDrawingOrderCallback);
        }
    }

    private void removeChildDrawingOrderCallbackIfNecessary(View view) {
        if (view == this.mOverdrawChild) {
            this.mOverdrawChild = null;
            if (this.mChildDrawingOrderCallback != null) {
                this.mRecyclerView.setChildDrawingOrderCallback(null);
            }
        }
    }
}
