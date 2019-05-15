package com.android.grafika.audio;

public class SetVolumeAudioFilter extends BaseSoftAudioFilter {
    private float volumeScale = 1.0f;

    public void setVolumeScale(float scale) {
        this.volumeScale = scale;
    }

    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        for (int i = 0; i < this.SIZE; i += 2) {
            short origin = (short) ((int) (((float) ((short) ((orignBuff[i + 1] << 8) | (orignBuff[i] & 255)))) * this.volumeScale));
            orignBuff[i + 1] = (byte) (origin >> 8);
            orignBuff[i] = (byte) origin;
        }
        return false;
    }
}
