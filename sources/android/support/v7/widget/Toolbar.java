package android.support.v7.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.ClassLoaderCreator;
import android.os.Parcelable.Creator;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.view.CollapsibleActionView;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.view.menu.MenuPresenter;
import android.support.v7.view.menu.MenuPresenter.Callback;
import android.support.v7.view.menu.MenuView;
import android.support.v7.view.menu.SubMenuBuilder;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.List;

public class Toolbar extends ViewGroup {
    private static final String TAG = "Toolbar";
    private Callback mActionMenuPresenterCallback;
    int mButtonGravity;
    ImageButton mCollapseButtonView;
    private CharSequence mCollapseDescription;
    private Drawable mCollapseIcon;
    private boolean mCollapsible;
    private int mContentInsetEndWithActions;
    private int mContentInsetStartWithNavigation;
    private RtlSpacingHelper mContentInsets;
    private boolean mEatingHover;
    private boolean mEatingTouch;
    View mExpandedActionView;
    private ExpandedActionViewMenuPresenter mExpandedMenuPresenter;
    private int mGravity;
    private final ArrayList<View> mHiddenViews;
    private ImageView mLogoView;
    private int mMaxButtonHeight;
    private MenuBuilder.Callback mMenuBuilderCallback;
    private ActionMenuView mMenuView;
    private final android.support.v7.widget.ActionMenuView.OnMenuItemClickListener mMenuViewItemClickListener;
    private ImageButton mNavButtonView;
    OnMenuItemClickListener mOnMenuItemClickListener;
    private ActionMenuPresenter mOuterActionMenuPresenter;
    private Context mPopupContext;
    private int mPopupTheme;
    private final Runnable mShowOverflowMenuRunnable;
    private CharSequence mSubtitleText;
    private int mSubtitleTextAppearance;
    private int mSubtitleTextColor;
    private TextView mSubtitleTextView;
    private final int[] mTempMargins;
    private final ArrayList<View> mTempViews;
    private int mTitleMarginBottom;
    private int mTitleMarginEnd;
    private int mTitleMarginStart;
    private int mTitleMarginTop;
    private CharSequence mTitleText;
    private int mTitleTextAppearance;
    private int mTitleTextColor;
    private TextView mTitleTextView;
    private ToolbarWidgetWrapper mWrapper;

    public interface OnMenuItemClickListener {
        boolean onMenuItemClick(MenuItem menuItem);
    }

    private class ExpandedActionViewMenuPresenter implements MenuPresenter {
        MenuItemImpl mCurrentExpandedItem;
        MenuBuilder mMenu;

        ExpandedActionViewMenuPresenter() {
        }

        public void initForMenu(Context context, MenuBuilder menu) {
            if (!(this.mMenu == null || this.mCurrentExpandedItem == null)) {
                this.mMenu.collapseItemActionView(this.mCurrentExpandedItem);
            }
            this.mMenu = menu;
        }

        public MenuView getMenuView(ViewGroup root) {
            return null;
        }

        public void updateMenuView(boolean cleared) {
            if (this.mCurrentExpandedItem != null) {
                boolean found = false;
                if (this.mMenu != null) {
                    int count = this.mMenu.size();
                    for (int i = 0; i < count; i++) {
                        if (this.mMenu.getItem(i) == this.mCurrentExpandedItem) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    collapseItemActionView(this.mMenu, this.mCurrentExpandedItem);
                }
            }
        }

        public void setCallback(Callback cb) {
        }

        public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
            return false;
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        }

        public boolean flagActionItems() {
            return false;
        }

        public boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item) {
            Toolbar.this.ensureCollapseButtonView();
            ViewParent collapseButtonParent = Toolbar.this.mCollapseButtonView.getParent();
            if (collapseButtonParent != Toolbar.this) {
                if (collapseButtonParent instanceof ViewGroup) {
                    ((ViewGroup) collapseButtonParent).removeView(Toolbar.this.mCollapseButtonView);
                }
                Toolbar.this.addView(Toolbar.this.mCollapseButtonView);
            }
            Toolbar.this.mExpandedActionView = item.getActionView();
            this.mCurrentExpandedItem = item;
            ViewParent expandedActionParent = Toolbar.this.mExpandedActionView.getParent();
            if (expandedActionParent != Toolbar.this) {
                if (expandedActionParent instanceof ViewGroup) {
                    ((ViewGroup) expandedActionParent).removeView(Toolbar.this.mExpandedActionView);
                }
                LayoutParams lp = Toolbar.this.generateDefaultLayoutParams();
                lp.gravity = GravityCompat.START | (Toolbar.this.mButtonGravity & 112);
                lp.mViewType = 2;
                Toolbar.this.mExpandedActionView.setLayoutParams(lp);
                Toolbar.this.addView(Toolbar.this.mExpandedActionView);
            }
            Toolbar.this.removeChildrenForExpandedActionView();
            Toolbar.this.requestLayout();
            item.setActionViewExpanded(true);
            if (Toolbar.this.mExpandedActionView instanceof CollapsibleActionView) {
                ((CollapsibleActionView) Toolbar.this.mExpandedActionView).onActionViewExpanded();
            }
            return true;
        }

        public boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item) {
            if (Toolbar.this.mExpandedActionView instanceof CollapsibleActionView) {
                ((CollapsibleActionView) Toolbar.this.mExpandedActionView).onActionViewCollapsed();
            }
            Toolbar.this.removeView(Toolbar.this.mExpandedActionView);
            Toolbar.this.removeView(Toolbar.this.mCollapseButtonView);
            Toolbar.this.mExpandedActionView = null;
            Toolbar.this.addChildrenForExpandedActionView();
            this.mCurrentExpandedItem = null;
            Toolbar.this.requestLayout();
            item.setActionViewExpanded(false);
            return true;
        }

        public int getId() {
            return 0;
        }

        public Parcelable onSaveInstanceState() {
            return null;
        }

        public void onRestoreInstanceState(Parcelable state) {
        }
    }

    public static class LayoutParams extends android.support.v7.app.ActionBar.LayoutParams {
        static final int CUSTOM = 0;
        static final int EXPANDED = 2;
        static final int SYSTEM = 1;
        int mViewType;

        public LayoutParams(@NonNull Context c, AttributeSet attrs) {
            super(c, attrs);
            this.mViewType = 0;
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.mViewType = 0;
            this.gravity = 8388627;
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.mViewType = 0;
            this.gravity = gravity;
        }

        public LayoutParams(int gravity) {
            this(-2, -1, gravity);
        }

        public LayoutParams(LayoutParams source) {
            super((android.support.v7.app.ActionBar.LayoutParams) source);
            this.mViewType = 0;
            this.mViewType = source.mViewType;
        }

        public LayoutParams(android.support.v7.app.ActionBar.LayoutParams source) {
            super(source);
            this.mViewType = 0;
        }

        public LayoutParams(MarginLayoutParams source) {
            super((android.view.ViewGroup.LayoutParams) source);
            this.mViewType = 0;
            copyMarginsFromCompat(source);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
            this.mViewType = 0;
        }

        /* Access modifiers changed, original: 0000 */
        public void copyMarginsFromCompat(MarginLayoutParams source) {
            this.leftMargin = source.leftMargin;
            this.topMargin = source.topMargin;
            this.rightMargin = source.rightMargin;
            this.bottomMargin = source.bottomMargin;
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
        int expandedMenuItemId;
        boolean isOverflowOpen;

        public SavedState(Parcel source) {
            this(source, null);
        }

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            this.expandedMenuItemId = source.readInt();
            this.isOverflowOpen = source.readInt() != 0;
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.expandedMenuItemId);
            out.writeInt(this.isOverflowOpen);
        }
    }

    public Toolbar(Context context) {
        this(context, null);
    }

    public Toolbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.toolbarStyle);
    }

    public Toolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        int navDesc;
        super(context, attrs, defStyleAttr);
        this.mGravity = 8388627;
        this.mTempViews = new ArrayList();
        this.mHiddenViews = new ArrayList();
        this.mTempMargins = new int[2];
        this.mMenuViewItemClickListener = new android.support.v7.widget.ActionMenuView.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (Toolbar.this.mOnMenuItemClickListener != null) {
                    return Toolbar.this.mOnMenuItemClickListener.onMenuItemClick(item);
                }
                return false;
            }
        };
        this.mShowOverflowMenuRunnable = new Runnable() {
            public void run() {
                Toolbar.this.showOverflowMenu();
            }
        };
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(getContext(), attrs, R.styleable.Toolbar, defStyleAttr, 0);
        this.mTitleTextAppearance = a.getResourceId(R.styleable.Toolbar_titleTextAppearance, 0);
        this.mSubtitleTextAppearance = a.getResourceId(R.styleable.Toolbar_subtitleTextAppearance, 0);
        this.mGravity = a.getInteger(R.styleable.Toolbar_android_gravity, this.mGravity);
        this.mButtonGravity = a.getInteger(R.styleable.Toolbar_buttonGravity, 48);
        int titleMargin = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMargin, 0);
        if (a.hasValue(R.styleable.Toolbar_titleMargins)) {
            titleMargin = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMargins, titleMargin);
        }
        this.mTitleMarginBottom = titleMargin;
        this.mTitleMarginTop = titleMargin;
        this.mTitleMarginEnd = titleMargin;
        this.mTitleMarginStart = titleMargin;
        int marginStart = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginStart, -1);
        if (marginStart >= 0) {
            this.mTitleMarginStart = marginStart;
        }
        int marginEnd = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginEnd, -1);
        if (marginEnd >= 0) {
            this.mTitleMarginEnd = marginEnd;
        }
        int marginTop = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginTop, -1);
        if (marginTop >= 0) {
            this.mTitleMarginTop = marginTop;
        }
        int marginBottom = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginBottom, -1);
        if (marginBottom >= 0) {
            this.mTitleMarginBottom = marginBottom;
        }
        this.mMaxButtonHeight = a.getDimensionPixelSize(R.styleable.Toolbar_maxButtonHeight, -1);
        int contentInsetStart = a.getDimensionPixelOffset(R.styleable.Toolbar_contentInsetStart, Integer.MIN_VALUE);
        int contentInsetEnd = a.getDimensionPixelOffset(R.styleable.Toolbar_contentInsetEnd, Integer.MIN_VALUE);
        int contentInsetLeft = a.getDimensionPixelSize(R.styleable.Toolbar_contentInsetLeft, 0);
        int contentInsetRight = a.getDimensionPixelSize(R.styleable.Toolbar_contentInsetRight, 0);
        ensureContentInsets();
        this.mContentInsets.setAbsolute(contentInsetLeft, contentInsetRight);
        if (!(contentInsetStart == Integer.MIN_VALUE && contentInsetEnd == Integer.MIN_VALUE)) {
            this.mContentInsets.setRelative(contentInsetStart, contentInsetEnd);
        }
        this.mContentInsetStartWithNavigation = a.getDimensionPixelOffset(R.styleable.Toolbar_contentInsetStartWithNavigation, Integer.MIN_VALUE);
        this.mContentInsetEndWithActions = a.getDimensionPixelOffset(R.styleable.Toolbar_contentInsetEndWithActions, Integer.MIN_VALUE);
        this.mCollapseIcon = a.getDrawable(R.styleable.Toolbar_collapseIcon);
        this.mCollapseDescription = a.getText(R.styleable.Toolbar_collapseContentDescription);
        CharSequence title = a.getText(R.styleable.Toolbar_title);
        if (!TextUtils.isEmpty(title)) {
            setTitle(title);
        }
        CharSequence subtitle = a.getText(R.styleable.Toolbar_subtitle);
        if (!TextUtils.isEmpty(subtitle)) {
            setSubtitle(subtitle);
        }
        this.mPopupContext = getContext();
        setPopupTheme(a.getResourceId(R.styleable.Toolbar_popupTheme, 0));
        Drawable navIcon = a.getDrawable(R.styleable.Toolbar_navigationIcon);
        if (navIcon != null) {
            setNavigationIcon(navIcon);
        }
        CharSequence navDesc2 = a.getText(R.styleable.Toolbar_navigationContentDescription);
        if (!TextUtils.isEmpty(navDesc2)) {
            setNavigationContentDescription(navDesc2);
        }
        navIcon = a.getDrawable(R.styleable.Toolbar_logo);
        if (navIcon != null) {
            setLogo(navIcon);
        }
        CharSequence logoDesc = a.getText(R.styleable.Toolbar_logoDescription);
        if (!TextUtils.isEmpty(logoDesc)) {
            setLogoDescription(logoDesc);
        }
        if (a.hasValue(R.styleable.Toolbar_titleTextColor)) {
            navDesc = -1;
            setTitleTextColor(a.getColor(R.styleable.Toolbar_titleTextColor, -1));
        } else {
            navDesc = -1;
        }
        if (a.hasValue(R.styleable.Toolbar_subtitleTextColor)) {
            setSubtitleTextColor(a.getColor(R.styleable.Toolbar_subtitleTextColor, navDesc));
        }
        a.recycle();
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

    public void setTitleMargin(int start, int top, int end, int bottom) {
        this.mTitleMarginStart = start;
        this.mTitleMarginTop = top;
        this.mTitleMarginEnd = end;
        this.mTitleMarginBottom = bottom;
        requestLayout();
    }

    public int getTitleMarginStart() {
        return this.mTitleMarginStart;
    }

    public void setTitleMarginStart(int margin) {
        this.mTitleMarginStart = margin;
        requestLayout();
    }

    public int getTitleMarginTop() {
        return this.mTitleMarginTop;
    }

    public void setTitleMarginTop(int margin) {
        this.mTitleMarginTop = margin;
        requestLayout();
    }

    public int getTitleMarginEnd() {
        return this.mTitleMarginEnd;
    }

    public void setTitleMarginEnd(int margin) {
        this.mTitleMarginEnd = margin;
        requestLayout();
    }

    public int getTitleMarginBottom() {
        return this.mTitleMarginBottom;
    }

    public void setTitleMarginBottom(int margin) {
        this.mTitleMarginBottom = margin;
        requestLayout();
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        if (VERSION.SDK_INT >= 17) {
            super.onRtlPropertiesChanged(layoutDirection);
        }
        ensureContentInsets();
        RtlSpacingHelper rtlSpacingHelper = this.mContentInsets;
        boolean z = true;
        if (layoutDirection != 1) {
            z = false;
        }
        rtlSpacingHelper.setDirection(z);
    }

    public void setLogo(@DrawableRes int resId) {
        setLogo(AppCompatResources.getDrawable(getContext(), resId));
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public boolean canShowOverflowMenu() {
        return getVisibility() == 0 && this.mMenuView != null && this.mMenuView.isOverflowReserved();
    }

    public boolean isOverflowMenuShowing() {
        return this.mMenuView != null && this.mMenuView.isOverflowMenuShowing();
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public boolean isOverflowMenuShowPending() {
        return this.mMenuView != null && this.mMenuView.isOverflowMenuShowPending();
    }

    public boolean showOverflowMenu() {
        return this.mMenuView != null && this.mMenuView.showOverflowMenu();
    }

    public boolean hideOverflowMenu() {
        return this.mMenuView != null && this.mMenuView.hideOverflowMenu();
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public void setMenu(MenuBuilder menu, ActionMenuPresenter outerPresenter) {
        if (menu != null || this.mMenuView != null) {
            ensureMenuView();
            MenuBuilder oldMenu = this.mMenuView.peekMenu();
            if (oldMenu != menu) {
                if (oldMenu != null) {
                    oldMenu.removeMenuPresenter(this.mOuterActionMenuPresenter);
                    oldMenu.removeMenuPresenter(this.mExpandedMenuPresenter);
                }
                if (this.mExpandedMenuPresenter == null) {
                    this.mExpandedMenuPresenter = new ExpandedActionViewMenuPresenter();
                }
                outerPresenter.setExpandedActionViewsExclusive(true);
                if (menu != null) {
                    menu.addMenuPresenter(outerPresenter, this.mPopupContext);
                    menu.addMenuPresenter(this.mExpandedMenuPresenter, this.mPopupContext);
                } else {
                    outerPresenter.initForMenu(this.mPopupContext, null);
                    this.mExpandedMenuPresenter.initForMenu(this.mPopupContext, null);
                    outerPresenter.updateMenuView(true);
                    this.mExpandedMenuPresenter.updateMenuView(true);
                }
                this.mMenuView.setPopupTheme(this.mPopupTheme);
                this.mMenuView.setPresenter(outerPresenter);
                this.mOuterActionMenuPresenter = outerPresenter;
            }
        }
    }

    public void dismissPopupMenus() {
        if (this.mMenuView != null) {
            this.mMenuView.dismissPopupMenus();
        }
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public boolean isTitleTruncated() {
        if (this.mTitleTextView == null) {
            return false;
        }
        Layout titleLayout = this.mTitleTextView.getLayout();
        if (titleLayout == null) {
            return false;
        }
        int lineCount = titleLayout.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            if (titleLayout.getEllipsisCount(i) > 0) {
                return true;
            }
        }
        return false;
    }

    public void setLogo(Drawable drawable) {
        if (drawable != null) {
            ensureLogoView();
            if (!isChildOrHidden(this.mLogoView)) {
                addSystemView(this.mLogoView, true);
            }
        } else if (this.mLogoView != null && isChildOrHidden(this.mLogoView)) {
            removeView(this.mLogoView);
            this.mHiddenViews.remove(this.mLogoView);
        }
        if (this.mLogoView != null) {
            this.mLogoView.setImageDrawable(drawable);
        }
    }

    public Drawable getLogo() {
        return this.mLogoView != null ? this.mLogoView.getDrawable() : null;
    }

    public void setLogoDescription(@StringRes int resId) {
        setLogoDescription(getContext().getText(resId));
    }

    public void setLogoDescription(CharSequence description) {
        if (!TextUtils.isEmpty(description)) {
            ensureLogoView();
        }
        if (this.mLogoView != null) {
            this.mLogoView.setContentDescription(description);
        }
    }

    public CharSequence getLogoDescription() {
        return this.mLogoView != null ? this.mLogoView.getContentDescription() : null;
    }

    private void ensureLogoView() {
        if (this.mLogoView == null) {
            this.mLogoView = new AppCompatImageView(getContext());
        }
    }

    public boolean hasExpandedActionView() {
        return (this.mExpandedMenuPresenter == null || this.mExpandedMenuPresenter.mCurrentExpandedItem == null) ? false : true;
    }

    public void collapseActionView() {
        MenuItemImpl item = this.mExpandedMenuPresenter == null ? null : this.mExpandedMenuPresenter.mCurrentExpandedItem;
        if (item != null) {
            item.collapseActionView();
        }
    }

    public CharSequence getTitle() {
        return this.mTitleText;
    }

    public void setTitle(@StringRes int resId) {
        setTitle(getContext().getText(resId));
    }

    public void setTitle(CharSequence title) {
        if (!TextUtils.isEmpty(title)) {
            if (this.mTitleTextView == null) {
                Context context = getContext();
                this.mTitleTextView = new AppCompatTextView(context);
                this.mTitleTextView.setSingleLine();
                this.mTitleTextView.setEllipsize(TruncateAt.END);
                if (this.mTitleTextAppearance != 0) {
                    this.mTitleTextView.setTextAppearance(context, this.mTitleTextAppearance);
                }
                if (this.mTitleTextColor != 0) {
                    this.mTitleTextView.setTextColor(this.mTitleTextColor);
                }
            }
            if (!isChildOrHidden(this.mTitleTextView)) {
                addSystemView(this.mTitleTextView, true);
            }
        } else if (this.mTitleTextView != null && isChildOrHidden(this.mTitleTextView)) {
            removeView(this.mTitleTextView);
            this.mHiddenViews.remove(this.mTitleTextView);
        }
        if (this.mTitleTextView != null) {
            this.mTitleTextView.setText(title);
        }
        this.mTitleText = title;
    }

    public CharSequence getSubtitle() {
        return this.mSubtitleText;
    }

    public void setSubtitle(@StringRes int resId) {
        setSubtitle(getContext().getText(resId));
    }

    public void setSubtitle(CharSequence subtitle) {
        if (!TextUtils.isEmpty(subtitle)) {
            if (this.mSubtitleTextView == null) {
                Context context = getContext();
                this.mSubtitleTextView = new AppCompatTextView(context);
                this.mSubtitleTextView.setSingleLine();
                this.mSubtitleTextView.setEllipsize(TruncateAt.END);
                if (this.mSubtitleTextAppearance != 0) {
                    this.mSubtitleTextView.setTextAppearance(context, this.mSubtitleTextAppearance);
                }
                if (this.mSubtitleTextColor != 0) {
                    this.mSubtitleTextView.setTextColor(this.mSubtitleTextColor);
                }
            }
            if (!isChildOrHidden(this.mSubtitleTextView)) {
                addSystemView(this.mSubtitleTextView, true);
            }
        } else if (this.mSubtitleTextView != null && isChildOrHidden(this.mSubtitleTextView)) {
            removeView(this.mSubtitleTextView);
            this.mHiddenViews.remove(this.mSubtitleTextView);
        }
        if (this.mSubtitleTextView != null) {
            this.mSubtitleTextView.setText(subtitle);
        }
        this.mSubtitleText = subtitle;
    }

    public void setTitleTextAppearance(Context context, @StyleRes int resId) {
        this.mTitleTextAppearance = resId;
        if (this.mTitleTextView != null) {
            this.mTitleTextView.setTextAppearance(context, resId);
        }
    }

    public void setSubtitleTextAppearance(Context context, @StyleRes int resId) {
        this.mSubtitleTextAppearance = resId;
        if (this.mSubtitleTextView != null) {
            this.mSubtitleTextView.setTextAppearance(context, resId);
        }
    }

    public void setTitleTextColor(@ColorInt int color) {
        this.mTitleTextColor = color;
        if (this.mTitleTextView != null) {
            this.mTitleTextView.setTextColor(color);
        }
    }

    public void setSubtitleTextColor(@ColorInt int color) {
        this.mSubtitleTextColor = color;
        if (this.mSubtitleTextView != null) {
            this.mSubtitleTextView.setTextColor(color);
        }
    }

    @Nullable
    public CharSequence getNavigationContentDescription() {
        return this.mNavButtonView != null ? this.mNavButtonView.getContentDescription() : null;
    }

    public void setNavigationContentDescription(@StringRes int resId) {
        setNavigationContentDescription(resId != 0 ? getContext().getText(resId) : null);
    }

    public void setNavigationContentDescription(@Nullable CharSequence description) {
        if (!TextUtils.isEmpty(description)) {
            ensureNavButtonView();
        }
        if (this.mNavButtonView != null) {
            this.mNavButtonView.setContentDescription(description);
        }
    }

    public void setNavigationIcon(@DrawableRes int resId) {
        setNavigationIcon(AppCompatResources.getDrawable(getContext(), resId));
    }

    public void setNavigationIcon(@Nullable Drawable icon) {
        if (icon != null) {
            ensureNavButtonView();
            if (!isChildOrHidden(this.mNavButtonView)) {
                addSystemView(this.mNavButtonView, true);
            }
        } else if (this.mNavButtonView != null && isChildOrHidden(this.mNavButtonView)) {
            removeView(this.mNavButtonView);
            this.mHiddenViews.remove(this.mNavButtonView);
        }
        if (this.mNavButtonView != null) {
            this.mNavButtonView.setImageDrawable(icon);
        }
    }

    @Nullable
    public Drawable getNavigationIcon() {
        return this.mNavButtonView != null ? this.mNavButtonView.getDrawable() : null;
    }

    public void setNavigationOnClickListener(OnClickListener listener) {
        ensureNavButtonView();
        this.mNavButtonView.setOnClickListener(listener);
    }

    public Menu getMenu() {
        ensureMenu();
        return this.mMenuView.getMenu();
    }

    public void setOverflowIcon(@Nullable Drawable icon) {
        ensureMenu();
        this.mMenuView.setOverflowIcon(icon);
    }

    @Nullable
    public Drawable getOverflowIcon() {
        ensureMenu();
        return this.mMenuView.getOverflowIcon();
    }

    private void ensureMenu() {
        ensureMenuView();
        if (this.mMenuView.peekMenu() == null) {
            MenuBuilder menu = (MenuBuilder) this.mMenuView.getMenu();
            if (this.mExpandedMenuPresenter == null) {
                this.mExpandedMenuPresenter = new ExpandedActionViewMenuPresenter();
            }
            this.mMenuView.setExpandedActionViewsExclusive(true);
            menu.addMenuPresenter(this.mExpandedMenuPresenter, this.mPopupContext);
        }
    }

    private void ensureMenuView() {
        if (this.mMenuView == null) {
            this.mMenuView = new ActionMenuView(getContext());
            this.mMenuView.setPopupTheme(this.mPopupTheme);
            this.mMenuView.setOnMenuItemClickListener(this.mMenuViewItemClickListener);
            this.mMenuView.setMenuCallbacks(this.mActionMenuPresenterCallback, this.mMenuBuilderCallback);
            LayoutParams lp = generateDefaultLayoutParams();
            lp.gravity = GravityCompat.END | (this.mButtonGravity & 112);
            this.mMenuView.setLayoutParams(lp);
            addSystemView(this.mMenuView, false);
        }
    }

    private MenuInflater getMenuInflater() {
        return new SupportMenuInflater(getContext());
    }

    public void inflateMenu(@MenuRes int resId) {
        getMenuInflater().inflate(resId, getMenu());
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.mOnMenuItemClickListener = listener;
    }

    public void setContentInsetsRelative(int contentInsetStart, int contentInsetEnd) {
        ensureContentInsets();
        this.mContentInsets.setRelative(contentInsetStart, contentInsetEnd);
    }

    public int getContentInsetStart() {
        return this.mContentInsets != null ? this.mContentInsets.getStart() : 0;
    }

    public int getContentInsetEnd() {
        return this.mContentInsets != null ? this.mContentInsets.getEnd() : 0;
    }

    public void setContentInsetsAbsolute(int contentInsetLeft, int contentInsetRight) {
        ensureContentInsets();
        this.mContentInsets.setAbsolute(contentInsetLeft, contentInsetRight);
    }

    public int getContentInsetLeft() {
        return this.mContentInsets != null ? this.mContentInsets.getLeft() : 0;
    }

    public int getContentInsetRight() {
        return this.mContentInsets != null ? this.mContentInsets.getRight() : 0;
    }

    public int getContentInsetStartWithNavigation() {
        if (this.mContentInsetStartWithNavigation != Integer.MIN_VALUE) {
            return this.mContentInsetStartWithNavigation;
        }
        return getContentInsetStart();
    }

    public void setContentInsetStartWithNavigation(int insetStartWithNavigation) {
        if (insetStartWithNavigation < 0) {
            insetStartWithNavigation = Integer.MIN_VALUE;
        }
        if (insetStartWithNavigation != this.mContentInsetStartWithNavigation) {
            this.mContentInsetStartWithNavigation = insetStartWithNavigation;
            if (getNavigationIcon() != null) {
                requestLayout();
            }
        }
    }

    public int getContentInsetEndWithActions() {
        if (this.mContentInsetEndWithActions != Integer.MIN_VALUE) {
            return this.mContentInsetEndWithActions;
        }
        return getContentInsetEnd();
    }

    public void setContentInsetEndWithActions(int insetEndWithActions) {
        if (insetEndWithActions < 0) {
            insetEndWithActions = Integer.MIN_VALUE;
        }
        if (insetEndWithActions != this.mContentInsetEndWithActions) {
            this.mContentInsetEndWithActions = insetEndWithActions;
            if (getNavigationIcon() != null) {
                requestLayout();
            }
        }
    }

    public int getCurrentContentInsetStart() {
        if (getNavigationIcon() != null) {
            return Math.max(getContentInsetStart(), Math.max(this.mContentInsetStartWithNavigation, 0));
        }
        return getContentInsetStart();
    }

    public int getCurrentContentInsetEnd() {
        boolean hasActions = false;
        if (this.mMenuView != null) {
            MenuBuilder mb = this.mMenuView.peekMenu();
            boolean z = mb != null && mb.hasVisibleItems();
            hasActions = z;
        }
        if (hasActions) {
            return Math.max(getContentInsetEnd(), Math.max(this.mContentInsetEndWithActions, 0));
        }
        return getContentInsetEnd();
    }

    public int getCurrentContentInsetLeft() {
        if (ViewCompat.getLayoutDirection(this) == 1) {
            return getCurrentContentInsetEnd();
        }
        return getCurrentContentInsetStart();
    }

    public int getCurrentContentInsetRight() {
        if (ViewCompat.getLayoutDirection(this) == 1) {
            return getCurrentContentInsetStart();
        }
        return getCurrentContentInsetEnd();
    }

    private void ensureNavButtonView() {
        if (this.mNavButtonView == null) {
            this.mNavButtonView = new AppCompatImageButton(getContext(), null, R.attr.toolbarNavigationButtonStyle);
            LayoutParams lp = generateDefaultLayoutParams();
            lp.gravity = GravityCompat.START | (this.mButtonGravity & 112);
            this.mNavButtonView.setLayoutParams(lp);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void ensureCollapseButtonView() {
        if (this.mCollapseButtonView == null) {
            this.mCollapseButtonView = new AppCompatImageButton(getContext(), null, R.attr.toolbarNavigationButtonStyle);
            this.mCollapseButtonView.setImageDrawable(this.mCollapseIcon);
            this.mCollapseButtonView.setContentDescription(this.mCollapseDescription);
            LayoutParams lp = generateDefaultLayoutParams();
            lp.gravity = GravityCompat.START | (this.mButtonGravity & 112);
            lp.mViewType = 2;
            this.mCollapseButtonView.setLayoutParams(lp);
            this.mCollapseButtonView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Toolbar.this.collapseActionView();
                }
            });
        }
    }

    private void addSystemView(View v, boolean allowHide) {
        LayoutParams lp;
        android.view.ViewGroup.LayoutParams vlp = v.getLayoutParams();
        if (vlp == null) {
            lp = generateDefaultLayoutParams();
        } else if (checkLayoutParams(vlp)) {
            lp = (LayoutParams) vlp;
        } else {
            lp = generateLayoutParams(vlp);
        }
        lp.mViewType = 1;
        if (!allowHide || this.mExpandedActionView == null) {
            addView(v, lp);
            return;
        }
        v.setLayoutParams(lp);
        this.mHiddenViews.add(v);
    }

    /* Access modifiers changed, original: protected */
    public Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        if (!(this.mExpandedMenuPresenter == null || this.mExpandedMenuPresenter.mCurrentExpandedItem == null)) {
            state.expandedMenuItemId = this.mExpandedMenuPresenter.mCurrentExpandedItem.getItemId();
        }
        state.isOverflowOpen = isOverflowMenuShowing();
        return state;
    }

    /* Access modifiers changed, original: protected */
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss.getSuperState());
            Menu menu = this.mMenuView != null ? this.mMenuView.peekMenu() : null;
            if (!(ss.expandedMenuItemId == 0 || this.mExpandedMenuPresenter == null || menu == null)) {
                MenuItem item = menu.findItem(ss.expandedMenuItemId);
                if (item != null) {
                    item.expandActionView();
                }
            }
            if (ss.isOverflowOpen) {
                postShowOverflowMenu();
            }
            return;
        }
        super.onRestoreInstanceState(state);
    }

    private void postShowOverflowMenu() {
        removeCallbacks(this.mShowOverflowMenuRunnable);
        post(this.mShowOverflowMenuRunnable);
    }

    /* Access modifiers changed, original: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this.mShowOverflowMenuRunnable);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == 0) {
            this.mEatingTouch = false;
        }
        if (!this.mEatingTouch) {
            boolean handled = super.onTouchEvent(ev);
            if (action == 0 && !handled) {
                this.mEatingTouch = true;
            }
        }
        if (action == 1 || action == 3) {
            this.mEatingTouch = false;
        }
        return true;
    }

    public boolean onHoverEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == 9) {
            this.mEatingHover = false;
        }
        if (!this.mEatingHover) {
            boolean handled = super.onHoverEvent(ev);
            if (action == 9 && !handled) {
                this.mEatingHover = true;
            }
        }
        if (action == 10 || action == 3) {
            this.mEatingHover = false;
        }
        return true;
    }

    private void measureChildConstrained(View child, int parentWidthSpec, int widthUsed, int parentHeightSpec, int heightUsed, int heightConstraint) {
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        int childWidthSpec = getChildMeasureSpec(parentWidthSpec, (((getPaddingLeft() + getPaddingRight()) + lp.leftMargin) + lp.rightMargin) + widthUsed, lp.width);
        int childHeightSpec = getChildMeasureSpec(parentHeightSpec, (((getPaddingTop() + getPaddingBottom()) + lp.topMargin) + lp.bottomMargin) + heightUsed, lp.height);
        int childHeightMode = MeasureSpec.getMode(childHeightSpec);
        if (childHeightMode != Ints.MAX_POWER_OF_TWO && heightConstraint >= 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(childHeightMode != 0 ? Math.min(MeasureSpec.getSize(childHeightSpec), heightConstraint) : heightConstraint, Ints.MAX_POWER_OF_TWO);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    private int measureChildCollapseMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed, int[] collapsingMargins) {
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        int leftDiff = lp.leftMargin - collapsingMargins[0];
        int rightDiff = lp.rightMargin - collapsingMargins[1];
        int hMargins = Math.max(0, leftDiff) + Math.max(0, rightDiff);
        collapsingMargins[0] = Math.max(0, -leftDiff);
        collapsingMargins[1] = Math.max(0, -rightDiff);
        child.measure(getChildMeasureSpec(parentWidthMeasureSpec, ((getPaddingLeft() + getPaddingRight()) + hMargins) + widthUsed, lp.width), getChildMeasureSpec(parentHeightMeasureSpec, (((getPaddingTop() + getPaddingBottom()) + lp.topMargin) + lp.bottomMargin) + heightUsed, lp.height));
        return child.getMeasuredWidth() + hMargins;
    }

    private boolean shouldCollapse() {
        if (!this.mCollapsible) {
            return false;
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (shouldLayout(child) && child.getMeasuredWidth() > 0 && child.getMeasuredHeight() > 0) {
                return false;
            }
        }
        return true;
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int marginStartIndex;
        int marginEndIndex;
        int menuWidth;
        int childState;
        int i;
        int menuWidth2;
        int height = 0;
        int childState2 = 0;
        int[] collapsingMargins = this.mTempMargins;
        if (ViewUtils.isLayoutRtl(this)) {
            marginStartIndex = 1;
            marginEndIndex = 0;
        } else {
            marginStartIndex = 0;
            marginEndIndex = 1;
        }
        int marginStartIndex2 = marginStartIndex;
        int marginEndIndex2 = marginEndIndex;
        int navWidth = 0;
        if (shouldLayout(this.mNavButtonView)) {
            measureChildConstrained(this.mNavButtonView, widthMeasureSpec, 0, heightMeasureSpec, 0, this.mMaxButtonHeight);
            navWidth = this.mNavButtonView.getMeasuredWidth() + getHorizontalMargins(this.mNavButtonView);
            height = Math.max(0, this.mNavButtonView.getMeasuredHeight() + getVerticalMargins(this.mNavButtonView));
            childState2 = View.combineMeasuredStates(0, this.mNavButtonView.getMeasuredState());
        }
        if (shouldLayout(this.mCollapseButtonView)) {
            measureChildConstrained(this.mCollapseButtonView, widthMeasureSpec, 0, heightMeasureSpec, 0, this.mMaxButtonHeight);
            navWidth = this.mCollapseButtonView.getMeasuredWidth() + getHorizontalMargins(this.mCollapseButtonView);
            height = Math.max(height, this.mCollapseButtonView.getMeasuredHeight() + getVerticalMargins(this.mCollapseButtonView));
            childState2 = View.combineMeasuredStates(childState2, this.mCollapseButtonView.getMeasuredState());
        }
        int contentInsetStart = getCurrentContentInsetStart();
        int width = 0 + Math.max(contentInsetStart, navWidth);
        collapsingMargins[marginStartIndex2] = Math.max(0, contentInsetStart - navWidth);
        if (shouldLayout(this.mMenuView)) {
            marginStartIndex2 = 0;
            measureChildConstrained(this.mMenuView, widthMeasureSpec, width, heightMeasureSpec, 0, this.mMaxButtonHeight);
            menuWidth = this.mMenuView.getMeasuredWidth() + getHorizontalMargins(this.mMenuView);
            height = Math.max(height, this.mMenuView.getMeasuredHeight() + getVerticalMargins(this.mMenuView));
            childState = View.combineMeasuredStates(childState2, this.mMenuView.getMeasuredState());
            childState2 = height;
            height = menuWidth;
        } else {
            marginStartIndex2 = 0;
            childState = childState2;
            childState2 = height;
            height = 0;
        }
        int contentInsetEnd = getCurrentContentInsetEnd();
        width += Math.max(contentInsetEnd, height);
        collapsingMargins[marginEndIndex2] = Math.max(marginStartIndex2, contentInsetEnd - height);
        if (shouldLayout(this.mExpandedActionView)) {
            marginStartIndex2 = childState;
            width += measureChildCollapseMargins(this.mExpandedActionView, widthMeasureSpec, width, heightMeasureSpec, 0, collapsingMargins);
            childState2 = Math.max(childState2, this.mExpandedActionView.getMeasuredHeight() + getVerticalMargins(this.mExpandedActionView));
            marginStartIndex2 = View.combineMeasuredStates(marginStartIndex2, this.mExpandedActionView.getMeasuredState());
        } else {
            marginStartIndex2 = childState;
        }
        if (shouldLayout(this.mLogoView)) {
            width += measureChildCollapseMargins(this.mLogoView, widthMeasureSpec, width, heightMeasureSpec, 0, collapsingMargins);
            childState2 = Math.max(childState2, this.mLogoView.getMeasuredHeight() + getVerticalMargins(this.mLogoView));
            marginStartIndex2 = View.combineMeasuredStates(marginStartIndex2, this.mLogoView.getMeasuredState());
        }
        childState = getChildCount();
        marginStartIndex = 0;
        while (true) {
            contentInsetEnd = marginStartIndex;
            if (contentInsetEnd >= childState) {
                break;
            }
            View child = getChildAt(contentInsetEnd);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.mViewType != 0) {
                i = contentInsetEnd;
                menuWidth = childState;
                menuWidth2 = height;
            } else if (shouldLayout(child)) {
                menuWidth2 = height;
                height = child;
                i = contentInsetEnd;
                menuWidth = childState;
                width += measureChildCollapseMargins(child, widthMeasureSpec, width, heightMeasureSpec, 0, collapsingMargins);
                childState2 = Math.max(childState2, height.getMeasuredHeight() + getVerticalMargins(height));
                marginStartIndex2 = View.combineMeasuredStates(marginStartIndex2, height.getMeasuredState());
            } else {
                i = contentInsetEnd;
                menuWidth = childState;
                menuWidth2 = height;
            }
            marginStartIndex = i + 1;
            childState = menuWidth;
            height = menuWidth2;
        }
        menuWidth2 = height;
        height = 0;
        int titleHeight = 0;
        i = this.mTitleMarginTop + this.mTitleMarginBottom;
        int titleHorizMargins = this.mTitleMarginStart + this.mTitleMarginEnd;
        if (shouldLayout(this.mTitleTextView)) {
            marginStartIndex = measureChildCollapseMargins(this.mTitleTextView, widthMeasureSpec, width + titleHorizMargins, heightMeasureSpec, i, collapsingMargins);
            height = this.mTitleTextView.getMeasuredWidth() + getHorizontalMargins(this.mTitleTextView);
            titleHeight = this.mTitleTextView.getMeasuredHeight() + getVerticalMargins(this.mTitleTextView);
            marginStartIndex2 = View.combineMeasuredStates(marginStartIndex2, this.mTitleTextView.getMeasuredState());
        }
        if (shouldLayout(this.mSubtitleTextView)) {
            height = Math.max(height, measureChildCollapseMargins(this.mSubtitleTextView, widthMeasureSpec, width + titleHorizMargins, heightMeasureSpec, titleHeight + i, collapsingMargins));
            titleHeight += this.mSubtitleTextView.getMeasuredHeight() + getVerticalMargins(this.mSubtitleTextView);
            marginStartIndex2 = View.combineMeasuredStates(marginStartIndex2, this.mSubtitleTextView.getMeasuredState());
        }
        setMeasuredDimension(View.resolveSizeAndState(Math.max((width + height) + (getPaddingLeft() + getPaddingRight()), getSuggestedMinimumWidth()), widthMeasureSpec, ViewCompat.MEASURED_STATE_MASK & marginStartIndex2), shouldCollapse() ? 0 : View.resolveSizeAndState(Math.max(Math.max(childState2, titleHeight) + (getPaddingTop() + getPaddingBottom()), getSuggestedMinimumHeight()), heightMeasureSpec, marginStartIndex2 << 16));
    }

    /* Access modifiers changed, original: protected */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x01c7  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x017a  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x025c  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x01d8  */
    /* JADX WARNING: Missing block: B:51:0x015c, code skipped:
            if (r0.mTitleTextView.getMeasuredWidth() <= 0) goto L_0x0161;
     */
    public void onLayout(boolean r41, int r42, int r43, int r44, int r45) {
        /*
        r40 = this;
        r0 = r40;
        r1 = android.support.v4.view.ViewCompat.getLayoutDirection(r40);
        r2 = 1;
        r3 = 0;
        if (r1 != r2) goto L_0x000c;
    L_0x000a:
        r1 = r2;
        goto L_0x000d;
    L_0x000c:
        r1 = r3;
    L_0x000d:
        r4 = r40.getWidth();
        r5 = r40.getHeight();
        r6 = r40.getPaddingLeft();
        r7 = r40.getPaddingRight();
        r8 = r40.getPaddingTop();
        r9 = r40.getPaddingBottom();
        r10 = r6;
        r11 = r4 - r7;
        r12 = r0.mTempMargins;
        r12[r2] = r3;
        r12[r3] = r3;
        r13 = android.support.v4.view.ViewCompat.getMinimumHeight(r40);
        if (r13 < 0) goto L_0x003b;
    L_0x0034:
        r2 = r45 - r43;
        r2 = java.lang.Math.min(r13, r2);
        goto L_0x003c;
    L_0x003b:
        r2 = r3;
    L_0x003c:
        r3 = r0.mNavButtonView;
        r3 = r0.shouldLayout(r3);
        if (r3 == 0) goto L_0x0053;
    L_0x0044:
        if (r1 == 0) goto L_0x004d;
    L_0x0046:
        r3 = r0.mNavButtonView;
        r11 = r0.layoutChildRight(r3, r11, r12, r2);
        goto L_0x0053;
    L_0x004d:
        r3 = r0.mNavButtonView;
        r10 = r0.layoutChildLeft(r3, r10, r12, r2);
    L_0x0053:
        r3 = r0.mCollapseButtonView;
        r3 = r0.shouldLayout(r3);
        if (r3 == 0) goto L_0x006a;
    L_0x005b:
        if (r1 == 0) goto L_0x0064;
    L_0x005d:
        r3 = r0.mCollapseButtonView;
        r11 = r0.layoutChildRight(r3, r11, r12, r2);
        goto L_0x006a;
    L_0x0064:
        r3 = r0.mCollapseButtonView;
        r10 = r0.layoutChildLeft(r3, r10, r12, r2);
    L_0x006a:
        r3 = r0.mMenuView;
        r3 = r0.shouldLayout(r3);
        if (r3 == 0) goto L_0x0081;
    L_0x0072:
        if (r1 == 0) goto L_0x007b;
    L_0x0074:
        r3 = r0.mMenuView;
        r10 = r0.layoutChildLeft(r3, r10, r12, r2);
        goto L_0x0081;
    L_0x007b:
        r3 = r0.mMenuView;
        r11 = r0.layoutChildRight(r3, r11, r12, r2);
    L_0x0081:
        r3 = r40.getCurrentContentInsetLeft();
        r17 = r40.getCurrentContentInsetRight();
        r18 = r13;
        r13 = r3 - r10;
        r14 = 0;
        r13 = java.lang.Math.max(r14, r13);
        r12[r14] = r13;
        r13 = r4 - r7;
        r13 = r13 - r11;
        r13 = r17 - r13;
        r13 = java.lang.Math.max(r14, r13);
        r14 = 1;
        r12[r14] = r13;
        r10 = java.lang.Math.max(r10, r3);
        r13 = r4 - r7;
        r13 = r13 - r17;
        r11 = java.lang.Math.min(r11, r13);
        r13 = r0.mExpandedActionView;
        r13 = r0.shouldLayout(r13);
        if (r13 == 0) goto L_0x00c3;
    L_0x00b4:
        if (r1 == 0) goto L_0x00bd;
    L_0x00b6:
        r13 = r0.mExpandedActionView;
        r11 = r0.layoutChildRight(r13, r11, r12, r2);
        goto L_0x00c3;
    L_0x00bd:
        r13 = r0.mExpandedActionView;
        r10 = r0.layoutChildLeft(r13, r10, r12, r2);
    L_0x00c3:
        r13 = r0.mLogoView;
        r13 = r0.shouldLayout(r13);
        if (r13 == 0) goto L_0x00da;
    L_0x00cb:
        if (r1 == 0) goto L_0x00d4;
    L_0x00cd:
        r13 = r0.mLogoView;
        r11 = r0.layoutChildRight(r13, r11, r12, r2);
        goto L_0x00da;
    L_0x00d4:
        r13 = r0.mLogoView;
        r10 = r0.layoutChildLeft(r13, r10, r12, r2);
    L_0x00da:
        r13 = r0.mTitleTextView;
        r13 = r0.shouldLayout(r13);
        r14 = r0.mSubtitleTextView;
        r14 = r0.shouldLayout(r14);
        r19 = 0;
        if (r13 == 0) goto L_0x0105;
    L_0x00ea:
        r20 = r3;
        r3 = r0.mTitleTextView;
        r3 = r3.getLayoutParams();
        r3 = (android.support.v7.widget.Toolbar.LayoutParams) r3;
        r15 = r3.topMargin;
        r21 = r7;
        r7 = r0.mTitleTextView;
        r7 = r7.getMeasuredHeight();
        r15 = r15 + r7;
        r7 = r3.bottomMargin;
        r15 = r15 + r7;
        r19 = r19 + r15;
        goto L_0x0109;
    L_0x0105:
        r20 = r3;
        r21 = r7;
    L_0x0109:
        if (r14 == 0) goto L_0x0121;
    L_0x010b:
        r3 = r0.mSubtitleTextView;
        r3 = r3.getLayoutParams();
        r3 = (android.support.v7.widget.Toolbar.LayoutParams) r3;
        r7 = r3.topMargin;
        r15 = r0.mSubtitleTextView;
        r15 = r15.getMeasuredHeight();
        r7 = r7 + r15;
        r15 = r3.bottomMargin;
        r7 = r7 + r15;
        r19 = r19 + r7;
    L_0x0121:
        if (r13 != 0) goto L_0x0134;
    L_0x0123:
        if (r14 == 0) goto L_0x0126;
    L_0x0125:
        goto L_0x0134;
    L_0x0126:
        r30 = r1;
        r28 = r2;
        r25 = r4;
        r33 = r5;
        r26 = r6;
        r36 = r8;
        goto L_0x02d7;
    L_0x0134:
        if (r13 == 0) goto L_0x0139;
    L_0x0136:
        r3 = r0.mTitleTextView;
        goto L_0x013b;
    L_0x0139:
        r3 = r0.mSubtitleTextView;
    L_0x013b:
        if (r14 == 0) goto L_0x0140;
    L_0x013d:
        r7 = r0.mSubtitleTextView;
        goto L_0x0142;
    L_0x0140:
        r7 = r0.mTitleTextView;
    L_0x0142:
        r15 = r3.getLayoutParams();
        r15 = (android.support.v7.widget.Toolbar.LayoutParams) r15;
        r22 = r7.getLayoutParams();
        r23 = r3;
        r3 = r22;
        r3 = (android.support.v7.widget.Toolbar.LayoutParams) r3;
        if (r13 == 0) goto L_0x015f;
    L_0x0154:
        r24 = r7;
        r7 = r0.mTitleTextView;
        r7 = r7.getMeasuredWidth();
        if (r7 > 0) goto L_0x016b;
    L_0x015e:
        goto L_0x0161;
    L_0x015f:
        r24 = r7;
    L_0x0161:
        if (r14 == 0) goto L_0x016d;
    L_0x0163:
        r7 = r0.mSubtitleTextView;
        r7 = r7.getMeasuredWidth();
        if (r7 <= 0) goto L_0x016d;
    L_0x016b:
        r7 = 1;
        goto L_0x016e;
    L_0x016d:
        r7 = 0;
    L_0x016e:
        r25 = r4;
        r4 = r0.mGravity;
        r4 = r4 & 112;
        r26 = r6;
        r6 = 48;
        if (r4 == r6) goto L_0x01c7;
    L_0x017a:
        r6 = 80;
        if (r4 == r6) goto L_0x01b8;
    L_0x017e:
        r4 = r5 - r8;
        r4 = r4 - r9;
        r6 = r4 - r19;
        r6 = r6 / 2;
        r27 = r4;
        r4 = r15.topMargin;
        r28 = r2;
        r2 = r0.mTitleMarginTop;
        r4 = r4 + r2;
        if (r6 >= r4) goto L_0x0199;
    L_0x0190:
        r2 = r15.topMargin;
        r4 = r0.mTitleMarginTop;
        r6 = r2 + r4;
        r29 = r10;
        goto L_0x01b5;
    L_0x0199:
        r2 = r5 - r9;
        r2 = r2 - r19;
        r2 = r2 - r6;
        r2 = r2 - r8;
        r4 = r15.bottomMargin;
        r29 = r10;
        r10 = r0.mTitleMarginBottom;
        r4 = r4 + r10;
        if (r2 >= r4) goto L_0x01b5;
    L_0x01a8:
        r4 = r3.bottomMargin;
        r10 = r0.mTitleMarginBottom;
        r4 = r4 + r10;
        r4 = r4 - r2;
        r4 = r6 - r4;
        r10 = 0;
        r6 = java.lang.Math.max(r10, r4);
    L_0x01b5:
        r2 = r8 + r6;
        goto L_0x01d6;
    L_0x01b8:
        r28 = r2;
        r29 = r10;
        r2 = r5 - r9;
        r4 = r3.bottomMargin;
        r2 = r2 - r4;
        r4 = r0.mTitleMarginBottom;
        r2 = r2 - r4;
        r2 = r2 - r19;
        goto L_0x01d6;
    L_0x01c7:
        r28 = r2;
        r29 = r10;
        r2 = r40.getPaddingTop();
        r4 = r15.topMargin;
        r2 = r2 + r4;
        r4 = r0.mTitleMarginTop;
        r2 = r2 + r4;
    L_0x01d6:
        if (r1 == 0) goto L_0x025c;
    L_0x01d8:
        if (r7 == 0) goto L_0x01dd;
    L_0x01da:
        r4 = r0.mTitleMarginStart;
        goto L_0x01de;
    L_0x01dd:
        r4 = 0;
    L_0x01de:
        r6 = 1;
        r10 = r12[r6];
        r4 = r4 - r10;
        r10 = 0;
        r16 = java.lang.Math.max(r10, r4);
        r11 = r11 - r16;
        r30 = r1;
        r1 = -r4;
        r1 = java.lang.Math.max(r10, r1);
        r12[r6] = r1;
        r1 = r11;
        r6 = r11;
        if (r13 == 0) goto L_0x0221;
    L_0x01f6:
        r10 = r0.mTitleTextView;
        r10 = r10.getLayoutParams();
        r10 = (android.support.v7.widget.Toolbar.LayoutParams) r10;
        r31 = r3;
        r3 = r0.mTitleTextView;
        r3 = r3.getMeasuredWidth();
        r3 = r1 - r3;
        r32 = r4;
        r4 = r0.mTitleTextView;
        r4 = r4.getMeasuredHeight();
        r4 = r4 + r2;
        r33 = r5;
        r5 = r0.mTitleTextView;
        r5.layout(r3, r2, r1, r4);
        r5 = r0.mTitleMarginEnd;
        r1 = r3 - r5;
        r5 = r10.bottomMargin;
        r2 = r4 + r5;
        goto L_0x0227;
    L_0x0221:
        r31 = r3;
        r32 = r4;
        r33 = r5;
    L_0x0227:
        if (r14 == 0) goto L_0x024f;
    L_0x0229:
        r3 = r0.mSubtitleTextView;
        r3 = r3.getLayoutParams();
        r3 = (android.support.v7.widget.Toolbar.LayoutParams) r3;
        r4 = r3.topMargin;
        r2 = r2 + r4;
        r4 = r0.mSubtitleTextView;
        r4 = r4.getMeasuredWidth();
        r4 = r6 - r4;
        r5 = r0.mSubtitleTextView;
        r5 = r5.getMeasuredHeight();
        r5 = r5 + r2;
        r10 = r0.mSubtitleTextView;
        r10.layout(r4, r2, r6, r5);
        r10 = r0.mTitleMarginEnd;
        r6 = r6 - r10;
        r10 = r3.bottomMargin;
        r2 = r5 + r10;
    L_0x024f:
        if (r7 == 0) goto L_0x0256;
    L_0x0251:
        r1 = java.lang.Math.min(r1, r6);
        r11 = r1;
    L_0x0256:
        r36 = r8;
        r10 = r29;
        goto L_0x02d7;
    L_0x025c:
        r30 = r1;
        r31 = r3;
        r33 = r5;
        if (r7 == 0) goto L_0x0267;
    L_0x0264:
        r3 = r0.mTitleMarginStart;
        goto L_0x0268;
    L_0x0267:
        r3 = 0;
    L_0x0268:
        r1 = 0;
        r4 = r12[r1];
        r3 = r3 - r4;
        r4 = java.lang.Math.max(r1, r3);
        r10 = r29 + r4;
        r4 = -r3;
        r4 = java.lang.Math.max(r1, r4);
        r12[r1] = r4;
        r4 = r10;
        r5 = r10;
        if (r13 == 0) goto L_0x02a5;
    L_0x027d:
        r6 = r0.mTitleTextView;
        r6 = r6.getLayoutParams();
        r6 = (android.support.v7.widget.Toolbar.LayoutParams) r6;
        r1 = r0.mTitleTextView;
        r1 = r1.getMeasuredWidth();
        r1 = r1 + r4;
        r35 = r3;
        r3 = r0.mTitleTextView;
        r3 = r3.getMeasuredHeight();
        r3 = r3 + r2;
        r36 = r8;
        r8 = r0.mTitleTextView;
        r8.layout(r4, r2, r1, r3);
        r8 = r0.mTitleMarginEnd;
        r4 = r1 + r8;
        r8 = r6.bottomMargin;
        r2 = r3 + r8;
        goto L_0x02a9;
    L_0x02a5:
        r35 = r3;
        r36 = r8;
    L_0x02a9:
        if (r14 == 0) goto L_0x02d1;
    L_0x02ab:
        r1 = r0.mSubtitleTextView;
        r1 = r1.getLayoutParams();
        r1 = (android.support.v7.widget.Toolbar.LayoutParams) r1;
        r3 = r1.topMargin;
        r2 = r2 + r3;
        r3 = r0.mSubtitleTextView;
        r3 = r3.getMeasuredWidth();
        r3 = r3 + r5;
        r6 = r0.mSubtitleTextView;
        r6 = r6.getMeasuredHeight();
        r6 = r6 + r2;
        r8 = r0.mSubtitleTextView;
        r8.layout(r5, r2, r3, r6);
        r8 = r0.mTitleMarginEnd;
        r5 = r3 + r8;
        r8 = r1.bottomMargin;
        r2 = r6 + r8;
    L_0x02d1:
        if (r7 == 0) goto L_0x02d7;
    L_0x02d3:
        r10 = java.lang.Math.max(r4, r5);
    L_0x02d7:
        r1 = r0.mTempViews;
        r2 = 3;
        r0.addCustomViewsWithGravity(r1, r2);
        r1 = r0.mTempViews;
        r1 = r1.size();
        r2 = 0;
    L_0x02e4:
        if (r2 >= r1) goto L_0x02f7;
    L_0x02e6:
        r3 = r0.mTempViews;
        r3 = r3.get(r2);
        r3 = (android.view.View) r3;
        r4 = r28;
        r10 = r0.layoutChildLeft(r3, r10, r12, r4);
        r2 = r2 + 1;
        goto L_0x02e4;
    L_0x02f7:
        r4 = r28;
        r2 = r0.mTempViews;
        r3 = 5;
        r0.addCustomViewsWithGravity(r2, r3);
        r2 = r0.mTempViews;
        r2 = r2.size();
        r3 = 0;
    L_0x0306:
        if (r3 >= r2) goto L_0x0317;
    L_0x0308:
        r5 = r0.mTempViews;
        r5 = r5.get(r3);
        r5 = (android.view.View) r5;
        r11 = r0.layoutChildRight(r5, r11, r12, r4);
        r3 = r3 + 1;
        goto L_0x0306;
    L_0x0317:
        r3 = r0.mTempViews;
        r5 = 1;
        r0.addCustomViewsWithGravity(r3, r5);
        r3 = r0.mTempViews;
        r3 = r0.getViewListMeasuredWidth(r3, r12);
        r5 = r25 - r26;
        r5 = r5 - r21;
        r5 = r5 / 2;
        r6 = r26 + r5;
        r5 = r3 / 2;
        r7 = r6 - r5;
        r8 = r7 + r3;
        if (r7 >= r10) goto L_0x0335;
    L_0x0333:
        r7 = r10;
        goto L_0x033a;
    L_0x0335:
        if (r8 <= r11) goto L_0x033a;
    L_0x0337:
        r15 = r8 - r11;
        r7 = r7 - r15;
    L_0x033a:
        r15 = r0.mTempViews;
        r15 = r15.size();
        r34 = 0;
    L_0x0342:
        r37 = r34;
        r38 = r1;
        r1 = r37;
        if (r1 >= r15) goto L_0x035f;
    L_0x034a:
        r39 = r2;
        r2 = r0.mTempViews;
        r2 = r2.get(r1);
        r2 = (android.view.View) r2;
        r7 = r0.layoutChildLeft(r2, r7, r12, r4);
        r34 = r1 + 1;
        r1 = r38;
        r2 = r39;
        goto L_0x0342;
    L_0x035f:
        r39 = r2;
        r1 = r0.mTempViews;
        r1.clear();
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.widget.Toolbar.onLayout(boolean, int, int, int, int):void");
    }

    private int getViewListMeasuredWidth(List<View> views, int[] collapsingMargins) {
        int collapseLeft = collapsingMargins[0];
        int collapseRight = collapsingMargins[1];
        int count = views.size();
        int width = 0;
        int collapseRight2 = collapseRight;
        collapseRight = collapseLeft;
        for (collapseLeft = 0; collapseLeft < count; collapseLeft++) {
            View v = (View) views.get(collapseLeft);
            LayoutParams lp = (LayoutParams) v.getLayoutParams();
            int l = lp.leftMargin - collapseRight;
            int r = lp.rightMargin - collapseRight2;
            int leftMargin = Math.max(0, l);
            int rightMargin = Math.max(0, r);
            collapseRight = Math.max(0, -l);
            collapseRight2 = Math.max(0, -r);
            width += (v.getMeasuredWidth() + leftMargin) + rightMargin;
        }
        return width;
    }

    private int layoutChildLeft(View child, int left, int[] collapsingMargins, int alignmentHeight) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int l = lp.leftMargin - collapsingMargins[0];
        left += Math.max(0, l);
        collapsingMargins[0] = Math.max(0, -l);
        int top = getChildTop(child, alignmentHeight);
        int childWidth = child.getMeasuredWidth();
        child.layout(left, top, left + childWidth, child.getMeasuredHeight() + top);
        return left + (lp.rightMargin + childWidth);
    }

    private int layoutChildRight(View child, int right, int[] collapsingMargins, int alignmentHeight) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int r = lp.rightMargin - collapsingMargins[1];
        right -= Math.max(0, r);
        collapsingMargins[1] = Math.max(0, -r);
        int top = getChildTop(child, alignmentHeight);
        int childWidth = child.getMeasuredWidth();
        child.layout(right - childWidth, top, right, child.getMeasuredHeight() + top);
        return right - (lp.leftMargin + childWidth);
    }

    private int getChildTop(View child, int alignmentHeight) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int childHeight = child.getMeasuredHeight();
        int alignmentOffset = alignmentHeight > 0 ? (childHeight - alignmentHeight) / 2 : 0;
        int childVerticalGravity = getChildVerticalGravity(lp.gravity);
        if (childVerticalGravity == 48) {
            return getPaddingTop() - alignmentOffset;
        }
        if (childVerticalGravity == 80) {
            return (((getHeight() - getPaddingBottom()) - childHeight) - lp.bottomMargin) - alignmentOffset;
        }
        childVerticalGravity = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int height = getHeight();
        int spaceAbove = (((height - childVerticalGravity) - paddingBottom) - childHeight) / 2;
        if (spaceAbove < lp.topMargin) {
            spaceAbove = lp.topMargin;
        } else {
            int spaceBelow = (((height - paddingBottom) - childHeight) - spaceAbove) - childVerticalGravity;
            if (spaceBelow < lp.bottomMargin) {
                spaceAbove = Math.max(0, spaceAbove - (lp.bottomMargin - spaceBelow));
            }
        }
        return childVerticalGravity + spaceAbove;
    }

    private int getChildVerticalGravity(int gravity) {
        int vgrav = gravity & 112;
        if (vgrav == 16 || vgrav == 48 || vgrav == 80) {
            return vgrav;
        }
        return this.mGravity & 112;
    }

    private void addCustomViewsWithGravity(List<View> views, int gravity) {
        int i = 0;
        boolean z = true;
        if (ViewCompat.getLayoutDirection(this) != 1) {
            z = false;
        }
        boolean isRtl = z;
        int childCount = getChildCount();
        int absGrav = GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this));
        views.clear();
        View child;
        LayoutParams lp;
        if (isRtl) {
            for (i = childCount - 1; i >= 0; i--) {
                child = getChildAt(i);
                lp = (LayoutParams) child.getLayoutParams();
                if (lp.mViewType == 0 && shouldLayout(child) && getChildHorizontalGravity(lp.gravity) == absGrav) {
                    views.add(child);
                }
            }
            return;
        }
        while (i < childCount) {
            child = getChildAt(i);
            lp = (LayoutParams) child.getLayoutParams();
            if (lp.mViewType == 0 && shouldLayout(child) && getChildHorizontalGravity(lp.gravity) == absGrav) {
                views.add(child);
            }
            i++;
        }
    }

    private int getChildHorizontalGravity(int gravity) {
        int ld = ViewCompat.getLayoutDirection(this);
        int hGrav = GravityCompat.getAbsoluteGravity(gravity, ld) & 7;
        if (hGrav != 1) {
            int i = 3;
            if (!(hGrav == 3 || hGrav == 5)) {
                if (ld == 1) {
                    i = 5;
                }
                return i;
            }
        }
        return hGrav;
    }

    private boolean shouldLayout(View view) {
        return (view == null || view.getParent() != this || view.getVisibility() == 8) ? false : true;
    }

    private int getHorizontalMargins(View v) {
        MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
        return MarginLayoutParamsCompat.getMarginStart(mlp) + MarginLayoutParamsCompat.getMarginEnd(mlp);
    }

    private int getVerticalMargins(View v) {
        MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
        return mlp.topMargin + mlp.bottomMargin;
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /* Access modifiers changed, original: protected */
    public LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        if (p instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) p);
        }
        if (p instanceof android.support.v7.app.ActionBar.LayoutParams) {
            return new LayoutParams((android.support.v7.app.ActionBar.LayoutParams) p);
        }
        if (p instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) p);
        }
        return new LayoutParams(p);
    }

    /* Access modifiers changed, original: protected */
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    /* Access modifiers changed, original: protected */
    public boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return super.checkLayoutParams(p) && (p instanceof LayoutParams);
    }

    private static boolean isCustomView(View child) {
        return ((LayoutParams) child.getLayoutParams()).mViewType == 0;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public DecorToolbar getWrapper() {
        if (this.mWrapper == null) {
            this.mWrapper = new ToolbarWidgetWrapper(this, true);
        }
        return this.mWrapper;
    }

    /* Access modifiers changed, original: 0000 */
    public void removeChildrenForExpandedActionView() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (!(((LayoutParams) child.getLayoutParams()).mViewType == 2 || child == this.mMenuView)) {
                removeViewAt(i);
                this.mHiddenViews.add(child);
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void addChildrenForExpandedActionView() {
        for (int i = this.mHiddenViews.size() - 1; i >= 0; i--) {
            addView((View) this.mHiddenViews.get(i));
        }
        this.mHiddenViews.clear();
    }

    private boolean isChildOrHidden(View child) {
        return child.getParent() == this || this.mHiddenViews.contains(child);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public void setCollapsible(boolean collapsible) {
        this.mCollapsible = collapsible;
        requestLayout();
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public void setMenuCallbacks(Callback pcb, MenuBuilder.Callback mcb) {
        this.mActionMenuPresenterCallback = pcb;
        this.mMenuBuilderCallback = mcb;
        if (this.mMenuView != null) {
            this.mMenuView.setMenuCallbacks(pcb, mcb);
        }
    }

    private void ensureContentInsets() {
        if (this.mContentInsets == null) {
            this.mContentInsets = new RtlSpacingHelper();
        }
    }

    /* Access modifiers changed, original: 0000 */
    public ActionMenuPresenter getOuterActionMenuPresenter() {
        return this.mOuterActionMenuPresenter;
    }

    /* Access modifiers changed, original: 0000 */
    public Context getPopupContext() {
        return this.mPopupContext;
    }
}
