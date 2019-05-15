package com.hmdglobal.app.camera.ui;

import android.widget.Adapter;
import com.hmdglobal.app.camera.app.ModuleManager;

public interface ModeStrip extends Lockable {

    public interface OnModeIdListener {
        void onModeIdChanged(int i);

        void onModeIdChanging();
    }

    void attachScrollIndicator(ScrollIndicator scrollIndicator);

    void init(ModuleManager moduleManager);

    boolean isLocked();

    Integer lockView();

    void notifyDatasetChanged();

    void pause();

    void resume();

    void setAdapter(Adapter adapter);

    void setCurrentModeWithModeIndex(int i);

    void setModeIndexChangeListener(OnModeIdListener onModeIdListener);

    boolean unLockView(Integer num);
}
