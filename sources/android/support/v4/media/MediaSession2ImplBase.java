package android.support.v4.media;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.BaseMediaPlayer.PlayerEventCallback;
import android.support.v4.media.MediaController2.PlaybackInfo;
import android.support.v4.media.MediaMetadata2.Builder;
import android.support.v4.media.MediaPlaylistAgent.PlaylistEventCallback;
import android.support.v4.media.MediaSession2.CommandButton;
import android.support.v4.media.MediaSession2.ControllerInfo;
import android.support.v4.media.MediaSession2.OnDataSourceMissingHelper;
import android.support.v4.media.MediaSession2.SessionCallback;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.util.ObjectsCompat;
import android.text.TextUtils;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;

@TargetApi(19)
class MediaSession2ImplBase implements SupportLibraryImpl {
    static final boolean DEBUG = Log.isLoggable(TAG, 3);
    static final String TAG = "MS2ImplBase";
    private final AudioFocusHandler mAudioFocusHandler;
    private final AudioManager mAudioManager;
    private final SessionCallback mCallback;
    private final Executor mCallbackExecutor;
    private final Context mContext;
    @GuardedBy("mLock")
    private OnDataSourceMissingHelper mDsmHelper;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private final MediaSession2 mInstance;
    final Object mLock = new Object();
    @GuardedBy("mLock")
    private PlaybackInfo mPlaybackInfo;
    @GuardedBy("mLock")
    private BaseMediaPlayer mPlayer;
    private final PlayerEventCallback mPlayerEventCallback;
    @GuardedBy("mLock")
    private MediaPlaylistAgent mPlaylistAgent;
    private final PlaylistEventCallback mPlaylistEventCallback;
    private final MediaSession2Stub mSession2Stub;
    private final PendingIntent mSessionActivity;
    private final MediaSessionCompat mSessionCompat;
    private final MediaSessionLegacyStub mSessionLegacyStub;
    @GuardedBy("mLock")
    private SessionPlaylistAgentImplBase mSessionPlaylistAgent;
    private final SessionToken2 mSessionToken;
    @GuardedBy("mLock")
    private VolumeProviderCompat mVolumeProvider;

    @FunctionalInterface
    interface NotifyRunnable {
        void run(ControllerCb controllerCb) throws RemoteException;
    }

    private static class MyPlayerEventCallback extends PlayerEventCallback {
        private final WeakReference<MediaSession2ImplBase> mSession;

        /* synthetic */ MyPlayerEventCallback(MediaSession2ImplBase x0, AnonymousClass1 x1) {
            this(x0);
        }

        private MyPlayerEventCallback(MediaSession2ImplBase session) {
            this.mSession = new WeakReference(session);
        }

        public void onCurrentDataSourceChanged(final BaseMediaPlayer player, final DataSourceDesc dsd) {
            final MediaSession2ImplBase session = getSession();
            if (session != null) {
                session.getCallbackExecutor().execute(new Runnable() {
                    public void run() {
                        MediaItem2 item;
                        if (dsd == null) {
                            item = null;
                        } else {
                            item = MyPlayerEventCallback.this.getMediaItem(session, dsd);
                            if (item == null) {
                                String str = MediaSession2ImplBase.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Cannot obtain media item from the dsd=");
                                stringBuilder.append(dsd);
                                Log.w(str, stringBuilder.toString());
                                return;
                            }
                        }
                        session.getCallback().onCurrentMediaItemChanged(session.getInstance(), player, item);
                        session.notifyToAllControllers(new NotifyRunnable() {
                            public void run(ControllerCb callback) throws RemoteException {
                                callback.onCurrentMediaItemChanged(item);
                            }
                        });
                    }
                });
            }
        }

        public void onMediaPrepared(final BaseMediaPlayer mpb, final DataSourceDesc dsd) {
            final MediaSession2ImplBase session = getSession();
            if (session != null && dsd != null) {
                session.getCallbackExecutor().execute(new Runnable() {
                    public void run() {
                        MediaItem2 item = MyPlayerEventCallback.this.getMediaItem(session, dsd);
                        if (item != null) {
                            if (item.equals(session.getCurrentMediaItem())) {
                                long duration = session.getDuration();
                                if (duration >= 0) {
                                    MediaMetadata2 metadata = item.getMetadata();
                                    if (metadata == null) {
                                        metadata = new Builder().putLong("android.media.metadata.DURATION", duration).putString("android.media.metadata.MEDIA_ID", item.getMediaId()).build();
                                    } else if (metadata.containsKey("android.media.metadata.DURATION")) {
                                        long durationFromMetadata = metadata.getLong("android.media.metadata.DURATION");
                                        if (duration != durationFromMetadata) {
                                            String str = MediaSession2ImplBase.TAG;
                                            StringBuilder stringBuilder = new StringBuilder();
                                            stringBuilder.append("duration mismatch for an item. duration from player=");
                                            stringBuilder.append(duration);
                                            stringBuilder.append(" duration from metadata=");
                                            stringBuilder.append(durationFromMetadata);
                                            stringBuilder.append(". May be a timing issue?");
                                            Log.w(str, stringBuilder.toString());
                                        }
                                        metadata = null;
                                    } else {
                                        metadata = new Builder(metadata).putLong("android.media.metadata.DURATION", duration).build();
                                    }
                                    if (metadata != null) {
                                        item.setMetadata(metadata);
                                        session.notifyToAllControllers(new NotifyRunnable() {
                                            public void run(ControllerCb callback) throws RemoteException {
                                                callback.onPlaylistChanged(session.getPlaylist(), session.getPlaylistMetadata());
                                            }
                                        });
                                    }
                                } else {
                                    return;
                                }
                            }
                            session.getCallback().onMediaPrepared(session.getInstance(), mpb, item);
                        }
                    }
                });
            }
        }

        public void onPlayerStateChanged(final BaseMediaPlayer player, final int state) {
            final MediaSession2ImplBase session = getSession();
            if (session != null) {
                session.getCallbackExecutor().execute(new Runnable() {
                    public void run() {
                        session.mAudioFocusHandler.onPlayerStateChanged(state);
                        session.getCallback().onPlayerStateChanged(session.getInstance(), player, state);
                        session.notifyToAllControllers(new NotifyRunnable() {
                            public void run(ControllerCb callback) throws RemoteException {
                                callback.onPlayerStateChanged(SystemClock.elapsedRealtime(), player.getCurrentPosition(), state);
                            }
                        });
                    }
                });
            }
        }

        public void onBufferingStateChanged(BaseMediaPlayer mpb, DataSourceDesc dsd, int state) {
            MediaSession2ImplBase session = getSession();
            if (session != null && dsd != null) {
                final MediaSession2ImplBase mediaSession2ImplBase = session;
                final DataSourceDesc dataSourceDesc = dsd;
                final BaseMediaPlayer baseMediaPlayer = mpb;
                final int i = state;
                session.getCallbackExecutor().execute(new Runnable() {
                    public void run() {
                        final MediaItem2 item = MyPlayerEventCallback.this.getMediaItem(mediaSession2ImplBase, dataSourceDesc);
                        if (item != null) {
                            mediaSession2ImplBase.getCallback().onBufferingStateChanged(mediaSession2ImplBase.getInstance(), baseMediaPlayer, item, i);
                            mediaSession2ImplBase.notifyToAllControllers(new NotifyRunnable() {
                                public void run(ControllerCb callback) throws RemoteException {
                                    callback.onBufferingStateChanged(item, i, baseMediaPlayer.getBufferedPosition());
                                }
                            });
                        }
                    }
                });
            }
        }

        public void onPlaybackSpeedChanged(final BaseMediaPlayer mpb, final float speed) {
            final MediaSession2ImplBase session = getSession();
            if (session != null) {
                session.getCallbackExecutor().execute(new Runnable() {
                    public void run() {
                        session.getCallback().onPlaybackSpeedChanged(session.getInstance(), mpb, speed);
                        session.notifyToAllControllers(new NotifyRunnable() {
                            public void run(ControllerCb callback) throws RemoteException {
                                callback.onPlaybackSpeedChanged(SystemClock.elapsedRealtime(), session.getCurrentPosition(), speed);
                            }
                        });
                    }
                });
            }
        }

        public void onSeekCompleted(BaseMediaPlayer mpb, long position) {
            MediaSession2ImplBase session = getSession();
            if (session != null) {
                final MediaSession2ImplBase mediaSession2ImplBase = session;
                final BaseMediaPlayer baseMediaPlayer = mpb;
                final long j = position;
                session.getCallbackExecutor().execute(new Runnable() {
                    public void run() {
                        mediaSession2ImplBase.getCallback().onSeekCompleted(mediaSession2ImplBase.getInstance(), baseMediaPlayer, j);
                        mediaSession2ImplBase.notifyToAllControllers(new NotifyRunnable() {
                            public void run(ControllerCb callback) throws RemoteException {
                                callback.onSeekCompleted(SystemClock.elapsedRealtime(), mediaSession2ImplBase.getCurrentPosition(), j);
                            }
                        });
                    }
                });
            }
        }

        private MediaSession2ImplBase getSession() {
            MediaSession2ImplBase session = (MediaSession2ImplBase) this.mSession.get();
            if (session == null && MediaSession2ImplBase.DEBUG) {
                Log.d(MediaSession2ImplBase.TAG, "Session is closed", new IllegalStateException());
            }
            return session;
        }

        private MediaItem2 getMediaItem(MediaSession2ImplBase session, DataSourceDesc dsd) {
            MediaPlaylistAgent agent = session.getPlaylistAgent();
            if (agent == null) {
                if (MediaSession2ImplBase.DEBUG) {
                    Log.d(MediaSession2ImplBase.TAG, "Session is closed", new IllegalStateException());
                }
                return null;
            }
            MediaItem2 item = agent.getMediaItem(dsd);
            if (item == null && MediaSession2ImplBase.DEBUG) {
                String str = MediaSession2ImplBase.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Could not find matching item for dsd=");
                stringBuilder.append(dsd);
                Log.d(str, stringBuilder.toString(), new NoSuchElementException());
            }
            return item;
        }
    }

    private static class MyPlaylistEventCallback extends PlaylistEventCallback {
        private final WeakReference<MediaSession2ImplBase> mSession;

        /* synthetic */ MyPlaylistEventCallback(MediaSession2ImplBase x0, AnonymousClass1 x1) {
            this(x0);
        }

        private MyPlaylistEventCallback(MediaSession2ImplBase session) {
            this.mSession = new WeakReference(session);
        }

        public void onPlaylistChanged(MediaPlaylistAgent playlistAgent, List<MediaItem2> list, MediaMetadata2 metadata) {
            MediaSession2ImplBase session = (MediaSession2ImplBase) this.mSession.get();
            if (session != null) {
                session.notifyPlaylistChangedOnExecutor(playlistAgent, list, metadata);
            }
        }

        public void onPlaylistMetadataChanged(MediaPlaylistAgent playlistAgent, MediaMetadata2 metadata) {
            MediaSession2ImplBase session = (MediaSession2ImplBase) this.mSession.get();
            if (session != null) {
                session.notifyPlaylistMetadataChangedOnExecutor(playlistAgent, metadata);
            }
        }

        public void onRepeatModeChanged(MediaPlaylistAgent playlistAgent, int repeatMode) {
            MediaSession2ImplBase session = (MediaSession2ImplBase) this.mSession.get();
            if (session != null) {
                session.notifyRepeatModeChangedOnExecutor(playlistAgent, repeatMode);
            }
        }

        public void onShuffleModeChanged(MediaPlaylistAgent playlistAgent, int shuffleMode) {
            MediaSession2ImplBase session = (MediaSession2ImplBase) this.mSession.get();
            if (session != null) {
                session.notifyShuffleModeChangedOnExecutor(playlistAgent, shuffleMode);
            }
        }
    }

    MediaSession2ImplBase(MediaSession2 instance, Context context, String id, BaseMediaPlayer player, MediaPlaylistAgent playlistAgent, VolumeProviderCompat volumeProvider, PendingIntent sessionActivity, Executor callbackExecutor, SessionCallback callback) {
        Context context2 = context;
        String str = id;
        PendingIntent pendingIntent = sessionActivity;
        this.mContext = context2;
        this.mInstance = instance;
        this.mHandlerThread = new HandlerThread("MediaController2_Thread");
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
        this.mSession2Stub = new MediaSession2Stub(this);
        this.mSessionLegacyStub = new MediaSessionLegacyStub(this);
        this.mSessionActivity = pendingIntent;
        this.mCallback = callback;
        this.mCallbackExecutor = callbackExecutor;
        this.mAudioManager = (AudioManager) context2.getSystemService("audio");
        this.mPlayerEventCallback = new MyPlayerEventCallback(this, null);
        this.mPlaylistEventCallback = new MyPlaylistEventCallback(this, null);
        this.mAudioFocusHandler = new AudioFocusHandler(context2, getInstance());
        String libraryService = getServiceName(context2, MediaLibraryService2.SERVICE_INTERFACE, str);
        String sessionService = getServiceName(context2, MediaSessionService2.SERVICE_INTERFACE, str);
        if (sessionService == null || libraryService == null) {
            SessionToken2ImplBase sessionToken2ImplBase;
            if (libraryService != null) {
                SessionToken2ImplBase sessionToken2ImplBase2 = sessionToken2ImplBase;
                sessionToken2ImplBase = new SessionToken2ImplBase(Process.myUid(), 2, context.getPackageName(), libraryService, str, this.mSession2Stub);
                this.mSessionToken = new SessionToken2(sessionToken2ImplBase2);
            } else if (sessionService != null) {
                SessionToken2ImplBase sessionToken2ImplBase3 = sessionToken2ImplBase;
                sessionToken2ImplBase = new SessionToken2ImplBase(Process.myUid(), 1, context.getPackageName(), sessionService, str, this.mSession2Stub);
                this.mSessionToken = new SessionToken2(sessionToken2ImplBase3);
            } else {
                this.mSessionToken = new SessionToken2(new SessionToken2ImplBase(Process.myUid(), 0, context.getPackageName(), null, str, this.mSession2Stub));
            }
            this.mSessionCompat = new MediaSessionCompat(context2, str, this.mSessionToken);
            this.mSessionCompat.setCallback(this.mSessionLegacyStub, this.mHandler);
            this.mSessionCompat.setSessionActivity(pendingIntent);
            updatePlayer(player, playlistAgent, volumeProvider);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ambiguous session type. Multiple session services define the same id=");
        stringBuilder.append(str);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:29:0x004b, code skipped:
            if (r13 != null) goto L_0x005a;
     */
    /* JADX WARNING: Missing block: B:30:0x004d, code skipped:
            r10.mSessionCompat.setPlaybackToLocal(getLegacyStreamType(r11.getAudioAttributes()));
     */
    /* JADX WARNING: Missing block: B:31:0x005a, code skipped:
            if (r11 == r5) goto L_0x006a;
     */
    /* JADX WARNING: Missing block: B:32:0x005c, code skipped:
            r11.registerPlayerEventCallback(r10.mCallbackExecutor, r10.mPlayerEventCallback);
     */
    /* JADX WARNING: Missing block: B:33:0x0063, code skipped:
            if (r5 == null) goto L_0x006a;
     */
    /* JADX WARNING: Missing block: B:34:0x0065, code skipped:
            r5.unregisterPlayerEventCallback(r10.mPlayerEventCallback);
     */
    /* JADX WARNING: Missing block: B:35:0x006a, code skipped:
            if (r12 == r2) goto L_0x007a;
     */
    /* JADX WARNING: Missing block: B:36:0x006c, code skipped:
            r12.registerPlaylistEventCallback(r10.mCallbackExecutor, r10.mPlaylistEventCallback);
     */
    /* JADX WARNING: Missing block: B:37:0x0073, code skipped:
            if (r2 == null) goto L_0x007a;
     */
    /* JADX WARNING: Missing block: B:38:0x0075, code skipped:
            r2.unregisterPlaylistEventCallback(r10.mPlaylistEventCallback);
     */
    /* JADX WARNING: Missing block: B:39:0x007a, code skipped:
            if (r5 == null) goto L_?;
     */
    /* JADX WARNING: Missing block: B:40:0x007c, code skipped:
            if (r6 == false) goto L_0x0081;
     */
    /* JADX WARNING: Missing block: B:41:0x007e, code skipped:
            notifyAgentUpdatedNotLocked(r2);
     */
    /* JADX WARNING: Missing block: B:42:0x0081, code skipped:
            if (r4 == false) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:43:0x0083, code skipped:
            notifyPlayerUpdatedNotLocked(r5);
     */
    /* JADX WARNING: Missing block: B:44:0x0086, code skipped:
            if (r3 == null) goto L_?;
     */
    /* JADX WARNING: Missing block: B:45:0x0088, code skipped:
            notifyToAllControllers(new android.support.v4.media.MediaSession2ImplBase.AnonymousClass1(r10));
     */
    /* JADX WARNING: Missing block: B:64:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:65:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:66:?, code skipped:
            return;
     */
    public void updatePlayer(@android.support.annotation.NonNull android.support.v4.media.BaseMediaPlayer r11, @android.support.annotation.Nullable android.support.v4.media.MediaPlaylistAgent r12, @android.support.annotation.Nullable android.support.v4.media.VolumeProviderCompat r13) {
        /*
        r10 = this;
        if (r11 == 0) goto L_0x00a6;
    L_0x0002:
        r0 = r11.getAudioAttributes();
        r0 = r10.createPlaybackInfo(r13, r0);
        r1 = r10.mLock;
        monitor-enter(r1);
        r2 = 0;
        r3 = 0;
        r4 = r10.mPlayer;	 Catch:{ all -> 0x009d }
        r5 = 1;
        if (r4 == r11) goto L_0x0016;
    L_0x0014:
        r4 = r5;
        goto L_0x0017;
    L_0x0016:
        r4 = r3;
    L_0x0017:
        r6 = r10.mPlaylistAgent;	 Catch:{ all -> 0x009a }
        if (r6 == r12) goto L_0x001d;
    L_0x001b:
        r6 = r5;
        goto L_0x001e;
    L_0x001d:
        r6 = r3;
    L_0x001e:
        r7 = r10.mPlaybackInfo;	 Catch:{ all -> 0x0097 }
        if (r7 == r0) goto L_0x0024;
    L_0x0022:
        r3 = r5;
    L_0x0024:
        r5 = r10.mPlayer;	 Catch:{ all -> 0x0097 }
        r7 = r10.mPlaylistAgent;	 Catch:{ all -> 0x0091 }
        r2 = r7;
        r10.mPlayer = r11;	 Catch:{ all -> 0x0091 }
        if (r12 != 0) goto L_0x0044;
    L_0x002d:
        r7 = new android.support.v4.media.SessionPlaylistAgentImplBase;	 Catch:{ all -> 0x0091 }
        r8 = r10.mPlayer;	 Catch:{ all -> 0x0091 }
        r7.<init>(r10, r8);	 Catch:{ all -> 0x0091 }
        r10.mSessionPlaylistAgent = r7;	 Catch:{ all -> 0x0091 }
        r7 = r10.mDsmHelper;	 Catch:{ all -> 0x0091 }
        if (r7 == 0) goto L_0x0041;
    L_0x003a:
        r7 = r10.mSessionPlaylistAgent;	 Catch:{ all -> 0x0091 }
        r8 = r10.mDsmHelper;	 Catch:{ all -> 0x0091 }
        r7.setOnDataSourceMissingHelper(r8);	 Catch:{ all -> 0x0091 }
    L_0x0041:
        r7 = r10.mSessionPlaylistAgent;	 Catch:{ all -> 0x0091 }
        r12 = r7;
    L_0x0044:
        r10.mPlaylistAgent = r12;	 Catch:{ all -> 0x0091 }
        r10.mVolumeProvider = r13;	 Catch:{ all -> 0x0091 }
        r10.mPlaybackInfo = r0;	 Catch:{ all -> 0x0091 }
        monitor-exit(r1);	 Catch:{ all -> 0x0091 }
        if (r13 != 0) goto L_0x005a;
    L_0x004d:
        r1 = r11.getAudioAttributes();
        r1 = r10.getLegacyStreamType(r1);
        r7 = r10.mSessionCompat;
        r7.setPlaybackToLocal(r1);
    L_0x005a:
        if (r11 == r5) goto L_0x006a;
    L_0x005c:
        r1 = r10.mCallbackExecutor;
        r7 = r10.mPlayerEventCallback;
        r11.registerPlayerEventCallback(r1, r7);
        if (r5 == 0) goto L_0x006a;
    L_0x0065:
        r1 = r10.mPlayerEventCallback;
        r5.unregisterPlayerEventCallback(r1);
    L_0x006a:
        if (r12 == r2) goto L_0x007a;
    L_0x006c:
        r1 = r10.mCallbackExecutor;
        r7 = r10.mPlaylistEventCallback;
        r12.registerPlaylistEventCallback(r1, r7);
        if (r2 == 0) goto L_0x007a;
    L_0x0075:
        r1 = r10.mPlaylistEventCallback;
        r2.unregisterPlaylistEventCallback(r1);
    L_0x007a:
        if (r5 == 0) goto L_0x0090;
    L_0x007c:
        if (r6 == 0) goto L_0x0081;
    L_0x007e:
        r10.notifyAgentUpdatedNotLocked(r2);
    L_0x0081:
        if (r4 == 0) goto L_0x0086;
    L_0x0083:
        r10.notifyPlayerUpdatedNotLocked(r5);
    L_0x0086:
        if (r3 == 0) goto L_0x0090;
    L_0x0088:
        r1 = new android.support.v4.media.MediaSession2ImplBase$1;
        r1.<init>(r0);
        r10.notifyToAllControllers(r1);
    L_0x0090:
        return;
    L_0x0091:
        r7 = move-exception;
        r9 = r3;
        r3 = r2;
        r2 = r5;
        r5 = r9;
        goto L_0x00a2;
    L_0x0097:
        r7 = move-exception;
        r5 = r3;
        goto L_0x00a1;
    L_0x009a:
        r7 = move-exception;
        r5 = r3;
        goto L_0x00a0;
    L_0x009d:
        r7 = move-exception;
        r4 = r3;
        r5 = r4;
    L_0x00a0:
        r6 = r5;
    L_0x00a1:
        r3 = r2;
    L_0x00a2:
        monitor-exit(r1);	 Catch:{ all -> 0x00a4 }
        throw r7;
    L_0x00a4:
        r7 = move-exception;
        goto L_0x00a2;
    L_0x00a6:
        r0 = new java.lang.IllegalArgumentException;
        r1 = "player shouldn't be null";
        r0.<init>(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.updatePlayer(android.support.v4.media.BaseMediaPlayer, android.support.v4.media.MediaPlaylistAgent, android.support.v4.media.VolumeProviderCompat):void");
    }

    private PlaybackInfo createPlaybackInfo(VolumeProviderCompat volumeProvider, AudioAttributesCompat attrs) {
        if (volumeProvider != null) {
            return PlaybackInfo.createPlaybackInfo(2, attrs, volumeProvider.getVolumeControl(), volumeProvider.getMaxVolume(), volumeProvider.getCurrentVolume());
        }
        int stream = getLegacyStreamType(attrs);
        int controlType = 2;
        if (VERSION.SDK_INT >= 21 && this.mAudioManager.isVolumeFixed()) {
            controlType = 0;
        }
        return PlaybackInfo.createPlaybackInfo(1, attrs, controlType, this.mAudioManager.getStreamMaxVolume(stream), this.mAudioManager.getStreamVolume(stream));
    }

    private int getLegacyStreamType(@Nullable AudioAttributesCompat attrs) {
        if (attrs == null) {
            return 3;
        }
        int stream = attrs.getLegacyStreamType();
        if (stream == Integer.MIN_VALUE) {
            return 3;
        }
        return stream;
    }

    /* JADX WARNING: Missing block: B:14:0x0044, code skipped:
            return;
     */
    public void close() {
        /*
        r3 = this;
        r0 = r3.mLock;
        monitor-enter(r0);
        r1 = r3.mPlayer;	 Catch:{ all -> 0x0045 }
        if (r1 != 0) goto L_0x0009;
    L_0x0007:
        monitor-exit(r0);	 Catch:{ all -> 0x0045 }
        return;
    L_0x0009:
        r1 = r3.mAudioFocusHandler;	 Catch:{ all -> 0x0045 }
        r1.close();	 Catch:{ all -> 0x0045 }
        r1 = r3.mPlayer;	 Catch:{ all -> 0x0045 }
        r2 = r3.mPlayerEventCallback;	 Catch:{ all -> 0x0045 }
        r1.unregisterPlayerEventCallback(r2);	 Catch:{ all -> 0x0045 }
        r1 = 0;
        r3.mPlayer = r1;	 Catch:{ all -> 0x0045 }
        r2 = r3.mSessionCompat;	 Catch:{ all -> 0x0045 }
        r2.release();	 Catch:{ all -> 0x0045 }
        r2 = new android.support.v4.media.MediaSession2ImplBase$2;	 Catch:{ all -> 0x0045 }
        r2.<init>();	 Catch:{ all -> 0x0045 }
        r3.notifyToAllControllers(r2);	 Catch:{ all -> 0x0045 }
        r2 = r3.mHandler;	 Catch:{ all -> 0x0045 }
        r2.removeCallbacksAndMessages(r1);	 Catch:{ all -> 0x0045 }
        r1 = r3.mHandlerThread;	 Catch:{ all -> 0x0045 }
        r1 = r1.isAlive();	 Catch:{ all -> 0x0045 }
        if (r1 == 0) goto L_0x0043;
    L_0x0032:
        r1 = android.os.Build.VERSION.SDK_INT;	 Catch:{ all -> 0x0045 }
        r2 = 18;
        if (r1 < r2) goto L_0x003e;
    L_0x0038:
        r1 = r3.mHandlerThread;	 Catch:{ all -> 0x0045 }
        r1.quitSafely();	 Catch:{ all -> 0x0045 }
        goto L_0x0043;
    L_0x003e:
        r1 = r3.mHandlerThread;	 Catch:{ all -> 0x0045 }
        r1.quit();	 Catch:{ all -> 0x0045 }
    L_0x0043:
        monitor-exit(r0);	 Catch:{ all -> 0x0045 }
        return;
    L_0x0045:
        r1 = move-exception;
        monitor-exit(r0);	 Catch:{ all -> 0x0045 }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.close():void");
    }

    @NonNull
    public BaseMediaPlayer getPlayer() {
        BaseMediaPlayer baseMediaPlayer;
        synchronized (this.mLock) {
            baseMediaPlayer = this.mPlayer;
        }
        return baseMediaPlayer;
    }

    @NonNull
    public MediaPlaylistAgent getPlaylistAgent() {
        MediaPlaylistAgent mediaPlaylistAgent;
        synchronized (this.mLock) {
            mediaPlaylistAgent = this.mPlaylistAgent;
        }
        return mediaPlaylistAgent;
    }

    @Nullable
    public VolumeProviderCompat getVolumeProvider() {
        VolumeProviderCompat volumeProviderCompat;
        synchronized (this.mLock) {
            volumeProviderCompat = this.mVolumeProvider;
        }
        return volumeProviderCompat;
    }

    @NonNull
    public SessionToken2 getToken() {
        return this.mSessionToken;
    }

    @NonNull
    public List<ControllerInfo> getConnectedControllers() {
        return this.mSession2Stub.getConnectedControllers();
    }

    public void setCustomLayout(@NonNull ControllerInfo controller, @NonNull final List<CommandButton> layout) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        } else if (layout != null) {
            notifyToController(controller, new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onCustomLayoutChanged(layout);
                }
            });
        } else {
            throw new IllegalArgumentException("layout shouldn't be null");
        }
    }

    public void setAllowedCommands(@NonNull ControllerInfo controller, @NonNull final SessionCommandGroup2 commands) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        } else if (commands != null) {
            this.mSession2Stub.setAllowedCommands(controller, commands);
            notifyToController(controller, new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onAllowedCommandsChanged(commands);
                }
            });
        } else {
            throw new IllegalArgumentException("commands shouldn't be null");
        }
    }

    public void sendCustomCommand(@NonNull final SessionCommand2 command, @Nullable final Bundle args) {
        if (command != null) {
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onCustomCommand(command, args, null);
                }
            });
            return;
        }
        throw new IllegalArgumentException("command shouldn't be null");
    }

    public void sendCustomCommand(@NonNull ControllerInfo controller, @NonNull final SessionCommand2 command, @Nullable final Bundle args, @Nullable final ResultReceiver receiver) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        } else if (command != null) {
            notifyToController(controller, new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onCustomCommand(command, args, receiver);
                }
            });
        } else {
            throw new IllegalArgumentException("command shouldn't be null");
        }
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x001c;
     */
    /* JADX WARNING: Missing block: B:8:0x000e, code skipped:
            if (r4.mAudioFocusHandler.onPlayRequested() == false) goto L_0x0014;
     */
    /* JADX WARNING: Missing block: B:9:0x0010, code skipped:
            r1.play();
     */
    /* JADX WARNING: Missing block: B:10:0x0014, code skipped:
            android.util.Log.w(TAG, "play() wouldn't be called because of the failure in audio focus");
     */
    /* JADX WARNING: Missing block: B:12:0x001e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:13:0x0020, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            return;
     */
    public void play() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlayer;	 Catch:{ all -> 0x002f }
        monitor-exit(r0);	 Catch:{ all -> 0x002d }
        if (r1 == 0) goto L_0x001c;
    L_0x0008:
        r0 = r4.mAudioFocusHandler;
        r0 = r0.onPlayRequested();
        if (r0 == 0) goto L_0x0014;
    L_0x0010:
        r1.play();
        goto L_0x002c;
    L_0x0014:
        r0 = "MS2ImplBase";
        r2 = "play() wouldn't be called because of the failure in audio focus";
        android.util.Log.w(r0, r2);
        goto L_0x002c;
    L_0x001c:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x002c;
    L_0x0020:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x002c:
        return;
    L_0x002d:
        r2 = move-exception;
        goto L_0x0031;
    L_0x002f:
        r2 = move-exception;
        r1 = 0;
    L_0x0031:
        monitor-exit(r0);	 Catch:{ all -> 0x002d }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.play():void");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x001c;
     */
    /* JADX WARNING: Missing block: B:8:0x000e, code skipped:
            if (r4.mAudioFocusHandler.onPauseRequested() == false) goto L_0x0014;
     */
    /* JADX WARNING: Missing block: B:9:0x0010, code skipped:
            r1.pause();
     */
    /* JADX WARNING: Missing block: B:10:0x0014, code skipped:
            android.util.Log.w(TAG, "pause() wouldn't be called of the failure in audio focus");
     */
    /* JADX WARNING: Missing block: B:12:0x001e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:13:0x0020, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            return;
     */
    public void pause() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlayer;	 Catch:{ all -> 0x002f }
        monitor-exit(r0);	 Catch:{ all -> 0x002d }
        if (r1 == 0) goto L_0x001c;
    L_0x0008:
        r0 = r4.mAudioFocusHandler;
        r0 = r0.onPauseRequested();
        if (r0 == 0) goto L_0x0014;
    L_0x0010:
        r1.pause();
        goto L_0x002c;
    L_0x0014:
        r0 = "MS2ImplBase";
        r2 = "pause() wouldn't be called of the failure in audio focus";
        android.util.Log.w(r0, r2);
        goto L_0x002c;
    L_0x001c:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x002c;
    L_0x0020:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x002c:
        return;
    L_0x002d:
        r2 = move-exception;
        goto L_0x0031;
    L_0x002f:
        r2 = move-exception;
        r1 = 0;
    L_0x0031:
        monitor-exit(r0);	 Catch:{ all -> 0x002d }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.pause():void");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.reset();
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
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
    public void reset() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlayer;	 Catch:{ all -> 0x001f }
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        if (r1 == 0) goto L_0x000c;
    L_0x0008:
        r1.reset();
        goto L_0x001c;
    L_0x000c:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001c;
    L_0x0010:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001c:
        return;
    L_0x001d:
        r2 = move-exception;
        goto L_0x0021;
    L_0x001f:
        r2 = move-exception;
        r1 = 0;
    L_0x0021:
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.reset():void");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.prepare();
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
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
    public void prepare() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlayer;	 Catch:{ all -> 0x001f }
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        if (r1 == 0) goto L_0x000c;
    L_0x0008:
        r1.prepare();
        goto L_0x001c;
    L_0x000c:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001c;
    L_0x0010:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001c:
        return;
    L_0x001d:
        r2 = move-exception;
        goto L_0x0021;
    L_0x001f:
        r2 = move-exception;
        r1 = 0;
    L_0x0021:
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.prepare():void");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.seekTo(r5);
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
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
    public void seekTo(long r5) {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlayer;	 Catch:{ all -> 0x001f }
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        if (r1 == 0) goto L_0x000c;
    L_0x0008:
        r1.seekTo(r5);
        goto L_0x001c;
    L_0x000c:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001c;
    L_0x0010:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001c:
        return;
    L_0x001d:
        r2 = move-exception;
        goto L_0x0021;
    L_0x001f:
        r2 = move-exception;
        r1 = 0;
    L_0x0021:
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.seekTo(long):void");
    }

    public void skipForward() {
    }

    public void skipBackward() {
    }

    public void notifyError(final int errorCode, @Nullable final Bundle extras) {
        notifyToAllControllers(new NotifyRunnable() {
            public void run(ControllerCb callback) throws RemoteException {
                callback.onError(errorCode, extras);
            }
        });
    }

    public void notifyRoutesInfoChanged(@NonNull ControllerInfo controller, @Nullable final List<Bundle> routes) {
        notifyToController(controller, new NotifyRunnable() {
            public void run(ControllerCb callback) throws RemoteException {
                callback.onRoutesInfoChanged(routes);
            }
        });
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getPlayerState();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return 3;
     */
    public int getPlayerState() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlayer;	 Catch:{ all -> 0x0021 }
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        if (r1 == 0) goto L_0x000d;
    L_0x0008:
        r0 = r1.getPlayerState();
        return r0;
    L_0x000d:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001d;
    L_0x0011:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001d:
        r0 = 3;
        return r0;
    L_0x001f:
        r2 = move-exception;
        goto L_0x0023;
    L_0x0021:
        r2 = move-exception;
        r1 = 0;
    L_0x0023:
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.getPlayerState():int");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getCurrentPosition();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001f, code skipped:
            return -1;
     */
    public long getCurrentPosition() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlayer;	 Catch:{ all -> 0x0022 }
        monitor-exit(r0);	 Catch:{ all -> 0x0020 }
        if (r1 == 0) goto L_0x000d;
    L_0x0008:
        r2 = r1.getCurrentPosition();
        return r2;
    L_0x000d:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001d;
    L_0x0011:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001d:
        r2 = -1;
        return r2;
    L_0x0020:
        r2 = move-exception;
        goto L_0x0024;
    L_0x0022:
        r2 = move-exception;
        r1 = 0;
    L_0x0024:
        monitor-exit(r0);	 Catch:{ all -> 0x0020 }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.getCurrentPosition():long");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getDuration();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001f, code skipped:
            return -1;
     */
    public long getDuration() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlayer;	 Catch:{ all -> 0x0022 }
        monitor-exit(r0);	 Catch:{ all -> 0x0020 }
        if (r1 == 0) goto L_0x000d;
    L_0x0008:
        r2 = r1.getDuration();
        return r2;
    L_0x000d:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001d;
    L_0x0011:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001d:
        r2 = -1;
        return r2;
    L_0x0020:
        r2 = move-exception;
        goto L_0x0024;
    L_0x0022:
        r2 = move-exception;
        r1 = 0;
    L_0x0024:
        monitor-exit(r0);	 Catch:{ all -> 0x0020 }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.getDuration():long");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getBufferedPosition();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001f, code skipped:
            return -1;
     */
    public long getBufferedPosition() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlayer;	 Catch:{ all -> 0x0022 }
        monitor-exit(r0);	 Catch:{ all -> 0x0020 }
        if (r1 == 0) goto L_0x000d;
    L_0x0008:
        r2 = r1.getBufferedPosition();
        return r2;
    L_0x000d:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001d;
    L_0x0011:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001d:
        r2 = -1;
        return r2;
    L_0x0020:
        r2 = move-exception;
        goto L_0x0024;
    L_0x0022:
        r2 = move-exception;
        r1 = 0;
    L_0x0024:
        monitor-exit(r0);	 Catch:{ all -> 0x0020 }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.getBufferedPosition():long");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getBufferingState();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return 0;
     */
    public int getBufferingState() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlayer;	 Catch:{ all -> 0x0021 }
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        if (r1 == 0) goto L_0x000d;
    L_0x0008:
        r0 = r1.getBufferingState();
        return r0;
    L_0x000d:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001d;
    L_0x0011:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001d:
        r0 = 0;
        return r0;
    L_0x001f:
        r2 = move-exception;
        goto L_0x0023;
    L_0x0021:
        r2 = move-exception;
        r1 = 0;
    L_0x0023:
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.getBufferingState():int");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getPlaybackSpeed();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001f, code skipped:
            return 1.0f;
     */
    public float getPlaybackSpeed() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlayer;	 Catch:{ all -> 0x0022 }
        monitor-exit(r0);	 Catch:{ all -> 0x0020 }
        if (r1 == 0) goto L_0x000d;
    L_0x0008:
        r0 = r1.getPlaybackSpeed();
        return r0;
    L_0x000d:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001d;
    L_0x0011:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001d:
        r0 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        return r0;
    L_0x0020:
        r2 = move-exception;
        goto L_0x0024;
    L_0x0022:
        r2 = move-exception;
        r1 = 0;
    L_0x0024:
        monitor-exit(r0);	 Catch:{ all -> 0x0020 }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.getPlaybackSpeed():float");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.setPlaybackSpeed(r5);
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
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
    public void setPlaybackSpeed(float r5) {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlayer;	 Catch:{ all -> 0x001f }
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        if (r1 == 0) goto L_0x000c;
    L_0x0008:
        r1.setPlaybackSpeed(r5);
        goto L_0x001c;
    L_0x000c:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001c;
    L_0x0010:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001c:
        return;
    L_0x001d:
        r2 = move-exception;
        goto L_0x0021;
    L_0x001f:
        r2 = move-exception;
        r1 = 0;
    L_0x0021:
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.setPlaybackSpeed(float):void");
    }

    public void setOnDataSourceMissingHelper(@NonNull OnDataSourceMissingHelper helper) {
        if (helper != null) {
            synchronized (this.mLock) {
                this.mDsmHelper = helper;
                if (this.mSessionPlaylistAgent != null) {
                    this.mSessionPlaylistAgent.setOnDataSourceMissingHelper(helper);
                }
            }
            return;
        }
        throw new IllegalArgumentException("helper shouldn't be null");
    }

    public void clearOnDataSourceMissingHelper() {
        synchronized (this.mLock) {
            this.mDsmHelper = null;
            if (this.mSessionPlaylistAgent != null) {
                this.mSessionPlaylistAgent.clearOnDataSourceMissingHelper();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0007, code skipped:
            if (r2 == null) goto L_0x000e;
     */
    /* JADX WARNING: Missing block: B:9:0x000d, code skipped:
            return r2.getPlaylist();
     */
    /* JADX WARNING: Missing block: B:11:0x0010, code skipped:
            if (DEBUG == false) goto L_0x001e;
     */
    /* JADX WARNING: Missing block: B:12:0x0012, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return null;
     */
    public java.util.List<android.support.v4.media.MediaItem2> getPlaylist() {
        /*
        r6 = this;
        r0 = r6.mLock;
        monitor-enter(r0);
        r1 = 0;
        r2 = r6.mPlaylistAgent;	 Catch:{ all -> 0x0021 }
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        if (r2 == 0) goto L_0x000e;
    L_0x0009:
        r0 = r2.getPlaylist();
        return r0;
    L_0x000e:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001e;
    L_0x0012:
        r0 = "MS2ImplBase";
        r3 = "API calls after the close()";
        r4 = new java.lang.IllegalStateException;
        r4.<init>();
        android.util.Log.d(r0, r3, r4);
    L_0x001e:
        return r1;
    L_0x001f:
        r1 = move-exception;
        goto L_0x0025;
    L_0x0021:
        r2 = move-exception;
        r5 = r2;
        r2 = r1;
        r1 = r5;
    L_0x0025:
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.getPlaylist():java.util.List");
    }

    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            if (r1 == null) goto L_0x000e;
     */
    /* JADX WARNING: Missing block: B:8:0x000a, code skipped:
            r1.setPlaylist(r5, r6);
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:11:0x0012, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            return;
     */
    public void setPlaylist(@android.support.annotation.NonNull java.util.List<android.support.v4.media.MediaItem2> r5, @android.support.annotation.Nullable android.support.v4.media.MediaMetadata2 r6) {
        /*
        r4 = this;
        if (r5 == 0) goto L_0x0025;
    L_0x0002:
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlaylistAgent;	 Catch:{ all -> 0x0021 }
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        if (r1 == 0) goto L_0x000e;
    L_0x000a:
        r1.setPlaylist(r5, r6);
        goto L_0x001e;
    L_0x000e:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001e;
    L_0x0012:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001e:
        return;
    L_0x001f:
        r2 = move-exception;
        goto L_0x0023;
    L_0x0021:
        r2 = move-exception;
        r1 = 0;
    L_0x0023:
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        throw r2;
    L_0x0025:
        r0 = new java.lang.IllegalArgumentException;
        r1 = "list shouldn't be null";
        r0.<init>(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.setPlaylist(java.util.List, android.support.v4.media.MediaMetadata2):void");
    }

    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            if (r1 == null) goto L_0x000e;
     */
    /* JADX WARNING: Missing block: B:8:0x000a, code skipped:
            r1.skipToPlaylistItem(r5);
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:11:0x0012, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            return;
     */
    public void skipToPlaylistItem(@android.support.annotation.NonNull android.support.v4.media.MediaItem2 r5) {
        /*
        r4 = this;
        if (r5 == 0) goto L_0x0025;
    L_0x0002:
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlaylistAgent;	 Catch:{ all -> 0x0021 }
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        if (r1 == 0) goto L_0x000e;
    L_0x000a:
        r1.skipToPlaylistItem(r5);
        goto L_0x001e;
    L_0x000e:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001e;
    L_0x0012:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001e:
        return;
    L_0x001f:
        r2 = move-exception;
        goto L_0x0023;
    L_0x0021:
        r2 = move-exception;
        r1 = 0;
    L_0x0023:
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        throw r2;
    L_0x0025:
        r0 = new java.lang.IllegalArgumentException;
        r1 = "item shouldn't be null";
        r0.<init>(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.skipToPlaylistItem(android.support.v4.media.MediaItem2):void");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.skipToPreviousItem();
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
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
    public void skipToPreviousItem() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlaylistAgent;	 Catch:{ all -> 0x001f }
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        if (r1 == 0) goto L_0x000c;
    L_0x0008:
        r1.skipToPreviousItem();
        goto L_0x001c;
    L_0x000c:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001c;
    L_0x0010:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001c:
        return;
    L_0x001d:
        r2 = move-exception;
        goto L_0x0021;
    L_0x001f:
        r2 = move-exception;
        r1 = 0;
    L_0x0021:
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.skipToPreviousItem():void");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.skipToNextItem();
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
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
    public void skipToNextItem() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlaylistAgent;	 Catch:{ all -> 0x001f }
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        if (r1 == 0) goto L_0x000c;
    L_0x0008:
        r1.skipToNextItem();
        goto L_0x001c;
    L_0x000c:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001c;
    L_0x0010:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001c:
        return;
    L_0x001d:
        r2 = move-exception;
        goto L_0x0021;
    L_0x001f:
        r2 = move-exception;
        r1 = 0;
    L_0x0021:
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.skipToNextItem():void");
    }

    /* JADX WARNING: Missing block: B:7:0x0007, code skipped:
            if (r2 == null) goto L_0x000e;
     */
    /* JADX WARNING: Missing block: B:9:0x000d, code skipped:
            return r2.getPlaylistMetadata();
     */
    /* JADX WARNING: Missing block: B:11:0x0010, code skipped:
            if (DEBUG == false) goto L_0x001e;
     */
    /* JADX WARNING: Missing block: B:12:0x0012, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return null;
     */
    public android.support.v4.media.MediaMetadata2 getPlaylistMetadata() {
        /*
        r6 = this;
        r0 = r6.mLock;
        monitor-enter(r0);
        r1 = 0;
        r2 = r6.mPlaylistAgent;	 Catch:{ all -> 0x0021 }
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        if (r2 == 0) goto L_0x000e;
    L_0x0009:
        r0 = r2.getPlaylistMetadata();
        return r0;
    L_0x000e:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001e;
    L_0x0012:
        r0 = "MS2ImplBase";
        r3 = "API calls after the close()";
        r4 = new java.lang.IllegalStateException;
        r4.<init>();
        android.util.Log.d(r0, r3, r4);
    L_0x001e:
        return r1;
    L_0x001f:
        r1 = move-exception;
        goto L_0x0025;
    L_0x0021:
        r2 = move-exception;
        r5 = r2;
        r2 = r1;
        r1 = r5;
    L_0x0025:
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.getPlaylistMetadata():android.support.v4.media.MediaMetadata2");
    }

    /* JADX WARNING: Missing block: B:8:0x000a, code skipped:
            if (r1 == null) goto L_0x0010;
     */
    /* JADX WARNING: Missing block: B:9:0x000c, code skipped:
            r1.addPlaylistItem(r5, r6);
     */
    /* JADX WARNING: Missing block: B:11:0x0012, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:12:0x0014, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:24:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:25:?, code skipped:
            return;
     */
    public void addPlaylistItem(int r5, @android.support.annotation.NonNull android.support.v4.media.MediaItem2 r6) {
        /*
        r4 = this;
        if (r5 < 0) goto L_0x002f;
    L_0x0002:
        if (r6 == 0) goto L_0x0027;
    L_0x0004:
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlaylistAgent;	 Catch:{ all -> 0x0023 }
        monitor-exit(r0);	 Catch:{ all -> 0x0021 }
        if (r1 == 0) goto L_0x0010;
    L_0x000c:
        r1.addPlaylistItem(r5, r6);
        goto L_0x0020;
    L_0x0010:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x0020;
    L_0x0014:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x0020:
        return;
    L_0x0021:
        r2 = move-exception;
        goto L_0x0025;
    L_0x0023:
        r2 = move-exception;
        r1 = 0;
    L_0x0025:
        monitor-exit(r0);	 Catch:{ all -> 0x0021 }
        throw r2;
    L_0x0027:
        r0 = new java.lang.IllegalArgumentException;
        r1 = "item shouldn't be null";
        r0.<init>(r1);
        throw r0;
    L_0x002f:
        r0 = new java.lang.IllegalArgumentException;
        r1 = "index shouldn't be negative";
        r0.<init>(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.addPlaylistItem(int, android.support.v4.media.MediaItem2):void");
    }

    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            if (r1 == null) goto L_0x000e;
     */
    /* JADX WARNING: Missing block: B:8:0x000a, code skipped:
            r1.removePlaylistItem(r5);
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:11:0x0012, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:20:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:22:?, code skipped:
            return;
     */
    public void removePlaylistItem(@android.support.annotation.NonNull android.support.v4.media.MediaItem2 r5) {
        /*
        r4 = this;
        if (r5 == 0) goto L_0x0025;
    L_0x0002:
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlaylistAgent;	 Catch:{ all -> 0x0021 }
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        if (r1 == 0) goto L_0x000e;
    L_0x000a:
        r1.removePlaylistItem(r5);
        goto L_0x001e;
    L_0x000e:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001e;
    L_0x0012:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001e:
        return;
    L_0x001f:
        r2 = move-exception;
        goto L_0x0023;
    L_0x0021:
        r2 = move-exception;
        r1 = 0;
    L_0x0023:
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        throw r2;
    L_0x0025:
        r0 = new java.lang.IllegalArgumentException;
        r1 = "item shouldn't be null";
        r0.<init>(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.removePlaylistItem(android.support.v4.media.MediaItem2):void");
    }

    /* JADX WARNING: Missing block: B:8:0x000a, code skipped:
            if (r1 == null) goto L_0x0010;
     */
    /* JADX WARNING: Missing block: B:9:0x000c, code skipped:
            r1.replacePlaylistItem(r5, r6);
     */
    /* JADX WARNING: Missing block: B:11:0x0012, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:12:0x0014, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:24:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:25:?, code skipped:
            return;
     */
    public void replacePlaylistItem(int r5, @android.support.annotation.NonNull android.support.v4.media.MediaItem2 r6) {
        /*
        r4 = this;
        if (r5 < 0) goto L_0x002f;
    L_0x0002:
        if (r6 == 0) goto L_0x0027;
    L_0x0004:
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlaylistAgent;	 Catch:{ all -> 0x0023 }
        monitor-exit(r0);	 Catch:{ all -> 0x0021 }
        if (r1 == 0) goto L_0x0010;
    L_0x000c:
        r1.replacePlaylistItem(r5, r6);
        goto L_0x0020;
    L_0x0010:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x0020;
    L_0x0014:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x0020:
        return;
    L_0x0021:
        r2 = move-exception;
        goto L_0x0025;
    L_0x0023:
        r2 = move-exception;
        r1 = 0;
    L_0x0025:
        monitor-exit(r0);	 Catch:{ all -> 0x0021 }
        throw r2;
    L_0x0027:
        r0 = new java.lang.IllegalArgumentException;
        r1 = "item shouldn't be null";
        r0.<init>(r1);
        throw r0;
    L_0x002f:
        r0 = new java.lang.IllegalArgumentException;
        r1 = "index shouldn't be negative";
        r0.<init>(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.replacePlaylistItem(int, android.support.v4.media.MediaItem2):void");
    }

    /* JADX WARNING: Missing block: B:7:0x0007, code skipped:
            if (r2 == null) goto L_0x000e;
     */
    /* JADX WARNING: Missing block: B:9:0x000d, code skipped:
            return r2.getCurrentMediaItem();
     */
    /* JADX WARNING: Missing block: B:11:0x0010, code skipped:
            if (DEBUG == false) goto L_0x001e;
     */
    /* JADX WARNING: Missing block: B:12:0x0012, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return null;
     */
    public android.support.v4.media.MediaItem2 getCurrentMediaItem() {
        /*
        r6 = this;
        r0 = r6.mLock;
        monitor-enter(r0);
        r1 = 0;
        r2 = r6.mPlaylistAgent;	 Catch:{ all -> 0x0021 }
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        if (r2 == 0) goto L_0x000e;
    L_0x0009:
        r0 = r2.getCurrentMediaItem();
        return r0;
    L_0x000e:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001e;
    L_0x0012:
        r0 = "MS2ImplBase";
        r3 = "API calls after the close()";
        r4 = new java.lang.IllegalStateException;
        r4.<init>();
        android.util.Log.d(r0, r3, r4);
    L_0x001e:
        return r1;
    L_0x001f:
        r1 = move-exception;
        goto L_0x0025;
    L_0x0021:
        r2 = move-exception;
        r5 = r2;
        r2 = r1;
        r1 = r5;
    L_0x0025:
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.getCurrentMediaItem():android.support.v4.media.MediaItem2");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.updatePlaylistMetadata(r5);
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
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
    public void updatePlaylistMetadata(@android.support.annotation.Nullable android.support.v4.media.MediaMetadata2 r5) {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlaylistAgent;	 Catch:{ all -> 0x001f }
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        if (r1 == 0) goto L_0x000c;
    L_0x0008:
        r1.updatePlaylistMetadata(r5);
        goto L_0x001c;
    L_0x000c:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001c;
    L_0x0010:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001c:
        return;
    L_0x001d:
        r2 = move-exception;
        goto L_0x0021;
    L_0x001f:
        r2 = move-exception;
        r1 = 0;
    L_0x0021:
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.updatePlaylistMetadata(android.support.v4.media.MediaMetadata2):void");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getRepeatMode();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return 0;
     */
    public int getRepeatMode() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlaylistAgent;	 Catch:{ all -> 0x0021 }
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        if (r1 == 0) goto L_0x000d;
    L_0x0008:
        r0 = r1.getRepeatMode();
        return r0;
    L_0x000d:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001d;
    L_0x0011:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001d:
        r0 = 0;
        return r0;
    L_0x001f:
        r2 = move-exception;
        goto L_0x0023;
    L_0x0021:
        r2 = move-exception;
        r1 = 0;
    L_0x0023:
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.getRepeatMode():int");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.setRepeatMode(r5);
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
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
    public void setRepeatMode(int r5) {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlaylistAgent;	 Catch:{ all -> 0x001f }
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        if (r1 == 0) goto L_0x000c;
    L_0x0008:
        r1.setRepeatMode(r5);
        goto L_0x001c;
    L_0x000c:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001c;
    L_0x0010:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001c:
        return;
    L_0x001d:
        r2 = move-exception;
        goto L_0x0021;
    L_0x001f:
        r2 = move-exception;
        r1 = 0;
    L_0x0021:
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.setRepeatMode(int):void");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000d;
     */
    /* JADX WARNING: Missing block: B:8:0x000c, code skipped:
            return r1.getShuffleMode();
     */
    /* JADX WARNING: Missing block: B:10:0x000f, code skipped:
            if (DEBUG == false) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return 0;
     */
    public int getShuffleMode() {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlaylistAgent;	 Catch:{ all -> 0x0021 }
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        if (r1 == 0) goto L_0x000d;
    L_0x0008:
        r0 = r1.getShuffleMode();
        return r0;
    L_0x000d:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001d;
    L_0x0011:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001d:
        r0 = 0;
        return r0;
    L_0x001f:
        r2 = move-exception;
        goto L_0x0023;
    L_0x0021:
        r2 = move-exception;
        r1 = 0;
    L_0x0023:
        monitor-exit(r0);	 Catch:{ all -> 0x001f }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.getShuffleMode():int");
    }

    /* JADX WARNING: Missing block: B:6:0x0006, code skipped:
            if (r1 == null) goto L_0x000c;
     */
    /* JADX WARNING: Missing block: B:7:0x0008, code skipped:
            r1.setShuffleMode(r5);
     */
    /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (DEBUG == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            android.util.Log.d(TAG, "API calls after the close()", new java.lang.IllegalStateException());
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
    public void setShuffleMode(int r5) {
        /*
        r4 = this;
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mPlaylistAgent;	 Catch:{ all -> 0x001f }
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        if (r1 == 0) goto L_0x000c;
    L_0x0008:
        r1.setShuffleMode(r5);
        goto L_0x001c;
    L_0x000c:
        r0 = DEBUG;
        if (r0 == 0) goto L_0x001c;
    L_0x0010:
        r0 = "MS2ImplBase";
        r2 = "API calls after the close()";
        r3 = new java.lang.IllegalStateException;
        r3.<init>();
        android.util.Log.d(r0, r2, r3);
    L_0x001c:
        return;
    L_0x001d:
        r2 = move-exception;
        goto L_0x0021;
    L_0x001f:
        r2 = move-exception;
        r1 = 0;
    L_0x0021:
        monitor-exit(r0);	 Catch:{ all -> 0x001d }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaSession2ImplBase.setShuffleMode(int):void");
    }

    @NonNull
    public MediaSession2 getInstance() {
        return this.mInstance;
    }

    @NonNull
    public IBinder getSessionBinder() {
        return this.mSession2Stub.asBinder();
    }

    public Context getContext() {
        return this.mContext;
    }

    public Executor getCallbackExecutor() {
        return this.mCallbackExecutor;
    }

    public SessionCallback getCallback() {
        return this.mCallback;
    }

    public MediaSessionCompat getSessionCompat() {
        return this.mSessionCompat;
    }

    public AudioFocusHandler getAudioFocusHandler() {
        return this.mAudioFocusHandler;
    }

    public boolean isClosed() {
        return this.mHandlerThread.isAlive() ^ 1;
    }

    public PlaybackStateCompat getPlaybackStateCompat() {
        PlaybackStateCompat build;
        synchronized (this.mLock) {
            build = new PlaybackStateCompat.Builder().setState(MediaUtils2.convertToPlaybackStateCompatState(getPlayerState(), getBufferingState()), getCurrentPosition(), getPlaybackSpeed()).setActions(3670015).setBufferedPosition(getBufferedPosition()).build();
        }
        return build;
    }

    public PlaybackInfo getPlaybackInfo() {
        PlaybackInfo playbackInfo;
        synchronized (this.mLock) {
            playbackInfo = this.mPlaybackInfo;
        }
        return playbackInfo;
    }

    public PendingIntent getSessionActivity() {
        return this.mSessionActivity;
    }

    private static String getServiceName(Context context, String serviceAction, String id) {
        PackageManager manager = context.getPackageManager();
        Intent serviceIntent = new Intent(serviceAction);
        serviceIntent.setPackage(context.getPackageName());
        List<ResolveInfo> services = manager.queryIntentServices(serviceIntent, 128);
        String serviceName = null;
        if (services != null) {
            int i = 0;
            while (i < services.size()) {
                String serviceId = SessionToken2.getSessionId((ResolveInfo) services.get(i));
                if (!(serviceId == null || !TextUtils.equals(id, serviceId) || ((ResolveInfo) services.get(i)).serviceInfo == null)) {
                    if (serviceName == null) {
                        serviceName = ((ResolveInfo) services.get(i)).serviceInfo.name;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Ambiguous session type. Multiple session services define the same id=");
                        stringBuilder.append(id);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                i++;
            }
        }
        return serviceName;
    }

    private void notifyAgentUpdatedNotLocked(MediaPlaylistAgent oldAgent) {
        List<MediaItem2> oldPlaylist = oldAgent.getPlaylist();
        final List<MediaItem2> newPlaylist = getPlaylist();
        if (ObjectsCompat.equals(oldPlaylist, newPlaylist)) {
            MediaMetadata2 oldMetadata = oldAgent.getPlaylistMetadata();
            final MediaMetadata2 newMetadata = getPlaylistMetadata();
            if (!ObjectsCompat.equals(oldMetadata, newMetadata)) {
                notifyToAllControllers(new NotifyRunnable() {
                    public void run(ControllerCb callback) throws RemoteException {
                        callback.onPlaylistMetadataChanged(newMetadata);
                    }
                });
            }
        } else {
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistChanged(newPlaylist, MediaSession2ImplBase.this.getPlaylistMetadata());
                }
            });
        }
        MediaItem2 oldCurrentItem = oldAgent.getCurrentMediaItem();
        final MediaItem2 newCurrentItem = getCurrentMediaItem();
        if (!ObjectsCompat.equals(oldCurrentItem, newCurrentItem)) {
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onCurrentMediaItemChanged(newCurrentItem);
                }
            });
        }
        final int repeatMode = getRepeatMode();
        if (oldAgent.getRepeatMode() != repeatMode) {
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onRepeatModeChanged(repeatMode);
                }
            });
        }
        final int shuffleMode = getShuffleMode();
        if (oldAgent.getShuffleMode() != shuffleMode) {
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onShuffleModeChanged(shuffleMode);
                }
            });
        }
    }

    private void notifyPlayerUpdatedNotLocked(BaseMediaPlayer oldPlayer) {
        long currentTimeMs = SystemClock.elapsedRealtime();
        long positionMs = getCurrentPosition();
        final long j = currentTimeMs;
        final long j2 = positionMs;
        final int playerState = getPlayerState();
        notifyToAllControllers(new NotifyRunnable() {
            public void run(ControllerCb callback) throws RemoteException {
                callback.onPlayerStateChanged(j, j2, playerState);
            }
        });
        MediaItem2 item = getCurrentMediaItem();
        if (item != null) {
            playerState = getBufferingState();
            final MediaItem2 mediaItem2 = item;
            final int i = playerState;
            AnonymousClass15 anonymousClass15 = r0;
            j2 = getBufferedPosition();
            AnonymousClass15 anonymousClass152 = new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onBufferingStateChanged(mediaItem2, i, j2);
                }
            };
            notifyToAllControllers(anonymousClass15);
        }
        float speed = getPlaybackSpeed();
        if (speed != oldPlayer.getPlaybackSpeed()) {
            j = currentTimeMs;
            j2 = positionMs;
            final float f = speed;
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaybackSpeedChanged(j, j2, f);
                }
            });
        }
    }

    private void notifyPlaylistChangedOnExecutor(MediaPlaylistAgent playlistAgent, final List<MediaItem2> list, final MediaMetadata2 metadata) {
        synchronized (this.mLock) {
            if (playlistAgent != this.mPlaylistAgent) {
                return;
            }
            this.mCallback.onPlaylistChanged(this.mInstance, playlistAgent, list, metadata);
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistChanged(list, metadata);
                }
            });
        }
    }

    private void notifyPlaylistMetadataChangedOnExecutor(MediaPlaylistAgent playlistAgent, final MediaMetadata2 metadata) {
        synchronized (this.mLock) {
            if (playlistAgent != this.mPlaylistAgent) {
                return;
            }
            this.mCallback.onPlaylistMetadataChanged(this.mInstance, playlistAgent, metadata);
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onPlaylistMetadataChanged(metadata);
                }
            });
        }
    }

    private void notifyRepeatModeChangedOnExecutor(MediaPlaylistAgent playlistAgent, final int repeatMode) {
        synchronized (this.mLock) {
            if (playlistAgent != this.mPlaylistAgent) {
                return;
            }
            this.mCallback.onRepeatModeChanged(this.mInstance, playlistAgent, repeatMode);
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onRepeatModeChanged(repeatMode);
                }
            });
        }
    }

    private void notifyShuffleModeChangedOnExecutor(MediaPlaylistAgent playlistAgent, final int shuffleMode) {
        synchronized (this.mLock) {
            if (playlistAgent != this.mPlaylistAgent) {
                return;
            }
            this.mCallback.onShuffleModeChanged(this.mInstance, playlistAgent, shuffleMode);
            notifyToAllControllers(new NotifyRunnable() {
                public void run(ControllerCb callback) throws RemoteException {
                    callback.onShuffleModeChanged(shuffleMode);
                }
            });
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void notifyToController(@NonNull final ControllerInfo controller, @NonNull NotifyRunnable runnable) {
        String str;
        StringBuilder stringBuilder;
        if (controller != null) {
            try {
                runnable.run(controller.getControllerCb());
            } catch (DeadObjectException e) {
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(controller.toString());
                    stringBuilder.append(" is gone");
                    Log.d(str, stringBuilder.toString(), e);
                }
                this.mSession2Stub.removeControllerInfo(controller);
                this.mCallbackExecutor.execute(new Runnable() {
                    public void run() {
                        MediaSession2ImplBase.this.mCallback.onDisconnected(MediaSession2ImplBase.this.getInstance(), controller);
                    }
                });
            } catch (RemoteException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception in ");
                stringBuilder.append(controller.toString());
                Log.w(str, stringBuilder.toString(), e2);
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void notifyToAllControllers(@NonNull NotifyRunnable runnable) {
        List<ControllerInfo> controllers = getConnectedControllers();
        for (int i = 0; i < controllers.size(); i++) {
            notifyToController((ControllerInfo) controllers.get(i), runnable);
        }
    }
}
