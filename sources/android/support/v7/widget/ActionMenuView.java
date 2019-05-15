package android.support.v7.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.annotation.StyleRes;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuBuilder.ItemInvoker;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.view.menu.MenuPresenter.Callback;
import android.support.v7.view.menu.MenuView;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewDebug.ExportedProperty;
import android.view.accessibility.AccessibilityEvent;
import com.google.common.primitives.Ints;

public class ActionMenuView extends LinearLayoutCompat implements ItemInvoker, MenuView {
    static final int GENERATED_ITEM_PADDING = 4;
    static final int MIN_CELL_SIZE = 56;
    private static final String TAG = "ActionMenuView";
    private Callback mActionMenuPresenterCallback;
    private boolean mFormatItems;
    private int mFormatItemsWidth;
    private int mGeneratedItemPadding;
    private MenuBuilder mMenu;
    MenuBuilder.Callback mMenuBuilderCallback;
    private int mMinCellSize;
    OnMenuItemClickListener mOnMenuItemClickListener;
    private Context mPopupContext;
    private int mPopupTheme;
    private ActionMenuPresenter mPresenter;
    private boolean mReserveOverflow;

    @RestrictTo({Scope.LIBRARY_GROUP})
    public interface ActionMenuChildView {
        boolean needsDividerAfter();

        boolean needsDividerBefore();
    }

    public interface OnMenuItemClickListener {
        boolean onMenuItemClick(MenuItem menuItem);
    }

    private static class ActionMenuPresenterCallback implements Callback {
        ActionMenuPresenterCallback() {
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        }

        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            return false;
        }
    }

    public static class LayoutParams extends android.support.v7.widget.LinearLayoutCompat.LayoutParams {
        @ExportedProperty
        public int cellsUsed;
        @ExportedProperty
        public boolean expandable;
        boolean expanded;
        @ExportedProperty
        public int extraPixels;
        @ExportedProperty
        public boolean isOverflowButton;
        @ExportedProperty
        public boolean preventEdgeOffset;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams other) {
            super(other);
        }

        public LayoutParams(LayoutParams other) {
            super((android.view.ViewGroup.LayoutParams) other);
            this.isOverflowButton = other.isOverflowButton;
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.isOverflowButton = false;
        }

        LayoutParams(int width, int height, boolean isOverflowButton) {
            super(width, height);
            this.isOverflowButton = isOverflowButton;
        }
    }

    private class MenuBuilderCallback implements MenuBuilder.Callback {
        MenuBuilderCallback() {
        }

        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            return ActionMenuView.this.mOnMenuItemClickListener != null && ActionMenuView.this.mOnMenuItemClickListener.onMenuItemClick(item);
        }

        public void onMenuModeChange(MenuBuilder menu) {
            if (ActionMenuView.this.mMenuBuilderCallback != null) {
                ActionMenuView.this.mMenuBuilderCallback.onMenuModeChange(menu);
            }
        }
    }

    public ActionMenuView(Context context) {
        this(context, null);
    }

    public ActionMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBaselineAligned(false);
        float density = context.getResources().getDisplayMetrics().density;
        this.mMinCellSize = (int) (56.0f * density);
        this.mGeneratedItemPadding = (int) (4.0f * density);
        this.mPopupContext = context;
        this.mPopupTheme = 0;
    }

    public void setPopupTheme(@StyleRes int resId) {
        if (this.mPopupTheme != resId) {
            this.mPopupTheme = resId;
            if (resId == 0) {
                this.mPopupContext = getContext();
            } else {
                this.mPopupContext = new ContextThemeWrapper(getContext(), resId);
            }
        }
    }

    public int getPopupTheme() {
        return this.mPopupTheme;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public void setPresenter(ActionMenuPresenter presenter) {
        this.mPresenter = presenter;
        this.mPresenter.setMenuView(this);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mPresenter != null) {
            this.mPresenter.updateMenuView(false);
            if (this.mPresenter.isOverflowMenuShowing()) {
                this.mPresenter.hideOverflowMenu();
                this.mPresenter.showOverflowMenu();
            }
        }
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.mOnMenuItemClickListener = listener;
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean wasFormatted = this.mFormatItems;
        this.mFormatItems = MeasureSpec.getMode(widthMeasureSpec) == Ints.MAX_POWER_OF_TWO;
        if (wasFormatted != this.mFormatItems) {
            this.mFormatItemsWidth = 0;
        }
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (!(!this.mFormatItems || this.mMenu == null || widthSize == this.mFormatItemsWidth)) {
            this.mFormatItemsWidth = widthSize;
            this.mMenu.onItemsChanged(true);
        }
        int childCount = getChildCount();
        if (!this.mFormatItems || childCount <= 0) {
            for (int i = 0; i < childCount; i++) {
                LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
                lp.rightMargin = 0;
                lp.leftMargin = 0;
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        onMeasureExactFormat(widthMeasureSpec, heightMeasureSpec);
    }

    private void onMeasureExactFormat(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthPadding = getPaddingLeft() + getPaddingRight();
        int heightPadding = getPaddingTop() + getPaddingBottom();
        int itemHeightSpec = getChildMeasureSpec(heightMeasureSpec, heightPadding, -2);
        widthSize -= widthPadding;
        int cellCount = widthSize / this.mMinCellSize;
        int cellSizeRemaining = widthSize % this.mMinCellSize;
        if (cellCount == 0) {
            setMeasuredDimension(widthSize, 0);
            return;
        }
        int cellCount2;
        int cellSizeRemaining2;
        boolean isGeneratedItem;
        int i;
        boolean needsExpansion;
        int widthSize2;
        boolean z;
        LayoutParams lp;
        int cellSize = this.mMinCellSize + (cellSizeRemaining / cellCount);
        int cellsRemaining = cellCount;
        boolean hasOverflow = false;
        long smallestItemsAt = 0;
        int childCount = getChildCount();
        int heightSize2 = heightSize;
        heightSize = 0;
        int visibleItemCount = 0;
        int expandableItemCount = 0;
        int maxCellsUsed = 0;
        int cellsRemaining2 = cellsRemaining;
        cellsRemaining = 0;
        while (true) {
            int widthPadding2 = widthPadding;
            if (cellsRemaining >= childCount) {
                break;
            }
            int heightPadding2;
            View child = getChildAt(cellsRemaining);
            cellCount2 = cellCount;
            if (child.getVisibility() == 8) {
                heightPadding2 = heightPadding;
                cellSizeRemaining2 = cellSizeRemaining;
            } else {
                int visibleItemCount2;
                boolean visibleItemCount3;
                isGeneratedItem = child instanceof ActionMenuItemView;
                visibleItemCount++;
                if (isGeneratedItem) {
                    cellSizeRemaining2 = cellSizeRemaining;
                    visibleItemCount2 = visibleItemCount;
                    visibleItemCount3 = false;
                    child.setPadding(this.mGeneratedItemPadding, 0, this.mGeneratedItemPadding, 0);
                } else {
                    cellSizeRemaining2 = cellSizeRemaining;
                    visibleItemCount2 = visibleItemCount;
                    visibleItemCount3 = false;
                }
                LayoutParams lp2 = (LayoutParams) child.getLayoutParams();
                lp2.expanded = visibleItemCount3;
                lp2.extraPixels = visibleItemCount3;
                lp2.cellsUsed = visibleItemCount3;
                lp2.expandable = visibleItemCount3;
                lp2.leftMargin = visibleItemCount3;
                lp2.rightMargin = visibleItemCount3;
                boolean z2 = isGeneratedItem && ((ActionMenuItemView) child).hasText();
                lp2.preventEdgeOffset = z2;
                visibleItemCount = measureChildForCells(child, cellSize, lp2.isOverflowButton ? 1 : cellsRemaining2, itemHeightSpec, heightPadding);
                maxCellsUsed = Math.max(maxCellsUsed, visibleItemCount);
                heightPadding2 = heightPadding;
                if (lp2.expandable != 0) {
                    expandableItemCount++;
                }
                if (lp2.isOverflowButton) {
                    hasOverflow = true;
                }
                cellsRemaining2 -= visibleItemCount;
                heightSize = Math.max(heightSize, child.getMeasuredHeight());
                if (visibleItemCount == 1) {
                    smallestItemsAt |= (long) (1 << cellsRemaining);
                    visibleItemCount = visibleItemCount2;
                    heightSize = heightSize;
                } else {
                    visibleItemCount = visibleItemCount2;
                }
            }
            cellsRemaining++;
            widthPadding = widthPadding2;
            cellCount = cellCount2;
            cellSizeRemaining = cellSizeRemaining2;
            heightPadding = heightPadding2;
            i = heightMeasureSpec;
        }
        cellCount2 = cellCount;
        cellSizeRemaining2 = cellSizeRemaining;
        boolean centerSingleExpandedItem = hasOverflow && visibleItemCount == 2;
        isGeneratedItem = false;
        while (expandableItemCount > 0 && cellsRemaining2 > 0) {
            long minCellsAt = 0;
            cellCount = ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
            widthPadding = 0;
            cellsRemaining = 0;
            while (true) {
                cellSizeRemaining = cellsRemaining;
                if (cellSizeRemaining >= childCount) {
                    break;
                }
                View child2 = getChildAt(cellSizeRemaining);
                needsExpansion = isGeneratedItem;
                LayoutParams needsExpansion2 = (LayoutParams) child2.getLayoutParams();
                if (needsExpansion2.expandable != null) {
                    if (needsExpansion2.cellsUsed < cellCount) {
                        cellCount = needsExpansion2.cellsUsed;
                        minCellsAt = 1 << cellSizeRemaining;
                        widthPadding = 1;
                    } else if (needsExpansion2.cellsUsed == cellCount) {
                        minCellsAt |= 1 << cellSizeRemaining;
                        widthPadding++;
                    }
                }
                cellsRemaining = cellSizeRemaining + 1;
                isGeneratedItem = needsExpansion;
            }
            needsExpansion = isGeneratedItem;
            smallestItemsAt |= minCellsAt;
            if (widthPadding > cellsRemaining2) {
                widthSize2 = widthSize;
                z = centerSingleExpandedItem;
                break;
            }
            int minCellsItemCount;
            cellCount++;
            i = 0;
            while (i < childCount) {
                View child3 = getChildAt(i);
                lp = (LayoutParams) child3.getLayoutParams();
                widthSize2 = widthSize;
                minCellsItemCount = widthPadding;
                z = centerSingleExpandedItem;
                if ((minCellsAt & ((long) (1 << i))) != 0) {
                    if (z && lp.preventEdgeOffset && cellsRemaining2 == 1) {
                        child3.setPadding(this.mGeneratedItemPadding + cellSize, 0, this.mGeneratedItemPadding, 0);
                    }
                    lp.cellsUsed++;
                    lp.expanded = true;
                    cellsRemaining2--;
                } else if (lp.cellsUsed == cellCount) {
                    smallestItemsAt |= (long) (1 << i);
                }
                i++;
                widthPadding = minCellsItemCount;
                widthSize = widthSize2;
                centerSingleExpandedItem = z;
            }
            minCellsItemCount = widthPadding;
            z = centerSingleExpandedItem;
            isGeneratedItem = true;
        }
        widthSize2 = widthSize;
        z = centerSingleExpandedItem;
        needsExpansion = isGeneratedItem;
        long smallestItemsAt2 = smallestItemsAt;
        boolean singleItem = !hasOverflow && visibleItemCount == 1;
        int i2;
        boolean singleItem2;
        if (cellsRemaining2 <= 0 || smallestItemsAt2 == 0) {
            i2 = visibleItemCount;
        } else if (cellsRemaining2 < visibleItemCount - 1 || singleItem || maxCellsUsed > 1) {
            int childCount2;
            float expandCount = (float) Long.bitCount(smallestItemsAt2);
            if (singleItem) {
                childCount2 = childCount;
            } else {
                if (!((1 & smallestItemsAt2) == 0 || ((LayoutParams) getChildAt(0).getLayoutParams()).preventEdgeOffset)) {
                    expandCount -= 0.5f;
                }
                childCount2 = childCount;
                if (!((smallestItemsAt2 & ((long) (1 << (childCount - 1)))) == 0 || ((LayoutParams) getChildAt(childCount2 - 1).getLayoutParams()).preventEdgeOffset)) {
                    expandCount -= 0.5f;
                }
            }
            cellCount = expandCount > 0.0f ? (int) (((float) (cellsRemaining2 * cellSize)) / expandCount) : 0;
            cellSizeRemaining = 0;
            while (true) {
                childCount = childCount2;
                if (cellSizeRemaining >= childCount) {
                    break;
                }
                singleItem2 = singleItem;
                i2 = visibleItemCount;
                if ((((long) (true << cellSizeRemaining)) & smallestItemsAt2) != 0) {
                    singleItem = getChildAt(cellSizeRemaining);
                    lp = (LayoutParams) singleItem.getLayoutParams();
                    if (singleItem instanceof ActionMenuItemView) {
                        lp.extraPixels = cellCount;
                        lp.expanded = true;
                        if (cellSizeRemaining == 0 && !lp.preventEdgeOffset) {
                            lp.leftMargin = (-cellCount) / 2;
                        }
                        needsExpansion = true;
                    } else {
                        if (lp.isOverflowButton) {
                            lp.extraPixels = cellCount;
                            lp.expanded = true;
                            lp.rightMargin = (-cellCount) / 2;
                            needsExpansion = true;
                        } else {
                            if (cellSizeRemaining != 0) {
                                lp.leftMargin = cellCount / 2;
                            }
                            if (cellSizeRemaining != childCount - 1) {
                                lp.rightMargin = cellCount / 2;
                            }
                        }
                        cellSizeRemaining++;
                        childCount2 = childCount;
                        singleItem = singleItem2;
                        visibleItemCount = i2;
                    }
                }
                cellSizeRemaining++;
                childCount2 = childCount;
                singleItem = singleItem2;
                visibleItemCount = i2;
            }
            i2 = visibleItemCount;
        } else {
            singleItem2 = singleItem;
            i2 = visibleItemCount;
        }
        if (needsExpansion) {
            int i3 = 0;
            while (true) {
                i = i3;
                if (i >= childCount) {
                    break;
                }
                View child4 = getChildAt(i);
                LayoutParams lp3 = (LayoutParams) child4.getLayoutParams();
                if (lp3.expanded) {
                    child4.measure(MeasureSpec.makeMeasureSpec((lp3.cellsUsed * cellSize) + lp3.extraPixels, Ints.MAX_POWER_OF_TWO), itemHeightSpec);
                }
                i3 = i + 1;
            }
        }
        if (heightMode != Ints.MAX_POWER_OF_TWO) {
            widthSize = heightSize;
        } else {
            widthSize = heightSize2;
        }
        setMeasuredDimension(widthSize2, widthSize);
    }

    static int measureChildForCells(View child, int cellSize, int cellsRemaining, int parentHeightMeasureSpec, int parentHeightPadding) {
        View view = child;
        int i = cellsRemaining;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        int childHeightSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(parentHeightMeasureSpec) - parentHeightPadding, MeasureSpec.getMode(parentHeightMeasureSpec));
        ActionMenuItemView itemView = view instanceof ActionMenuItemView ? (ActionMenuItemView) view : null;
        boolean expandable = false;
        boolean hasText = itemView != null && itemView.hasText();
        int cellsUsed = 0;
        if (i > 0 && (!hasText || i >= 2)) {
            view.measure(MeasureSpec.makeMeasureSpec(cellSize * i, Integer.MIN_VALUE), childHeightSpec);
            int measuredWidth = view.getMeasuredWidth();
            cellsUsed = measuredWidth / cellSize;
            if (measuredWidth % cellSize != 0) {
                cellsUsed++;
            }
            if (hasText && cellsUsed < 2) {
                cellsUsed = 2;
            }
        }
        if (!lp.isOverflowButton && hasText) {
            expandable = true;
        }
        lp.expandable = expandable;
        lp.cellsUsed = cellsUsed;
        view.measure(MeasureSpec.makeMeasureSpec(cellsUsed * cellSize, Ints.MAX_POWER_OF_TWO), childHeightSpec);
        return cellsUsed;
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mFormatItems) {
            int midVertical;
            boolean isLayoutRtl;
            int overflowWidth;
            int t;
            int size;
            int childCount = getChildCount();
            int midVertical2 = (bottom - top) / 2;
            int dividerWidth = getDividerWidth();
            int nonOverflowCount = 0;
            int widthRemaining = ((right - left) - getPaddingRight()) - getPaddingLeft();
            boolean hasOverflow = false;
            boolean isLayoutRtl2 = ViewUtils.isLayoutRtl(this);
            int widthRemaining2 = widthRemaining;
            widthRemaining = 0;
            int overflowWidth2 = 0;
            int i = 0;
            while (i < childCount) {
                View v = getChildAt(i);
                if (v.getVisibility() == 8) {
                    midVertical = midVertical2;
                    isLayoutRtl = isLayoutRtl2;
                } else {
                    LayoutParams p = (LayoutParams) v.getLayoutParams();
                    if (p.isOverflowButton) {
                        int l;
                        overflowWidth = v.getMeasuredWidth();
                        if (hasSupportDividerBeforeChildAt(i)) {
                            overflowWidth += dividerWidth;
                        }
                        overflowWidth2 = v.getMeasuredHeight();
                        if (isLayoutRtl2) {
                            isLayoutRtl = isLayoutRtl2;
                            l = getPaddingLeft() + p.leftMargin;
                            isLayoutRtl2 = l + overflowWidth;
                        } else {
                            isLayoutRtl = isLayoutRtl2;
                            isLayoutRtl2 = (getWidth() - getPaddingRight()) - p.rightMargin;
                            l = isLayoutRtl2 - overflowWidth;
                        }
                        t = midVertical2 - (overflowWidth2 / 2);
                        midVertical = midVertical2;
                        v.layout(l, t, isLayoutRtl2, t + overflowWidth2);
                        widthRemaining2 -= overflowWidth;
                        hasOverflow = true;
                        overflowWidth2 = overflowWidth;
                    } else {
                        midVertical = midVertical2;
                        isLayoutRtl = isLayoutRtl2;
                        size = (v.getMeasuredWidth() + p.leftMargin) + p.rightMargin;
                        widthRemaining += size;
                        widthRemaining2 -= size;
                        if (hasSupportDividerBeforeChildAt(i)) {
                            widthRemaining += dividerWidth;
                        }
                        nonOverflowCount++;
                    }
                }
                i++;
                isLayoutRtl2 = isLayoutRtl;
                midVertical2 = midVertical;
            }
            midVertical = midVertical2;
            isLayoutRtl = isLayoutRtl2;
            int i2 = 1;
            int spacerSize;
            int t2;
            if (childCount != 1 || hasOverflow) {
                if (hasOverflow) {
                    i2 = 0;
                }
                size = nonOverflowCount - i2;
                t = 0;
                spacerSize = Math.max(0, size > 0 ? widthRemaining2 / size : 0);
                int dividerWidth2;
                int overflowWidth3;
                if (isLayoutRtl) {
                    overflowWidth = getWidth() - getPaddingRight();
                    while (t < childCount) {
                        View v2 = getChildAt(t);
                        LayoutParams lp = (LayoutParams) v2.getLayoutParams();
                        int spacerCount = size;
                        if (v2.getVisibility() == 8) {
                            dividerWidth2 = dividerWidth;
                            overflowWidth3 = overflowWidth2;
                        } else if (lp.isOverflowButton != 0) {
                            dividerWidth2 = dividerWidth;
                            overflowWidth3 = overflowWidth2;
                        } else {
                            overflowWidth -= lp.rightMargin;
                            size = v2.getMeasuredWidth();
                            i2 = v2.getMeasuredHeight();
                            midVertical2 = midVertical - (i2 / 2);
                            dividerWidth2 = dividerWidth;
                            overflowWidth3 = overflowWidth2;
                            v2.layout(overflowWidth - size, midVertical2, overflowWidth, midVertical2 + i2);
                            overflowWidth -= (lp.leftMargin + size) + spacerSize;
                        }
                        t++;
                        size = spacerCount;
                        dividerWidth = dividerWidth2;
                        overflowWidth2 = overflowWidth3;
                    }
                    dividerWidth2 = dividerWidth;
                    overflowWidth3 = overflowWidth2;
                } else {
                    dividerWidth2 = dividerWidth;
                    overflowWidth3 = overflowWidth2;
                    size = getPaddingLeft();
                    while (t < childCount) {
                        View v3 = getChildAt(t);
                        LayoutParams lp2 = (LayoutParams) v3.getLayoutParams();
                        if (!(v3.getVisibility() == 8 || lp2.isOverflowButton)) {
                            size += lp2.leftMargin;
                            dividerWidth = v3.getMeasuredWidth();
                            overflowWidth2 = v3.getMeasuredHeight();
                            t2 = midVertical - (overflowWidth2 / 2);
                            v3.layout(size, t2, size + dividerWidth, t2 + overflowWidth2);
                            size += (lp2.rightMargin + dividerWidth) + spacerSize;
                        }
                        t++;
                    }
                }
                return;
            }
            View v4 = getChildAt(null);
            t = v4.getMeasuredWidth();
            spacerSize = v4.getMeasuredHeight();
            t2 = ((right - left) / 2) - (t / 2);
            i2 = midVertical - (spacerSize / 2);
            v4.layout(t2, i2, t2 + t, i2 + spacerSize);
            return;
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dismissPopupMenus();
    }

    public void setOverflowIcon(@Nullable Drawable icon) {
        getMenu();
        this.mPresenter.setOverflowIcon(icon);
    }

    @Nullable
    public Drawable getOverflowIcon() {
        getMenu();
        return this.mPresenter.getOverflowIcon();
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public boolean isOverflowReserved() {
        return this.mReserveOverflow;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public void setOverflowReserved(boolean reserveOverflow) {
        this.mReserveOverflow = reserveOverflow;
    }

    /* Access modifiers changed, original: protected */
    public LayoutParams generateDefaultLayoutParams() {
        LayoutParams params = new LayoutParams(-2, -2);
        params.gravity = 16;
        return params;
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /* Access modifiers changed, original: protected */
    public LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        if (p == null) {
            return generateDefaultLayoutParams();
        }
        LayoutParams result = p instanceof LayoutParams ? new LayoutParams((LayoutParams) p) : new LayoutParams(p);
        if (result.gravity <= 0) {
            result.gravity = 16;
        }
        return result;
    }

    /* Access modifiers changed, original: protected */
    public boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return p != null && (p instanceof LayoutParams);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public LayoutParams generateOverflowButtonLayoutParams() {
        LayoutParams result = generateDefaultLayoutParams();
        result.isOverflowButton = true;
        return result;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public boolean invokeItem(MenuItemImpl item) {
        return this.mMenu.performItemAction(item, 0);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public int getWindowAnimations() {
        return 0;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public void initialize(MenuBuilder menu) {
        this.mMenu = menu;
    }

    public Menu getMenu() {
        if (this.mMenu == null) {
            Context context = getContext();
            this.mMenu = new MenuBuilder(context);
            this.mMenu.setCallback(new MenuBuilderCallback());
            this.mPresenter = new ActionMenuPresenter(context);
            this.mPresenter.setReserveOverflow(true);
            this.mPresenter.setCallback(this.mActionMenuPresenterCallback != null ? this.mActionMenuPresenterCallback : new ActionMenuPresenterCallback());
            this.mMenu.addMenuPresenter(this.mPresenter, this.mPopupContext);
            this.mPresenter.setMenuView(this);
        }
        return this.mMenu;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public void setMenuCallbacks(Callback pcb, MenuBuilder.Callback mcb) {
        this.mActionMenuPresenterCallback = pcb;
        this.mMenuBuilderCallback = mcb;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public MenuBuilder peekMenu() {
        return this.mMenu;
    }

    public boolean showOverflowMenu() {
        return this.mPresenter != null && this.mPresenter.showOverflowMenu();
    }

    public boolean hideOverflowMenu() {
        return this.mPresenter != null && this.mPresenter.hideOverflowMenu();
    }

    public boolean isOverflowMenuShowing() {
        return this.mPresenter != null && this.mPresenter.isOverflowMenuShowing();
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public boolean isOverflowMenuShowPending() {
        return this.mPresenter != null && this.mPresenter.isOverflowMenuShowPending();
    }

    public void dismissPopupMenus() {
        if (this.mPresenter != null) {
            this.mPresenter.dismissPopupMenus();
        }
    }

    /* Access modifiers changed, original: protected */
    @RestrictTo({Scope.LIBRARY_GROUP})
    public boolean hasSupportDividerBeforeChildAt(int childIndex) {
        if (childIndex == 0) {
            return false;
        }
        View childBefore = getChildAt(childIndex - 1);
        View child = getChildAt(childIndex);
        boolean result = false;
        if (childIndex < getChildCount() && (childBefore instanceof ActionMenuChildView)) {
            result = false | ((ActionMenuChildView) childBefore).needsDividerAfter();
        }
        if (childIndex > 0 && (child instanceof ActionMenuChildView)) {
            result |= ((ActionMenuChildView) child).needsDividerBefore();
        }
        return result;
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return false;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public void setExpandedActionViewsExclusive(boolean exclusive) {
        this.mPresenter.setExpandedActionViewsExclusive(exclusive);
    }
}
