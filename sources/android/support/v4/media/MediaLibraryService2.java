package android.support.v4.media;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaSession2.ControllerInfo;
import android.support.v4.media.MediaSession2.SessionCallback;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class MediaLibraryService2 extends MediaSessionService2 {
    public static final String SERVICE_INTERFACE = "android.media.MediaLibraryService2";

    public static final class LibraryRoot {
        public static final String EXTRA_OFFLINE = "android.media.extra.OFFLINE";
        public static final String EXTRA_RECENT = "android.media.extra.RECENT";
        public static final String EXTRA_SUGGESTED = "android.media.extra.SUGGESTED";
        private final Bundle mExtras;
        private final String mRootId;

        public LibraryRoot(@NonNull String rootId, @Nullable Bundle extras) {
            if (rootId != null) {
                this.mRootId = rootId;
                this.mExtras = extras;
                return;
            }
            throw new IllegalArgumentException("rootId shouldn't be null");
        }

        public String getRootId() {
            return this.mRootId;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }
    }

    public static final class MediaLibrarySession extends MediaSession2 {

        public static final class Builder extends BuilderBase<MediaLibrarySession, Builder, MediaLibrarySessionCallback> {
            public Builder(@NonNull MediaLibraryService2 service, @NonNull Executor callbackExecutor, @NonNull MediaLibrarySessionCallback callback) {
                super(service);
                setSessionCallback(callbackExecutor, callback);
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
            public MediaLibrarySession build() {
                if (this.mCallbackExecutor == null) {
                    this.mCallbackExecutor = new MainHandlerExecutor(this.mContext);
                }
                if (this.mCallback == null) {
                    this.mCallback = new MediaLibrarySessionCallback() {
                    };
                }
                return new MediaLibrarySession(this.mContext, this.mId, this.mPlayer, this.mPlaylistAgent, this.mVolumeProvider, this.mSessionActivity, this.mCallbackExecutor, this.mCallback);
            }
        }

        public static class MediaLibrarySessionCallback extends SessionCallback {
            @Nullable
            public LibraryRoot onGetLibraryRoot(@NonNull MediaLibrarySession session, @NonNull ControllerInfo controller, @Nullable Bundle rootHints) {
                return null;
            }

            @Nullable
            public MediaItem2 onGetItem(@NonNull MediaLibrarySession session, @NonNull ControllerInfo controller, @NonNull String mediaId) {
                return null;
            }

            @Nullable
            public List<MediaItem2> onGetChildren(@NonNull MediaLibrarySession session, @NonNull ControllerInfo controller, @NonNull String parentId, int page, int pageSize, @Nullable Bundle extras) {
                return null;
            }

            public void onSubscribe(@NonNull MediaLibrarySession session, @NonNull ControllerInfo controller, @NonNull String parentId, @Nullable Bundle extras) {
            }

            public void onUnsubscribe(@NonNull MediaLibrarySession session, @NonNull ControllerInfo controller, @NonNull String parentId) {
            }

            public void onSearch(@NonNull MediaLibrarySession session, @NonNull ControllerInfo controller, @NonNull String query, @Nullable Bundle extras) {
            }

            @Nullable
            public List<MediaItem2> onGetSearchResult(@NonNull MediaLibrarySession session, @NonNull ControllerInfo controller, @NonNull String query, int page, int pageSize, @Nullable Bundle extras) {
                return null;
            }
        }

        interface SupportLibraryImpl extends SupportLibraryImpl {
            MediaLibrarySessionCallback getCallback();

            MediaLibrarySession getInstance();

            IBinder getLegacySessionBinder();

            void notifyChildrenChanged(@NonNull ControllerInfo controllerInfo, @NonNull String str, int i, @Nullable Bundle bundle);

            void notifyChildrenChanged(@NonNull String str, int i, @Nullable Bundle bundle);

            void notifySearchResultChanged(@NonNull ControllerInfo controllerInfo, @NonNull String str, int i, @Nullable Bundle bundle);

            void onGetChildrenOnExecutor(@NonNull ControllerInfo controllerInfo, @NonNull String str, int i, int i2, @Nullable Bundle bundle);

            void onGetItemOnExecutor(@NonNull ControllerInfo controllerInfo, @NonNull String str);

            void onGetLibraryRootOnExecutor(@NonNull ControllerInfo controllerInfo, @Nullable Bundle bundle);

            void onGetSearchResultOnExecutor(@NonNull ControllerInfo controllerInfo, @NonNull String str, int i, int i2, @Nullable Bundle bundle);

            void onSearchOnExecutor(@NonNull ControllerInfo controllerInfo, @NonNull String str, @Nullable Bundle bundle);

            void onSubscribeOnExecutor(@NonNull ControllerInfo controllerInfo, @NonNull String str, @Nullable Bundle bundle);

            void onUnsubscribeOnExecutor(@NonNull ControllerInfo controllerInfo, @NonNull String str);
        }

        MediaLibrarySession(Context context, String id, BaseMediaPlayer player, MediaPlaylistAgent playlistAgent, VolumeProviderCompat volumeProvider, PendingIntent sessionActivity, Executor callbackExecutor, SessionCallback callback) {
            super(context, id, player, playlistAgent, volumeProvider, sessionActivity, callbackExecutor, callback);
        }

        /* Access modifiers changed, original: 0000 */
        public SupportLibraryImpl createImpl(Context context, String id, BaseMediaPlayer player, MediaPlaylistAgent playlistAgent, VolumeProviderCompat volumeProvider, PendingIntent sessionActivity, Executor callbackExecutor, SessionCallback callback) {
            return new MediaLibrarySessionImplBase(this, context, id, player, playlistAgent, volumeProvider, sessionActivity, callbackExecutor, callback);
        }

        /* Access modifiers changed, original: 0000 */
        public SupportLibraryImpl getImpl() {
            return (SupportLibraryImpl) super.getImpl();
        }

        public void notifyChildrenChanged(@NonNull ControllerInfo controller, @NonNull String parentId, int itemCount, @Nullable Bundle extras) {
            getImpl().notifyChildrenChanged(controller, parentId, itemCount, extras);
        }

        public void notifyChildrenChanged(@NonNull String parentId, int itemCount, @Nullable Bundle extras) {
            getImpl().notifyChildrenChanged(parentId, itemCount, extras);
        }

        public void notifySearchResultChanged(@NonNull ControllerInfo controller, @NonNull String query, int itemCount, @Nullable Bundle extras) {
            getImpl().notifySearchResultChanged(controller, query, itemCount, extras);
        }

        /* Access modifiers changed, original: 0000 */
        public MediaLibrarySessionCallback getCallback() {
            return (MediaLibrarySessionCallback) super.getCallback();
        }
    }

    @NonNull
    public abstract MediaLibrarySession onCreateSession(String str);

    /* Access modifiers changed, original: 0000 */
    public SupportLibraryImpl createImpl() {
        return new MediaLibraryService2ImplBase();
    }

    public void onCreate() {
        super.onCreate();
        if (!(getSession() instanceof MediaLibrarySession)) {
            throw new RuntimeException("Expected MediaLibrarySession, but returned MediaSession2");
        }
    }

    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
}
