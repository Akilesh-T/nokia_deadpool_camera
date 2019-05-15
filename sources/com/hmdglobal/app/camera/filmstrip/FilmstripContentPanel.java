package com.hmdglobal.app.camera.filmstrip;

import com.hmdglobal.app.camera.filmstrip.FilmstripController.FilmstripListener;

public interface FilmstripContentPanel {

    public interface Listener extends FilmstripListener {
        void onFilmstripHidden();

        void onFilmstripShown();

        void onSwipeOut();

        void onSwipeOutBegin();
    }

    boolean animateHide();

    void hide();

    boolean onBackPressed();

    void setFilmstripListener(Listener listener);

    void show();
}
