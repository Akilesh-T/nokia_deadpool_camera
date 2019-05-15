package com.hmdglobal.app.camera.data;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import com.hmdglobal.app.camera.Storage;
import com.hmdglobal.app.camera.data.LocalData.ActionCallback;
import com.hmdglobal.app.camera.data.LocalData.NewestFirstComparator;
import com.hmdglobal.app.camera.data.LocalDataAdapter.LocalDataListener;
import com.hmdglobal.app.camera.data.LocalMediaData.PhotoData;
import com.hmdglobal.app.camera.data.LocalMediaData.VideoData;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.filmstrip.DataAdapter.Listener;
import com.hmdglobal.app.camera.filmstrip.DataAdapter.UpdateReporter;
import com.hmdglobal.app.camera.filmstrip.ImageData;
import com.hmdglobal.app.camera.util.Callback;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CameraDataAdapter implements LocalDataAdapter {
    private static final int DEFAULT_DECODE_SIZE = 1600;
    private static final Tag TAG = new Tag("CameraDataAdapter");
    private final Context mContext;
    private LocalDataList mImages;
    private long mLastPhotoId = -1;
    private Listener mListener;
    private LocalDataListener mLocalDataListener;
    private LocalData mLocalDataToDelete;
    private final int mPlaceHolderResourceId;
    private int mSuggestedHeight = 1600;
    private int mSuggestedWidth = 1600;

    private class DeletionTask extends AsyncTask<LocalData, Void, Void> {
        private DeletionTask() {
        }

        /* synthetic */ DeletionTask(CameraDataAdapter x0, AnonymousClass1 x1) {
            this();
        }

        /* Access modifiers changed, original: protected|varargs */
        public Void doInBackground(LocalData... data) {
            for (int i = 0; i < data.length; i++) {
                if (data[i].isDataActionSupported(2)) {
                    data[i].delete(CameraDataAdapter.this.mContext);
                } else {
                    Tag access$200 = CameraDataAdapter.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Deletion is not supported:");
                    stringBuilder.append(data[i]);
                    Log.v(access$200, stringBuilder.toString());
                }
            }
            return null;
        }
    }

    private class LoadNewPhotosTask extends AsyncTask<ContentResolver, Void, List<LocalData>> {
        private final long mMinPhotoId;

        public LoadNewPhotosTask(long lastPhotoId) {
            this.mMinPhotoId = lastPhotoId;
        }

        /* Access modifiers changed, original: protected|varargs */
        public List<LocalData> doInBackground(ContentResolver... contentResolvers) {
            if (this.mMinPhotoId == -1) {
                return new ArrayList(0);
            }
            Tag access$200 = CameraDataAdapter.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updating media metadata with photos newer than id: ");
            stringBuilder.append(this.mMinPhotoId);
            Log.v(access$200, stringBuilder.toString());
            return PhotoData.query(contentResolvers[0], PhotoData.CONTENT_URI, this.mMinPhotoId);
        }

        /* Access modifiers changed, original: protected */
        public void onPostExecute(List<LocalData> newPhotoData) {
            if (newPhotoData == null) {
                Log.w(CameraDataAdapter.TAG, "null data returned from new photos query");
                return;
            }
            Tag access$200 = CameraDataAdapter.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("new photos query return num items: ");
            stringBuilder.append(newPhotoData.size());
            Log.v(access$200, stringBuilder.toString());
            if (!newPhotoData.isEmpty()) {
                long newLastPhotoId = ((LocalData) newPhotoData.get(0)).getContentId();
                Tag access$2002 = CameraDataAdapter.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updating last photo id (old:new) ");
                stringBuilder2.append(CameraDataAdapter.this.mLastPhotoId);
                stringBuilder2.append(":");
                stringBuilder2.append(newLastPhotoId);
                Log.v(access$2002, stringBuilder2.toString());
                CameraDataAdapter.this.mLastPhotoId = Math.max(CameraDataAdapter.this.mLastPhotoId, newLastPhotoId);
            }
            for (LocalData localData : newPhotoData) {
                if (Storage.getSessionUriFromContentUri(localData.getUri()) == null) {
                    CameraDataAdapter.this.addData(localData);
                }
            }
        }
    }

    private class MetadataUpdateTask extends AsyncTask<Integer, Void, List<Integer>> {
        private MetadataUpdateTask() {
        }

        /* synthetic */ MetadataUpdateTask(CameraDataAdapter x0, AnonymousClass1 x1) {
            this();
        }

        /* Access modifiers changed, original: protected|varargs */
        public List<Integer> doInBackground(Integer... dataId) {
            List<Integer> updatedList = new ArrayList();
            for (Integer id : dataId) {
                if (id.intValue() >= 0 && id.intValue() < CameraDataAdapter.this.mImages.size()) {
                    if (MetadataLoader.loadMetadata(CameraDataAdapter.this.mContext, CameraDataAdapter.this.mImages.get(id.intValue()))) {
                        updatedList.add(id);
                    }
                }
            }
            return updatedList;
        }

        /* Access modifiers changed, original: protected */
        public void onPostExecute(final List<Integer> updatedData) {
            if (CameraDataAdapter.this.mListener != null) {
                CameraDataAdapter.this.mListener.onDataUpdated(new UpdateReporter() {
                    public boolean isDataRemoved(int dataID) {
                        return false;
                    }

                    public boolean isDataUpdated(int dataID) {
                        return updatedData.contains(Integer.valueOf(dataID));
                    }
                });
            }
            if (CameraDataAdapter.this.mLocalDataListener != null) {
                CameraDataAdapter.this.mLocalDataListener.onMetadataUpdated(updatedData);
            }
        }
    }

    private class QueryTask extends AsyncTask<Context, Void, QueryTaskResult> {
        private static final int MAX_METADATA = 5;
        private final Callback<Void> mDoneCallback;

        public QueryTask(Callback<Void> doneCallback) {
            this.mDoneCallback = doneCallback;
        }

        /* Access modifiers changed, original: protected|varargs */
        public QueryTaskResult doInBackground(Context... contexts) {
            Tag access$200;
            StringBuilder stringBuilder;
            int i = 0;
            Context context = contexts[0];
            ContentResolver cr = context.getContentResolver();
            LocalDataList l = new LocalDataList();
            List<LocalData> photoData = PhotoData.query(cr, PhotoData.CONTENT_URI, -1);
            List<LocalData> videoData = VideoData.query(cr, VideoData.CONTENT_URI, -1);
            long lastPhotoId = -1;
            if (!photoData.isEmpty()) {
                lastPhotoId = ((LocalData) photoData.get(0)).getContentId();
            }
            if (photoData != null) {
                access$200 = CameraDataAdapter.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("retrieved photo metadata, number of items: ");
                stringBuilder.append(photoData.size());
                Log.v(access$200, stringBuilder.toString());
                l.addAll(photoData);
            }
            if (videoData != null) {
                access$200 = CameraDataAdapter.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("retrieved video metadata, number of items: ");
                stringBuilder.append(videoData.size());
                Log.v(access$200, stringBuilder.toString());
                l.addAll(videoData);
            }
            Log.v(CameraDataAdapter.TAG, "sorting video/photo metadata");
            l.sort(new NewestFirstComparator());
            Log.v(CameraDataAdapter.TAG, "sorted video/photo metadata");
            while (i < 5 && i < l.size()) {
                MetadataLoader.loadMetadata(context, l.get(i));
                i++;
            }
            return new QueryTaskResult(l, lastPhotoId);
        }

        /* Access modifiers changed, original: protected */
        public void onPostExecute(QueryTaskResult result) {
            CameraDataAdapter.this.mLastPhotoId = result.mLastPhotoId;
            CameraDataAdapter.this.replaceData(result.mLocalDataList);
            if (this.mDoneCallback != null) {
                this.mDoneCallback.onCallback(null);
            }
            new LoadNewPhotosTask(CameraDataAdapter.this.mLastPhotoId).execute(new ContentResolver[]{CameraDataAdapter.this.mContext.getContentResolver()});
        }
    }

    private class QueryTaskResult {
        public long mLastPhotoId;
        public LocalDataList mLocalDataList;

        public QueryTaskResult(LocalDataList localDataList, long lastPhotoId) {
            this.mLocalDataList = localDataList;
            this.mLastPhotoId = lastPhotoId;
        }
    }

    public CameraDataAdapter(Context context, int placeholderResource) {
        this.mContext = context;
        this.mImages = new LocalDataList();
        this.mPlaceHolderResourceId = placeholderResource;
    }

    public void setLocalDataListener(LocalDataListener listener) {
        this.mLocalDataListener = listener;
    }

    public void requestLoadNewPhotos() {
        new LoadNewPhotosTask(this.mLastPhotoId).execute(new ContentResolver[]{this.mContext.getContentResolver()});
    }

    public void requestLoad(Callback<Void> doneCallback) {
        new QueryTask(doneCallback).execute(new Context[]{this.mContext});
    }

    public AsyncTask updateMetadata(int dataId) {
        MetadataUpdateTask result = new MetadataUpdateTask(this, null);
        result.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Integer[]{Integer.valueOf(dataId)});
        return result;
    }

    public boolean isMetadataUpdated(int dataId) {
        if (dataId < 0 || dataId >= this.mImages.size()) {
            return true;
        }
        return this.mImages.get(dataId).isMetadataUpdated();
    }

    public int getItemViewType(int dataId) {
        if (dataId < 0 || dataId >= this.mImages.size()) {
            return -1;
        }
        return this.mImages.get(dataId).getItemViewType().ordinal();
    }

    public LocalData getLocalData(int dataID) {
        if (dataID < 0 || dataID >= this.mImages.size()) {
            return null;
        }
        return this.mImages.get(dataID);
    }

    public int getTotalNumber() {
        return this.mImages.size();
    }

    public ImageData getImageData(int id) {
        return getLocalData(id);
    }

    public void suggestViewSizeBound(int w, int h) {
        this.mSuggestedWidth = w;
        this.mSuggestedHeight = h;
    }

    public View getView(Context context, View recycled, int dataID, ActionCallback actionCallback) {
        if (dataID >= this.mImages.size() || dataID < 0) {
            return null;
        }
        return this.mImages.get(dataID).getView(context, recycled, this.mSuggestedWidth, this.mSuggestedHeight, this.mPlaceHolderResourceId, this, false, actionCallback);
    }

    public void resizeView(Context context, int dataID, View view, int w, int h) {
        if (dataID < this.mImages.size() && dataID >= 0) {
            this.mImages.get(dataID).loadFullImage(context, this.mSuggestedWidth, this.mSuggestedHeight, view, this);
        }
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
        if (this.mImages.size() != 0) {
            this.mListener.onDataLoaded();
        }
    }

    public boolean canSwipeInFullScreen(int dataID) {
        if (dataID >= this.mImages.size() || dataID <= 0) {
            return true;
        }
        return this.mImages.get(dataID).canSwipeInFullScreen();
    }

    public void removeData(int dataID) {
        LocalData d = this.mImages.remove(dataID);
        if (d != null) {
            executeDeletion();
            this.mLocalDataToDelete = d;
            this.mListener.onDataRemoved(dataID, d);
        }
    }

    public boolean addData(LocalData newData) {
        Uri uri = newData.getUri();
        int pos = findDataByContentUri(uri);
        if (pos != -1) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("found duplicate data: ");
            stringBuilder.append(uri);
            Log.v(tag, stringBuilder.toString());
            updateData(pos, newData);
            return false;
        }
        insertData(newData);
        return true;
    }

    public int findDataByContentUri(Uri uri) {
        return this.mImages.indexOf(uri);
    }

    public boolean undoDataRemoval() {
        if (this.mLocalDataToDelete == null) {
            return false;
        }
        LocalData d = this.mLocalDataToDelete;
        this.mLocalDataToDelete = null;
        insertData(d);
        return true;
    }

    public boolean executeDeletion() {
        if (this.mLocalDataToDelete == null) {
            return false;
        }
        new DeletionTask(this, null).execute(new LocalData[]{this.mLocalDataToDelete});
        this.mLocalDataToDelete = null;
        return true;
    }

    public void flush() {
        replaceData(new LocalDataList());
    }

    public void refresh(Uri uri) {
        int pos = findDataByContentUri(uri);
        if (pos != -1) {
            LocalData data = this.mImages.get(pos);
            LocalData refreshedData = data.refresh(this.mContext);
            if (refreshedData != null || this.mListener == null) {
                updateData(pos, refreshedData);
            } else {
                this.mListener.onDataRemoved(pos, data);
            }
        }
    }

    public void updateData(final int pos, LocalData data) {
        this.mImages.set(pos, data);
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

    private void insertData(LocalData data) {
        int pos = 0;
        Comparator<LocalData> comp = new NewestFirstComparator();
        while (pos < this.mImages.size() && comp.compare(data, this.mImages.get(pos)) > 0) {
            pos++;
        }
        this.mImages.add(pos, data);
        if (this.mListener != null) {
            this.mListener.onDataInserted(pos, data);
        }
    }

    private void replaceData(LocalDataList list) {
        if (list.size() != 0 || this.mImages.size() != 0) {
            this.mImages = list;
            if (this.mListener != null) {
                this.mListener.onDataLoaded();
            }
        }
    }

    public List<AsyncTask> preloadItems(List<Integer> items) {
        List<AsyncTask> result = new ArrayList();
        for (Integer id : items) {
            if (!isMetadataUpdated(id.intValue())) {
                result.add(updateMetadata(id.intValue()));
            }
        }
        return result;
    }

    public void cancelItems(List<AsyncTask> loadTokens) {
        for (AsyncTask asyncTask : loadTokens) {
            if (asyncTask != null) {
                asyncTask.cancel(false);
            }
        }
    }

    public List<Integer> getItemsInRange(int startPosition, int endPosition) {
        List<Integer> result = new ArrayList();
        for (int i = Math.max(0, startPosition); i < endPosition; i++) {
            result.add(Integer.valueOf(i));
        }
        return result;
    }

    public int getCount() {
        return getTotalNumber();
    }
}
