package com.hmdglobal.app.camera.data;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import com.hmdglobal.app.camera.data.LocalData.ActionCallback;
import com.hmdglobal.app.camera.filmstrip.DataAdapter.Listener;
import com.hmdglobal.app.camera.filmstrip.DataAdapter.UpdateReporter;
import com.hmdglobal.app.camera.filmstrip.ImageData;

public class FixedLastDataAdapter extends AbstractLocalDataAdapterWrapper {
    private LocalData mLastData;
    private Listener mListener;

    public FixedLastDataAdapter(Context context, LocalDataAdapter wrappedAdapter, LocalData lastData) {
        super(context, wrappedAdapter);
        if (lastData != null) {
            this.mLastData = lastData;
            return;
        }
        throw new AssertionError("data is null");
    }

    public void setListener(Listener listener) {
        super.setListener(listener);
        this.mListener = listener;
    }

    public LocalData getLocalData(int dataID) {
        int totalNumber = this.mAdapter.getTotalNumber();
        if (dataID < totalNumber) {
            return this.mAdapter.getLocalData(dataID);
        }
        if (dataID == totalNumber) {
            return this.mLastData;
        }
        return null;
    }

    public void removeData(int dataID) {
        if (dataID < this.mAdapter.getTotalNumber()) {
            this.mAdapter.removeData(dataID);
        }
    }

    public int findDataByContentUri(Uri uri) {
        return this.mAdapter.findDataByContentUri(uri);
    }

    public void updateData(final int pos, LocalData data) {
        int totalNumber = this.mAdapter.getTotalNumber();
        if (pos < totalNumber) {
            this.mAdapter.updateData(pos, data);
        } else if (pos == totalNumber) {
            this.mLastData = data;
            if (this.mListener != null) {
                this.mListener.onDataUpdated(new UpdateReporter() {
                    public boolean isDataRemoved(int dataID) {
                        return false;
                    }

                    public boolean isDataUpdated(int dataID) {
                        return dataID == pos;
                    }
                });
            }
        }
    }

    public int getTotalNumber() {
        return this.mAdapter.getTotalNumber() + 1;
    }

    public View getView(Context context, View recycled, int dataID, ActionCallback actionCallback) {
        int totalNumber = this.mAdapter.getTotalNumber();
        if (dataID < totalNumber) {
            return this.mAdapter.getView(context, recycled, dataID, actionCallback);
        }
        if (dataID != totalNumber) {
            return null;
        }
        return this.mLastData.getView(context, recycled, this.mSuggestedWidth, this.mSuggestedHeight, 0, null, false, actionCallback);
    }

    public int getItemViewType(int dataId) {
        int totalNumber = this.mAdapter.getTotalNumber();
        if (dataId < totalNumber) {
            return this.mAdapter.getItemViewType(dataId);
        }
        if (dataId == totalNumber) {
            return this.mLastData.getItemViewType().ordinal();
        }
        return -1;
    }

    public void resizeView(Context context, int dataID, View view, int w, int h) {
    }

    public ImageData getImageData(int dataID) {
        int totalNumber = this.mAdapter.getTotalNumber();
        if (dataID < totalNumber) {
            return this.mAdapter.getImageData(dataID);
        }
        if (dataID == totalNumber) {
            return this.mLastData;
        }
        return null;
    }

    public boolean canSwipeInFullScreen(int dataID) {
        int totalNumber = this.mAdapter.getTotalNumber();
        if (dataID < totalNumber) {
            return this.mAdapter.canSwipeInFullScreen(dataID);
        }
        if (dataID == totalNumber) {
            return this.mLastData.canSwipeInFullScreen();
        }
        return false;
    }

    public AsyncTask updateMetadata(int dataId) {
        if (dataId < this.mAdapter.getTotalNumber()) {
            return this.mAdapter.updateMetadata(dataId);
        }
        MetadataLoader.loadMetadata(this.mContext, this.mLastData);
        return null;
    }

    public boolean isMetadataUpdated(int dataId) {
        if (dataId < this.mAdapter.getTotalNumber()) {
            return this.mAdapter.isMetadataUpdated(dataId);
        }
        return MetadataLoader.isMetadataCached(this.mLastData);
    }
}
