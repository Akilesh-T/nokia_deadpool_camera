package com.hmdglobal.app.camera.decoder;

import java.util.List;

public abstract class Remixer {
    protected RemixProgressListener mListener;

    public interface RemixProgressListener {
        void onRemixDone();
    }

    public abstract void prepareForRemixer(String str, List<String> list);

    public abstract void releaseRemixer();

    public abstract void setDisplayOrientation(int i);

    public abstract void startRemix();

    public void setRemxingProgressListener(RemixProgressListener listener) {
        this.mListener = listener;
    }
}
