package android.support.v4.media;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.IMediaSession2.Stub;
import android.support.v4.media.MediaController2.ControllerCallback;
import android.support.v4.media.MediaController2.PlaybackInfo;
import android.support.v4.media.MediaSession2.CommandButton;
import android.util.Log;
import java.util.List;
import java.util.concurrent.Executor;

class MediaController2ImplBase implements SupportLibraryImpl {
    static final boolean DEBUG = Log.isLoggable(TAG, 3);
    static final String TAG = "MC2ImplBase";
    @GuardedBy("mLock")
    private SessionCommandGroup2 mAllowedCommands;
    @GuardedBy("mLock")
    private long mBufferedPositionMs;
    @GuardedBy("mLock")
    private int mBufferingState;
    private final ControllerCallback mCallback;
    private final Executor mCallbackExecutor;
    private final Context mContext;
    final MediaController2Stub mControllerStub;
    @GuardedBy("mLock")
    private MediaItem2 mCurrentMediaItem;
    private final DeathRecipient mDeathRecipient;
    @GuardedBy("mLock")
    private volatile IMediaSession2 mISession2;
    private final MediaController2 mInstance;
    @GuardedBy("mLock")
    private boolean mIsReleased;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private PlaybackInfo mPlaybackInfo;
    @GuardedBy("mLock")
    private float mPlaybackSpeed;
    @GuardedBy("mLock")
    private int mPlayerState;
    @GuardedBy("mLock")
    private List<MediaItem2> mPlaylist;
    @GuardedBy("mLock")
    private MediaMetadata2 mPlaylistMetadata;
    @GuardedBy("mLock")
    private long mPositionEventTimeMs;
    @GuardedBy("mLock")
    private long mPositionMs;
    @GuardedBy("mLock")
    private int mRepeatMode;
    @GuardedBy("mLock")
    private SessionServiceConnection mServiceConnection;
    @GuardedBy("mLock")
    private PendingIntent mSessionActivity;
    @GuardedBy("mLock")
    private int mShuffleMode;
    private final SessionToken2 mToken;

    private class SessionServiceConnection implements ServiceConnection {
        private SessionServiceConnection() {
        }

        /* synthetic */ SessionServiceConnection(MediaController2ImplBase x0, AnonymousClass1 x1) {
            this();
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            String str;
            StringBuilder stringBuilder;
            if (MediaController2ImplBase.DEBUG) {
                str = MediaController2ImplBase.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onServiceConnected ");
                stringBuilder.append(name);
                stringBuilder.append(" ");
                stringBuilder.append(this);
                Log.d(str, stringBuilder.toString());
            }
            if (MediaController2ImplBase.this.mToken.getPackageName().equals(name.getPackageName())) {
                MediaController2ImplBase.this.connectToSession(Stub.asInterface(service));
                return;
            }
            str = MediaController2ImplBase.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(name);
            stringBuilder.append(" was connected, but expected pkg=");
            stringBuilder.append(MediaController2ImplBase.this.mToken.getPackageName());
            stringBuilder.append(" with id=");
            stringBuilder.append(MediaController2ImplBase.this.mToken.getId());
            Log.wtf(str, stringBuilder.toString());
        }

        public void onServiceDisconnected(ComponentName name) {
            if (MediaController2ImplBase.DEBUG) {
                String str = MediaController2ImplBase.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Session service ");
                stringBuilder.append(name);
                stringBuilder.append(" is disconnected.");
                Log.w(str, stringBuilder.toString());
            }
        }

        public void onBindingDied(ComponentName name) {
            MediaController2ImplBase.this.close();
        }
    }

    MediaController2ImplBase(Context context, MediaController2 instance, SessionToken2 token, Executor executor, ControllerCallback callback) {
        this.mInstance = instance;
        if (context == null) {
            throw new IllegalArgumentException("context shouldn't be null");
        } else if (token == null) {
            throw new IllegalArgumentException("token shouldn't be null");
        } else if (callback == null) {
            throw new IllegalArgumentException("callback shouldn't be null");
        } else if (executor != null) {
            this.mContext = context;
            this.mControllerStub = new MediaController2Stub(this);
            this.mToken = token;
            this.mCallback = callback;
            this.mCallbackExecutor = executor;
            this.mDeathRecipient = new DeathRecipient() {
                public void binderDied() {
                    MediaController2ImplBase.this.mInstance.close();
                }
            };
            IMediaSession2 iSession2 = Stub.asInterface((IBinder) this.mToken.getBinder());
            if (this.mToken.getType() == 0) {
                this.mServiceConnection = null;
                connectToSession(iSession2);
                return;
            }
            this.mServiceConnection = new SessionServiceConnection(this, null);
            connectToService();
        } else {
            throw new IllegalArgumentException("executor shouldn't be null");
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0040, code skipped:
            if (r2 == null) goto L_0x0053;
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            r2.asBinder().unlinkToDeath(r6.mDeathRecipient, 0);
            r2.release(r6.mControllerStub);
     */
    public void close() {
        /*
        r6 = this;
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001c;
    L_0x0004:
        r0 = "MC2ImplBase";
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "release from ";
        r1.append(r2);
        r2 = r6.mToken;
        r1.append(r2);
        r1 = r1.toString();
        android.util.Log.d(r0, r1);
    L_0x001c:
        r0 = r6.mLock;
        monitor-enter(r0);
        r1 = 0;
        r2 = r6.mISession2;	 Catch:{ all -> 0x0060 }
        r3 = r6.mIsReleased;	 Catch:{ all -> 0x005e }
        if (r3 == 0) goto L_0x0028;
    L_0x0026:
        monitor-exit(r0);	 Catch:{ all -> 0x005e }
        return;
    L_0x0028:
        r3 = 1;
        r6.mIsReleased = r3;	 Catch:{ all -> 0x005e }
        r3 = r6.mServiceConnection;	 Catch:{ all -> 0x005e }
        if (r3 == 0) goto L_0x0038;
    L_0x002f:
        r3 = r6.mContext;	 Catch:{ all -> 0x005e }
        r4 = r6.mServiceConnection;	 Catch:{ all -> 0x005e }
        r3.unbindService(r4);	 Catch:{ all -> 0x005e }
        r6.mServiceConnection = r1;	 Catch:{ all -> 0x005e }
    L_0x0038:
        r6.mISession2 = r1;	 Catch:{ all -> 0x005e }
        r1 = r6.mControllerStub;	 Catch:{ all -> 0x005e }
        r1.destroy();	 Catch:{ all -> 0x005e }
        monitor-exit(r0);	 Catch:{ all -> 0x005e }
        if (r2 == 0) goto L_0x0053;
    L_0x0042:
        r0 = r2.asBinder();	 Catch:{ RemoteException -> 0x0052 }
        r1 = r6.mDeathRecipient;	 Catch:{ RemoteException -> 0x0052 }
        r3 = 0;
        r0.unlinkToDeath(r1, r3);	 Catch:{ RemoteException -> 0x0052 }
        r0 = r6.mControllerStub;	 Catch:{ RemoteException -> 0x0052 }
        r2.release(r0);	 Catch:{ RemoteException -> 0x0052 }
        goto L_0x0053;
    L_0x0052:
        r0 = move-exception;
    L_0x0053:
        r0 = r6.mCallbackExecutor;
        r1 = new android.support.v4.media.MediaController2ImplBase$2;
        r1.<init>();
        r0.execute(r1);
        return;
    L_0x005e:
        r1 = move-exception;
        goto L_0x0064;
    L_0x0060:
        r2 = move-exception;
        r5 = r2;
        r2 = r1;
        r1 = r5;
    L_0x0064:
        monitor-exit(r0);	 Catch:{ all -> 0x005e }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaController2ImplBase.close():void");
    }

    public SessionToken2 getSessionToken() {
        return this.mToken;
    }

    public boolean isConnected() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mISession2 != null;
        }
        return z;
    }

    public void play() {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(1);
        if (iSession2 != null) {
            try {
                iSession2.play(this.mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void pause() {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(2);
        if (iSession2 != null) {
            try {
                iSession2.pause(this.mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void reset() {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(3);
        if (iSession2 != null) {
            try {
                iSession2.reset(this.mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void prepare() {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(6);
        if (iSession2 != null) {
            try {
                iSession2.prepare(this.mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void fastForward() {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(7);
        if (iSession2 != null) {
            try {
                iSession2.fastForward(this.mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void rewind() {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(8);
        if (iSession2 != null) {
            try {
                iSession2.rewind(this.mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void seekTo(long pos) {
        if (pos >= 0) {
            IMediaSession2 iSession2 = getSessionInterfaceIfAble(9);
            if (iSession2 != null) {
                try {
                    iSession2.seekTo(this.mControllerStub, pos);
                    return;
                } catch (RemoteException e) {
                    Log.w(TAG, "Cannot connect to the service or the session is gone", e);
                    return;
                }
            }
            return;
        }
        throw new IllegalArgumentException("position shouldn't be negative");
    }

    public void skipForward() {
    }

    public void skipBackward() {
    }

    public void playFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(22);
        if (iSession2 != null) {
            try {
                iSession2.playFromMediaId(this.mControllerStub, mediaId, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void playFromSearch(@NonNull String query, @Nullable Bundle extras) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(24);
        if (iSession2 != null) {
            try {
                iSession2.playFromSearch(this.mControllerStub, query, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void playFromUri(Uri uri, Bundle extras) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(23);
        if (iSession2 != null) {
            try {
                iSession2.playFromUri(this.mControllerStub, uri, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void prepareFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(25);
        if (iSession2 != null) {
            try {
                iSession2.prepareFromMediaId(this.mControllerStub, mediaId, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void prepareFromSearch(@NonNull String query, @Nullable Bundle extras) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(27);
        if (iSession2 != null) {
            try {
                iSession2.prepareFromSearch(this.mControllerStub, query, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void prepareFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(26);
        if (iSession2 != null) {
            try {
                iSession2.prepareFromUri(this.mControllerStub, uri, extras);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void setVolumeTo(int value, int flags) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(10);
        if (iSession2 != null) {
            try {
                iSession2.setVolumeTo(this.mControllerStub, value, flags);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void adjustVolume(int direction, int flags) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(11);
        if (iSession2 != null) {
            try {
                iSession2.adjustVolume(this.mControllerStub, direction, flags);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public PendingIntent getSessionActivity() {
        PendingIntent pendingIntent;
        synchronized (this.mLock) {
            pendingIntent = this.mSessionActivity;
        }
        return pendingIntent;
    }

    public int getPlayerState() {
        int i;
        synchronized (this.mLock) {
            i = this.mPlayerState;
        }
        return i;
    }

    /* JADX WARNING: Missing block: B:12:0x001e, code skipped:
            return -1;
     */
    public long getDuration() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mCurrentMediaItem;	 Catch:{ all -> 0x001f }
        r1 = r1.getMetadata();	 Catch:{ all -> 0x001f }
        if (r1 == 0) goto L_0x001b;
    L_0x000b:
        r2 = "android.media.metadata.DURATION";
        r2 = r1.containsKey(r2);	 Catch:{ all -> 0x001f }
        if (r2 == 0) goto L_0x001b;
    L_0x0013:
        r2 = "android.media.metadata.DURATION";
        r2 = r1.getLong(r2);	 Catch:{ all -> 0x001f }
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        return r2;
    L_0x001b:
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        r0 = -1;
        return r0;
    L_0x001f:
        r1 = move-exception;
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaController2ImplBase.getDuration():long");
    }

    public long getCurrentPosition() {
        synchronized (this.mLock) {
            if (this.mISession2 == null) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return -1;
            }
            long timeDiff;
            if (this.mInstance.mTimeDiff != null) {
                timeDiff = this.mInstance.mTimeDiff.longValue();
            } else {
                timeDiff = SystemClock.elapsedRealtime() - this.mPositionEventTimeMs;
            }
            long max = Math.max(0, this.mPositionMs + ((long) (this.mPlaybackSpeed * ((float) timeDiff))));
            return max;
        }
    }

    public float getPlaybackSpeed() {
        synchronized (this.mLock) {
            if (this.mISession2 == null) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return 0.0f;
            }
            float f = this.mPlaybackSpeed;
            return f;
        }
    }

    public void setPlaybackSpeed(float speed) {
        synchronized (this.mLock) {
            IMediaSession2 iSession2 = getSessionInterfaceIfAble(39);
            if (iSession2 != null) {
                try {
                    iSession2.setPlaybackSpeed(this.mControllerStub, speed);
                } catch (RemoteException e) {
                    Log.w(TAG, "Cannot connect to the service or the session is gone", e);
                }
            }
        }
    }

    public int getBufferingState() {
        synchronized (this.mLock) {
            if (this.mISession2 == null) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return 0;
            }
            int i = this.mBufferingState;
            return i;
        }
    }

    public long getBufferedPosition() {
        synchronized (this.mLock) {
            if (this.mISession2 == null) {
                Log.w(TAG, "Session isn't active", new IllegalStateException());
                return -1;
            }
            long j = this.mBufferedPositionMs;
            return j;
        }
    }

    public PlaybackInfo getPlaybackInfo() {
        PlaybackInfo playbackInfo;
        synchronized (this.mLock) {
            playbackInfo = this.mPlaybackInfo;
        }
        return playbackInfo;
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_?;
     */
    /* JADX WARNING: Missing block: B:8:?, code skipped:
            r1.setRating(r4.mControllerStub, r5, r6.toBundle());
     */
    /* JADX WARNING: Missing block: B:9:0x0012, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:10:0x0013, code skipped:
            android.util.Log.w(TAG, "Cannot connect to the service or the session is gone", r0);
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            return;
     */
    public void setRating(@android.support.annotation.NonNull java.lang.String r5, @android.support.annotation.NonNull android.support.mediacompat.Rating2 r6) {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mISession2;	 Catch:{ all -> 0x001d }
        monitor-exit(r0);	 Catch:{ all -> 0x001b }
        if (r1 == 0) goto L_0x001a;
    L_0x0008:
        r0 = r4.mControllerStub;	 Catch:{ RemoteException -> 0x0012 }
        r2 = r6.toBundle();	 Catch:{ RemoteException -> 0x0012 }
        r1.setRating(r0, r5, r2);	 Catch:{ RemoteException -> 0x0012 }
        goto L_0x001a;
    L_0x0012:
        r0 = move-exception;
        r2 = "MC2ImplBase";
        r3 = "Cannot connect to the service or the session is gone";
        android.util.Log.w(r2, r3, r0);
    L_0x001a:
        return;
    L_0x001b:
        r2 = move-exception;
        goto L_0x001f;
    L_0x001d:
        r2 = move-exception;
        r1 = 0;
    L_0x001f:
        monitor-exit(r0);	 Catch:{ all -> 0x001b }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaController2ImplBase.setRating(java.lang.String, android.support.mediacompat.Rating2):void");
    }

    public void sendCustomCommand(@NonNull SessionCommand2 command, Bundle args, @Nullable ResultReceiver cb) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(command);
        if (iSession2 != null) {
            try {
                iSession2.sendCustomCommand(this.mControllerStub, command.toBundle(), args, cb);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public List<MediaItem2> getPlaylist() {
        List list;
        synchronized (this.mLock) {
            list = this.mPlaylist;
        }
        return list;
    }

    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(19);
        if (iSession2 != null) {
            try {
                Bundle bundle;
                MediaController2Stub mediaController2Stub = this.mControllerStub;
                List convertMediaItem2ListToBundleList = MediaUtils2.convertMediaItem2ListToBundleList(list);
                if (metadata == null) {
                    bundle = null;
                } else {
                    bundle = metadata.toBundle();
                }
                iSession2.setPlaylist(mediaController2Stub, convertMediaItem2ListToBundleList, bundle);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(21);
        if (iSession2 != null) {
            try {
                Bundle bundle;
                MediaController2Stub mediaController2Stub = this.mControllerStub;
                if (metadata == null) {
                    bundle = null;
                } else {
                    bundle = metadata.toBundle();
                }
                iSession2.updatePlaylistMetadata(mediaController2Stub, bundle);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public MediaMetadata2 getPlaylistMetadata() {
        MediaMetadata2 mediaMetadata2;
        synchronized (this.mLock) {
            mediaMetadata2 = this.mPlaylistMetadata;
        }
        return mediaMetadata2;
    }

    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(15);
        if (iSession2 != null) {
            try {
                iSession2.addPlaylistItem(this.mControllerStub, index, item.toBundle());
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void removePlaylistItem(@NonNull MediaItem2 item) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(16);
        if (iSession2 != null) {
            try {
                iSession2.removePlaylistItem(this.mControllerStub, item.toBundle());
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(17);
        if (iSession2 != null) {
            try {
                iSession2.replacePlaylistItem(this.mControllerStub, index, item.toBundle());
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public MediaItem2 getCurrentMediaItem() {
        MediaItem2 mediaItem2;
        synchronized (this.mLock) {
            mediaItem2 = this.mCurrentMediaItem;
        }
        return mediaItem2;
    }

    public void skipToPreviousItem() {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(5);
        synchronized (this.mLock) {
            if (iSession2 != null) {
                try {
                    iSession2.skipToPreviousItem(this.mControllerStub);
                } catch (RemoteException e) {
                    Log.w(TAG, "Cannot connect to the service or the session is gone", e);
                }
            }
        }
    }

    public void skipToNextItem() {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(4);
        synchronized (this.mLock) {
            if (iSession2 != null) {
                try {
                    this.mISession2.skipToNextItem(this.mControllerStub);
                } catch (RemoteException e) {
                    Log.w(TAG, "Cannot connect to the service or the session is gone", e);
                }
            }
        }
    }

    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(12);
        synchronized (this.mLock) {
            if (iSession2 != null) {
                try {
                    this.mISession2.skipToPlaylistItem(this.mControllerStub, item.toBundle());
                } catch (RemoteException e) {
                    Log.w(TAG, "Cannot connect to the service or the session is gone", e);
                }
            }
        }
    }

    public int getRepeatMode() {
        int i;
        synchronized (this.mLock) {
            i = this.mRepeatMode;
        }
        return i;
    }

    public void setRepeatMode(int repeatMode) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(14);
        if (iSession2 != null) {
            try {
                iSession2.setRepeatMode(this.mControllerStub, repeatMode);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public int getShuffleMode() {
        int i;
        synchronized (this.mLock) {
            i = this.mShuffleMode;
        }
        return i;
    }

    public void setShuffleMode(int shuffleMode) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(13);
        if (iSession2 != null) {
            try {
                iSession2.setShuffleMode(this.mControllerStub, shuffleMode);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void subscribeRoutesInfo() {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(36);
        if (iSession2 != null) {
            try {
                iSession2.subscribeRoutesInfo(this.mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void unsubscribeRoutesInfo() {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(37);
        if (iSession2 != null) {
            try {
                iSession2.unsubscribeRoutesInfo(this.mControllerStub);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    public void selectRoute(@NonNull Bundle route) {
        IMediaSession2 iSession2 = getSessionInterfaceIfAble(38);
        if (iSession2 != null) {
            try {
                iSession2.selectRoute(this.mControllerStub, route);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to the service or the session is gone", e);
            }
        }
    }

    @NonNull
    public Context getContext() {
        return this.mContext;
    }

    @NonNull
    public ControllerCallback getCallback() {
        return this.mCallback;
    }

    @NonNull
    public Executor getCallbackExecutor() {
        return this.mCallbackExecutor;
    }

    @Nullable
    public MediaBrowserCompat getBrowserCompat() {
        return null;
    }

    @NonNull
    public MediaController2 getInstance() {
        return this.mInstance;
    }

    private void connectToService() {
        Intent intent = new Intent(MediaSessionService2.SERVICE_INTERFACE);
        intent.setClassName(this.mToken.getPackageName(), this.mToken.getServiceName());
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (!this.mContext.bindService(intent, this.mServiceConnection, 1)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("bind to ");
                stringBuilder.append(this.mToken);
                stringBuilder.append(" failed");
                Log.w(str, stringBuilder.toString());
            } else if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("bind to ");
                stringBuilder.append(this.mToken);
                stringBuilder.append(" success");
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    private void connectToSession(IMediaSession2 sessionBinder) {
        try {
            sessionBinder.connect(this.mControllerStub, this.mContext.getPackageName());
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call connection request. Framework will retry automatically");
        }
    }

    /* Access modifiers changed, original: 0000 */
    public IMediaSession2 getSessionInterfaceIfAble(int commandCode) {
        synchronized (this.mLock) {
            if (this.mAllowedCommands.hasCommand(commandCode)) {
                IMediaSession2 iMediaSession2 = this.mISession2;
                return iMediaSession2;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Controller isn't allowed to call command, commandCode=");
            stringBuilder.append(commandCode);
            Log.w(str, stringBuilder.toString());
            return null;
        }
    }

    /* Access modifiers changed, original: 0000 */
    public IMediaSession2 getSessionInterfaceIfAble(SessionCommand2 command) {
        synchronized (this.mLock) {
            if (this.mAllowedCommands.hasCommand(command)) {
                IMediaSession2 iMediaSession2 = this.mISession2;
                return iMediaSession2;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Controller isn't allowed to call command, command=");
            stringBuilder.append(command);
            Log.w(str, stringBuilder.toString());
            return null;
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void notifyCurrentMediaItemChanged(final MediaItem2 item) {
        synchronized (this.mLock) {
            this.mCurrentMediaItem = item;
        }
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                if (MediaController2ImplBase.this.mInstance.isConnected()) {
                    MediaController2ImplBase.this.mCallback.onCurrentMediaItemChanged(MediaController2ImplBase.this.mInstance, item);
                }
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void notifyPlayerStateChanges(long eventTimeMs, long positionMs, final int state) {
        synchronized (this.mLock) {
            this.mPositionEventTimeMs = eventTimeMs;
            this.mPositionMs = positionMs;
            this.mPlayerState = state;
        }
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                if (MediaController2ImplBase.this.mInstance.isConnected()) {
                    MediaController2ImplBase.this.mCallback.onPlayerStateChanged(MediaController2ImplBase.this.mInstance, state);
                }
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void notifyPlaybackSpeedChanges(long eventTimeMs, long positionMs, final float speed) {
        synchronized (this.mLock) {
            this.mPositionEventTimeMs = eventTimeMs;
            this.mPositionMs = positionMs;
            this.mPlaybackSpeed = speed;
        }
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                if (MediaController2ImplBase.this.mInstance.isConnected()) {
                    MediaController2ImplBase.this.mCallback.onPlaybackSpeedChanged(MediaController2ImplBase.this.mInstance, speed);
                }
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void notifyBufferingStateChanged(final MediaItem2 item, final int state, long bufferedPositionMs) {
        synchronized (this.mLock) {
            this.mBufferingState = state;
            this.mBufferedPositionMs = bufferedPositionMs;
        }
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                if (MediaController2ImplBase.this.mInstance.isConnected()) {
                    MediaController2ImplBase.this.mCallback.onBufferingStateChanged(MediaController2ImplBase.this.mInstance, item, state);
                }
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void notifyPlaylistChanges(final List<MediaItem2> playlist, final MediaMetadata2 metadata) {
        synchronized (this.mLock) {
            this.mPlaylist = playlist;
            this.mPlaylistMetadata = metadata;
        }
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                if (MediaController2ImplBase.this.mInstance.isConnected()) {
                    MediaController2ImplBase.this.mCallback.onPlaylistChanged(MediaController2ImplBase.this.mInstance, playlist, metadata);
                }
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void notifyPlaylistMetadataChanges(final MediaMetadata2 metadata) {
        synchronized (this.mLock) {
            this.mPlaylistMetadata = metadata;
        }
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                if (MediaController2ImplBase.this.mInstance.isConnected()) {
                    MediaController2ImplBase.this.mCallback.onPlaylistMetadataChanged(MediaController2ImplBase.this.mInstance, metadata);
                }
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void notifyPlaybackInfoChanges(final PlaybackInfo info) {
        synchronized (this.mLock) {
            this.mPlaybackInfo = info;
        }
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                if (MediaController2ImplBase.this.mInstance.isConnected()) {
                    MediaController2ImplBase.this.mCallback.onPlaybackInfoChanged(MediaController2ImplBase.this.mInstance, info);
                }
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void notifyRepeatModeChanges(final int repeatMode) {
        synchronized (this.mLock) {
            this.mRepeatMode = repeatMode;
        }
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                if (MediaController2ImplBase.this.mInstance.isConnected()) {
                    MediaController2ImplBase.this.mCallback.onRepeatModeChanged(MediaController2ImplBase.this.mInstance, repeatMode);
                }
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void notifyShuffleModeChanges(final int shuffleMode) {
        synchronized (this.mLock) {
            this.mShuffleMode = shuffleMode;
        }
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                if (MediaController2ImplBase.this.mInstance.isConnected()) {
                    MediaController2ImplBase.this.mCallback.onShuffleModeChanged(MediaController2ImplBase.this.mInstance, shuffleMode);
                }
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void notifySeekCompleted(long eventTimeMs, long positionMs, final long seekPositionMs) {
        synchronized (this.mLock) {
            this.mPositionEventTimeMs = eventTimeMs;
            this.mPositionMs = positionMs;
        }
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                if (MediaController2ImplBase.this.mInstance.isConnected()) {
                    MediaController2ImplBase.this.mCallback.onSeekCompleted(MediaController2ImplBase.this.mInstance, seekPositionMs);
                }
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void notifyError(final int errorCode, final Bundle extras) {
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                if (MediaController2ImplBase.this.mInstance.isConnected()) {
                    MediaController2ImplBase.this.mCallback.onError(MediaController2ImplBase.this.mInstance, errorCode, extras);
                }
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void notifyRoutesInfoChanged(final List<Bundle> routes) {
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                if (MediaController2ImplBase.this.mInstance.isConnected()) {
                    MediaController2ImplBase.this.mCallback.onRoutesInfoChanged(MediaController2ImplBase.this.mInstance, routes);
                }
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    /* JADX WARNING: Missing block: B:13:0x0037, code skipped:
            if (null == null) goto L_0x003e;
     */
    /* JADX WARNING: Missing block: B:14:0x0039, code skipped:
            r1.mInstance.close();
     */
    /* JADX WARNING: Missing block: B:15:0x003e, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:21:0x004c, code skipped:
            if (true == false) goto L_0x0053;
     */
    /* JADX WARNING: Missing block: B:22:0x004e, code skipped:
            r1.mInstance.close();
     */
    /* JADX WARNING: Missing block: B:23:0x0053, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:49:?, code skipped:
            r1.mCallbackExecutor.execute(new android.support.v4.media.MediaController2ImplBase.AnonymousClass15(r1));
     */
    /* JADX WARNING: Missing block: B:50:0x009c, code skipped:
            if (null == null) goto L_0x00a3;
     */
    /* JADX WARNING: Missing block: B:51:0x009e, code skipped:
            r1.mInstance.close();
     */
    /* JADX WARNING: Missing block: B:52:0x00a3, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:64:0x00ba, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:65:0x00bb, code skipped:
            r4 = true;
     */
    /* JADX WARNING: Missing block: B:82:0x00d6, code skipped:
            r0 = th;
     */
    public void onConnectedNotLocked(android.support.v4.media.IMediaSession2 r18, android.support.v4.media.SessionCommandGroup2 r19, int r20, android.support.v4.media.MediaItem2 r21, long r22, long r24, float r26, long r27, android.support.v4.media.MediaController2.PlaybackInfo r29, int r30, int r31, java.util.List<android.support.v4.media.MediaItem2> r32, android.app.PendingIntent r33) {
        /*
        r17 = this;
        r1 = r17;
        r2 = r18;
        r3 = r19;
        r0 = DEBUG;
        if (r0 == 0) goto L_0x0028;
    L_0x000a:
        r0 = "MC2ImplBase";
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "onConnectedNotLocked sessionBinder=";
        r4.append(r5);
        r4.append(r2);
        r5 = ", allowedCommands=";
        r4.append(r5);
        r4.append(r3);
        r4 = r4.toString();
        android.util.Log.d(r0, r4);
    L_0x0028:
        r4 = 0;
        if (r2 == 0) goto L_0x00ef;
    L_0x002b:
        if (r3 != 0) goto L_0x002f;
    L_0x002d:
        goto L_0x00ef;
    L_0x002f:
        r5 = r1.mLock;	 Catch:{ all -> 0x00da }
        monitor-enter(r5);	 Catch:{ all -> 0x00da }
        r0 = r1.mIsReleased;	 Catch:{ all -> 0x00c7 }
        if (r0 == 0) goto L_0x003f;
    L_0x0036:
        monitor-exit(r5);	 Catch:{ all -> 0x00c7 }
        if (r4 == 0) goto L_0x003e;
    L_0x0039:
        r0 = r1.mInstance;
        r0.close();
    L_0x003e:
        return;
    L_0x003f:
        r0 = r1.mISession2;	 Catch:{ all -> 0x00c7 }
        if (r0 == 0) goto L_0x0054;
    L_0x0043:
        r0 = "MC2ImplBase";
        r6 = "Cannot be notified about the connection result many times. Probably a bug or malicious app.";
        android.util.Log.e(r0, r6);	 Catch:{ all -> 0x00c7 }
        r4 = 1;
        monitor-exit(r5);	 Catch:{ all -> 0x00c7 }
        if (r4 == 0) goto L_0x0053;
    L_0x004e:
        r0 = r1.mInstance;
        r0.close();
    L_0x0053:
        return;
    L_0x0054:
        r1.mAllowedCommands = r3;	 Catch:{ all -> 0x00c7 }
        r6 = r20;
        r1.mPlayerState = r6;	 Catch:{ all -> 0x00c7 }
        r7 = r21;
        r1.mCurrentMediaItem = r7;	 Catch:{ all -> 0x00c5 }
        r8 = r22;
        r1.mPositionEventTimeMs = r8;	 Catch:{ all -> 0x00c3 }
        r10 = r24;
        r1.mPositionMs = r10;	 Catch:{ all -> 0x00c1 }
        r12 = r26;
        r1.mPlaybackSpeed = r12;	 Catch:{ all -> 0x00bf }
        r13 = r27;
        r1.mBufferedPositionMs = r13;	 Catch:{ all -> 0x00bd }
        r15 = r29;
        r1.mPlaybackInfo = r15;	 Catch:{ all -> 0x00d8 }
        r6 = r30;
        r1.mRepeatMode = r6;	 Catch:{ all -> 0x00d8 }
        r6 = r31;
        r1.mShuffleMode = r6;	 Catch:{ all -> 0x00d8 }
        r6 = r32;
        r1.mPlaylist = r6;	 Catch:{ all -> 0x00d8 }
        r6 = r33;
        r1.mSessionActivity = r6;	 Catch:{ all -> 0x00d8 }
        r1.mISession2 = r2;	 Catch:{ all -> 0x00d8 }
        r0 = r1.mISession2;	 Catch:{ RemoteException -> 0x00a4 }
        r0 = r0.asBinder();	 Catch:{ RemoteException -> 0x00a4 }
        r2 = r1.mDeathRecipient;	 Catch:{ RemoteException -> 0x00a4 }
        r6 = 0;
        r0.linkToDeath(r2, r6);	 Catch:{ RemoteException -> 0x00a4 }
        monitor-exit(r5);	 Catch:{ all -> 0x00d8 }
        r0 = r1.mCallbackExecutor;	 Catch:{ all -> 0x00d6 }
        r2 = new android.support.v4.media.MediaController2ImplBase$15;	 Catch:{ all -> 0x00d6 }
        r2.<init>(r3);	 Catch:{ all -> 0x00d6 }
        r0.execute(r2);	 Catch:{ all -> 0x00d6 }
        if (r4 == 0) goto L_0x00a3;
    L_0x009e:
        r0 = r1.mInstance;
        r0.close();
    L_0x00a3:
        return;
    L_0x00a4:
        r0 = move-exception;
        r2 = DEBUG;	 Catch:{ all -> 0x00d8 }
        if (r2 == 0) goto L_0x00b0;
    L_0x00a9:
        r2 = "MC2ImplBase";
        r6 = "Session died too early.";
        android.util.Log.d(r2, r6, r0);	 Catch:{ all -> 0x00d8 }
    L_0x00b0:
        r2 = 1;
        monitor-exit(r5);	 Catch:{ all -> 0x00ba }
        if (r2 == 0) goto L_0x00b9;
    L_0x00b4:
        r4 = r1.mInstance;
        r4.close();
    L_0x00b9:
        return;
    L_0x00ba:
        r0 = move-exception;
        r4 = r2;
        goto L_0x00d4;
    L_0x00bd:
        r0 = move-exception;
        goto L_0x00d2;
    L_0x00bf:
        r0 = move-exception;
        goto L_0x00d0;
    L_0x00c1:
        r0 = move-exception;
        goto L_0x00ce;
    L_0x00c3:
        r0 = move-exception;
        goto L_0x00cc;
    L_0x00c5:
        r0 = move-exception;
        goto L_0x00ca;
    L_0x00c7:
        r0 = move-exception;
        r7 = r21;
    L_0x00ca:
        r8 = r22;
    L_0x00cc:
        r10 = r24;
    L_0x00ce:
        r12 = r26;
    L_0x00d0:
        r13 = r27;
    L_0x00d2:
        r15 = r29;
    L_0x00d4:
        monitor-exit(r5);	 Catch:{ all -> 0x00d8 }
        throw r0;	 Catch:{ all -> 0x00d6 }
    L_0x00d6:
        r0 = move-exception;
        goto L_0x00e7;
    L_0x00d8:
        r0 = move-exception;
        goto L_0x00d4;
    L_0x00da:
        r0 = move-exception;
        r7 = r21;
        r8 = r22;
        r10 = r24;
        r12 = r26;
        r13 = r27;
        r15 = r29;
    L_0x00e7:
        if (r4 == 0) goto L_0x00ee;
    L_0x00e9:
        r2 = r1.mInstance;
        r2.close();
    L_0x00ee:
        throw r0;
    L_0x00ef:
        r7 = r21;
        r8 = r22;
        r10 = r24;
        r12 = r26;
        r13 = r27;
        r15 = r29;
        r0 = 1;
        if (r0 == 0) goto L_0x0103;
    L_0x00fe:
        r2 = r1.mInstance;
        r2.close();
    L_0x0103:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaController2ImplBase.onConnectedNotLocked(android.support.v4.media.IMediaSession2, android.support.v4.media.SessionCommandGroup2, int, android.support.v4.media.MediaItem2, long, long, float, long, android.support.v4.media.MediaController2$PlaybackInfo, int, int, java.util.List, android.app.PendingIntent):void");
    }

    /* Access modifiers changed, original: 0000 */
    public void onCustomCommand(final SessionCommand2 command, final Bundle args, final ResultReceiver receiver) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCustomCommand cmd=");
            stringBuilder.append(command);
            Log.d(str, stringBuilder.toString());
        }
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                MediaController2ImplBase.this.mCallback.onCustomCommand(MediaController2ImplBase.this.mInstance, command, args, receiver);
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void onAllowedCommandsChanged(final SessionCommandGroup2 commands) {
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                MediaController2ImplBase.this.mCallback.onAllowedCommandsChanged(MediaController2ImplBase.this.mInstance, commands);
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public void onCustomLayoutChanged(final List<CommandButton> layout) {
        this.mCallbackExecutor.execute(new Runnable() {
            public void run() {
                MediaController2ImplBase.this.mCallback.onCustomLayoutChanged(MediaController2ImplBase.this.mInstance, layout);
            }
        });
    }
}
