package com.morphoinc.app.panoramagp3;

import android.media.Image;

public class Camera2Image extends CaptureImage {
    public Camera2Image(Image image) {
        super(image);
    }

    public String getImageFormat() {
        return PanoramaGP3ImageFormat.getImageFormat(this.mImage);
    }

    public int getWidth() {
        return this.mImage.getWidth();
    }

    public int getHeight() {
        return this.mImage.getHeight();
    }
}
