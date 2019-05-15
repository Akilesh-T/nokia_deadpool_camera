package com.hmdglobal.app.camera.ui;

public interface Rotatable {

    public static class RotateEntity {
        public boolean animation;
        private boolean mOrientationLocked;
        public Rotatable rotatable;
        public int rotatableHashCode;

        public RotateEntity(Rotatable rotatable, boolean needAnimation) {
            this.rotatable = rotatable;
            this.animation = needAnimation;
            if (rotatable != null) {
                this.rotatableHashCode = rotatable.hashCode();
            }
        }

        public void setOrientationLocked(boolean locked) {
            this.mOrientationLocked = locked;
        }

        public boolean isOrientationLocked() {
            return this.mOrientationLocked;
        }
    }

    void setOrientation(int i, boolean z);
}
