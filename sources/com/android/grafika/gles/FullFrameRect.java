package com.android.grafika.gles;

import com.android.grafika.gles.Drawable2d.Prefab;

public class FullFrameRect {
    private Texture2dProgram mProgram;
    private final Drawable2d mRectDrawable = new Drawable2d(Prefab.FULL_RECTANGLE);

    public FullFrameRect(Texture2dProgram program) {
        this.mProgram = program;
    }

    public void release(boolean doEglCleanup) {
        if (this.mProgram != null) {
            if (doEglCleanup) {
                this.mProgram.release();
            }
            this.mProgram = null;
        }
    }

    public Texture2dProgram getProgram() {
        return this.mProgram;
    }

    public void changeProgram(Texture2dProgram program) {
        this.mProgram.release();
        this.mProgram = program;
    }

    public int createTextureObject() {
        return this.mProgram.createTextureObject();
    }
}
