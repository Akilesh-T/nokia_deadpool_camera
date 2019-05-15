package com.hmdglobal.app.camera;

import android.content.Context;
import android.media.SoundPool;
import android.util.SparseIntArray;

public class SoundPlayer {
    private final Context mAppContext;
    private final SparseIntArray mResourceToSoundId = new SparseIntArray();
    private final SoundPool mSoundPool;

    public SoundPlayer(Context appContext) {
        this.mAppContext = appContext;
        this.mSoundPool = new SoundPool(1, SoundClips.getAudioTypeForSoundPool(), 0);
    }

    public void loadSound(int resourceId) {
        this.mResourceToSoundId.put(resourceId, this.mSoundPool.load(this.mAppContext, resourceId, 1));
    }

    public void play(int resourceId, float volume) {
        Integer soundId = Integer.valueOf(this.mResourceToSoundId.get(resourceId));
        if (soundId != null) {
            this.mSoundPool.play(soundId.intValue(), volume, volume, 0, 0, 1.0f);
            return;
        }
        throw new IllegalStateException("Sound not loaded. Must call #loadSound first.");
    }

    public int play(int resourceId, float volume, int loop) {
        Integer soundId = Integer.valueOf(this.mResourceToSoundId.get(resourceId));
        if (soundId != null) {
            return this.mSoundPool.play(soundId.intValue(), volume, volume, 0, loop, 1.0f);
        }
        throw new IllegalStateException("Sound not loaded. Must call #loadSound first.");
    }

    public void unloadSound(int resourceId) {
        Integer soundId = Integer.valueOf(this.mResourceToSoundId.get(resourceId));
        if (soundId != null) {
            this.mSoundPool.unload(soundId.intValue());
            return;
        }
        throw new IllegalStateException("Sound not loaded. Must call #loadSound first.");
    }

    public void release() {
        this.mSoundPool.release();
    }

    public void stop(int resourceId) {
        Integer soundId = Integer.valueOf(this.mResourceToSoundId.get(resourceId));
        if (soundId != null) {
            this.mSoundPool.stop(soundId.intValue());
        }
    }

    public void pause(int resourceId) {
        this.mSoundPool.pause(resourceId);
    }
}
