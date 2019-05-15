package com.morphoinc.app.panoramagp3;

public class PanoramaState {
    IPanoramaStateEventListener listener;

    public interface IPanoramaStateEventListener {
        void requestEnd(PanoramaState panoramaState, int i);
    }

    /* Access modifiers changed, original: 0000 */
    public void clearListener() {
        this.listener = new IPanoramaStateEventListener() {
            public void requestEnd(PanoramaState sender, int resultCode) {
            }
        };
    }

    public void setPanoramaStateEventListener(IPanoramaStateEventListener listener) {
        this.listener = listener;
    }

    public PanoramaState() {
        clearListener();
    }

    public boolean onSaveImage(CaptureImage image) {
        image.close();
        return true;
    }

    public boolean hasImage() {
        return false;
    }

    public void repeatTakePicture() {
    }

    public boolean isEnableTvAnalysis() {
        return false;
    }

    public void notifyTvAnalyzed() {
    }

    public boolean isEnableTvChange() {
        return false;
    }

    public void notifyTvChanged() {
    }
}
