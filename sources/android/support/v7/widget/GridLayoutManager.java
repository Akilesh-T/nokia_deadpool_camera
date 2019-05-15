package android.support.v7.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.RecyclerView.LayoutManager.LayoutPrefetchRegistry;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.MarginLayoutParams;
import com.google.common.primitives.Ints;
import java.util.Arrays;

public class GridLayoutManager extends LinearLayoutManager {
    private static final boolean DEBUG = false;
    public static final int DEFAULT_SPAN_COUNT = -1;
    private static final String TAG = "GridLayoutManager";
    int[] mCachedBorders;
    final Rect mDecorInsets = new Rect();
    boolean mPendingSpanCountChange = false;
    final SparseIntArray mPreLayoutSpanIndexCache = new SparseIntArray();
    final SparseIntArray mPreLayoutSpanSizeCache = new SparseIntArray();
    View[] mSet;
    int mSpanCount = -1;
    SpanSizeLookup mSpanSizeLookup = new DefaultSpanSizeLookup();

    public static abstract class SpanSizeLookup {
        private boolean mCacheSpanIndices = false;
        final SparseIntArray mSpanIndexCache = new SparseIntArray();

        public abstract int getSpanSize(int i);

        public void setSpanIndexCacheEnabled(boolean cacheSpanIndices) {
            this.mCacheSpanIndices = cacheSpanIndices;
        }

        public void invalidateSpanIndexCache() {
            this.mSpanIndexCache.clear();
        }

        public boolean isSpanIndexCacheEnabled() {
            return this.mCacheSpanIndices;
        }

        /* Access modifiers changed, original: 0000 */
        public int getCachedSpanIndex(int position, int spanCount) {
            if (!this.mCacheSpanIndices) {
                return getSpanIndex(position, spanCount);
            }
            int existing = this.mSpanIndexCache.get(position, -1);
            if (existing != -1) {
                return existing;
            }
            int value = getSpanIndex(position, spanCount);
            this.mSpanIndexCache.put(position, value);
            return value;
        }

        public int getSpanIndex(int position, int spanCount) {
            int positionSpanSize = getSpanSize(position);
            if (positionSpanSize == spanCount) {
                return 0;
            }
            int prevKey;
            int span = 0;
            int startPos = 0;
            if (this.mCacheSpanIndices && this.mSpanIndexCache.size() > 0) {
                prevKey = findReferenceIndexFromCache(position);
                if (prevKey >= 0) {
                    span = this.mSpanIndexCache.get(prevKey) + getSpanSize(prevKey);
                    startPos = prevKey + 1;
                }
            }
            prevKey = span;
            for (span = startPos; span < position; span++) {
                int size = getSpanSize(span);
                prevKey += size;
                if (prevKey == spanCount) {
                    prevKey = 0;
                } else if (prevKey > spanCount) {
                    prevKey = size;
                }
            }
            if (prevKey + positionSpanSize <= spanCount) {
                return prevKey;
            }
            return 0;
        }

        /* Access modifiers changed, original: 0000 */
        public int findReferenceIndexFromCache(int position) {
            int hi;
            int lo = 0;
            int hi2 = this.mSpanIndexCache.size() - 1;
            while (lo <= hi2) {
                hi = (lo + hi2) >>> 1;
                if (this.mSpanIndexCache.keyAt(hi) < position) {
                    lo = hi + 1;
                } else {
                    hi2 = hi - 1;
                }
            }
            hi = lo - 1;
            if (hi < 0 || hi >= this.mSpanIndexCache.size()) {
                return -1;
            }
            return this.mSpanIndexCache.keyAt(hi);
        }

        public int getSpanGroupIndex(int adapterPosition, int spanCount) {
            int span = 0;
            int group = 0;
            int positionSpanSize = getSpanSize(adapterPosition);
            for (int i = 0; i < adapterPosition; i++) {
                int size = getSpanSize(i);
                span += size;
                if (span == spanCount) {
                    span = 0;
                    group++;
                } else if (span > spanCount) {
                    span = size;
                    group++;
                }
            }
            if (span + positionSpanSize > spanCount) {
                return group + 1;
            }
            return group;
        }
    }

    public static final class DefaultSpanSizeLookup extends SpanSizeLookup {
        public int getSpanSize(int position) {
            return 1;
        }

        public int getSpanIndex(int position, int spanCount) {
            return position % spanCount;
        }
    }

    public static class LayoutParams extends android.support.v7.widget.RecyclerView.LayoutParams {
        public static final int INVALID_SPAN_ID = -1;
        int mSpanIndex = -1;
        int mSpanSize = 0;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(android.support.v7.widget.RecyclerView.LayoutParams source) {
            super(source);
        }

        public int getSpanIndex() {
            return this.mSpanIndex;
        }

        public int getSpanSize() {
            return this.mSpanSize;
        }
    }

    public GridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setSpanCount(LayoutManager.getProperties(context, attrs, defStyleAttr, defStyleRes).spanCount);
    }

    public GridLayoutManager(Context context, int spanCount) {
        super(context);
        setSpanCount(spanCount);
    }

    public GridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        setSpanCount(spanCount);
    }

    public void setStackFromEnd(boolean stackFromEnd) {
        if (stackFromEnd) {
            throw new UnsupportedOperationException("GridLayoutManager does not support stack from end. Consider using reverse layout");
        }
        super.setStackFromEnd(false);
    }

    public int getRowCountForAccessibility(Recycler recycler, State state) {
        if (this.mOrientation == 0) {
            return this.mSpanCount;
        }
        if (state.getItemCount() < 1) {
            return 0;
        }
        return getSpanGroupIndex(recycler, state, state.getItemCount() - 1) + 1;
    }

    public int getColumnCountForAccessibility(Recycler recycler, State state) {
        if (this.mOrientation == 1) {
            return this.mSpanCount;
        }
        if (state.getItemCount() < 1) {
            return 0;
        }
        return getSpanGroupIndex(recycler, state, state.getItemCount() - 1) + 1;
    }

    public void onInitializeAccessibilityNodeInfoForItem(Recycler recycler, State state, View host, AccessibilityNodeInfoCompat info) {
        android.view.ViewGroup.LayoutParams lp = host.getLayoutParams();
        if (lp instanceof LayoutParams) {
            LayoutParams glp = (LayoutParams) lp;
            int spanGroupIndex = getSpanGroupIndex(recycler, state, glp.getViewLayoutPosition());
            if (this.mOrientation == 0) {
                int spanIndex = glp.getSpanIndex();
                int spanSize = glp.getSpanSize();
                boolean z = this.mSpanCount > 1 && glp.getSpanSize() == this.mSpanCount;
                info.setCollectionItemInfo(CollectionItemInfoCompat.obtain(spanIndex, spanSize, spanGroupIndex, 1, z, false));
            } else {
                int spanIndex2 = glp.getSpanIndex();
                int spanSize2 = glp.getSpanSize();
                boolean z2 = this.mSpanCount > 1 && glp.getSpanSize() == this.mSpanCount;
                info.setCollectionItemInfo(CollectionItemInfoCompat.obtain(spanGroupIndex, 1, spanIndex2, spanSize2, z2, false));
            }
            return;
        }
        super.onInitializeAccessibilityNodeInfoForItem(host, info);
    }

    public void onLayoutChildren(Recycler recycler, State state) {
        if (state.isPreLayout()) {
            cachePreLayoutSpanMapping();
        }
        super.onLayoutChildren(recycler, state);
        clearPreLayoutSpanMappingCache();
    }

    public void onLayoutCompleted(State state) {
        super.onLayoutCompleted(state);
        this.mPendingSpanCountChange = false;
    }

    private void clearPreLayoutSpanMappingCache() {
        this.mPreLayoutSpanSizeCache.clear();
        this.mPreLayoutSpanIndexCache.clear();
    }

    private void cachePreLayoutSpanMapping() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
            int viewPosition = lp.getViewLayoutPosition();
            this.mPreLayoutSpanSizeCache.put(viewPosition, lp.getSpanSize());
            this.mPreLayoutSpanIndexCache.put(viewPosition, lp.getSpanIndex());
        }
    }

    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    public void onItemsChanged(RecyclerView recyclerView) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        this.mSpanSizeLookup.invalidateSpanIndexCache();
    }

    public android.support.v7.widget.RecyclerView.LayoutParams generateDefaultLayoutParams() {
        if (this.mOrientation == 0) {
            return new LayoutParams(-2, -1);
        }
        return new LayoutParams(-1, -2);
    }

    public android.support.v7.widget.RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    public android.support.v7.widget.RecyclerView.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams lp) {
        if (lp instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    public boolean checkLayoutParams(android.support.v7.widget.RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    public void setSpanSizeLookup(SpanSizeLookup spanSizeLookup) {
        this.mSpanSizeLookup = spanSizeLookup;
    }

    public SpanSizeLookup getSpanSizeLookup() {
        return this.mSpanSizeLookup;
    }

    private void updateMeasurements() {
        int totalSpace;
        if (getOrientation() == 1) {
            totalSpace = (getWidth() - getPaddingRight()) - getPaddingLeft();
        } else {
            totalSpace = (getHeight() - getPaddingBottom()) - getPaddingTop();
        }
        calculateItemBorders(totalSpace);
    }

    public void setMeasuredDimension(Rect childrenBounds, int wSpec, int hSpec) {
        int width;
        int height;
        if (this.mCachedBorders == null) {
            super.setMeasuredDimension(childrenBounds, wSpec, hSpec);
        }
        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingTop() + getPaddingBottom();
        if (this.mOrientation == 1) {
            int chooseSize = LayoutManager.chooseSize(hSpec, childrenBounds.height() + verticalPadding, getMinimumHeight());
            width = LayoutManager.chooseSize(wSpec, this.mCachedBorders[this.mCachedBorders.length - 1] + horizontalPadding, getMinimumWidth());
            height = chooseSize;
        } else {
            width = LayoutManager.chooseSize(wSpec, childrenBounds.width() + horizontalPadding, getMinimumWidth());
            height = LayoutManager.chooseSize(hSpec, this.mCachedBorders[this.mCachedBorders.length - 1] + verticalPadding, getMinimumHeight());
        }
        setMeasuredDimension(width, height);
    }

    private void calculateItemBorders(int totalSpace) {
        this.mCachedBorders = calculateItemBorders(this.mCachedBorders, this.mSpanCount, totalSpace);
    }

    static int[] calculateItemBorders(int[] cachedBorders, int spanCount, int totalSpace) {
        int i = 1;
        if (!(cachedBorders != null && cachedBorders.length == spanCount + 1 && cachedBorders[cachedBorders.length - 1] == totalSpace)) {
            cachedBorders = new int[(spanCount + 1)];
        }
        cachedBorders[0] = 0;
        int sizePerSpan = totalSpace / spanCount;
        int sizePerSpanRemainder = totalSpace % spanCount;
        int consumedPixels = 0;
        int additionalSize = 0;
        while (i <= spanCount) {
            int itemSize = sizePerSpan;
            additionalSize += sizePerSpanRemainder;
            if (additionalSize > 0 && spanCount - additionalSize < sizePerSpanRemainder) {
                itemSize++;
                additionalSize -= spanCount;
            }
            consumedPixels += itemSize;
            cachedBorders[i] = consumedPixels;
            i++;
        }
        return cachedBorders;
    }

    /* Access modifiers changed, original: 0000 */
    public int getSpaceForSpanRange(int startSpan, int spanSize) {
        if (this.mOrientation == 1 && isLayoutRTL()) {
            return this.mCachedBorders[this.mSpanCount - startSpan] - this.mCachedBorders[(this.mSpanCount - startSpan) - spanSize];
        }
        return this.mCachedBorders[startSpan + spanSize] - this.mCachedBorders[startSpan];
    }

    /* Access modifiers changed, original: 0000 */
    public void onAnchorReady(Recycler recycler, State state, AnchorInfo anchorInfo, int itemDirection) {
        super.onAnchorReady(recycler, state, anchorInfo, itemDirection);
        updateMeasurements();
        if (state.getItemCount() > 0 && !state.isPreLayout()) {
            ensureAnchorIsInCorrectSpan(recycler, state, anchorInfo, itemDirection);
        }
        ensureViewSet();
    }

    private void ensureViewSet() {
        if (this.mSet == null || this.mSet.length != this.mSpanCount) {
            this.mSet = new View[this.mSpanCount];
        }
    }

    public int scrollHorizontallyBy(int dx, Recycler recycler, State state) {
        updateMeasurements();
        ensureViewSet();
        return super.scrollHorizontallyBy(dx, recycler, state);
    }

    public int scrollVerticallyBy(int dy, Recycler recycler, State state) {
        updateMeasurements();
        ensureViewSet();
        return super.scrollVerticallyBy(dy, recycler, state);
    }

    private void ensureAnchorIsInCorrectSpan(Recycler recycler, State state, AnchorInfo anchorInfo, int itemDirection) {
        boolean layingOutInPrimaryDirection = itemDirection == 1;
        int span = getSpanIndex(recycler, state, anchorInfo.mPosition);
        if (layingOutInPrimaryDirection) {
            while (span > 0 && anchorInfo.mPosition > 0) {
                anchorInfo.mPosition--;
                span = getSpanIndex(recycler, state, anchorInfo.mPosition);
            }
            return;
        }
        int indexLimit = state.getItemCount() - 1;
        int pos = anchorInfo.mPosition;
        int bestSpan = span;
        while (pos < indexLimit) {
            int next = getSpanIndex(recycler, state, pos + 1);
            if (next <= bestSpan) {
                break;
            }
            pos++;
            bestSpan = next;
        }
        anchorInfo.mPosition = pos;
    }

    /* Access modifiers changed, original: 0000 */
    public View findReferenceChild(Recycler recycler, State state, int start, int end, int itemCount) {
        ensureLayoutState();
        View outOfBoundsMatch = null;
        int boundsStart = this.mOrientationHelper.getStartAfterPadding();
        int boundsEnd = this.mOrientationHelper.getEndAfterPadding();
        int diff = end > start ? 1 : -1;
        View invalidMatch = null;
        for (int i = start; i != end; i += diff) {
            View view = getChildAt(i);
            int position = getPosition(view);
            if (position >= 0 && position < itemCount && getSpanIndex(recycler, state, position) == 0) {
                if (((android.support.v7.widget.RecyclerView.LayoutParams) view.getLayoutParams()).isItemRemoved()) {
                    if (invalidMatch == null) {
                        invalidMatch = view;
                    }
                } else if (this.mOrientationHelper.getDecoratedStart(view) < boundsEnd && this.mOrientationHelper.getDecoratedEnd(view) >= boundsStart) {
                    return view;
                } else {
                    if (outOfBoundsMatch == null) {
                        outOfBoundsMatch = view;
                    }
                }
            }
        }
        return outOfBoundsMatch != null ? outOfBoundsMatch : invalidMatch;
    }

    private int getSpanGroupIndex(Recycler recycler, State state, int viewPosition) {
        if (!state.isPreLayout()) {
            return this.mSpanSizeLookup.getSpanGroupIndex(viewPosition, this.mSpanCount);
        }
        int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(viewPosition);
        if (adapterPosition != -1) {
            return this.mSpanSizeLookup.getSpanGroupIndex(adapterPosition, this.mSpanCount);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot find span size for pre layout position. ");
        stringBuilder.append(viewPosition);
        Log.w(str, stringBuilder.toString());
        return 0;
    }

    private int getSpanIndex(Recycler recycler, State state, int pos) {
        if (!state.isPreLayout()) {
            return this.mSpanSizeLookup.getCachedSpanIndex(pos, this.mSpanCount);
        }
        int cached = this.mPreLayoutSpanIndexCache.get(pos, -1);
        if (cached != -1) {
            return cached;
        }
        int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition != -1) {
            return this.mSpanSizeLookup.getCachedSpanIndex(adapterPosition, this.mSpanCount);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot find span size for pre layout position. It is not cached, not in the adapter. Pos:");
        stringBuilder.append(pos);
        Log.w(str, stringBuilder.toString());
        return 0;
    }

    private int getSpanSize(Recycler recycler, State state, int pos) {
        if (!state.isPreLayout()) {
            return this.mSpanSizeLookup.getSpanSize(pos);
        }
        int cached = this.mPreLayoutSpanSizeCache.get(pos, -1);
        if (cached != -1) {
            return cached;
        }
        int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(pos);
        if (adapterPosition != -1) {
            return this.mSpanSizeLookup.getSpanSize(adapterPosition);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot find span size for pre layout position. It is not cached, not in the adapter. Pos:");
        stringBuilder.append(pos);
        Log.w(str, stringBuilder.toString());
        return 1;
    }

    /* Access modifiers changed, original: 0000 */
    public void collectPrefetchPositionsForLayoutState(State state, LayoutState layoutState, LayoutPrefetchRegistry layoutPrefetchRegistry) {
        int remainingSpan = this.mSpanCount;
        for (int count = 0; count < this.mSpanCount && layoutState.hasMore(state) && remainingSpan > 0; count++) {
            int pos = layoutState.mCurrentPosition;
            layoutPrefetchRegistry.addPosition(pos, Math.max(0, layoutState.mScrollingOffset));
            remainingSpan -= this.mSpanSizeLookup.getSpanSize(pos);
            layoutState.mCurrentPosition += layoutState.mItemDirection;
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void layoutChunk(Recycler recycler, State state, LayoutState layoutState, LayoutChunkResult result) {
        int pos;
        int spanSize;
        Recycler recycler2 = recycler;
        State state2 = state;
        LayoutState layoutState2 = layoutState;
        LayoutChunkResult layoutChunkResult = result;
        int otherDirSpecMode = this.mOrientationHelper.getModeInOther();
        int i = 0;
        boolean flexibleInOtherDir = otherDirSpecMode != Ints.MAX_POWER_OF_TWO;
        int currentOtherDirSize = getChildCount() > 0 ? this.mCachedBorders[this.mSpanCount] : 0;
        if (flexibleInOtherDir) {
            updateMeasurements();
        }
        boolean layingOutInPrimaryDirection = layoutState2.mItemDirection == 1;
        int remainingSpan = this.mSpanCount;
        if (!layingOutInPrimaryDirection) {
            remainingSpan = getSpanIndex(recycler2, state2, layoutState2.mCurrentPosition) + getSpanSize(recycler2, state2, layoutState2.mCurrentPosition);
        }
        int count = 0;
        int consumedSpanCount = 0;
        while (count < this.mSpanCount && layoutState2.hasMore(state2) && remainingSpan > 0) {
            pos = layoutState2.mCurrentPosition;
            spanSize = getSpanSize(recycler2, state2, pos);
            if (spanSize <= this.mSpanCount) {
                remainingSpan -= spanSize;
                if (remainingSpan < 0) {
                    break;
                }
                View view = layoutState2.next(recycler2);
                if (view == null) {
                    break;
                }
                consumedSpanCount += spanSize;
                this.mSet[count] = view;
                count++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Item at position ");
                stringBuilder.append(pos);
                stringBuilder.append(" requires ");
                stringBuilder.append(spanSize);
                stringBuilder.append(" spans but GridLayoutManager has only ");
                stringBuilder.append(this.mSpanCount);
                stringBuilder.append(" spans.");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        int remainingSpan2 = remainingSpan;
        if (count == 0) {
            layoutChunkResult.mFinished = true;
            return;
        }
        View view2;
        int size;
        float maxSizeInOther;
        int horizontalInsets;
        int otherDirSpecMode2;
        int remainingSpan3;
        int count2 = count;
        int currentOtherDirSize2 = currentOtherDirSize;
        assignSpans(recycler2, state2, count, consumedSpanCount, layingOutInPrimaryDirection);
        pos = 0;
        spanSize = 0;
        float maxSizeInOther2 = 0.0f;
        while (pos < count2) {
            view2 = this.mSet[pos];
            if (layoutState2.mScrapList == null) {
                if (layingOutInPrimaryDirection) {
                    addView(view2);
                } else {
                    addView(view2, i);
                }
            } else if (layingOutInPrimaryDirection) {
                addDisappearingView(view2);
            } else {
                addDisappearingView(view2, i);
            }
            calculateItemDecorationsForChild(view2, this.mDecorInsets);
            measureChild(view2, otherDirSpecMode, i);
            size = this.mOrientationHelper.getDecoratedMeasurement(view2);
            if (size > spanSize) {
                spanSize = size;
            }
            float otherSize = (1.0f * ((float) this.mOrientationHelper.getDecoratedMeasurementInOther(view2))) / ((float) ((LayoutParams) view2.getLayoutParams()).mSpanSize);
            if (otherSize > maxSizeInOther2) {
                maxSizeInOther2 = otherSize;
            }
            pos++;
            i = 0;
        }
        if (flexibleInOtherDir) {
            guessMeasurement(maxSizeInOther2, currentOtherDirSize2);
            spanSize = 0;
            for (pos = 0; pos < count2; pos++) {
                view2 = this.mSet[pos];
                measureChild(view2, Ints.MAX_POWER_OF_TWO, true);
                size = this.mOrientationHelper.getDecoratedMeasurement(view2);
                if (size > spanSize) {
                    spanSize = size;
                }
            }
            count = spanSize;
        } else {
            count = spanSize;
        }
        pos = 0;
        while (pos < count2) {
            View view3 = this.mSet[pos];
            if (this.mOrientationHelper.getDecoratedMeasurement(view3) != count) {
                int wSpec;
                LayoutParams lp = (LayoutParams) view3.getLayoutParams();
                Rect decorInsets = lp.mDecorInsets;
                maxSizeInOther = maxSizeInOther2;
                maxSizeInOther2 = ((decorInsets.top + decorInsets.bottom) + lp.topMargin) + lp.bottomMargin;
                horizontalInsets = ((decorInsets.left + decorInsets.right) + lp.leftMargin) + lp.rightMargin;
                size = getSpaceForSpanRange(lp.mSpanIndex, lp.mSpanSize);
                otherDirSpecMode2 = otherDirSpecMode;
                if (this.mOrientation == 1) {
                    remainingSpan3 = remainingSpan2;
                    wSpec = LayoutManager.getChildMeasureSpec(size, Ints.MAX_POWER_OF_TWO, horizontalInsets, lp.width, false);
                    otherDirSpecMode = MeasureSpec.makeMeasureSpec(count - maxSizeInOther2, Ints.MAX_POWER_OF_TWO);
                    LayoutParams layoutParams = lp;
                } else {
                    remainingSpan3 = remainingSpan2;
                    wSpec = MeasureSpec.makeMeasureSpec(count - horizontalInsets, Ints.MAX_POWER_OF_TWO);
                    otherDirSpecMode = LayoutManager.getChildMeasureSpec(size, Ints.MAX_POWER_OF_TWO, maxSizeInOther2, lp.height, null);
                }
                measureChildWithDecorationsAndMargin(view3, wSpec, otherDirSpecMode, true);
            } else {
                maxSizeInOther = maxSizeInOther2;
                otherDirSpecMode2 = otherDirSpecMode;
                remainingSpan3 = remainingSpan2;
            }
            pos++;
            maxSizeInOther2 = maxSizeInOther;
            otherDirSpecMode = otherDirSpecMode2;
            remainingSpan2 = remainingSpan3;
            recycler2 = recycler;
            state2 = state;
        }
        maxSizeInOther = maxSizeInOther2;
        otherDirSpecMode2 = otherDirSpecMode;
        remainingSpan3 = remainingSpan2;
        layoutChunkResult.mConsumed = count;
        pos = 0;
        spanSize = 0;
        remainingSpan = 0;
        size = 0;
        if (this.mOrientation == 1) {
            if (layoutState2.mLayoutDirection == -1) {
                size = layoutState2.mOffset;
                remainingSpan = size - count;
            } else {
                remainingSpan = layoutState2.mOffset;
                size = remainingSpan + count;
            }
        } else if (layoutState2.mLayoutDirection == -1) {
            spanSize = layoutState2.mOffset;
            pos = spanSize - count;
        } else {
            pos = layoutState2.mOffset;
            spanSize = pos + count;
        }
        int i2 = 0;
        while (true) {
            horizontalInsets = i2;
            int right;
            int maxSize;
            float maxSizeInOther3;
            if (horizontalInsets < count2) {
                int left;
                int bottom;
                View view4 = this.mSet[horizontalInsets];
                LayoutParams params = (LayoutParams) view4.getLayoutParams();
                if (this.mOrientation != 1) {
                    left = pos;
                    right = spanSize;
                    pos = getPaddingTop() + this.mCachedBorders[params.mSpanIndex];
                    remainingSpan2 = pos;
                    bottom = this.mOrientationHelper.getDecoratedMeasurementInOther(view4) + pos;
                } else if (isLayoutRTL()) {
                    currentOtherDirSize = getPaddingLeft() + this.mCachedBorders[this.mSpanCount - params.mSpanIndex];
                    left = currentOtherDirSize - this.mOrientationHelper.getDecoratedMeasurementInOther(view4);
                    remainingSpan2 = remainingSpan;
                    bottom = size;
                    right = currentOtherDirSize;
                } else {
                    left = pos;
                    right = spanSize;
                    pos = getPaddingLeft() + this.mCachedBorders[params.mSpanIndex];
                    left = pos;
                    right = this.mOrientationHelper.getDecoratedMeasurementInOther(view4) + pos;
                    remainingSpan2 = remainingSpan;
                    bottom = size;
                }
                maxSize = count;
                maxSizeInOther3 = maxSizeInOther;
                layoutDecoratedWithMargins(view4, left, remainingSpan2, right, bottom);
                if (params.isItemRemoved() != 0 || params.isItemChanged() != 0) {
                    layoutChunkResult.mIgnoreConsumed = true;
                }
                layoutChunkResult.mFocusable |= view4.hasFocusable();
                i2 = horizontalInsets + 1;
                remainingSpan = remainingSpan2;
                size = bottom;
                count = maxSize;
                maxSizeInOther = maxSizeInOther3;
                pos = left;
                spanSize = right;
            } else {
                right = spanSize;
                maxSize = count;
                maxSizeInOther3 = maxSizeInOther;
                Arrays.fill(this.mSet, null);
                return;
            }
        }
    }

    private void measureChild(View view, int otherDirParentSpecMode, boolean alreadyMeasured) {
        int wSpec;
        int hSpec;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        Rect decorInsets = lp.mDecorInsets;
        int verticalInsets = ((decorInsets.top + decorInsets.bottom) + lp.topMargin) + lp.bottomMargin;
        int horizontalInsets = ((decorInsets.left + decorInsets.right) + lp.leftMargin) + lp.rightMargin;
        int availableSpaceInOther = getSpaceForSpanRange(lp.mSpanIndex, lp.mSpanSize);
        if (this.mOrientation == 1) {
            wSpec = LayoutManager.getChildMeasureSpec(availableSpaceInOther, otherDirParentSpecMode, horizontalInsets, lp.width, false);
            hSpec = LayoutManager.getChildMeasureSpec(this.mOrientationHelper.getTotalSpace(), getHeightMode(), verticalInsets, lp.height, true);
        } else {
            hSpec = LayoutManager.getChildMeasureSpec(availableSpaceInOther, otherDirParentSpecMode, verticalInsets, lp.height, false);
            wSpec = LayoutManager.getChildMeasureSpec(this.mOrientationHelper.getTotalSpace(), getWidthMode(), horizontalInsets, lp.width, true);
        }
        measureChildWithDecorationsAndMargin(view, wSpec, hSpec, alreadyMeasured);
    }

    private void guessMeasurement(float maxSizeInOther, int currentOtherDirSize) {
        calculateItemBorders(Math.max(Math.round(((float) this.mSpanCount) * maxSizeInOther), currentOtherDirSize));
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec, boolean alreadyMeasured) {
        boolean measure;
        android.support.v7.widget.RecyclerView.LayoutParams lp = (android.support.v7.widget.RecyclerView.LayoutParams) child.getLayoutParams();
        if (alreadyMeasured) {
            measure = shouldReMeasureChild(child, widthSpec, heightSpec, lp);
        } else {
            measure = shouldMeasureChild(child, widthSpec, heightSpec, lp);
        }
        if (measure) {
            child.measure(widthSpec, heightSpec);
        }
    }

    private void assignSpans(Recycler recycler, State state, int count, int consumedSpanCount, boolean layingOutInPrimaryDirection) {
        int start;
        int end;
        int diff;
        if (layingOutInPrimaryDirection) {
            start = 0;
            end = count;
            diff = 1;
        } else {
            start = count - 1;
            end = -1;
            diff = -1;
        }
        int span = 0;
        for (int i = start; i != end; i += diff) {
            View view = this.mSet[i];
            LayoutParams params = (LayoutParams) view.getLayoutParams();
            params.mSpanSize = getSpanSize(recycler, state, getPosition(view));
            params.mSpanIndex = span;
            span += params.mSpanSize;
        }
    }

    public int getSpanCount() {
        return this.mSpanCount;
    }

    public void setSpanCount(int spanCount) {
        if (spanCount != this.mSpanCount) {
            this.mPendingSpanCountChange = true;
            if (spanCount >= 1) {
                this.mSpanCount = spanCount;
                this.mSpanSizeLookup.invalidateSpanIndexCache();
                requestLayout();
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Span count should be at least 1. Provided ");
            stringBuilder.append(spanCount);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:74:0x013e  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x013e  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x013e  */
    /* JADX WARNING: Missing block: B:28:0x0089, code skipped:
            r29 = r3;
            r33 = r4;
            r34 = r5;
            r30 = r11;
     */
    public android.view.View onFocusSearchFailed(android.view.View r37, int r38, android.support.v7.widget.RecyclerView.Recycler r39, android.support.v7.widget.RecyclerView.State r40) {
        /*
        r36 = this;
        r0 = r36;
        r1 = r39;
        r2 = r40;
        r3 = r36.findContainingItemView(r37);
        r4 = 0;
        if (r3 != 0) goto L_0x000e;
    L_0x000d:
        return r4;
    L_0x000e:
        r5 = r3.getLayoutParams();
        r5 = (android.support.v7.widget.GridLayoutManager.LayoutParams) r5;
        r6 = r5.mSpanIndex;
        r7 = r5.mSpanIndex;
        r8 = r5.mSpanSize;
        r7 = r7 + r8;
        r8 = super.onFocusSearchFailed(r37, r38, r39, r40);
        if (r8 != 0) goto L_0x0022;
    L_0x0021:
        return r4;
    L_0x0022:
        r4 = r38;
        r9 = r0.convertFocusDirectionToLayoutDirection(r4);
        r11 = 1;
        if (r9 != r11) goto L_0x002d;
    L_0x002b:
        r12 = r11;
        goto L_0x002e;
    L_0x002d:
        r12 = 0;
    L_0x002e:
        r13 = r0.mShouldReverseLayout;
        if (r12 == r13) goto L_0x0034;
    L_0x0032:
        r12 = r11;
        goto L_0x0035;
    L_0x0034:
        r12 = 0;
    L_0x0035:
        if (r12 == 0) goto L_0x003f;
    L_0x0037:
        r13 = r36.getChildCount();
        r13 = r13 - r11;
        r14 = -1;
        r15 = -1;
        goto L_0x0045;
    L_0x003f:
        r13 = 0;
        r14 = 1;
        r15 = r36.getChildCount();
    L_0x0045:
        r10 = r0.mOrientation;
        if (r10 != r11) goto L_0x0051;
    L_0x0049:
        r10 = r36.isLayoutRTL();
        if (r10 == 0) goto L_0x0051;
    L_0x004f:
        r10 = r11;
        goto L_0x0052;
    L_0x0051:
        r10 = 0;
    L_0x0052:
        r17 = 0;
        r18 = -1;
        r19 = 0;
        r20 = 0;
        r21 = -1;
        r22 = 0;
        r11 = r0.getSpanGroupIndex(r1, r2, r13);
        r23 = r5;
        r24 = r8;
        r25 = r9;
        r4 = r18;
        r5 = r19;
        r8 = r21;
        r9 = r22;
        r18 = r17;
        r17 = r13;
    L_0x0074:
        r26 = r17;
        r27 = r12;
        r12 = r26;
        if (r12 == r15) goto L_0x0181;
    L_0x007c:
        r28 = r13;
        r13 = r0.getSpanGroupIndex(r1, r2, r12);
        r1 = r0.getChildAt(r12);
        if (r1 != r3) goto L_0x0093;
    L_0x0089:
        r29 = r3;
        r33 = r4;
        r34 = r5;
        r30 = r11;
        goto L_0x018b;
    L_0x0093:
        r17 = r1.hasFocusable();
        if (r17 == 0) goto L_0x00a8;
    L_0x0099:
        if (r13 == r11) goto L_0x00a8;
    L_0x009b:
        if (r18 == 0) goto L_0x009e;
    L_0x009d:
        goto L_0x0089;
    L_0x009e:
        r29 = r3;
        r33 = r4;
        r34 = r5;
        r30 = r11;
        goto L_0x016d;
    L_0x00a8:
        r17 = r1.getLayoutParams();
        r2 = r17;
        r2 = (android.support.v7.widget.GridLayoutManager.LayoutParams) r2;
        r29 = r3;
        r3 = r2.mSpanIndex;
        r30 = r11;
        r11 = r2.mSpanIndex;
        r31 = r13;
        r13 = r2.mSpanSize;
        r11 = r11 + r13;
        r13 = r1.hasFocusable();
        if (r13 == 0) goto L_0x00c8;
    L_0x00c3:
        if (r3 != r6) goto L_0x00c8;
    L_0x00c5:
        if (r11 != r7) goto L_0x00c8;
    L_0x00c7:
        return r1;
    L_0x00c8:
        r13 = 0;
        r17 = r1.hasFocusable();
        if (r17 == 0) goto L_0x00d1;
    L_0x00cf:
        if (r18 == 0) goto L_0x00d9;
    L_0x00d1:
        r17 = r1.hasFocusable();
        if (r17 != 0) goto L_0x00e1;
    L_0x00d7:
        if (r20 != 0) goto L_0x00e1;
    L_0x00d9:
        r13 = 1;
        r33 = r4;
    L_0x00dc:
        r34 = r5;
    L_0x00de:
        r5 = 0;
        goto L_0x013c;
    L_0x00e1:
        r17 = java.lang.Math.max(r3, r6);
        r19 = java.lang.Math.min(r11, r7);
        r32 = r13;
        r13 = r19 - r17;
        r21 = r1.hasFocusable();
        if (r21 == 0) goto L_0x0118;
    L_0x00f3:
        if (r13 <= r5) goto L_0x00fe;
    L_0x00f5:
        r21 = 1;
        r33 = r4;
        r34 = r5;
        r13 = r21;
        goto L_0x00de;
    L_0x00fe:
        if (r13 != r5) goto L_0x0112;
    L_0x0100:
        if (r3 <= r4) goto L_0x0106;
    L_0x0102:
        r33 = r4;
        r4 = 1;
        goto L_0x0109;
    L_0x0106:
        r33 = r4;
        r4 = 0;
    L_0x0109:
        if (r10 != r4) goto L_0x010e;
    L_0x010b:
        r4 = 1;
        r13 = r4;
        goto L_0x00dc;
    L_0x010e:
        r34 = r5;
        r5 = 0;
        goto L_0x013a;
    L_0x0112:
        r33 = r4;
        r34 = r5;
        r5 = 0;
        goto L_0x013a;
    L_0x0118:
        r33 = r4;
        if (r18 != 0) goto L_0x0137;
    L_0x011c:
        r34 = r5;
        r4 = 1;
        r5 = 0;
        r16 = r0.isViewPartiallyVisible(r1, r5, r4);
        if (r16 == 0) goto L_0x013a;
    L_0x0126:
        if (r13 <= r9) goto L_0x012d;
    L_0x0128:
        r16 = 1;
        r13 = r16;
        goto L_0x013c;
    L_0x012d:
        if (r13 != r9) goto L_0x013a;
    L_0x012f:
        if (r3 <= r8) goto L_0x0132;
    L_0x0131:
        goto L_0x0133;
    L_0x0132:
        r4 = r5;
    L_0x0133:
        if (r10 != r4) goto L_0x013a;
    L_0x0135:
        r13 = 1;
        goto L_0x013c;
    L_0x0137:
        r34 = r5;
        r5 = 0;
    L_0x013a:
        r13 = r32;
    L_0x013c:
        if (r13 == 0) goto L_0x016d;
    L_0x013e:
        r4 = r1.hasFocusable();
        if (r4 == 0) goto L_0x0157;
    L_0x0144:
        r4 = r1;
        r5 = r2.mSpanIndex;
        r16 = java.lang.Math.min(r11, r7);
        r17 = java.lang.Math.max(r3, r6);
        r16 = r16 - r17;
        r18 = r4;
        r4 = r5;
        r5 = r16;
        goto L_0x0171;
    L_0x0157:
        r4 = r1;
        r5 = r2.mSpanIndex;
        r8 = java.lang.Math.min(r11, r7);
        r16 = java.lang.Math.max(r3, r6);
        r8 = r8 - r16;
        r20 = r4;
        r9 = r8;
        r4 = r33;
        r8 = r5;
        r5 = r34;
        goto L_0x0171;
    L_0x016d:
        r4 = r33;
        r5 = r34;
    L_0x0171:
        r17 = r12 + r14;
        r12 = r27;
        r13 = r28;
        r3 = r29;
        r11 = r30;
        r1 = r39;
        r2 = r40;
        goto L_0x0074;
    L_0x0181:
        r29 = r3;
        r33 = r4;
        r34 = r5;
        r30 = r11;
        r28 = r13;
    L_0x018b:
        if (r18 == 0) goto L_0x0190;
    L_0x018d:
        r1 = r18;
        goto L_0x0192;
    L_0x0190:
        r1 = r20;
    L_0x0192:
        return r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.widget.GridLayoutManager.onFocusSearchFailed(android.view.View, int, android.support.v7.widget.RecyclerView$Recycler, android.support.v7.widget.RecyclerView$State):android.view.View");
    }

    public boolean supportsPredictiveItemAnimations() {
        return this.mPendingSavedState == null && !this.mPendingSpanCountChange;
    }
}
