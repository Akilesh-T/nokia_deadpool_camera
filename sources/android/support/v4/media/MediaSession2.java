package android.support.v4.media;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.mediacompat.Rating2;
import android.support.v4.media.MediaController2.PlaybackInfo;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@TargetApi(19)
public class MediaSession2 implements SessionPlayer, AutoCloseable {
    public static final int ERROR_CODE_ACTION_ABORTED = 10;
    public static final int ERROR_CODE_APP_ERROR = 1;
    public static final int ERROR_CODE_AUTHENTICATION_EXPIRED = 3;
    public static final int ERROR_CODE_CONCURRENT_STREAM_LIMIT = 5;
    public static final int ERROR_CODE_CONTENT_ALREADY_PLAYING = 8;
    public static final int ERROR_CODE_END_OF_QUEUE = 11;
    public static final int ERROR_CODE_NOT_AVAILABLE_IN_REGION = 7;
    public static final int ERROR_CODE_NOT_SUPPORTED = 2;
    public static final int ERROR_CODE_PARENTAL_CONTROL_RESTRICTED = 6;
    public static final int ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED = 4;
    public static final int ERROR_CODE_SETUP_REQUIRED = 12;
    public static final int ERROR_CODE_SKIP_LIMIT_REACHED = 9;
    public static final int ERROR_CODE_UNKNOWN_ERROR = 0;
    static final String TAG = "MediaSession2";
    private final SupportLibraryImpl mImpl;

    @RestrictTo({Scope.LIBRARY_GROUP})
    static abstract class BuilderBase<T extends MediaSession2, U extends BuilderBase<T, U, C>, C extends SessionCallback> {
        C mCallback;
        Executor mCallbackExecutor;
        final Context mContext;
        String mId;
        BaseMediaPlayer mPlayer;
        MediaPlaylistAgent mPlaylistAgent;
        PendingIntent mSessionActivity;
        VolumeProviderCompat mVolumeProvider;

        @NonNull
        public abstract T build();

        BuilderBase(Context context) {
            if (context != null) {
                this.mContext = context;
                this.mId = MediaSession2.TAG;
                return;
            }
            throw new IllegalArgumentException("context shouldn't be null");
        }

        /* Access modifiers changed, original: 0000 */
        @NonNull
        public U setPlayer(@NonNull BaseMediaPlayer player) {
            if (player != null) {
                this.mPlayer = player;
                return this;
            }
            throw new IllegalArgumentException("player shouldn't be null");
        }

        /* Access modifiers changed, original: 0000 */
        public U setPlaylistAgent(@NonNull MediaPlaylistAgent playlistAgent) {
            if (playlistAgent != null) {
                this.mPlaylistAgent = playlistAgent;
                return this;
            }
            throw new IllegalArgumentException("playlistAgent shouldn't be null");
        }

        /* Access modifiers changed, original: 0000 */
        @NonNull
        public U setVolumeProvider(@Nullable VolumeProviderCompat volumeProvider) {
            this.mVolumeProvider = volumeProvider;
            return this;
        }

        /* Access modifiers changed, original: 0000 */
        @NonNull
        public U setSessionActivity(@Nullable PendingIntent pi) {
            this.mSessionActivity = pi;
            return this;
        }

        /* Access modifiers changed, original: 0000 */
        @NonNull
        public U setId(@NonNull String id) {
            if (id != null) {
                this.mId = id;
                return this;
            }
            throw new IllegalArgumentException("id shouldn't be null");
        }

        /* Access modifiers changed, original: 0000 */
        @NonNull
        public U setSessionCallback(@NonNull Executor executor, @NonNull C callback) {
            if (executor == null) {
                throw new IllegalArgumentException("executor shouldn't be null");
            } else if (callback != null) {
                this.mCallbackExecutor = executor;
                this.mCallback = callback;
                return this;
            } else {
                throw new IllegalArgumentException("callback shouldn't be null");
            }
        }
    }

    public static final class CommandButton {
        private static final String KEY_COMMAND = "android.media.media_session2.command_button.command";
        private static final String KEY_DISPLAY_NAME = "android.media.media_session2.command_button.display_name";
        private static final String KEY_ENABLED = "android.media.media_session2.command_button.enabled";
        private static final String KEY_EXTRAS = "android.media.media_session2.command_button.extras";
        private static final String KEY_ICON_RES_ID = "android.media.media_session2.command_button.icon_res_id";
        private SessionCommand2 mCommand;
        private String mDisplayName;
        private boolean mEnabled;
        private Bundle mExtras;
        private int mIconResId;

        public static final class Builder {
            private SessionCommand2 mCommand;
            private String mDisplayName;
            private boolean mEnabled;
            private Bundle mExtras;
            private int mIconResId;

            @NonNull
            public Builder setCommand(@Nullable SessionCommand2 command) {
                this.mCommand = command;
                return this;
            }

            @NonNull
            public Builder setIconResId(int resId) {
                this.mIconResId = resId;
                return this;
            }

            @NonNull
            public Builder setDisplayName(@Nullable String displayName) {
                this.mDisplayName = displayName;
                return this;
            }

            @NonNull
            public Builder setEnabled(boolean enabled) {
                this.mEnabled = enabled;
                return this;
            }

            @NonNull
            public Builder setExtras(@Nullable Bundle extras) {
                this.mExtras = extras;
                return this;
            }

            @NonNull
            public CommandButton build() {
                return new CommandButton(this.mCommand, this.mIconResId, this.mDisplayName, this.mExtras, this.mEnabled);
            }
        }

        private CommandButton(@Nullable SessionCommand2 command, int iconResId, @Nullable String displayName, Bundle extras, boolean enabled) {
            this.mCommand = command;
            this.mIconResId = iconResId;
            this.mDisplayName = displayName;
            this.mExtras = extras;
            this.mEnabled = enabled;
        }

        @Nullable
        public SessionCommand2 getCommand() {
            return this.mCommand;
        }

        public int getIconResId() {
            return this.mIconResId;
        }

        @Nullable
        public String getDisplayName() {
            return this.mDisplayName;
        }

        @Nullable
        public Bundle getExtras() {
            return this.mExtras;
        }

        public boolean isEnabled() {
            return this.mEnabled;
        }

        @RestrictTo({Scope.LIBRARY_GROUP})
        @NonNull
        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putBundle(KEY_COMMAND, this.mCommand.toBundle());
            bundle.putInt(KEY_ICON_RES_ID, this.mIconResId);
            bundle.putString(KEY_DISPLAY_NAME, this.mDisplayName);
            bundle.putBundle(KEY_EXTRAS, this.mExtras);
            bundle.putBoolean(KEY_ENABLED, this.mEnabled);
            return bundle;
        }

        @Nullable
        @RestrictTo({Scope.LIBRARY_GROUP})
        public static CommandButton fromBundle(Bundle bundle) {
            if (bundle == null) {
                return null;
            }
            Builder builder = new Builder();
            builder.setCommand(SessionCommand2.fromBundle(bundle.getBundle(KEY_COMMAND)));
            builder.setIconResId(bundle.getInt(KEY_ICON_RES_ID, 0));
            builder.setDisplayName(bundle.getString(KEY_DISPLAY_NAME));
            builder.setExtras(bundle.getBundle(KEY_EXTRAS));
            builder.setEnabled(bundle.getBoolean(KEY_ENABLED));
            try {
                return builder.build();
            } catch (IllegalStateException e) {
                return null;
            }
        }
    }

    static abstract class ControllerCb {
        @NonNull
        public abstract IBinder getId();

        public abstract void onAllowedCommandsChanged(@NonNull SessionCommandGroup2 sessionCommandGroup2) throws RemoteException;

        public abstract void onBufferingStateChanged(@NonNull MediaItem2 mediaItem2, int i, long j) throws RemoteException;

        public abstract void onChildrenChanged(@NonNull String str, int i, @Nullable Bundle bundle) throws RemoteException;

        public abstract void onCurrentMediaItemChanged(@Nullable MediaItem2 mediaItem2) throws RemoteException;

        public abstract void onCustomCommand(@NonNull SessionCommand2 sessionCommand2, @Nullable Bundle bundle, @Nullable ResultReceiver resultReceiver) throws RemoteException;

        public abstract void onCustomLayoutChanged(@NonNull List<CommandButton> list) throws RemoteException;

        public abstract void onDisconnected() throws RemoteException;

        public abstract void onError(int i, @Nullable Bundle bundle) throws RemoteException;

        public abstract void onGetChildrenDone(@NonNull String str, int i, int i2, @Nullable List<MediaItem2> list, @Nullable Bundle bundle) throws RemoteException;

        public abstract void onGetItemDone(@NonNull String str, @Nullable MediaItem2 mediaItem2) throws RemoteException;

        public abstract void onGetLibraryRootDone(@Nullable Bundle bundle, @Nullable String str, @Nullable Bundle bundle2) throws RemoteException;

        public abstract void onGetSearchResultDone(@NonNull String str, int i, int i2, @Nullable List<MediaItem2> list, @Nullable Bundle bundle) throws RemoteException;

        public abstract void onPlaybackInfoChanged(@NonNull PlaybackInfo playbackInfo) throws RemoteException;

        public abstract void onPlaybackSpeedChanged(long j, long j2, float f) throws RemoteException;

        public abstract void onPlayerStateChanged(long j, long j2, int i) throws RemoteException;

        public abstract void onPlaylistChanged(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 mediaMetadata2) throws RemoteException;

        public abstract void onPlaylistMetadataChanged(@Nullable MediaMetadata2 mediaMetadata2) throws RemoteException;

        public abstract void onRepeatModeChanged(int i) throws RemoteException;

        public abstract void onRoutesInfoChanged(@Nullable List<Bundle> list) throws RemoteException;

        public abstract void onSearchResultChanged(@NonNull String str, int i, @Nullable Bundle bundle) throws RemoteException;

        public abstract void onSeekCompleted(long j, long j2, long j3) throws RemoteException;

        public abstract void onShuffleModeChanged(int i) throws RemoteException;

        ControllerCb() {
        }

        public int hashCode() {
            return getId().hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ControllerCb)) {
                return false;
            }
            return getId().equals(((ControllerCb) obj).getId());
        }
    }

    public static final class ControllerInfo {
        private final ControllerCb mControllerCb;
        private final boolean mIsTrusted = false;
        private final String mPackageName;
        private final int mUid;

        @RestrictTo({Scope.LIBRARY_GROUP})
        ControllerInfo(@NonNull String packageName, int pid, int uid, @NonNull ControllerCb cb) {
            this.mUid = uid;
            this.mPackageName = packageName;
            this.mControllerCb = cb;
        }

        @NonNull
        public String getPackageName() {
            return this.mPackageName;
        }

        public int getUid() {
            return this.mUid;
        }

        @RestrictTo({Scope.LIBRARY_GROUP})
        public boolean isTrusted() {
            return this.mIsTrusted;
        }

        public int hashCode() {
            return this.mControllerCb.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ControllerInfo)) {
                return false;
            }
            return this.mControllerCb.equals(((ControllerInfo) obj).mControllerCb);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ControllerInfo {pkg=");
            stringBuilder.append(this.mPackageName);
            stringBuilder.append(", uid=");
            stringBuilder.append(this.mUid);
            stringBuilder.append("})");
            return stringBuilder.toString();
        }

        /* Access modifiers changed, original: 0000 */
        @NonNull
        public IBinder getId() {
            return this.mControllerCb.getId();
        }

        /* Access modifiers changed, original: 0000 */
        @NonNull
        public ControllerCb getControllerCb() {
            return this.mControllerCb;
        }
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorCode {
    }

    static class MainHandlerExecutor implements Executor {
        private final Handler mHandler;

        MainHandlerExecutor(Context context) {
            this.mHandler = new Handler(context.getMainLooper());
        }

        public void execute(Runnable command) {
            if (!this.mHandler.post(command)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.mHandler);
                stringBuilder.append(" is shutting down");
                throw new RejectedExecutionException(stringBuilder.toString());
            }
        }
    }

    public interface OnDataSourceMissingHelper {
        @Nullable
        DataSourceDesc onDataSourceMissing(@NonNull MediaSession2 mediaSession2, @NonNull MediaItem2 mediaItem2);
    }

    public static abstract class SessionCallback {
        @Nullable
        public SessionCommandGroup2 onConnect(@NonNull MediaSession2 session, @NonNull ControllerInfo controller) {
            SessionCommandGroup2 commands = new SessionCommandGroup2();
            commands.addAllPredefinedCommands();
            return commands;
        }

        public void onDisconnected(@NonNull MediaSession2 session, @NonNull ControllerInfo controller) {
        }

        public boolean onCommandRequest(@NonNull MediaSession2 session, @NonNull ControllerInfo controller, @NonNull SessionCommand2 command) {
            return true;
        }

        public void onSetRating(@NonNull MediaSession2 session, @NonNull ControllerInfo controller, @NonNull String mediaId, @NonNull Rating2 rating) {
        }

        public void onCustomCommand(@NonNull MediaSession2 session, @NonNull ControllerInfo controller, @NonNull SessionCommand2 customCommand, @Nullable Bundle args, @Nullable ResultReceiver cb) {
        }

        public void onPlayFromMediaId(@NonNull MediaSession2 session, @NonNull ControllerInfo controller, @NonNull String mediaId, @Nullable Bundle extras) {
        }

        public void onPlayFromSearch(@NonNull MediaSession2 session, @NonNull ControllerInfo controller, @NonNull String query, @Nullable Bundle extras) {
        }

        public void onPlayFromUri(@NonNull MediaSession2 session, @NonNull ControllerInfo controller, @NonNull Uri uri, @Nullable Bundle extras) {
        }

        public void onPrepareFromMediaId(@NonNull MediaSession2 session, @NonNull ControllerInfo controller, @NonNull String mediaId, @Nullable Bundle extras) {
        }

        public void onPrepareFromSearch(@NonNull MediaSession2 session, @NonNull ControllerInfo controller, @NonNull String query, @Nullable Bundle extras) {
        }

        public void onPrepareFromUri(@NonNull MediaSession2 session, @NonNull ControllerInfo controller, @NonNull Uri uri, @Nullable Bundle extras) {
        }

        public void onFastForward(@NonNull MediaSession2 session, ControllerInfo controller) {
        }

        public void onRewind(@NonNull MediaSession2 session, ControllerInfo controller) {
        }

        public void onSubscribeRoutesInfo(@NonNull MediaSession2 session, @NonNull ControllerInfo controller) {
        }

        public void onUnsubscribeRoutesInfo(@NonNull MediaSession2 session, @NonNull ControllerInfo controller) {
        }

        public void onSelectRoute(@NonNull MediaSession2 session, @NonNull ControllerInfo controller, @NonNull Bundle route) {
        }

        public void onCurrentMediaItemChanged(@NonNull MediaSession2 session, @NonNull BaseMediaPlayer player, @Nullable MediaItem2 item) {
        }

        public void onMediaPrepared(@NonNull MediaSession2 session, @NonNull BaseMediaPlayer player, @NonNull MediaItem2 item) {
        }

        public void onPlayerStateChanged(@NonNull MediaSession2 session, @NonNull BaseMediaPlayer player, int state) {
        }

        public void onBufferingStateChanged(@NonNull MediaSession2 session, @NonNull BaseMediaPlayer player, @NonNull MediaItem2 item, int state) {
        }

        public void onPlaybackSpeedChanged(@NonNull MediaSession2 session, @NonNull BaseMediaPlayer player, float speed) {
        }

        public void onSeekCompleted(@NonNull MediaSession2 session, @NonNull BaseMediaPlayer player, long position) {
        }

        public void onPlaylistChanged(@NonNull MediaSession2 session, @NonNull MediaPlaylistAgent playlistAgent, @NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        }

        public void onPlaylistMetadataChanged(@NonNull MediaSession2 session, @NonNull MediaPlaylistAgent playlistAgent, @Nullable MediaMetadata2 metadata) {
        }

        public void onShuffleModeChanged(@NonNull MediaSession2 session, @NonNull MediaPlaylistAgent playlistAgent, int shuffleMode) {
        }

        public void onRepeatModeChanged(@NonNull MediaSession2 session, @NonNull MediaPlaylistAgent playlistAgent, int repeatMode) {
        }
    }

    public static final class Builder extends BuilderBase<MediaSession2, Builder, SessionCallback> {
        public Builder(Context context) {
            super(context);
        }

        @NonNull
        public Builder setPlayer(@NonNull BaseMediaPlayer player) {
            return (Builder) super.setPlayer(player);
        }

        @NonNull
        public Builder setPlaylistAgent(@NonNull MediaPlaylistAgent playlistAgent) {
            return (Builder) super.setPlaylistAgent(playlistAgent);
        }

        @NonNull
        public Builder setVolumeProvider(@Nullable VolumeProviderCompat volumeProvider) {
            return (Builder) super.setVolumeProvider(volumeProvider);
        }

        @NonNull
        public Builder setSessionActivity(@Nullable PendingIntent pi) {
            return (Builder) super.setSessionActivity(pi);
        }

        @NonNull
        public Builder setId(@NonNull String id) {
            return (Builder) super.setId(id);
        }

        @NonNull
        public Builder setSessionCallback(@NonNull Executor executor, @NonNull SessionCallback callback) {
            return (Builder) super.setSessionCallback(executor, callback);
        }

        @NonNull
        public MediaSession2 build() {
            if (this.mCallbackExecutor == null) {
                this.mCallbackExecutor = new MainHandlerExecutor(this.mContext);
            }
            if (this.mCallback == null) {
                this.mCallback = new SessionCallback() {
                };
            }
            return new MediaSession2(this.mContext, this.mId, this.mPlayer, this.mPlaylistAgent, this.mVolumeProvider, this.mSessionActivity, this.mCallbackExecutor, this.mCallback);
        }
    }

    interface SupportLibraryImpl extends SessionPlayer, AutoCloseable {
        AudioFocusHandler getAudioFocusHandler();

        SessionCallback getCallback();

        Executor getCallbackExecutor();

        @NonNull
        List<ControllerInfo> getConnectedControllers();

        Context getContext();

        MediaSession2 getInstance();

        PlaybackInfo getPlaybackInfo();

        PlaybackStateCompat getPlaybackStateCompat();

        @NonNull
        BaseMediaPlayer getPlayer();

        @NonNull
        MediaPlaylistAgent getPlaylistAgent();

        PendingIntent getSessionActivity();

        IBinder getSessionBinder();

        MediaSessionCompat getSessionCompat();

        @NonNull
        SessionToken2 getToken();

        @Nullable
        VolumeProviderCompat getVolumeProvider();

        boolean isClosed();

        void notifyRoutesInfoChanged(@NonNull ControllerInfo controllerInfo, @Nullable List<Bundle> list);

        void sendCustomCommand(@NonNull ControllerInfo controllerInfo, @NonNull SessionCommand2 sessionCommand2, @Nullable Bundle bundle, @Nullable ResultReceiver resultReceiver);

        void sendCustomCommand(@NonNull SessionCommand2 sessionCommand2, @Nullable Bundle bundle);

        void setAllowedCommands(@NonNull ControllerInfo controllerInfo, @NonNull SessionCommandGroup2 sessionCommandGroup2);

        void setCustomLayout(@NonNull ControllerInfo controllerInfo, @NonNull List<CommandButton> list);

        void updatePlayer(@NonNull BaseMediaPlayer baseMediaPlayer, @Nullable MediaPlaylistAgent mediaPlaylistAgent, @Nullable VolumeProviderCompat volumeProviderCompat);
    }

    MediaSession2(Context context, String id, BaseMediaPlayer player, MediaPlaylistAgent playlistAgent, VolumeProviderCompat volumeProvider, PendingIntent sessionActivity, Executor callbackExecutor, SessionCallback callback) {
        this.mImpl = createImpl(context, id, player, playlistAgent, volumeProvider, sessionActivity, callbackExecutor, callback);
    }

    /* Access modifiers changed, original: 0000 */
    public SupportLibraryImpl createImpl(Context context, String id, BaseMediaPlayer player, MediaPlaylistAgent playlistAgent, VolumeProviderCompat volumeProvider, PendingIntent sessionActivity, Executor callbackExecutor, SessionCallback callback) {
        return new MediaSession2ImplBase(this, context, id, player, playlistAgent, volumeProvider, sessionActivity, callbackExecutor, callback);
    }

    /* Access modifiers changed, original: 0000 */
    public SupportLibraryImpl getImpl() {
        return this.mImpl;
    }

    public void updatePlayer(@NonNull BaseMediaPlayer player, @Nullable MediaPlaylistAgent playlistAgent, @Nullable VolumeProviderCompat volumeProvider) {
        this.mImpl.updatePlayer(player, playlistAgent, volumeProvider);
    }

    public void close() {
        try {
            this.mImpl.close();
        } catch (Exception e) {
        }
    }

    @NonNull
    public BaseMediaPlayer getPlayer() {
        return this.mImpl.getPlayer();
    }

    @NonNull
    public MediaPlaylistAgent getPlaylistAgent() {
        return this.mImpl.getPlaylistAgent();
    }

    @Nullable
    public VolumeProviderCompat getVolumeProvider() {
        return this.mImpl.getVolumeProvider();
    }

    @NonNull
    public SessionToken2 getToken() {
        return this.mImpl.getToken();
    }

    /* Access modifiers changed, original: 0000 */
    @NonNull
    public Context getContext() {
        return this.mImpl.getContext();
    }

    /* Access modifiers changed, original: 0000 */
    @NonNull
    public Executor getCallbackExecutor() {
        return this.mImpl.getCallbackExecutor();
    }

    /* Access modifiers changed, original: 0000 */
    @NonNull
    public SessionCallback getCallback() {
        return this.mImpl.getCallback();
    }

    /* Access modifiers changed, original: 0000 */
    @NonNull
    public AudioFocusHandler getAudioFocusHandler() {
        return this.mImpl.getAudioFocusHandler();
    }

    @NonNull
    public List<ControllerInfo> getConnectedControllers() {
        return this.mImpl.getConnectedControllers();
    }

    public void setCustomLayout(@NonNull ControllerInfo controller, @NonNull List<CommandButton> layout) {
        this.mImpl.setCustomLayout(controller, layout);
    }

    public void setAllowedCommands(@NonNull ControllerInfo controller, @NonNull SessionCommandGroup2 commands) {
        this.mImpl.setAllowedCommands(controller, commands);
    }

    public void sendCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args) {
        this.mImpl.sendCustomCommand(command, args);
    }

    public void sendCustomCommand(@NonNull ControllerInfo controller, @NonNull SessionCommand2 command, @Nullable Bundle args, @Nullable ResultReceiver receiver) {
        this.mImpl.sendCustomCommand(controller, command, args, receiver);
    }

    public void play() {
        this.mImpl.play();
    }

    public void pause() {
        this.mImpl.pause();
    }

    public void reset() {
        this.mImpl.reset();
    }

    public void prepare() {
        this.mImpl.prepare();
    }

    public void seekTo(long pos) {
        this.mImpl.seekTo(pos);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public void skipForward() {
        this.mImpl.skipForward();
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public void skipBackward() {
        this.mImpl.skipBackward();
    }

    public void notifyError(int errorCode, @Nullable Bundle extras) {
        this.mImpl.notifyError(errorCode, extras);
    }

    public void notifyRoutesInfoChanged(@NonNull ControllerInfo controller, @Nullable List<Bundle> routes) {
        this.mImpl.notifyRoutesInfoChanged(controller, routes);
    }

    public int getPlayerState() {
        return this.mImpl.getPlayerState();
    }

    public long getCurrentPosition() {
        return this.mImpl.getCurrentPosition();
    }

    public long getDuration() {
        return this.mImpl.getDuration();
    }

    public long getBufferedPosition() {
        return this.mImpl.getBufferedPosition();
    }

    public int getBufferingState() {
        return this.mImpl.getBufferingState();
    }

    public float getPlaybackSpeed() {
        return this.mImpl.getPlaybackSpeed();
    }

    public void setPlaybackSpeed(float speed) {
        this.mImpl.setPlaybackSpeed(speed);
    }

    public void setOnDataSourceMissingHelper(@NonNull OnDataSourceMissingHelper helper) {
        this.mImpl.setOnDataSourceMissingHelper(helper);
    }

    public void clearOnDataSourceMissingHelper() {
        this.mImpl.clearOnDataSourceMissingHelper();
    }

    public List<MediaItem2> getPlaylist() {
        return this.mImpl.getPlaylist();
    }

    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        this.mImpl.setPlaylist(list, metadata);
    }

    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
        this.mImpl.skipToPlaylistItem(item);
    }

    public void skipToPreviousItem() {
        this.mImpl.skipToPreviousItem();
    }

    public void skipToNextItem() {
        this.mImpl.skipToNextItem();
    }

    public MediaMetadata2 getPlaylistMetadata() {
        return this.mImpl.getPlaylistMetadata();
    }

    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        this.mImpl.addPlaylistItem(index, item);
    }

    public void removePlaylistItem(@NonNull MediaItem2 item) {
        this.mImpl.removePlaylistItem(item);
    }

    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
        this.mImpl.replacePlaylistItem(index, item);
    }

    public MediaItem2 getCurrentMediaItem() {
        return this.mImpl.getCurrentMediaItem();
    }

    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        this.mImpl.updatePlaylistMetadata(metadata);
    }

    public int getRepeatMode() {
        return this.mImpl.getRepeatMode();
    }

    public void setRepeatMode(int repeatMode) {
        this.mImpl.setRepeatMode(repeatMode);
    }

    public int getShuffleMode() {
        return this.mImpl.getShuffleMode();
    }

    public void setShuffleMode(int shuffleMode) {
        this.mImpl.setShuffleMode(shuffleMode);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public MediaSessionCompat getSessionCompat() {
        return this.mImpl.getSessionCompat();
    }

    /* Access modifiers changed, original: 0000 */
    public IBinder getSessionBinder() {
        return this.mImpl.getSessionBinder();
    }
}
