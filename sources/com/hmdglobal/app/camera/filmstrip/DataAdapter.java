package com.hmdglobal.app.camera.filmstrip;

import android.content.Context;
import android.view.View;
import com.hmdglobal.app.camera.data.LocalData.ActionCallback;

public interface DataAdapter {

    public interface Listener {
        void onDataInserted(int i, ImageData imageData);

        void onDataLoaded();

        void onDataRemoved(int i, ImageData imageData);

        void onDataUpdated(UpdateReporter updateReporter);
    }

    public interface UpdateReporter {
        boolean isDataRemoved(int i);

        boolean isDataUpdated(int i);
    }

    boolean canSwipeInFullScreen(int i);

    ImageData getImageData(int i);

    int getItemViewType(int i);

    int getTotalNumber();

    View getView(Context context, View view, int i, ActionCallback actionCallback);

    void resizeView(Context context, int i, View view, int i2, int i3);

    void setListener(Listener listener);

    void suggestViewSizeBound(int i, int i2);
}
