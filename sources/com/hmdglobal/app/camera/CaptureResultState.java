package com.hmdglobal.app.camera;

public class CaptureResultState {
    private boolean isAnimationReady = false;
    private boolean isThumbReady = false;
    private ProgressListener mListener;
    private Thumbnail mThumb;

    public enum CaptureProgress {
        ANIMATION_DONE,
        THUMB_DONE
    }

    public interface ProgressListener {
        void onProgressArchieved(Thumbnail thumbnail);
    }

    public CaptureResultState(ProgressListener listener) {
        this.mListener = listener;
    }

    public void setThumbnail(Thumbnail thumb) {
        this.mThumb = thumb;
    }

    public void setProgress(CaptureProgress progress) {
        switch (progress) {
            case ANIMATION_DONE:
                this.isAnimationReady = true;
                break;
            case THUMB_DONE:
                if (this.mThumb != null) {
                    this.isThumbReady = true;
                    break;
                }
                break;
        }
        if (isReady()) {
            this.isThumbReady = false;
            this.isAnimationReady = false;
            this.mListener.onProgressArchieved(this.mThumb);
        }
    }

    public boolean isReady() {
        return this.isThumbReady && this.isAnimationReady;
    }
}
