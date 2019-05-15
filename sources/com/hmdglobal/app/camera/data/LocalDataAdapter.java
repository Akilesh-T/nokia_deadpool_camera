package com.hmdglobal.app.camera.data;

import android.net.Uri;
import android.os.AsyncTask;
import com.hmdglobal.app.camera.filmstrip.DataAdapter;
import com.hmdglobal.app.camera.util.Callback;
import com.hmdglobal.app.camera.widget.Preloader.ItemLoader;
import com.hmdglobal.app.camera.widget.Preloader.ItemSource;
import java.util.List;

public interface LocalDataAdapter extends DataAdapter, ItemLoader<Integer, AsyncTask>, ItemSource<Integer> {

    public interface LocalDataListener {
        void onMetadataUpdated(List<Integer> list);
    }

    boolean addData(LocalData localData);

    boolean executeDeletion();

    int findDataByContentUri(Uri uri);

    void flush();

    LocalData getLocalData(int i);

    boolean isMetadataUpdated(int i);

    void refresh(Uri uri);

    void removeData(int i);

    void requestLoad(Callback<Void> callback);

    void requestLoadNewPhotos();

    void setLocalDataListener(LocalDataListener localDataListener);

    boolean undoDataRemoval();

    void updateData(int i, LocalData localData);

    AsyncTask updateMetadata(int i);
}
