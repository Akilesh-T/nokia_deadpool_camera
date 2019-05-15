package com.hmdglobal.app.camera.app;

import android.view.View;
import android.widget.FrameLayout;
import com.hmdglobal.app.camera.filmstrip.FilmstripController;

public interface AppUi {
    FilmstripController getFilmstripController();

    FrameLayout getModuleLayoutRoot();

    void init(View view, boolean z, boolean z2);
}
