package com.hmdglobal.app.camera.data;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import com.hmdglobal.app.camera.data.LocalDataAdapter.LocalDataListener;
import com.hmdglobal.app.camera.filmstrip.DataAdapter.Listener;
import com.hmdglobal.app.camera.util.Callback;
import java.util.List;

public abstract class AbstractLocalDataAdapterWrapper implements LocalDataAdapter {
    protected final LocalDataAdapter mAdapter;
    protected final Context mContext;
    protected int mSuggestedHeight;
    protected int mSuggestedWidth;

    AbstractLocalDataAdapterWrapper(Context context, LocalDataAdapter wrappedAdapter) {
        if (wrappedAdapter != null) {
            this.mContext = context;
            this.mAdapter = wrappedAdapter;
            return;
        }
        throw new AssertionError("data adapter is null");
    }

    public void suggestViewSizeBound(int w, int h) {
        this.mSuggestedWidth = w;
        this.mSuggestedHeight = h;
        this.mAdapter.suggestViewSizeBound(w, h);
    }

    public void setListener(Listener listener) {
        this.mAdapter.setListener(listener);
    }

    public void setLocalDataListener(LocalDataListener listener) {
        this.mAdapter.setLocalDataListener(listener);
    }

    public void requestLoad(Callback<Void> doneCallback) {
        this.mAdapter.requestLoad(doneCallback);
    }

    public void requestLoadNewPhotos() {
        this.mAdapter.requestLoadNewPhotos();
    }

    public boolean addData(LocalData data) {
        return this.mAdapter.addData(data);
    }

    public void flush() {
        this.mAdapter.flush();
    }

    public boolean executeDeletion() {
        return this.mAdapter.executeDeletion();
    }

    public boolean undoDataRemoval() {
        return this.mAdapter.undoDataRemoval();
    }

    public void refresh(Uri uri) {
        this.mAdapter.refresh(uri);
    }

    public AsyncTask updateMetadata(int dataId) {
        return this.mAdapter.updateMetadata(dataId);
    }

    public boolean isMetadataUpdated(int dataId) {
        return this.mAdapter.isMetadataUpdated(dataId);
    }

    public List<AsyncTask> preloadItems(List<Integer> items) {
        return this.mAdapter.preloadItems(items);
    }

    public void cancelItems(List<AsyncTask> loadTokens) {
        this.mAdapter.cancelItems(loadTokens);
    }

    public List<Integer> getItemsInRange(int startPosition, int endPosition) {
        return this.mAdapter.getItemsInRange(startPosition, endPosition);
    }

    public int getCount() {
        return this.mAdapter.getCount();
    }
}
