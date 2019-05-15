package com.hmdglobal.app.camera.filmstrip;

public interface FilmstripController {

    public interface FilmstripListener {
        void onDataFocusChanged(int i, int i2);

        void onDataReloaded();

        void onDataUpdated(int i);

        void onEnterFilmstrip(int i);

        void onEnterFullScreenUiHidden(int i);

        void onEnterFullScreenUiShown(int i);

        void onEnterZoomView(int i);

        void onFocusedDataDemoted(int i);

        void onFocusedDataLongPressed(int i);

        void onFocusedDataPromoted(int i);

        void onLeaveFilmstrip(int i);

        void onLeaveFullScreenUiHidden(int i);

        void onLeaveFullScreenUiShown(int i);

        void onScroll(int i, int i2, int i3);

        void onZoomAtIndexChanged(int i, float f);
    }

    void fling(float f);

    int getCurrentId();

    void goToFilmstrip();

    void goToFirstItem();

    void goToFullScreen();

    boolean goToNextItem();

    boolean goToPreviousItem();

    boolean inCameraFullscreen();

    boolean inFilmstrip();

    boolean inFullScreen();

    boolean isCameraPreview();

    boolean isScaling();

    boolean isScrolling();

    void scroll(float f);

    void scrollToPosition(int i, int i2, boolean z);

    void setDataAdapter(DataAdapter dataAdapter);

    void setImageGap(int i);

    void setListener(FilmstripListener filmstripListener);

    boolean stopScrolling(boolean z);
}
