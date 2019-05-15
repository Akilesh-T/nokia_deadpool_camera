package com.morphoinc.app.panoramagp3;

class SaveThread extends Thread {
    private final ISaveThreadEventListener mListener;

    public interface ISaveThreadEventListener {
        void onSaveFinish(boolean z);

        boolean requestSaveProcess();
    }

    public SaveThread(ISaveThreadEventListener listener) {
        if (listener == null) {
            listener = new ISaveThreadEventListener() {
                public boolean requestSaveProcess() {
                    return true;
                }

                public void onSaveFinish(boolean result) {
                }
            };
        }
        this.mListener = listener;
    }

    public void run() {
        this.mListener.onSaveFinish(this.mListener.requestSaveProcess());
    }
}
