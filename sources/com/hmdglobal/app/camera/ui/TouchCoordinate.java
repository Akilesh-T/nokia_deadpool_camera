package com.hmdglobal.app.camera.ui;

public class TouchCoordinate {
    private float maxX;
    private float maxY;
    private float x;
    private float y;

    public TouchCoordinate(float x, float y, float maxX, float maxY) {
        this.x = x;
        this.y = y;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getMaxX() {
        return this.maxX;
    }

    public float getMaxY() {
        return this.maxY;
    }
}
