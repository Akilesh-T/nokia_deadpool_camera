package com.hmdglobal.app.camera.widget;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Preloader<T, Y> implements OnScrollListener {
    private static final Tag TAG = new Tag("Preloader");
    private Queue<List<Y>> mItemLoadTokens = new LinkedBlockingQueue();
    private ItemLoader<T, Y> mItemLoader;
    private ItemSource<T> mItemSource;
    private int mLastEnd = -1;
    private int mLastStart;
    private int mLastVisibleItem;
    private final int mLoadAheadItems;
    private final int mMaxConcurrentPreloads;
    private boolean mScrollingDown = false;

    public interface ItemLoader<T, Y> {
        void cancelItems(List<Y> list);

        List<Y> preloadItems(List<T> list);
    }

    public interface ItemSource<T> {
        int getCount();

        List<T> getItemsInRange(int i, int i2);
    }

    public Preloader(int loadAheadItems, ItemSource<T> itemSource, ItemLoader<T, Y> itemLoader) {
        this.mItemSource = itemSource;
        this.mItemLoader = itemLoader;
        this.mLoadAheadItems = loadAheadItems;
        this.mMaxConcurrentPreloads = loadAheadItems + 1;
    }

    private void preload(int first, boolean increasing) {
        int start;
        int end;
        if (increasing) {
            start = Math.max(first, this.mLastEnd);
            end = Math.min(this.mLoadAheadItems + first, this.mItemSource.getCount());
        } else {
            start = Math.max(0, first - this.mLoadAheadItems);
            end = Math.min(first, this.mLastStart);
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("preload first=");
        stringBuilder.append(first);
        stringBuilder.append(" increasing=");
        stringBuilder.append(increasing);
        stringBuilder.append(" start=");
        stringBuilder.append(start);
        stringBuilder.append(" end=");
        stringBuilder.append(end);
        Log.v(tag, stringBuilder.toString());
        this.mLastEnd = end;
        this.mLastStart = start;
        if (start != 0 || end != 0) {
            List<T> items = this.mItemSource.getItemsInRange(start, end);
            if (!increasing) {
                Collections.reverse(items);
            }
            registerLoadTokens(this.mItemLoader.preloadItems(items));
        }
    }

    private void registerLoadTokens(List<Y> loadTokens) {
        this.mItemLoadTokens.offer(loadTokens);
        if (this.mItemLoadTokens.size() > this.mMaxConcurrentPreloads) {
            this.mItemLoader.cancelItems((List) this.mItemLoadTokens.poll());
        }
    }

    public void cancelAllLoads() {
        for (List<Y> loadTokens : this.mItemLoadTokens) {
            this.mItemLoader.cancelItems(loadTokens);
        }
        this.mItemLoadTokens.clear();
    }

    public void onScrollStateChanged(AbsListView absListView, int i) {
    }

    public void onScroll(AbsListView absListView, int firstVisible, int visibleItemCount, int totalItemCount) {
        boolean wasScrollingDown = this.mScrollingDown;
        int preloadStart = -1;
        if (firstVisible > this.mLastVisibleItem) {
            this.mScrollingDown = true;
            preloadStart = firstVisible + visibleItemCount;
        } else if (firstVisible < this.mLastVisibleItem) {
            this.mScrollingDown = false;
            preloadStart = firstVisible;
        }
        if (wasScrollingDown != this.mScrollingDown) {
            cancelAllLoads();
        }
        if (preloadStart != -1) {
            preload(preloadStart, this.mScrollingDown);
        }
        this.mLastVisibleItem = firstVisible;
    }
}
