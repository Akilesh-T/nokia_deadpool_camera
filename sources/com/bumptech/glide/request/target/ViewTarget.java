package com.bumptech.glide.request.target;

import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.WindowManager;
import com.bumptech.glide.request.Request;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public abstract class ViewTarget<T extends View, Z> extends BaseTarget<Z> {
    private static final String TAG = "ViewTarget";
    private final SizeDeterminer sizeDeterminer;
    protected final T view;

    private static class SizeDeterminer {
        private final List<SizeReadyCallback> cbs = new ArrayList();
        private SizeDeterminerLayoutListener layoutListener;
        private final View view;

        private static class SizeDeterminerLayoutListener implements OnPreDrawListener {
            private final WeakReference<SizeDeterminer> sizeDeterminerRef;

            public SizeDeterminerLayoutListener(SizeDeterminer sizeDeterminer) {
                this.sizeDeterminerRef = new WeakReference(sizeDeterminer);
            }

            public boolean onPreDraw() {
                if (Log.isLoggable(ViewTarget.TAG, 2)) {
                    String str = ViewTarget.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("OnGlobalLayoutListener called listener=");
                    stringBuilder.append(this);
                    Log.v(str, stringBuilder.toString());
                }
                SizeDeterminer sizeDeterminer = (SizeDeterminer) this.sizeDeterminerRef.get();
                if (sizeDeterminer != null) {
                    sizeDeterminer.checkCurrentDimens();
                }
                return true;
            }
        }

        public SizeDeterminer(View view) {
            this.view = view;
        }

        private void notifyCbs(int width, int height) {
            for (SizeReadyCallback cb : this.cbs) {
                cb.onSizeReady(width, height);
            }
            this.cbs.clear();
        }

        private void checkCurrentDimens() {
            if (!this.cbs.isEmpty()) {
                boolean calledCallback = true;
                LayoutParams layoutParams = this.view.getLayoutParams();
                if (isViewSizeValid()) {
                    notifyCbs(this.view.getWidth(), this.view.getHeight());
                } else if (isLayoutParamsSizeValid()) {
                    notifyCbs(layoutParams.width, layoutParams.height);
                } else {
                    calledCallback = false;
                }
                if (calledCallback) {
                    ViewTreeObserver observer = this.view.getViewTreeObserver();
                    if (observer.isAlive()) {
                        observer.removeOnPreDrawListener(this.layoutListener);
                    }
                    this.layoutListener = null;
                }
            }
        }

        public void getSize(SizeReadyCallback cb) {
            LayoutParams layoutParams = this.view.getLayoutParams();
            if (isViewSizeValid()) {
                cb.onSizeReady(this.view.getWidth(), this.view.getHeight());
            } else if (isLayoutParamsSizeValid()) {
                cb.onSizeReady(layoutParams.width, layoutParams.height);
            } else if (isUsingWrapContent()) {
                Display display = ((WindowManager) this.view.getContext().getSystemService("window")).getDefaultDisplay();
                int width = display.getWidth();
                int height = display.getHeight();
                if (Log.isLoggable(ViewTarget.TAG, 5)) {
                    String str = ViewTarget.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Trying to load image into ImageView using WRAP_CONTENT, defaulting to screen dimensions: [");
                    stringBuilder.append(width);
                    stringBuilder.append("x");
                    stringBuilder.append(height);
                    stringBuilder.append("]. Give the view an actual width and height  for better performance.");
                    Log.w(str, stringBuilder.toString());
                }
                cb.onSizeReady(width, height);
            } else {
                if (!this.cbs.contains(cb)) {
                    this.cbs.add(cb);
                }
                if (this.layoutListener == null) {
                    ViewTreeObserver observer = this.view.getViewTreeObserver();
                    this.layoutListener = new SizeDeterminerLayoutListener(this);
                    observer.addOnPreDrawListener(this.layoutListener);
                }
            }
        }

        private boolean isViewSizeValid() {
            return this.view.getWidth() > 0 && this.view.getHeight() > 0;
        }

        private boolean isUsingWrapContent() {
            LayoutParams layoutParams = this.view.getLayoutParams();
            return layoutParams != null && (layoutParams.width == -2 || layoutParams.height == -2);
        }

        private boolean isLayoutParamsSizeValid() {
            LayoutParams layoutParams = this.view.getLayoutParams();
            return layoutParams != null && layoutParams.width > 0 && layoutParams.height > 0;
        }
    }

    public ViewTarget(T view) {
        if (view != null) {
            this.view = view;
            this.sizeDeterminer = new SizeDeterminer(view);
            return;
        }
        throw new NullPointerException("View must not be null!");
    }

    public T getView() {
        return this.view;
    }

    public void getSize(SizeReadyCallback cb) {
        this.sizeDeterminer.getSize(cb);
    }

    public void setRequest(Request request) {
        this.view.setTag(request);
    }

    public Request getRequest() {
        Request tag = this.view.getTag();
        if (tag == null) {
            return null;
        }
        if (tag instanceof Request) {
            return tag;
        }
        throw new IllegalArgumentException("You must not call setTag() on a view Glide is targeting");
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Target for: ");
        stringBuilder.append(this.view);
        return stringBuilder.toString();
    }
}
