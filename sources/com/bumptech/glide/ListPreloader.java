package com.bumptech.glide;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BaseTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.util.Util;
import java.util.List;
import java.util.Queue;

public abstract class ListPreloader<T> implements OnScrollListener {
    private boolean isIncreasing = true;
    private int lastEnd;
    private int lastFirstVisible;
    private int lastStart;
    private final int maxPreload;
    private final PreloadTargetQueue preloadTargetQueue;
    private int totalItemCount;

    private static final class PreloadTargetQueue {
        private final Queue<PreloadTarget> queue;

        public PreloadTargetQueue(int size) {
            this.queue = Util.createQueue(size);
            for (int i = 0; i < size; i++) {
                this.queue.offer(new PreloadTarget());
            }
        }

        public PreloadTarget next(int width, int height) {
            PreloadTarget result = (PreloadTarget) this.queue.poll();
            this.queue.offer(result);
            result.photoWidth = width;
            result.photoHeight = height;
            return result;
        }
    }

    private static class PreloadTarget extends BaseTarget<Object> {
        private int photoHeight;
        private int photoWidth;

        private PreloadTarget() {
        }

        public void onResourceReady(Object resource, GlideAnimation<? super Object> glideAnimation) {
        }

        public void getSize(SizeReadyCallback cb) {
            cb.onSizeReady(this.photoWidth, this.photoHeight);
        }
    }

    public abstract int[] getDimensions(T t);

    public abstract List<T> getItems(int i, int i2);

    public abstract GenericRequestBuilder getRequestBuilder(T t);

    public ListPreloader(int maxPreload) {
        this.maxPreload = maxPreload;
        this.preloadTargetQueue = new PreloadTargetQueue(maxPreload + 1);
    }

    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
    }

    public void onScroll(AbsListView absListView, int firstVisible, int visibleCount, int totalCount) {
        this.totalItemCount = totalCount;
        if (firstVisible > this.lastFirstVisible) {
            preload(firstVisible + visibleCount, true);
        } else if (firstVisible < this.lastFirstVisible) {
            preload(firstVisible, false);
        }
        this.lastFirstVisible = firstVisible;
    }

    private void preload(int start, boolean increasing) {
        if (this.isIncreasing != increasing) {
            this.isIncreasing = increasing;
            cancelAll();
        }
        preload(start, (increasing ? this.maxPreload : -this.maxPreload) + start);
    }

    private void preload(int from, int to) {
        int start;
        int end;
        if (from < to) {
            start = Math.max(this.lastEnd, from);
            end = to;
        } else {
            start = to;
            end = Math.min(this.lastStart, from);
        }
        end = Math.min(this.totalItemCount, end);
        int i = 0;
        start = Math.min(this.totalItemCount, Math.max(0, start));
        List<T> items = getItems(start, end);
        if (from < to) {
            int numItems = items.size();
            while (i < numItems) {
                preloadItem(items, i);
                i++;
            }
        } else {
            for (i = items.size() - 1; i >= 0; i--) {
                preloadItem(items, i);
            }
        }
        this.lastStart = start;
        this.lastEnd = end;
    }

    private void preloadItem(List<T> items, int position) {
        T item = items.get(position);
        int[] dimensions = getDimensions(item);
        if (dimensions != null) {
            getRequestBuilder(item).into(this.preloadTargetQueue.next(dimensions[0], dimensions[1]));
        }
    }

    private void cancelAll() {
        for (int i = 0; i < this.maxPreload; i++) {
            Glide.clear(this.preloadTargetQueue.next(0, 0));
        }
    }
}
