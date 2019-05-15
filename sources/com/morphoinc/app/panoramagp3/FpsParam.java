package com.morphoinc.app.panoramagp3;

class FpsParam {
    public final float Fps;
    public final Long FrameDuration;

    public FpsParam(float fps, Long frameDuration) {
        this.Fps = fps;
        this.FrameDuration = frameDuration;
    }

    public String getFpsString() {
        return Float.toString(this.Fps);
    }
}
