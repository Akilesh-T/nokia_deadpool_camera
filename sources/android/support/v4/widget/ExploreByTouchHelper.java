package android.support.v4.widget;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewParentCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeProviderCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.support.v4.widget.FocusStrategy.BoundsAdapter;
import android.support.v4.widget.FocusStrategy.CollectionAdapter;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import java.util.ArrayList;
import java.util.List;

public abstract class ExploreByTouchHelper extends AccessibilityDelegateCompat {
    private static final String DEFAULT_CLASS_NAME = "android.view.View";
    public static final int HOST_ID = -1;
    public static final int INVALID_ID = Integer.MIN_VALUE;
    private static final Rect INVALID_PARENT_BOUNDS = new Rect(ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED, ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED, Integer.MIN_VALUE, Integer.MIN_VALUE);
    private static final BoundsAdapter<AccessibilityNodeInfoCompat> NODE_ADAPTER = new BoundsAdapter<AccessibilityNodeInfoCompat>() {
        public void obtainBounds(AccessibilityNodeInfoCompat node, Rect outBounds) {
            node.getBoundsInParent(outBounds);
        }
    };
    private static final CollectionAdapter<SparseArrayCompat<AccessibilityNodeInfoCompat>, AccessibilityNodeInfoCompat> SPARSE_VALUES_ADAPTER = new CollectionAdapter<SparseArrayCompat<AccessibilityNodeInfoCompat>, AccessibilityNodeInfoCompat>() {
        public AccessibilityNodeInfoCompat get(SparseArrayCompat<AccessibilityNodeInfoCompat> collection, int index) {
            return (AccessibilityNodeInfoCompat) collection.valueAt(index);
        }

        public int size(SparseArrayCompat<AccessibilityNodeInfoCompat> collection) {
            return collection.size();
        }
    };
    private int mAccessibilityFocusedVirtualViewId = Integer.MIN_VALUE;
    private final View mHost;
    private int mHoveredVirtualViewId = Integer.MIN_VALUE;
    private int mKeyboardFocusedVirtualViewId = Integer.MIN_VALUE;
    private final AccessibilityManager mManager;
    private MyNodeProvider mNodeProvider;
    private final int[] mTempGlobalRect = new int[2];
    private final Rect mTempParentRect = new Rect();
    private final Rect mTempScreenRect = new Rect();
    private final Rect mTempVisibleRect = new Rect();

    private class MyNodeProvider extends AccessibilityNodeProviderCompat {
        MyNodeProvider() {
        }

        public AccessibilityNodeInfoCompat createAccessibilityNodeInfo(int virtualViewId) {
            return AccessibilityNodeInfoCompat.obtain(ExploreByTouchHelper.this.obtainAccessibilityNodeInfo(virtualViewId));
        }

        public boolean performAction(int virtualViewId, int action, Bundle arguments) {
            return ExploreByTouchHelper.this.performAction(virtualViewId, action, arguments);
        }

        public AccessibilityNodeInfoCompat findFocus(int focusType) {
            int focusedId = focusType == 2 ? ExploreByTouchHelper.this.mAccessibilityFocusedVirtualViewId : ExploreByTouchHelper.this.mKeyboardFocusedVirtualViewId;
            if (focusedId == Integer.MIN_VALUE) {
                return null;
            }
            return createAccessibilityNodeInfo(focusedId);
        }
    }

    public abstract int getVirtualViewAt(float f, float f2);

    public abstract void getVisibleVirtualViews(List<Integer> list);

    public abstract boolean onPerformActionForVirtualView(int i, int i2, @Nullable Bundle bundle);

    public abstract void onPopulateNodeForVirtualView(int i, @NonNull AccessibilityNodeInfoCompat accessibilityNodeInfoCompat);

    public ExploreByTouchHelper(@NonNull View host) {
        if (host != null) {
            this.mHost = host;
            this.mManager = (AccessibilityManager) host.getContext().getSystemService("accessibility");
            host.setFocusable(true);
            if (ViewCompat.getImportantForAccessibility(host) == 0) {
                ViewCompat.setImportantForAccessibility(host, 1);
                return;
            }
            return;
        }
        throw new IllegalArgumentException("View may not be null");
    }

    public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View host) {
        if (this.mNodeProvider == null) {
            this.mNodeProvider = new MyNodeProvider();
        }
        return this.mNodeProvider;
    }

    public final boolean dispatchHoverEvent(@NonNull MotionEvent event) {
        boolean z = false;
        if (!this.mManager.isEnabled() || !this.mManager.isTouchExplorationEnabled()) {
            return false;
        }
        int action = event.getAction();
        if (action != 7) {
            switch (action) {
                case 9:
                    break;
                case 10:
                    if (this.mHoveredVirtualViewId == Integer.MIN_VALUE) {
                        return false;
                    }
                    updateHoveredVirtualView(Integer.MIN_VALUE);
                    return true;
                default:
                    return false;
            }
        }
        action = getVirtualViewAt(event.getX(), event.getY());
        updateHoveredVirtualView(action);
        if (action != Integer.MIN_VALUE) {
            z = true;
        }
        return z;
    }

    public final boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        boolean handled = false;
        if (event.getAction() == 1) {
            return false;
        }
        int keyCode = event.getKeyCode();
        if (keyCode != 61) {
            if (keyCode != 66) {
                switch (keyCode) {
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                        if (!event.hasNoModifiers()) {
                            return false;
                        }
                        int direction = keyToDirection(keyCode);
                        int count = 1 + event.getRepeatCount();
                        for (int i = 0; i < count && moveFocus(direction, null); i++) {
                            handled = true;
                        }
                        return handled;
                    case 23:
                        break;
                    default:
                        return false;
                }
            }
            if (!event.hasNoModifiers() || event.getRepeatCount() != 0) {
                return false;
            }
            clickKeyboardFocusedVirtualView();
            return true;
        } else if (event.hasNoModifiers()) {
            return moveFocus(2, null);
        } else {
            if (event.hasModifiers(1)) {
                return moveFocus(1, null);
            }
            return false;
        }
    }

    public final void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        if (this.mKeyboardFocusedVirtualViewId != Integer.MIN_VALUE) {
            clearKeyboardFocusForVirtualView(this.mKeyboardFocusedVirtualViewId);
        }
        if (gainFocus) {
            moveFocus(direction, previouslyFocusedRect);
        }
    }

    public final int getAccessibilityFocusedVirtualViewId() {
        return this.mAccessibilityFocusedVirtualViewId;
    }

    public final int getKeyboardFocusedVirtualViewId() {
        return this.mKeyboardFocusedVirtualViewId;
    }

    private static int keyToDirection(int keyCode) {
        switch (keyCode) {
            case 19:
                return 33;
            case 21:
                return 17;
            case 22:
                return 66;
            default:
                return Const.CODE_C1_CW2;
        }
    }

    private void getBoundsInParent(int virtualViewId, Rect outBounds) {
        obtainAccessibilityNodeInfo(virtualViewId).getBoundsInParent(outBounds);
    }

    private boolean moveFocus(int direction, @Nullable Rect previouslyFocusedRect) {
        AccessibilityNodeInfoCompat accessibilityNodeInfoCompat;
        AccessibilityNodeInfoCompat nextFocusedNode;
        int nextFocusedNodeId;
        SparseArrayCompat<AccessibilityNodeInfoCompat> allNodes = getAllNodes();
        int focusedNodeId = this.mKeyboardFocusedVirtualViewId;
        if (focusedNodeId == Integer.MIN_VALUE) {
            accessibilityNodeInfoCompat = null;
        } else {
            accessibilityNodeInfoCompat = (AccessibilityNodeInfoCompat) allNodes.get(focusedNodeId);
        }
        AccessibilityNodeInfoCompat focusedNode = accessibilityNodeInfoCompat;
        if (direction == 17 || direction == 33 || direction == 66 || direction == Const.CODE_C1_CW2) {
            Rect selectedRect = new Rect();
            if (this.mKeyboardFocusedVirtualViewId != Integer.MIN_VALUE) {
                getBoundsInParent(this.mKeyboardFocusedVirtualViewId, selectedRect);
            } else if (previouslyFocusedRect != null) {
                selectedRect.set(previouslyFocusedRect);
            } else {
                guessPreviouslyFocusedRect(this.mHost, direction, selectedRect);
            }
            nextFocusedNode = (AccessibilityNodeInfoCompat) FocusStrategy.findNextFocusInAbsoluteDirection(allNodes, SPARSE_VALUES_ADAPTER, NODE_ADAPTER, focusedNode, selectedRect, direction);
        } else {
            switch (direction) {
                case 1:
                case 2:
                    nextFocusedNode = (AccessibilityNodeInfoCompat) FocusStrategy.findNextFocusInRelativeDirection(allNodes, SPARSE_VALUES_ADAPTER, NODE_ADAPTER, focusedNode, direction, ViewCompat.getLayoutDirection(this.mHost) == 1, false);
                    break;
                default:
                    throw new IllegalArgumentException("direction must be one of {FOCUS_FORWARD, FOCUS_BACKWARD, FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
            }
        }
        if (nextFocusedNode == null) {
            nextFocusedNodeId = Integer.MIN_VALUE;
        } else {
            nextFocusedNodeId = allNodes.keyAt(allNodes.indexOfValue(nextFocusedNode));
        }
        return requestKeyboardFocusForVirtualView(nextFocusedNodeId);
    }

    private SparseArrayCompat<AccessibilityNodeInfoCompat> getAllNodes() {
        List<Integer> virtualViewIds = new ArrayList();
        getVisibleVirtualViews(virtualViewIds);
        SparseArrayCompat<AccessibilityNodeInfoCompat> allNodes = new SparseArrayCompat();
        for (int virtualViewId = 0; virtualViewId < virtualViewIds.size(); virtualViewId++) {
            allNodes.put(virtualViewId, createNodeForChild(virtualViewId));
        }
        return allNodes;
    }

    private static Rect guessPreviouslyFocusedRect(@NonNull View host, int direction, @NonNull Rect outBounds) {
        int w = host.getWidth();
        int h = host.getHeight();
        if (direction == 17) {
            outBounds.set(w, 0, w, h);
        } else if (direction == 33) {
            outBounds.set(0, h, w, h);
        } else if (direction == 66) {
            outBounds.set(-1, 0, -1, h);
        } else if (direction == Const.CODE_C1_CW2) {
            outBounds.set(0, -1, w, -1);
        } else {
            throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
        }
        return outBounds;
    }

    private boolean clickKeyboardFocusedVirtualView() {
        return this.mKeyboardFocusedVirtualViewId != Integer.MIN_VALUE && onPerformActionForVirtualView(this.mKeyboardFocusedVirtualViewId, 16, null);
    }

    public final boolean sendEventForVirtualView(int virtualViewId, int eventType) {
        if (virtualViewId == Integer.MIN_VALUE || !this.mManager.isEnabled()) {
            return false;
        }
        ViewParent parent = this.mHost.getParent();
        if (parent == null) {
            return false;
        }
        return ViewParentCompat.requestSendAccessibilityEvent(parent, this.mHost, createEvent(virtualViewId, eventType));
    }

    public final void invalidateRoot() {
        invalidateVirtualView(-1, 1);
    }

    public final void invalidateVirtualView(int virtualViewId) {
        invalidateVirtualView(virtualViewId, 0);
    }

    public final void invalidateVirtualView(int virtualViewId, int changeTypes) {
        if (virtualViewId != Integer.MIN_VALUE && this.mManager.isEnabled()) {
            ViewParent parent = this.mHost.getParent();
            if (parent != null) {
                AccessibilityEvent event = createEvent(virtualViewId, 2048);
                AccessibilityEventCompat.setContentChangeTypes(event, changeTypes);
                ViewParentCompat.requestSendAccessibilityEvent(parent, this.mHost, event);
            }
        }
    }

    @Deprecated
    public int getFocusedVirtualView() {
        return getAccessibilityFocusedVirtualViewId();
    }

    /* Access modifiers changed, original: protected */
    public void onVirtualViewKeyboardFocusChanged(int virtualViewId, boolean hasFocus) {
    }

    private void updateHoveredVirtualView(int virtualViewId) {
        if (this.mHoveredVirtualViewId != virtualViewId) {
            int previousVirtualViewId = this.mHoveredVirtualViewId;
            this.mHoveredVirtualViewId = virtualViewId;
            sendEventForVirtualView(virtualViewId, 128);
            sendEventForVirtualView(previousVirtualViewId, 256);
        }
    }

    private AccessibilityEvent createEvent(int virtualViewId, int eventType) {
        if (virtualViewId != -1) {
            return createEventForChild(virtualViewId, eventType);
        }
        return createEventForHost(eventType);
    }

    private AccessibilityEvent createEventForHost(int eventType) {
        AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
        this.mHost.onInitializeAccessibilityEvent(event);
        return event;
    }

    public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(host, event);
        onPopulateEventForHost(event);
    }

    private AccessibilityEvent createEventForChild(int virtualViewId, int eventType) {
        AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
        AccessibilityNodeInfoCompat node = obtainAccessibilityNodeInfo(virtualViewId);
        event.getText().add(node.getText());
        event.setContentDescription(node.getContentDescription());
        event.setScrollable(node.isScrollable());
        event.setPassword(node.isPassword());
        event.setEnabled(node.isEnabled());
        event.setChecked(node.isChecked());
        onPopulateEventForVirtualView(virtualViewId, event);
        if (event.getText().isEmpty() && event.getContentDescription() == null) {
            throw new RuntimeException("Callbacks must add text or a content description in populateEventForVirtualViewId()");
        }
        event.setClassName(node.getClassName());
        AccessibilityRecordCompat.setSource(event, this.mHost, virtualViewId);
        event.setPackageName(this.mHost.getContext().getPackageName());
        return event;
    }

    /* Access modifiers changed, original: 0000 */
    @NonNull
    public AccessibilityNodeInfoCompat obtainAccessibilityNodeInfo(int virtualViewId) {
        if (virtualViewId == -1) {
            return createNodeForHost();
        }
        return createNodeForChild(virtualViewId);
    }

    @NonNull
    private AccessibilityNodeInfoCompat createNodeForHost() {
        AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain(this.mHost);
        ViewCompat.onInitializeAccessibilityNodeInfo(this.mHost, info);
        ArrayList<Integer> virtualViewIds = new ArrayList();
        getVisibleVirtualViews(virtualViewIds);
        if (info.getChildCount() <= 0 || virtualViewIds.size() <= 0) {
            int count = virtualViewIds.size();
            for (int i = 0; i < count; i++) {
                info.addChild(this.mHost, ((Integer) virtualViewIds.get(i)).intValue());
            }
            return info;
        }
        throw new RuntimeException("Views cannot have both real and virtual children");
    }

    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
        super.onInitializeAccessibilityNodeInfo(host, info);
        onPopulateNodeForHost(info);
    }

    @NonNull
    private AccessibilityNodeInfoCompat createNodeForChild(int virtualViewId) {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        node.setEnabled(true);
        node.setFocusable(true);
        node.setClassName(DEFAULT_CLASS_NAME);
        node.setBoundsInParent(INVALID_PARENT_BOUNDS);
        node.setBoundsInScreen(INVALID_PARENT_BOUNDS);
        node.setParent(this.mHost);
        onPopulateNodeForVirtualView(virtualViewId, node);
        if (node.getText() == null && node.getContentDescription() == null) {
            throw new RuntimeException("Callbacks must add text or a content description in populateNodeForVirtualViewId()");
        }
        node.getBoundsInParent(this.mTempParentRect);
        if (this.mTempParentRect.equals(INVALID_PARENT_BOUNDS)) {
            throw new RuntimeException("Callbacks must set parent bounds in populateNodeForVirtualViewId()");
        }
        int actions = node.getActions();
        if ((actions & 64) != 0) {
            throw new RuntimeException("Callbacks must not add ACTION_ACCESSIBILITY_FOCUS in populateNodeForVirtualViewId()");
        } else if ((actions & 128) == 0) {
            node.setPackageName(this.mHost.getContext().getPackageName());
            node.setSource(this.mHost, virtualViewId);
            if (this.mAccessibilityFocusedVirtualViewId == virtualViewId) {
                node.setAccessibilityFocused(true);
                node.addAction(128);
            } else {
                node.setAccessibilityFocused(false);
                node.addAction(64);
            }
            boolean isFocused = this.mKeyboardFocusedVirtualViewId == virtualViewId;
            if (isFocused) {
                node.addAction(2);
            } else if (node.isFocusable()) {
                node.addAction(1);
            }
            node.setFocused(isFocused);
            this.mHost.getLocationOnScreen(this.mTempGlobalRect);
            node.getBoundsInScreen(this.mTempScreenRect);
            if (this.mTempScreenRect.equals(INVALID_PARENT_BOUNDS)) {
                node.getBoundsInParent(this.mTempScreenRect);
                if (node.mParentVirtualDescendantId != -1) {
                    AccessibilityNodeInfoCompat parentNode = AccessibilityNodeInfoCompat.obtain();
                    for (int virtualDescendantId = node.mParentVirtualDescendantId; virtualDescendantId != -1; virtualDescendantId = parentNode.mParentVirtualDescendantId) {
                        parentNode.setParent(this.mHost, -1);
                        parentNode.setBoundsInParent(INVALID_PARENT_BOUNDS);
                        onPopulateNodeForVirtualView(virtualDescendantId, parentNode);
                        parentNode.getBoundsInParent(this.mTempParentRect);
                        this.mTempScreenRect.offset(this.mTempParentRect.left, this.mTempParentRect.top);
                    }
                    parentNode.recycle();
                }
                this.mTempScreenRect.offset(this.mTempGlobalRect[0] - this.mHost.getScrollX(), this.mTempGlobalRect[1] - this.mHost.getScrollY());
            }
            if (this.mHost.getLocalVisibleRect(this.mTempVisibleRect)) {
                this.mTempVisibleRect.offset(this.mTempGlobalRect[0] - this.mHost.getScrollX(), this.mTempGlobalRect[1] - this.mHost.getScrollY());
                if (this.mTempScreenRect.intersect(this.mTempVisibleRect)) {
                    node.setBoundsInScreen(this.mTempScreenRect);
                    if (isVisibleToUser(this.mTempScreenRect)) {
                        node.setVisibleToUser(true);
                    }
                }
            }
            return node;
        } else {
            throw new RuntimeException("Callbacks must not add ACTION_CLEAR_ACCESSIBILITY_FOCUS in populateNodeForVirtualViewId()");
        }
    }

    /* Access modifiers changed, original: 0000 */
    public boolean performAction(int virtualViewId, int action, Bundle arguments) {
        if (virtualViewId != -1) {
            return performActionForChild(virtualViewId, action, arguments);
        }
        return performActionForHost(action, arguments);
    }

    private boolean performActionForHost(int action, Bundle arguments) {
        return ViewCompat.performAccessibilityAction(this.mHost, action, arguments);
    }

    private boolean performActionForChild(int virtualViewId, int action, Bundle arguments) {
        if (action == 64) {
            return requestAccessibilityFocus(virtualViewId);
        }
        if (action == 128) {
            return clearAccessibilityFocus(virtualViewId);
        }
        switch (action) {
            case 1:
                return requestKeyboardFocusForVirtualView(virtualViewId);
            case 2:
                return clearKeyboardFocusForVirtualView(virtualViewId);
            default:
                return onPerformActionForVirtualView(virtualViewId, action, arguments);
        }
    }

    /* JADX WARNING: Missing block: B:19:0x003b, code skipped:
            return false;
     */
    private boolean isVisibleToUser(android.graphics.Rect r6) {
        /*
        r5 = this;
        r0 = 0;
        if (r6 == 0) goto L_0x003b;
    L_0x0003:
        r1 = r6.isEmpty();
        if (r1 == 0) goto L_0x000a;
    L_0x0009:
        goto L_0x003b;
    L_0x000a:
        r1 = r5.mHost;
        r1 = r1.getWindowVisibility();
        if (r1 == 0) goto L_0x0013;
    L_0x0012:
        return r0;
    L_0x0013:
        r1 = r5.mHost;
        r1 = r1.getParent();
    L_0x0019:
        r2 = r1 instanceof android.view.View;
        if (r2 == 0) goto L_0x0036;
    L_0x001d:
        r2 = r1;
        r2 = (android.view.View) r2;
        r3 = r2.getAlpha();
        r4 = 0;
        r3 = (r3 > r4 ? 1 : (r3 == r4 ? 0 : -1));
        if (r3 <= 0) goto L_0x0035;
    L_0x0029:
        r3 = r2.getVisibility();
        if (r3 == 0) goto L_0x0030;
    L_0x002f:
        goto L_0x0035;
    L_0x0030:
        r1 = r2.getParent();
        goto L_0x0019;
    L_0x0035:
        return r0;
    L_0x0036:
        if (r1 == 0) goto L_0x003a;
    L_0x0038:
        r0 = 1;
    L_0x003a:
        return r0;
    L_0x003b:
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.widget.ExploreByTouchHelper.isVisibleToUser(android.graphics.Rect):boolean");
    }

    /* JADX WARNING: Missing block: B:12:0x0031, code skipped:
            return false;
     */
    private boolean requestAccessibilityFocus(int r3) {
        /*
        r2 = this;
        r0 = r2.mManager;
        r0 = r0.isEnabled();
        r1 = 0;
        if (r0 == 0) goto L_0x0031;
    L_0x0009:
        r0 = r2.mManager;
        r0 = r0.isTouchExplorationEnabled();
        if (r0 != 0) goto L_0x0012;
    L_0x0011:
        goto L_0x0031;
    L_0x0012:
        r0 = r2.mAccessibilityFocusedVirtualViewId;
        if (r0 == r3) goto L_0x0030;
    L_0x0016:
        r0 = r2.mAccessibilityFocusedVirtualViewId;
        r1 = -2147483648; // 0xffffffff80000000 float:-0.0 double:NaN;
        if (r0 == r1) goto L_0x0021;
    L_0x001c:
        r0 = r2.mAccessibilityFocusedVirtualViewId;
        r2.clearAccessibilityFocus(r0);
    L_0x0021:
        r2.mAccessibilityFocusedVirtualViewId = r3;
        r0 = r2.mHost;
        r0.invalidate();
        r0 = 32768; // 0x8000 float:4.5918E-41 double:1.61895E-319;
        r2.sendEventForVirtualView(r3, r0);
        r0 = 1;
        return r0;
    L_0x0030:
        return r1;
    L_0x0031:
        return r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.widget.ExploreByTouchHelper.requestAccessibilityFocus(int):boolean");
    }

    private boolean clearAccessibilityFocus(int virtualViewId) {
        if (this.mAccessibilityFocusedVirtualViewId != virtualViewId) {
            return false;
        }
        this.mAccessibilityFocusedVirtualViewId = Integer.MIN_VALUE;
        this.mHost.invalidate();
        sendEventForVirtualView(virtualViewId, 65536);
        return true;
    }

    public final boolean requestKeyboardFocusForVirtualView(int virtualViewId) {
        if ((!this.mHost.isFocused() && !this.mHost.requestFocus()) || this.mKeyboardFocusedVirtualViewId == virtualViewId) {
            return false;
        }
        if (this.mKeyboardFocusedVirtualViewId != Integer.MIN_VALUE) {
            clearKeyboardFocusForVirtualView(this.mKeyboardFocusedVirtualViewId);
        }
        this.mKeyboardFocusedVirtualViewId = virtualViewId;
        onVirtualViewKeyboardFocusChanged(virtualViewId, true);
        sendEventForVirtualView(virtualViewId, 8);
        return true;
    }

    public final boolean clearKeyboardFocusForVirtualView(int virtualViewId) {
        if (this.mKeyboardFocusedVirtualViewId != virtualViewId) {
            return false;
        }
        this.mKeyboardFocusedVirtualViewId = Integer.MIN_VALUE;
        onVirtualViewKeyboardFocusChanged(virtualViewId, false);
        sendEventForVirtualView(virtualViewId, 8);
        return true;
    }

    /* Access modifiers changed, original: protected */
    public void onPopulateEventForVirtualView(int virtualViewId, @NonNull AccessibilityEvent event) {
    }

    /* Access modifiers changed, original: protected */
    public void onPopulateEventForHost(@NonNull AccessibilityEvent event) {
    }

    /* Access modifiers changed, original: protected */
    public void onPopulateNodeForHost(@NonNull AccessibilityNodeInfoCompat node) {
    }
}
