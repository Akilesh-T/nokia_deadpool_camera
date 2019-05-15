package android.support.v7.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.ActionProvider.SubUiVisibilityListener;
import android.support.v4.view.GravityCompat;
import android.support.v7.appcompat.R;
import android.support.v7.view.ActionBarPolicy;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.view.menu.ActionMenuItemView.PopupCallback;
import android.support.v7.view.menu.BaseMenuPresenter;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.view.menu.MenuPresenter.Callback;
import android.support.v7.view.menu.MenuView;
import android.support.v7.view.menu.MenuView.ItemView;
import android.support.v7.view.menu.ShowableListMenu;
import android.support.v7.view.menu.SubMenuBuilder;
import android.support.v7.widget.ActionMenuView.ActionMenuChildView;
import android.util.SparseBooleanArray;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import java.util.ArrayList;

class ActionMenuPresenter extends BaseMenuPresenter implements SubUiVisibilityListener {
    private static final String TAG = "ActionMenuPresenter";
    private final SparseBooleanArray mActionButtonGroups = new SparseBooleanArray();
    ActionButtonSubmenu mActionButtonPopup;
    private int mActionItemWidthLimit;
    private boolean mExpandedActionViewsExclusive;
    private int mMaxItems;
    private boolean mMaxItemsSet;
    private int mMinCellSize;
    int mOpenSubMenuId;
    OverflowMenuButton mOverflowButton;
    OverflowPopup mOverflowPopup;
    private Drawable mPendingOverflowIcon;
    private boolean mPendingOverflowIconSet;
    private ActionMenuPopupCallback mPopupCallback;
    final PopupPresenterCallback mPopupPresenterCallback = new PopupPresenterCallback();
    OpenOverflowRunnable mPostedOpenRunnable;
    private boolean mReserveOverflow;
    private boolean mReserveOverflowSet;
    private View mScrapActionButtonView;
    private boolean mStrictWidthLimit;
    private int mWidthLimit;
    private boolean mWidthLimitSet;

    private class OpenOverflowRunnable implements Runnable {
        private OverflowPopup mPopup;

        public OpenOverflowRunnable(OverflowPopup popup) {
            this.mPopup = popup;
        }

        public void run() {
            if (ActionMenuPresenter.this.mMenu != null) {
                ActionMenuPresenter.this.mMenu.changeMenuMode();
            }
            View menuView = (View) ActionMenuPresenter.this.mMenuView;
            if (!(menuView == null || menuView.getWindowToken() == null || !this.mPopup.tryShow())) {
                ActionMenuPresenter.this.mOverflowPopup = this.mPopup;
            }
            ActionMenuPresenter.this.mPostedOpenRunnable = null;
        }
    }

    private static class SavedState implements Parcelable {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        public int openSubMenuId;

        SavedState() {
        }

        SavedState(Parcel in) {
            this.openSubMenuId = in.readInt();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.openSubMenuId);
        }
    }

    private class ActionMenuPopupCallback extends PopupCallback {
        ActionMenuPopupCallback() {
        }

        public ShowableListMenu getPopup() {
            return ActionMenuPresenter.this.mActionButtonPopup != null ? ActionMenuPresenter.this.mActionButtonPopup.getPopup() : null;
        }
    }

    private class PopupPresenterCallback implements Callback {
        PopupPresenterCallback() {
        }

        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            boolean z = false;
            if (subMenu == null) {
                return false;
            }
            ActionMenuPresenter.this.mOpenSubMenuId = ((SubMenuBuilder) subMenu).getItem().getItemId();
            Callback cb = ActionMenuPresenter.this.getCallback();
            if (cb != null) {
                z = cb.onOpenSubMenu(subMenu);
            }
            return z;
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            if (menu instanceof SubMenuBuilder) {
                menu.getRootMenu().close(false);
            }
            Callback cb = ActionMenuPresenter.this.getCallback();
            if (cb != null) {
                cb.onCloseMenu(menu, allMenusAreClosing);
            }
        }
    }

    private class ActionButtonSubmenu extends MenuPopupHelper {
        public ActionButtonSubmenu(Context context, SubMenuBuilder subMenu, View anchorView) {
            super(context, subMenu, anchorView, false, R.attr.actionOverflowMenuStyle);
            if (!((MenuItemImpl) subMenu.getItem()).isActionButton()) {
                setAnchorView(ActionMenuPresenter.this.mOverflowButton == null ? (View) ActionMenuPresenter.this.mMenuView : ActionMenuPresenter.this.mOverflowButton);
            }
            setPresenterCallback(ActionMenuPresenter.this.mPopupPresenterCallback);
        }

        /* Access modifiers changed, original: protected */
        public void onDismiss() {
            ActionMenuPresenter.this.mActionButtonPopup = null;
            ActionMenuPresenter.this.mOpenSubMenuId = 0;
            super.onDismiss();
        }
    }

    private class OverflowMenuButton extends AppCompatImageView implements ActionMenuChildView {
        private final float[] mTempPts = new float[2];

        public OverflowMenuButton(Context context) {
            super(context, null, R.attr.actionOverflowButtonStyle);
            setClickable(true);
            setFocusable(true);
            setVisibility(0);
            setEnabled(true);
            TooltipCompat.setTooltipText(this, getContentDescription());
            setOnTouchListener(new ForwardingListener(this, ActionMenuPresenter.this) {
                public ShowableListMenu getPopup() {
                    if (ActionMenuPresenter.this.mOverflowPopup == null) {
                        return null;
                    }
                    return ActionMenuPresenter.this.mOverflowPopup.getPopup();
                }

                public boolean onForwardingStarted() {
                    ActionMenuPresenter.this.showOverflowMenu();
                    return true;
                }

                public boolean onForwardingStopped() {
                    if (ActionMenuPresenter.this.mPostedOpenRunnable != null) {
                        return false;
                    }
                    ActionMenuPresenter.this.hideOverflowMenu();
                    return true;
                }
            });
        }

        public boolean performClick() {
            if (super.performClick()) {
                return true;
            }
            playSoundEffect(0);
            ActionMenuPresenter.this.showOverflowMenu();
            return true;
        }

        public boolean needsDividerBefore() {
            return false;
        }

        public boolean needsDividerAfter() {
            return false;
        }

        /* Access modifiers changed, original: protected */
        public boolean setFrame(int l, int t, int r, int b) {
            boolean changed = super.setFrame(l, t, r, b);
            Drawable d = getDrawable();
            Drawable bg = getBackground();
            if (!(d == null || bg == null)) {
                int width = getWidth();
                int height = getHeight();
                int halfEdge = Math.max(width, height) / 2;
                int centerX = (width + (getPaddingLeft() - getPaddingRight())) / 2;
                int centerY = (height + (getPaddingTop() - getPaddingBottom())) / 2;
                DrawableCompat.setHotspotBounds(bg, centerX - halfEdge, centerY - halfEdge, centerX + halfEdge, centerY + halfEdge);
            }
            return changed;
        }
    }

    private class OverflowPopup extends MenuPopupHelper {
        public OverflowPopup(Context context, MenuBuilder menu, View anchorView, boolean overflowOnly) {
            super(context, menu, anchorView, overflowOnly, R.attr.actionOverflowMenuStyle);
            setGravity(GravityCompat.END);
            setPresenterCallback(ActionMenuPresenter.this.mPopupPresenterCallback);
        }

        /* Access modifiers changed, original: protected */
        public void onDismiss() {
            if (ActionMenuPresenter.this.mMenu != null) {
                ActionMenuPresenter.this.mMenu.close();
            }
            ActionMenuPresenter.this.mOverflowPopup = null;
            super.onDismiss();
        }
    }

    public ActionMenuPresenter(Context context) {
        super(context, R.layout.abc_action_menu_layout, R.layout.abc_action_menu_item_layout);
    }

    public void initForMenu(@NonNull Context context, @Nullable MenuBuilder menu) {
        super.initForMenu(context, menu);
        Resources res = context.getResources();
        ActionBarPolicy abp = ActionBarPolicy.get(context);
        if (!this.mReserveOverflowSet) {
            this.mReserveOverflow = abp.showsOverflowMenuButton();
        }
        if (!this.mWidthLimitSet) {
            this.mWidthLimit = abp.getEmbeddedMenuWidthLimit();
        }
        if (!this.mMaxItemsSet) {
            this.mMaxItems = abp.getMaxActionButtons();
        }
        int width = this.mWidthLimit;
        if (this.mReserveOverflow) {
            if (this.mOverflowButton == null) {
                this.mOverflowButton = new OverflowMenuButton(this.mSystemContext);
                if (this.mPendingOverflowIconSet) {
                    this.mOverflowButton.setImageDrawable(this.mPendingOverflowIcon);
                    this.mPendingOverflowIcon = null;
                    this.mPendingOverflowIconSet = false;
                }
                int spec = MeasureSpec.makeMeasureSpec(0, 0);
                this.mOverflowButton.measure(spec, spec);
            }
            width -= this.mOverflowButton.getMeasuredWidth();
        } else {
            this.mOverflowButton = null;
        }
        this.mActionItemWidthLimit = width;
        this.mMinCellSize = (int) (56.0f * res.getDisplayMetrics().density);
        this.mScrapActionButtonView = null;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (!this.mMaxItemsSet) {
            this.mMaxItems = ActionBarPolicy.get(this.mContext).getMaxActionButtons();
        }
        if (this.mMenu != null) {
            this.mMenu.onItemsChanged(true);
        }
    }

    public void setWidthLimit(int width, boolean strict) {
        this.mWidthLimit = width;
        this.mStrictWidthLimit = strict;
        this.mWidthLimitSet = true;
    }

    public void setReserveOverflow(boolean reserveOverflow) {
        this.mReserveOverflow = reserveOverflow;
        this.mReserveOverflowSet = true;
    }

    public void setItemLimit(int itemCount) {
        this.mMaxItems = itemCount;
        this.mMaxItemsSet = true;
    }

    public void setExpandedActionViewsExclusive(boolean isExclusive) {
        this.mExpandedActionViewsExclusive = isExclusive;
    }

    public void setOverflowIcon(Drawable icon) {
        if (this.mOverflowButton != null) {
            this.mOverflowButton.setImageDrawable(icon);
            return;
        }
        this.mPendingOverflowIconSet = true;
        this.mPendingOverflowIcon = icon;
    }

    public Drawable getOverflowIcon() {
        if (this.mOverflowButton != null) {
            return this.mOverflowButton.getDrawable();
        }
        if (this.mPendingOverflowIconSet) {
            return this.mPendingOverflowIcon;
        }
        return null;
    }

    public MenuView getMenuView(ViewGroup root) {
        MenuView oldMenuView = this.mMenuView;
        MenuView result = super.getMenuView(root);
        if (oldMenuView != result) {
            ((ActionMenuView) result).setPresenter(this);
        }
        return result;
    }

    public View getItemView(MenuItemImpl item, View convertView, ViewGroup parent) {
        View actionView = item.getActionView();
        if (actionView == null || item.hasCollapsibleActionView()) {
            actionView = super.getItemView(item, convertView, parent);
        }
        actionView.setVisibility(item.isActionViewExpanded() ? 8 : 0);
        ActionMenuView menuParent = (ActionMenuView) parent;
        LayoutParams lp = actionView.getLayoutParams();
        if (!menuParent.checkLayoutParams(lp)) {
            actionView.setLayoutParams(menuParent.generateLayoutParams(lp));
        }
        return actionView;
    }

    public void bindItemView(MenuItemImpl item, ItemView itemView) {
        itemView.initialize(item, 0);
        ActionMenuItemView actionItemView = (ActionMenuItemView) itemView;
        actionItemView.setItemInvoker(this.mMenuView);
        if (this.mPopupCallback == null) {
            this.mPopupCallback = new ActionMenuPopupCallback();
        }
        actionItemView.setPopupCallback(this.mPopupCallback);
    }

    public boolean shouldIncludeItem(int childIndex, MenuItemImpl item) {
        return item.isActionButton();
    }

    public void updateMenuView(boolean cleared) {
        ArrayList<MenuItemImpl> actionItems;
        int i;
        super.updateMenuView(cleared);
        ((View) this.mMenuView).requestLayout();
        boolean z = false;
        if (this.mMenu != null) {
            actionItems = this.mMenu.getActionItems();
            int count = actionItems.size();
            for (i = 0; i < count; i++) {
                ActionProvider provider = ((MenuItemImpl) actionItems.get(i)).getSupportActionProvider();
                if (provider != null) {
                    provider.setSubUiVisibilityListener(this);
                }
            }
        }
        actionItems = this.mMenu != null ? this.mMenu.getNonActionItems() : null;
        boolean hasOverflow = false;
        if (this.mReserveOverflow && actionItems != null) {
            i = actionItems.size();
            if (i == 1) {
                hasOverflow = ((MenuItemImpl) actionItems.get(0)).isActionViewExpanded() ^ 1;
            } else {
                if (i > 0) {
                    z = true;
                }
                hasOverflow = z;
            }
        }
        if (hasOverflow) {
            if (this.mOverflowButton == null) {
                this.mOverflowButton = new OverflowMenuButton(this.mSystemContext);
            }
            ViewGroup parent = (ViewGroup) this.mOverflowButton.getParent();
            if (parent != this.mMenuView) {
                if (parent != null) {
                    parent.removeView(this.mOverflowButton);
                }
                ActionMenuView menuView = this.mMenuView;
                menuView.addView(this.mOverflowButton, menuView.generateOverflowButtonLayoutParams());
            }
        } else if (this.mOverflowButton != null && this.mOverflowButton.getParent() == this.mMenuView) {
            ((ViewGroup) this.mMenuView).removeView(this.mOverflowButton);
        }
        ((ActionMenuView) this.mMenuView).setOverflowReserved(this.mReserveOverflow);
    }

    public boolean filterLeftoverView(ViewGroup parent, int childIndex) {
        if (parent.getChildAt(childIndex) == this.mOverflowButton) {
            return false;
        }
        return super.filterLeftoverView(parent, childIndex);
    }

    public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
        int i = 0;
        if (!subMenu.hasVisibleItems()) {
            return false;
        }
        SubMenuBuilder topSubMenu = subMenu;
        while (topSubMenu.getParentMenu() != this.mMenu) {
            topSubMenu = (SubMenuBuilder) topSubMenu.getParentMenu();
        }
        View anchor = findViewForItem(topSubMenu.getItem());
        if (anchor == null) {
            return false;
        }
        this.mOpenSubMenuId = subMenu.getItem().getItemId();
        boolean preserveIconSpacing = false;
        int count = subMenu.size();
        while (i < count) {
            MenuItem childItem = subMenu.getItem(i);
            if (childItem.isVisible() && childItem.getIcon() != null) {
                preserveIconSpacing = true;
                break;
            }
            i++;
        }
        this.mActionButtonPopup = new ActionButtonSubmenu(this.mContext, subMenu, anchor);
        this.mActionButtonPopup.setForceShowIcon(preserveIconSpacing);
        this.mActionButtonPopup.show();
        super.onSubMenuSelected(subMenu);
        return true;
    }

    private View findViewForItem(MenuItem item) {
        ViewGroup parent = this.mMenuView;
        if (parent == null) {
            return null;
        }
        int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = parent.getChildAt(i);
            if ((child instanceof ItemView) && ((ItemView) child).getItemData() == item) {
                return child;
            }
        }
        return null;
    }

    public boolean showOverflowMenu() {
        if (!this.mReserveOverflow || isOverflowMenuShowing() || this.mMenu == null || this.mMenuView == null || this.mPostedOpenRunnable != null || this.mMenu.getNonActionItems().isEmpty()) {
            return false;
        }
        this.mPostedOpenRunnable = new OpenOverflowRunnable(new OverflowPopup(this.mContext, this.mMenu, this.mOverflowButton, true));
        ((View) this.mMenuView).post(this.mPostedOpenRunnable);
        super.onSubMenuSelected(null);
        return true;
    }

    public boolean hideOverflowMenu() {
        if (this.mPostedOpenRunnable == null || this.mMenuView == null) {
            MenuPopupHelper popup = this.mOverflowPopup;
            if (popup == null) {
                return false;
            }
            popup.dismiss();
            return true;
        }
        ((View) this.mMenuView).removeCallbacks(this.mPostedOpenRunnable);
        this.mPostedOpenRunnable = null;
        return true;
    }

    public boolean dismissPopupMenus() {
        return hideOverflowMenu() | hideSubMenus();
    }

    public boolean hideSubMenus() {
        if (this.mActionButtonPopup == null) {
            return false;
        }
        this.mActionButtonPopup.dismiss();
        return true;
    }

    public boolean isOverflowMenuShowing() {
        return this.mOverflowPopup != null && this.mOverflowPopup.isShowing();
    }

    public boolean isOverflowMenuShowPending() {
        return this.mPostedOpenRunnable != null || isOverflowMenuShowing();
    }

    public boolean isOverflowReserved() {
        return this.mReserveOverflow;
    }

    /* JADX WARNING: Removed duplicated region for block: B:86:0x0142  */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x00f9  */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x0155  */
    /* JADX WARNING: Removed duplicated region for block: B:101:0x017b  */
    public boolean flagActionItems() {
        /*
        r26 = this;
        r0 = r26;
        r1 = r0.mMenu;
        r2 = 0;
        if (r1 == 0) goto L_0x0012;
    L_0x0007:
        r1 = r0.mMenu;
        r1 = r1.getVisibleItems();
        r3 = r1.size();
        goto L_0x0014;
    L_0x0012:
        r1 = 0;
        r3 = r2;
    L_0x0014:
        r4 = r0.mMaxItems;
        r5 = r0.mActionItemWidthLimit;
        r6 = android.view.View.MeasureSpec.makeMeasureSpec(r2, r2);
        r7 = r0.mMenuView;
        r7 = (android.view.ViewGroup) r7;
        r8 = 0;
        r9 = 0;
        r10 = 0;
        r11 = 0;
        r12 = r4;
        r4 = r2;
    L_0x0026:
        if (r4 >= r3) goto L_0x004f;
    L_0x0028:
        r13 = r1.get(r4);
        r13 = (android.support.v7.view.menu.MenuItemImpl) r13;
        r14 = r13.requiresActionButton();
        if (r14 == 0) goto L_0x0037;
    L_0x0034:
        r8 = r8 + 1;
        goto L_0x0041;
    L_0x0037:
        r14 = r13.requestsActionButton();
        if (r14 == 0) goto L_0x0040;
    L_0x003d:
        r9 = r9 + 1;
        goto L_0x0041;
    L_0x0040:
        r11 = 1;
    L_0x0041:
        r14 = r0.mExpandedActionViewsExclusive;
        if (r14 == 0) goto L_0x004c;
    L_0x0045:
        r14 = r13.isActionViewExpanded();
        if (r14 == 0) goto L_0x004c;
    L_0x004b:
        r12 = 0;
    L_0x004c:
        r4 = r4 + 1;
        goto L_0x0026;
    L_0x004f:
        r4 = r0.mReserveOverflow;
        if (r4 == 0) goto L_0x005b;
    L_0x0053:
        if (r11 != 0) goto L_0x0059;
    L_0x0055:
        r4 = r8 + r9;
        if (r4 <= r12) goto L_0x005b;
    L_0x0059:
        r12 = r12 + -1;
    L_0x005b:
        r12 = r12 - r8;
        r4 = r0.mActionButtonGroups;
        r4.clear();
        r13 = 0;
        r14 = 0;
        r15 = r0.mStrictWidthLimit;
        if (r15 == 0) goto L_0x0075;
    L_0x0067:
        r15 = r0.mMinCellSize;
        r14 = r5 / r15;
        r15 = r0.mMinCellSize;
        r15 = r5 % r15;
        r2 = r0.mMinCellSize;
        r16 = r15 / r14;
        r13 = r2 + r16;
    L_0x0075:
        r2 = 0;
    L_0x0076:
        if (r2 >= r3) goto L_0x019c;
    L_0x0078:
        r16 = r1.get(r2);
        r15 = r16;
        r15 = (android.support.v7.view.menu.MenuItemImpl) r15;
        r16 = r15.requiresActionButton();
        if (r16 == 0) goto L_0x00ce;
    L_0x0086:
        r17 = r3;
        r3 = r0.mScrapActionButtonView;
        r3 = r0.getItemView(r15, r3, r7);
        r18 = r8;
        r8 = r0.mScrapActionButtonView;
        if (r8 != 0) goto L_0x0096;
    L_0x0094:
        r0.mScrapActionButtonView = r3;
    L_0x0096:
        r8 = r0.mStrictWidthLimit;
        if (r8 == 0) goto L_0x00a2;
    L_0x009a:
        r8 = 0;
        r16 = android.support.v7.widget.ActionMenuView.measureChildForCells(r3, r13, r14, r6, r8);
        r14 = r14 - r16;
        goto L_0x00a5;
    L_0x00a2:
        r3.measure(r6, r6);
    L_0x00a5:
        r8 = r3.getMeasuredWidth();
        r5 = r5 - r8;
        if (r10 != 0) goto L_0x00ad;
    L_0x00ac:
        r10 = r8;
    L_0x00ad:
        r19 = r3;
        r3 = r15.getGroupId();
        if (r3 == 0) goto L_0x00bc;
    L_0x00b5:
        r20 = r5;
        r5 = 1;
        r4.put(r3, r5);
        goto L_0x00bf;
    L_0x00bc:
        r20 = r5;
        r5 = 1;
    L_0x00bf:
        r15.setIsActionButton(r5);
        r24 = r6;
        r23 = r7;
        r21 = r9;
        r5 = r20;
    L_0x00cb:
        r0 = 0;
        goto L_0x018c;
    L_0x00ce:
        r17 = r3;
        r18 = r8;
        r3 = r15.requestsActionButton();
        if (r3 == 0) goto L_0x0182;
    L_0x00d8:
        r3 = r15.getGroupId();
        r8 = r4.get(r3);
        if (r12 > 0) goto L_0x00e8;
    L_0x00e2:
        if (r8 == 0) goto L_0x00e5;
    L_0x00e4:
        goto L_0x00e8;
    L_0x00e5:
        r21 = r9;
        goto L_0x00f6;
    L_0x00e8:
        if (r5 <= 0) goto L_0x00f4;
    L_0x00ea:
        r21 = r9;
        r9 = r0.mStrictWidthLimit;
        if (r9 == 0) goto L_0x00f2;
    L_0x00f0:
        if (r14 <= 0) goto L_0x00f6;
    L_0x00f2:
        r9 = 1;
        goto L_0x00f7;
    L_0x00f4:
        r21 = r9;
    L_0x00f6:
        r9 = 0;
    L_0x00f7:
        if (r9 == 0) goto L_0x0142;
    L_0x00f9:
        r22 = r9;
        r9 = r0.mScrapActionButtonView;
        r9 = r0.getItemView(r15, r9, r7);
        r23 = r7;
        r7 = r0.mScrapActionButtonView;
        if (r7 != 0) goto L_0x0109;
    L_0x0107:
        r0.mScrapActionButtonView = r9;
    L_0x0109:
        r7 = r0.mStrictWidthLimit;
        if (r7 == 0) goto L_0x011b;
    L_0x010d:
        r7 = 0;
        r16 = android.support.v7.widget.ActionMenuView.measureChildForCells(r9, r13, r14, r6, r7);
        r14 = r14 - r16;
        if (r16 != 0) goto L_0x0118;
    L_0x0116:
        r7 = 0;
        goto L_0x011a;
    L_0x0118:
        r7 = r22;
    L_0x011a:
        goto L_0x0120;
    L_0x011b:
        r9.measure(r6, r6);
        r7 = r22;
    L_0x0120:
        r16 = r9.getMeasuredWidth();
        r5 = r5 - r16;
        if (r10 != 0) goto L_0x012a;
    L_0x0128:
        r10 = r16;
    L_0x012a:
        r24 = r6;
        r6 = r0.mStrictWidthLimit;
        if (r6 == 0) goto L_0x0137;
    L_0x0130:
        if (r5 < 0) goto L_0x0134;
    L_0x0132:
        r6 = 1;
        goto L_0x0135;
    L_0x0134:
        r6 = 0;
    L_0x0135:
        r6 = r6 & r7;
        goto L_0x014a;
    L_0x0137:
        r6 = r5 + r10;
        if (r6 <= 0) goto L_0x013d;
    L_0x013b:
        r6 = 1;
        goto L_0x013e;
    L_0x013d:
        r6 = 0;
    L_0x013e:
        r9 = r7 & r6;
        r6 = r9;
        goto L_0x014a;
    L_0x0142:
        r24 = r6;
        r23 = r7;
        r22 = r9;
        r6 = r22;
    L_0x014a:
        if (r6 == 0) goto L_0x0153;
    L_0x014c:
        if (r3 == 0) goto L_0x0153;
    L_0x014e:
        r7 = 1;
        r4.put(r3, r7);
        goto L_0x0179;
    L_0x0153:
        if (r8 == 0) goto L_0x0179;
    L_0x0155:
        r7 = 0;
        r4.put(r3, r7);
        r7 = 0;
    L_0x015a:
        if (r7 >= r2) goto L_0x0179;
    L_0x015c:
        r9 = r1.get(r7);
        r9 = (android.support.v7.view.menu.MenuItemImpl) r9;
        r0 = r9.getGroupId();
        if (r0 != r3) goto L_0x0174;
    L_0x0168:
        r0 = r9.isActionButton();
        if (r0 == 0) goto L_0x0170;
    L_0x016e:
        r12 = r12 + 1;
    L_0x0170:
        r0 = 0;
        r9.setIsActionButton(r0);
    L_0x0174:
        r7 = r7 + 1;
        r0 = r26;
        goto L_0x015a;
    L_0x0179:
        if (r6 == 0) goto L_0x017d;
    L_0x017b:
        r12 = r12 + -1;
    L_0x017d:
        r15.setIsActionButton(r6);
        goto L_0x00cb;
    L_0x0182:
        r24 = r6;
        r23 = r7;
        r21 = r9;
        r0 = 0;
        r15.setIsActionButton(r0);
    L_0x018c:
        r2 = r2 + 1;
        r3 = r17;
        r8 = r18;
        r9 = r21;
        r7 = r23;
        r6 = r24;
        r0 = r26;
        goto L_0x0076;
    L_0x019c:
        r17 = r3;
        r24 = r6;
        r23 = r7;
        r18 = r8;
        r21 = r9;
        r0 = 1;
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.widget.ActionMenuPresenter.flagActionItems():boolean");
    }

    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        dismissPopupMenus();
        super.onCloseMenu(menu, allMenusAreClosing);
    }

    public Parcelable onSaveInstanceState() {
        SavedState state = new SavedState();
        state.openSubMenuId = this.mOpenSubMenuId;
        return state;
    }

    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState saved = (SavedState) state;
            if (saved.openSubMenuId > 0) {
                MenuItem item = this.mMenu.findItem(saved.openSubMenuId);
                if (item != null) {
                    onSubMenuSelected((SubMenuBuilder) item.getSubMenu());
                }
            }
        }
    }

    public void onSubUiVisibilityChanged(boolean isVisible) {
        if (isVisible) {
            super.onSubMenuSelected(null);
        } else if (this.mMenu != null) {
            this.mMenu.close(false);
        }
    }

    public void setMenuView(ActionMenuView menuView) {
        this.mMenuView = menuView;
        menuView.initialize(this.mMenu);
    }
}
