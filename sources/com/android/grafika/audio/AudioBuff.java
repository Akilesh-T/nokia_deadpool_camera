package com.android.grafika.audio;

public class AudioBuff {
    public int audioFormat = -1;
    public byte[] buff;
    public boolean isReadyToFill = true;

    public AudioBuff(int audioFormat, int size) {
        this.audioFormat = audioFormat;
        this.buff = new byte[size];
    }
}
