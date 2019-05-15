package android.support.v4.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.MediaController2.PlaybackInfo;
import android.support.v4.media.MediaSession2.CommandButton;
import android.support.v4.media.MediaSession2.ControllerInfo;
import android.support.v4.media.session.IMediaControllerCallback;
import android.support.v4.media.session.IMediaControllerCallback.Stub;
import android.support.v4.media.session.MediaSessionCompat.Callback;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@TargetApi(19)
class MediaSessionLegacyStub extends Callback {
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String TAG = "MediaSessionLegacyStub";
    private static final SparseArray<SessionCommand2> sCommandsForOnCommandRequest = new SparseArray();
    @GuardedBy("mLock")
    private final ArrayMap<ControllerInfo, SessionCommandGroup2> mAllowedCommandGroupMap = new ArrayMap();
    @GuardedBy("mLock")
    private final Set<IBinder> mConnectingControllers = new HashSet();
    final Context mContext;
    @GuardedBy("mLock")
    private final ArrayMap<IBinder, ControllerInfo> mControllers = new ArrayMap();
    private final Object mLock = new Object();
    final SupportLibraryImpl mSession;

    @FunctionalInterface
    private interface Session2Runnable {
        void run(ControllerInfo controllerInfo) throws RemoteException;
    }

    final class ControllerLegacyCb extends ControllerCb {
        private final IMediaControllerCallback mIControllerCallback;

        ControllerLegacyCb(@NonNull IMediaControllerCallback callback) {
            this.mIControllerCallback = callback;
        }

        /* Access modifiers changed, original: 0000 */
        @NonNull
        public IBinder getId() {
            return this.mIControllerCallback.asBinder();
        }

        /* Access modifiers changed, original: 0000 */
        public void onCustomLayoutChanged(List<CommandButton> layout) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putParcelableArray("android.support.v4.media.argument.COMMAND_BUTTONS", MediaUtils2.convertCommandButtonListToParcelableArray(layout));
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.SET_CUSTOM_LAYOUT", bundle);
        }

        /* Access modifiers changed, original: 0000 */
        public void onPlaybackInfoChanged(PlaybackInfo info) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putBundle("android.support.v4.media.argument.PLAYBACK_INFO", info.toBundle());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_PLAYBACK_INFO_CHANGED", bundle);
        }

        /* Access modifiers changed, original: 0000 */
        public void onAllowedCommandsChanged(SessionCommandGroup2 commands) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putBundle("android.support.v4.media.argument.ALLOWED_COMMANDS", commands.toBundle());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_ALLOWED_COMMANDS_CHANGED", bundle);
        }

        /* Access modifiers changed, original: 0000 */
        public void onCustomCommand(SessionCommand2 command, Bundle args, ResultReceiver receiver) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putBundle("android.support.v4.media.argument.CUSTOM_COMMAND", command.toBundle());
            bundle.putBundle("android.support.v4.media.argument.ARGUMENTS", args);
            bundle.putParcelable("android.support.v4.media.argument.RESULT_RECEIVER", receiver);
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.SEND_CUSTOM_COMMAND", bundle);
        }

        /* Access modifiers changed, original: 0000 */
        public void onPlayerStateChanged(long eventTimeMs, long positionMs, int playerState) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putInt("android.support.v4.media.argument.PLAYER_STATE", playerState);
            bundle.putParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT", MediaSessionLegacyStub.this.mSession.getPlaybackStateCompat());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_PLAYER_STATE_CHANGED", bundle);
        }

        /* Access modifiers changed, original: 0000 */
        public void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT", MediaSessionLegacyStub.this.mSession.getPlaybackStateCompat());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_PLAYBACK_SPEED_CHANGED", bundle);
        }

        /* Access modifiers changed, original: 0000 */
        public void onBufferingStateChanged(MediaItem2 item, int state, long bufferedPositionMs) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putBundle("android.support.v4.media.argument.MEDIA_ITEM", item.toBundle());
            bundle.putInt("android.support.v4.media.argument.BUFFERING_STATE", state);
            bundle.putParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT", MediaSessionLegacyStub.this.mSession.getPlaybackStateCompat());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_BUFFERING_STATE_CHANGED", bundle);
        }

        /* Access modifiers changed, original: 0000 */
        public void onSeekCompleted(long eventTimeMs, long positionMs, long position) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putLong("android.support.v4.media.argument.SEEK_POSITION", position);
            bundle.putParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT", MediaSessionLegacyStub.this.mSession.getPlaybackStateCompat());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_SEEK_COMPLETED", bundle);
        }

        /* Access modifiers changed, original: 0000 */
        public void onError(int errorCode, Bundle extras) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putInt("android.support.v4.media.argument.ERROR_CODE", errorCode);
            bundle.putBundle("android.support.v4.media.argument.EXTRAS", extras);
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_ERROR", bundle);
        }

        /* Access modifiers changed, original: 0000 */
        public void onCurrentMediaItemChanged(MediaItem2 item) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putBundle("android.support.v4.media.argument.MEDIA_ITEM", item == null ? null : item.toBundle());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_CURRENT_MEDIA_ITEM_CHANGED", bundle);
        }

        /* Access modifiers changed, original: 0000 */
        public void onPlaylistChanged(List<MediaItem2> playlist, MediaMetadata2 metadata) throws RemoteException {
            Bundle bundle;
            Bundle bundle2 = new Bundle();
            bundle2.putParcelableArray("android.support.v4.media.argument.PLAYLIST", MediaUtils2.convertMediaItem2ListToParcelableArray(playlist));
            String str = "android.support.v4.media.argument.PLAYLIST_METADATA";
            if (metadata == null) {
                bundle = null;
            } else {
                bundle = metadata.toBundle();
            }
            bundle2.putBundle(str, bundle);
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_PLAYLIST_CHANGED", bundle2);
        }

        /* Access modifiers changed, original: 0000 */
        public void onPlaylistMetadataChanged(MediaMetadata2 metadata) throws RemoteException {
            Bundle bundle;
            Bundle bundle2 = new Bundle();
            String str = "android.support.v4.media.argument.PLAYLIST_METADATA";
            if (metadata == null) {
                bundle = null;
            } else {
                bundle = metadata.toBundle();
            }
            bundle2.putBundle(str, bundle);
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_PLAYLIST_METADATA_CHANGED", bundle2);
        }

        /* Access modifiers changed, original: 0000 */
        public void onShuffleModeChanged(int shuffleMode) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putInt("android.support.v4.media.argument.SHUFFLE_MODE", shuffleMode);
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_SHUFFLE_MODE_CHANGED", bundle);
        }

        /* Access modifiers changed, original: 0000 */
        public void onRepeatModeChanged(int repeatMode) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putInt("android.support.v4.media.argument.REPEAT_MODE", repeatMode);
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_REPEAT_MODE_CHANGED", bundle);
        }

        /* Access modifiers changed, original: 0000 */
        public void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
            Bundle bundle = null;
            if (routes != null) {
                bundle = new Bundle();
                bundle.putParcelableArray("android.support.v4.media.argument.ROUTE_BUNDLE", (Parcelable[]) routes.toArray(new Bundle[0]));
            }
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_ROUTES_INFO_CHANGED", bundle);
        }

        /* Access modifiers changed, original: 0000 */
        public void onGetLibraryRootDone(Bundle rootHints, String rootMediaId, Bundle rootExtra) throws RemoteException {
        }

        /* Access modifiers changed, original: 0000 */
        public void onChildrenChanged(String parentId, int itemCount, Bundle extras) throws RemoteException {
        }

        /* Access modifiers changed, original: 0000 */
        public void onGetChildrenDone(String parentId, int page, int pageSize, List<MediaItem2> list, Bundle extras) throws RemoteException {
        }

        /* Access modifiers changed, original: 0000 */
        public void onGetItemDone(String mediaId, MediaItem2 result) throws RemoteException {
        }

        /* Access modifiers changed, original: 0000 */
        public void onSearchResultChanged(String query, int itemCount, Bundle extras) throws RemoteException {
        }

        /* Access modifiers changed, original: 0000 */
        public void onGetSearchResultDone(String query, int page, int pageSize, List<MediaItem2> list, Bundle extras) throws RemoteException {
        }

        /* Access modifiers changed, original: 0000 */
        public void onDisconnected() throws RemoteException {
            this.mIControllerCallback.onSessionDestroyed();
        }
    }

    static {
        SessionCommandGroup2 group = new SessionCommandGroup2();
        group.addAllPlaybackCommands();
        group.addAllPlaylistCommands();
        group.addAllVolumeCommands();
        for (SessionCommand2 command : group.getCommands()) {
            sCommandsForOnCommandRequest.append(command.getCommandCode(), command);
        }
    }

    MediaSessionLegacyStub(SupportLibraryImpl session) {
        this.mSession = session;
        this.mContext = this.mSession.getContext();
    }

    public void onPrepare() {
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            public void run() {
                if (!MediaSessionLegacyStub.this.mSession.isClosed()) {
                    MediaSessionLegacyStub.this.mSession.prepare();
                }
            }
        });
    }

    public void onPlay() {
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            public void run() {
                if (!MediaSessionLegacyStub.this.mSession.isClosed()) {
                    MediaSessionLegacyStub.this.mSession.play();
                }
            }
        });
    }

    public void onPause() {
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            public void run() {
                if (!MediaSessionLegacyStub.this.mSession.isClosed()) {
                    MediaSessionLegacyStub.this.mSession.pause();
                }
            }
        });
    }

    public void onStop() {
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            public void run() {
                if (!MediaSessionLegacyStub.this.mSession.isClosed()) {
                    MediaSessionLegacyStub.this.mSession.reset();
                }
            }
        });
    }

    public void onSeekTo(final long pos) {
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            public void run() {
                if (!MediaSessionLegacyStub.this.mSession.isClosed()) {
                    MediaSessionLegacyStub.this.mSession.seekTo(pos);
                }
            }
        });
    }

    /* Access modifiers changed, original: 0000 */
    public List<ControllerInfo> getConnectedControllers() {
        ArrayList<ControllerInfo> controllers = new ArrayList();
        synchronized (this.mLock) {
            for (int i = 0; i < this.mControllers.size(); i++) {
                controllers.add(this.mControllers.valueAt(i));
            }
        }
        return controllers;
    }

    /* Access modifiers changed, original: 0000 */
    public void setAllowedCommands(ControllerInfo controller, SessionCommandGroup2 commands) {
        synchronized (this.mLock) {
            this.mAllowedCommandGroupMap.put(controller, commands);
        }
    }

    private boolean isAllowedCommand(ControllerInfo controller, SessionCommand2 command) {
        SessionCommandGroup2 allowedCommands;
        synchronized (this.mLock) {
            allowedCommands = (SessionCommandGroup2) this.mAllowedCommandGroupMap.get(controller);
        }
        return allowedCommands != null && allowedCommands.hasCommand(command);
    }

    private boolean isAllowedCommand(ControllerInfo controller, int commandCode) {
        SessionCommandGroup2 allowedCommands;
        synchronized (this.mLock) {
            allowedCommands = (SessionCommandGroup2) this.mAllowedCommandGroupMap.get(controller);
        }
        return allowedCommands != null && allowedCommands.hasCommand(commandCode);
    }

    private void onCommand2(@NonNull IBinder caller, int commandCode, @NonNull Session2Runnable runnable) {
        onCommand2Internal(caller, null, commandCode, runnable);
    }

    private void onCommand2(@NonNull IBinder caller, @NonNull SessionCommand2 sessionCommand, @NonNull Session2Runnable runnable) {
        onCommand2Internal(caller, sessionCommand, 0, runnable);
    }

    private void onCommand2Internal(@NonNull IBinder caller, @Nullable SessionCommand2 sessionCommand, int commandCode, @NonNull Session2Runnable runnable) {
        ControllerInfo controller;
        synchronized (this.mLock) {
            controller = (ControllerInfo) this.mControllers.get(caller);
        }
        if (this.mSession != null && controller != null) {
            final SessionCommand2 sessionCommand2 = sessionCommand;
            final ControllerInfo controllerInfo = controller;
            final int i = commandCode;
            final Session2Runnable session2Runnable = runnable;
            this.mSession.getCallbackExecutor().execute(new Runnable() {
                public void run() {
                    SessionCommand2 command;
                    String str;
                    StringBuilder stringBuilder;
                    if (sessionCommand2 != null) {
                        if (MediaSessionLegacyStub.this.isAllowedCommand(controllerInfo, sessionCommand2)) {
                            command = (SessionCommand2) MediaSessionLegacyStub.sCommandsForOnCommandRequest.get(sessionCommand2.getCommandCode());
                        } else {
                            return;
                        }
                    } else if (MediaSessionLegacyStub.this.isAllowedCommand(controllerInfo, i)) {
                        command = (SessionCommand2) MediaSessionLegacyStub.sCommandsForOnCommandRequest.get(i);
                    } else {
                        return;
                    }
                    if (command == null || MediaSessionLegacyStub.this.mSession.getCallback().onCommandRequest(MediaSessionLegacyStub.this.mSession.getInstance(), controllerInfo, command)) {
                        try {
                            session2Runnable.run(controllerInfo);
                        } catch (RemoteException e) {
                            str = MediaSessionLegacyStub.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Exception in ");
                            stringBuilder.append(controllerInfo.toString());
                            Log.w(str, stringBuilder.toString(), e);
                        }
                        return;
                    }
                    if (MediaSessionLegacyStub.DEBUG) {
                        str = MediaSessionLegacyStub.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Command (");
                        stringBuilder.append(command);
                        stringBuilder.append(") from ");
                        stringBuilder.append(controllerInfo);
                        stringBuilder.append(" was rejected by ");
                        stringBuilder.append(MediaSessionLegacyStub.this.mSession);
                        Log.d(str, stringBuilder.toString());
                    }
                }
            });
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void removeControllerInfo(ControllerInfo controller) {
        synchronized (this.mLock) {
            controller = (ControllerInfo) this.mControllers.remove(controller.getId());
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("releasing ");
                stringBuilder.append(controller);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    private ControllerInfo createControllerInfo(Bundle extras) {
        IMediaControllerCallback callback = Stub.asInterface(BundleCompat.getBinder(extras, "android.support.v4.media.argument.ICONTROLLER_CALLBACK"));
        return new ControllerInfo(extras.getString("android.support.v4.media.argument.PACKAGE_NAME"), extras.getInt("android.support.v4.media.argument.PID"), extras.getInt("android.support.v4.media.argument.UID"), new ControllerLegacyCb(callback));
    }

    private void connect(Bundle extras, final ResultReceiver cb) {
        final ControllerInfo controllerInfo = createControllerInfo(extras);
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            public void run() {
                if (!MediaSessionLegacyStub.this.mSession.isClosed()) {
                    synchronized (MediaSessionLegacyStub.this.mLock) {
                        MediaSessionLegacyStub.this.mConnectingControllers.add(controllerInfo.getId());
                    }
                    SessionCommandGroup2 allowedCommands = MediaSessionLegacyStub.this.mSession.getCallback().onConnect(MediaSessionLegacyStub.this.mSession.getInstance(), controllerInfo);
                    boolean accept = allowedCommands != null || controllerInfo.isTrusted();
                    MediaItem2 currentMediaItem = null;
                    if (accept) {
                        SessionCommandGroup2 allowedCommands2;
                        if (MediaSessionLegacyStub.DEBUG) {
                            String str = MediaSessionLegacyStub.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Accepting connection, controllerInfo=");
                            stringBuilder.append(controllerInfo);
                            stringBuilder.append(" allowedCommands=");
                            stringBuilder.append(allowedCommands);
                            Log.d(str, stringBuilder.toString());
                        }
                        if (allowedCommands == null) {
                            allowedCommands2 = new SessionCommandGroup2();
                        } else {
                            allowedCommands2 = allowedCommands;
                        }
                        synchronized (MediaSessionLegacyStub.this.mLock) {
                            MediaSessionLegacyStub.this.mConnectingControllers.remove(controllerInfo.getId());
                            MediaSessionLegacyStub.this.mControllers.put(controllerInfo.getId(), controllerInfo);
                            MediaSessionLegacyStub.this.mAllowedCommandGroupMap.put(controllerInfo, allowedCommands2);
                        }
                        Bundle resultData = new Bundle();
                        resultData.putBundle("android.support.v4.media.argument.ALLOWED_COMMANDS", allowedCommands2.toBundle());
                        resultData.putInt("android.support.v4.media.argument.PLAYER_STATE", MediaSessionLegacyStub.this.mSession.getPlayerState());
                        resultData.putInt("android.support.v4.media.argument.BUFFERING_STATE", MediaSessionLegacyStub.this.mSession.getBufferingState());
                        resultData.putParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT", MediaSessionLegacyStub.this.mSession.getPlaybackStateCompat());
                        resultData.putInt("android.support.v4.media.argument.REPEAT_MODE", MediaSessionLegacyStub.this.mSession.getRepeatMode());
                        resultData.putInt("android.support.v4.media.argument.SHUFFLE_MODE", MediaSessionLegacyStub.this.mSession.getShuffleMode());
                        List<MediaItem2> playlist = allowedCommands2.hasCommand(18) ? MediaSessionLegacyStub.this.mSession.getPlaylist() : null;
                        if (playlist != null) {
                            resultData.putParcelableArray("android.support.v4.media.argument.PLAYLIST", MediaUtils2.convertMediaItem2ListToParcelableArray(playlist));
                        }
                        if (allowedCommands2.hasCommand(20)) {
                            currentMediaItem = MediaSessionLegacyStub.this.mSession.getCurrentMediaItem();
                        }
                        if (currentMediaItem != null) {
                            resultData.putBundle("android.support.v4.media.argument.MEDIA_ITEM", currentMediaItem.toBundle());
                        }
                        resultData.putBundle("android.support.v4.media.argument.PLAYBACK_INFO", MediaSessionLegacyStub.this.mSession.getPlaybackInfo().toBundle());
                        MediaMetadata2 playlistMetadata = MediaSessionLegacyStub.this.mSession.getPlaylistMetadata();
                        if (playlistMetadata != null) {
                            resultData.putBundle("android.support.v4.media.argument.PLAYLIST_METADATA", playlistMetadata.toBundle());
                        }
                        if (!MediaSessionLegacyStub.this.mSession.isClosed()) {
                            cb.send(0, resultData);
                        } else {
                            return;
                        }
                    }
                    synchronized (MediaSessionLegacyStub.this.mLock) {
                        MediaSessionLegacyStub.this.mConnectingControllers.remove(controllerInfo.getId());
                    }
                    if (MediaSessionLegacyStub.DEBUG) {
                        String str2 = MediaSessionLegacyStub.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Rejecting connection, controllerInfo=");
                        stringBuilder2.append(controllerInfo);
                        Log.d(str2, stringBuilder2.toString());
                    }
                    cb.send(-1, null);
                }
            }
        });
    }

    private void disconnect(Bundle extras) {
        final ControllerInfo controllerInfo = createControllerInfo(extras);
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            public void run() {
                if (!MediaSessionLegacyStub.this.mSession.isClosed()) {
                    MediaSessionLegacyStub.this.mSession.getCallback().onDisconnected(MediaSessionLegacyStub.this.mSession.getInstance(), controllerInfo);
                }
            }
        });
    }
}
