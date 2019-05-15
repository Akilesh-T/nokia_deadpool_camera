package com.hmdglobal.app.camera.data;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import com.hmdglobal.app.camera.data.LocalData.ActionCallback;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.filmstrip.DataAdapter.Listener;
import com.hmdglobal.app.camera.filmstrip.DataAdapter.UpdateReporter;
import com.hmdglobal.app.camera.filmstrip.ImageData;

public class FixedFirstDataAdapter extends AbstractLocalDataAdapterWrapper implements Listener {
    private static final Tag TAG = new Tag("FixedFirstDataAdpt");
    private LocalData mFirstData;
    private Listener mListener;

    public FixedFirstDataAdapter(Context context, LocalDataAdapter wrappedAdapter, LocalData firstData) {
        super(context, wrappedAdapter);
        if (firstData != null) {
            this.mFirstData = firstData;
            return;
        }
        throw new AssertionError("data is null");
    }

    public LocalData getLocalData(int dataID) {
        if (dataID == 0) {
            return this.mFirstData;
        }
        return this.mAdapter.getLocalData(dataID - 1);
    }

    public void removeData(int dataID) {
        if (dataID > 0) {
            this.mAdapter.removeData(dataID - 1);
        }
    }

    public int findDataByContentUri(Uri uri) {
        int pos = this.mAdapter.findDataByContentUri(uri);
        if (pos != -1) {
            return pos + 1;
        }
        return -1;
    }

    public void updateData(int pos, LocalData data) {
        if (pos == 0) {
            this.mFirstData = data;
            if (this.mListener != null) {
                this.mListener.onDataUpdated(new UpdateReporter() {
                    public boolean isDataRemoved(int dataID) {
                        return false;
                    }

                    public boolean isDataUpdated(int dataID) {
                        return dataID == 0;
                    }
                });
                return;
            }
            return;
        }
        this.mAdapter.updateData(pos - 1, data);
    }

    public int getTotalNumber() {
        return this.mAdapter.getTotalNumber() + 1;
    }

    public View getView(Context context, View recycled, int dataID, ActionCallback actionCallback) {
        if (dataID != 0) {
            return this.mAdapter.getView(context, recycled, dataID - 1, actionCallback);
        }
        return this.mFirstData.getView(context, recycled, this.mSuggestedWidth, this.mSuggestedHeight, 0, null, false, actionCallback);
    }

    public int getItemViewType(int dataId) {
        if (dataId == 0) {
            return this.mFirstData.getItemViewType().ordinal();
        }
        return this.mAdapter.getItemViewType(dataId);
    }

    public void resizeView(Context context, int dataID, View view, int w, int h) {
    }

    public ImageData getImageData(int dataID) {
        if (dataID == 0) {
            return this.mFirstData;
        }
        return this.mAdapter.getImageData(dataID - 1);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
        this.mAdapter.setListener(listener == null ? null : this);
        if (this.mListener != null) {
            this.mListener.onDataLoaded();
        }
    }

    public boolean canSwipeInFullScreen(int dataID) {
        if (dataID == 0) {
            return this.mFirstData.canSwipeInFullScreen();
        }
        return this.mAdapter.canSwipeInFullScreen(dataID - 1);
    }

    public void onDataLoaded() {
        if (this.mListener != null) {
            this.mListener.onDataUpdated(new UpdateReporter() {
                public boolean isDataRemoved(int dataID) {
                    return false;
                }

                public boolean isDataUpdated(int dataID) {
                    return dataID != 0;
                }
            });
        }
    }

    public void onDataUpdated(final UpdateReporter reporter) {
        this.mListener.onDataUpdated(new UpdateReporter() {
            public boolean isDataRemoved(int dataID) {
                return dataID != 0 && reporter.isDataRemoved(dataID - 1);
            }

            public boolean isDataUpdated(int dataID) {
                return dataID != 0 && reporter.isDataUpdated(dataID - 1);
            }
        });
    }

    public void onDataInserted(int dataID, ImageData data) {
        this.mListener.onDataInserted(dataID + 1, data);
    }

    public void onDataRemoved(int dataID, ImageData data) {
        this.mListener.onDataRemoved(dataID + 1, data);
    }

    public AsyncTask updateMetadata(int dataId) {
        if (dataId > 0) {
            return this.mAdapter.updateMetadata(dataId - 1);
        }
        MetadataLoader.loadMetadata(this.mContext, this.mFirstData);
        return null;
    }

    public boolean isMetadataUpdated(int dataId) {
        if (dataId > 0) {
            return this.mAdapter.isMetadataUpdated(dataId - 1);
        }
        return this.mFirstData.isMetadataUpdated();
    }
}
