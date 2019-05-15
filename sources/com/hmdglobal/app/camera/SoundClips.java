package com.hmdglobal.app.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.ApiHelper;

public class SoundClips {
    public static final int FOCUS_COMPLETE = 0;
    public static final int SHUTTER_CLICK = 3;
    public static final int START_VIDEO_RECORDING = 1;
    public static final int STOP_VIDEO_RECORDING = 2;

    public interface Player {
        void play(int i);

        void release();
    }

    @TargetApi(16)
    private static class MediaActionSoundPlayer implements Player {
        private static final Tag TAG = new Tag("MediaActSndPlayer");
        private MediaActionSound mSound = new MediaActionSound();

        public void release() {
            if (this.mSound != null) {
                this.mSound.release();
                this.mSound = null;
            }
        }

        public MediaActionSoundPlayer() {
            this.mSound.load(2);
            this.mSound.load(3);
            this.mSound.load(1);
            this.mSound.load(0);
        }

        public synchronized void play(int action) {
            switch (action) {
                case 0:
                    this.mSound.play(1);
                    break;
                case 1:
                    this.mSound.play(2);
                    break;
                case 2:
                    this.mSound.play(3);
                    break;
                case 3:
                    this.mSound.play(0);
                    break;
                default:
                    Tag tag = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unrecognized action:");
                    stringBuilder.append(action);
                    Log.w(tag, stringBuilder.toString());
                    break;
            }
        }
    }

    private static class SoundPoolPlayer implements Player, OnLoadCompleteListener {
        private static final int ID_NOT_LOADED = 0;
        private static final int NUM_SOUND_STREAMS = 1;
        private static final int[] SOUND_RES = new int[]{R.raw.focus_complete, R.raw.video_record, R.raw.video_record, R.raw.shutter};
        private static final Tag TAG = new Tag("SoundPoolPlayer");
        private Context mContext;
        private final boolean[] mSoundIDReady;
        private int mSoundIDToPlay;
        private final int[] mSoundIDs;
        private SoundPool mSoundPool;
        private final int[] mSoundRes = new int[]{0, 1, 2, 3};

        public SoundPoolPlayer(Context context) {
            this.mContext = context;
            this.mSoundIDToPlay = 0;
            this.mSoundPool = new SoundPool(1, SoundClips.getAudioTypeForSoundPool(), 0);
            this.mSoundPool.setOnLoadCompleteListener(this);
            this.mSoundIDs = new int[SOUND_RES.length];
            this.mSoundIDReady = new boolean[SOUND_RES.length];
            for (int i = 0; i < SOUND_RES.length; i++) {
                this.mSoundIDs[i] = this.mSoundPool.load(this.mContext, SOUND_RES[i], 1);
                this.mSoundIDReady[i] = false;
            }
        }

        public synchronized void release() {
            if (this.mSoundPool != null) {
                this.mSoundPool.release();
                this.mSoundPool = null;
            }
        }

        /* JADX WARNING: Missing block: B:14:0x004a, code skipped:
            return;
     */
        public synchronized void play(int r9) {
            /*
            r8 = this;
            monitor-enter(r8);
            if (r9 < 0) goto L_0x004d;
        L_0x0003:
            r0 = r8.mSoundRes;	 Catch:{ all -> 0x004b }
            r0 = r0.length;	 Catch:{ all -> 0x004b }
            if (r9 < r0) goto L_0x0009;
        L_0x0008:
            goto L_0x004d;
        L_0x0009:
            r0 = r8.mSoundRes;	 Catch:{ all -> 0x004b }
            r0 = r0[r9];	 Catch:{ all -> 0x004b }
            r1 = r8.mSoundIDs;	 Catch:{ all -> 0x004b }
            r1 = r1[r0];	 Catch:{ all -> 0x004b }
            if (r1 != 0) goto L_0x002b;
        L_0x0013:
            r1 = r8.mSoundIDs;	 Catch:{ all -> 0x004b }
            r2 = r8.mSoundPool;	 Catch:{ all -> 0x004b }
            r3 = r8.mContext;	 Catch:{ all -> 0x004b }
            r4 = SOUND_RES;	 Catch:{ all -> 0x004b }
            r4 = r4[r0];	 Catch:{ all -> 0x004b }
            r5 = 1;
            r2 = r2.load(r3, r4, r5);	 Catch:{ all -> 0x004b }
            r1[r0] = r2;	 Catch:{ all -> 0x004b }
            r1 = r8.mSoundIDs;	 Catch:{ all -> 0x004b }
            r1 = r1[r0];	 Catch:{ all -> 0x004b }
            r8.mSoundIDToPlay = r1;	 Catch:{ all -> 0x004b }
            goto L_0x0049;
        L_0x002b:
            r1 = r8.mSoundIDReady;	 Catch:{ all -> 0x004b }
            r1 = r1[r0];	 Catch:{ all -> 0x004b }
            if (r1 != 0) goto L_0x0038;
        L_0x0031:
            r1 = r8.mSoundIDs;	 Catch:{ all -> 0x004b }
            r1 = r1[r0];	 Catch:{ all -> 0x004b }
            r8.mSoundIDToPlay = r1;	 Catch:{ all -> 0x004b }
            goto L_0x0049;
        L_0x0038:
            r1 = r8.mSoundPool;	 Catch:{ all -> 0x004b }
            r2 = r8.mSoundIDs;	 Catch:{ all -> 0x004b }
            r2 = r2[r0];	 Catch:{ all -> 0x004b }
            r3 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
            r4 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
            r5 = 0;
            r6 = 0;
            r7 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
            r1.play(r2, r3, r4, r5, r6, r7);	 Catch:{ all -> 0x004b }
        L_0x0049:
            monitor-exit(r8);
            return;
        L_0x004b:
            r9 = move-exception;
            goto L_0x006a;
        L_0x004d:
            r0 = TAG;	 Catch:{ all -> 0x004b }
            r1 = new java.lang.StringBuilder;	 Catch:{ all -> 0x004b }
            r1.<init>();	 Catch:{ all -> 0x004b }
            r2 = "Resource ID not found for action:";
            r1.append(r2);	 Catch:{ all -> 0x004b }
            r1.append(r9);	 Catch:{ all -> 0x004b }
            r2 = " in play().";
            r1.append(r2);	 Catch:{ all -> 0x004b }
            r1 = r1.toString();	 Catch:{ all -> 0x004b }
            com.hmdglobal.app.camera.debug.Log.e(r0, r1);	 Catch:{ all -> 0x004b }
            monitor-exit(r8);
            return;
        L_0x006a:
            monitor-exit(r8);
            throw r9;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.SoundClips$SoundPoolPlayer.play(int):void");
        }

        public void onLoadComplete(SoundPool pool, int soundID, int status) {
            int i;
            if (status != 0) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("loading sound tracks failed (status=");
                stringBuilder.append(status);
                stringBuilder.append(")");
                Log.e(tag, stringBuilder.toString());
                for (i = 0; i < this.mSoundIDs.length; i++) {
                    if (this.mSoundIDs[i] == soundID) {
                        this.mSoundIDs[i] = 0;
                        break;
                    }
                }
                return;
            }
            for (i = 0; i < this.mSoundIDs.length; i++) {
                if (this.mSoundIDs[i] == soundID) {
                    this.mSoundIDReady[i] = true;
                    break;
                }
            }
            if (soundID == this.mSoundIDToPlay) {
                this.mSoundIDToPlay = 0;
                this.mSoundPool.play(soundID, 1.0f, 1.0f, 0, 0, 1.0f);
            }
        }
    }

    public static Player getPlayer(Context context) {
        if (ApiHelper.HAS_MEDIA_ACTION_SOUND) {
            return new MediaActionSoundPlayer();
        }
        return new SoundPoolPlayer(context);
    }

    public static int getAudioTypeForSoundPool() {
        return ApiHelper.getIntFieldIfExists(AudioManager.class, "STREAM_SYSTEM_ENFORCED", null, 2);
    }
}
