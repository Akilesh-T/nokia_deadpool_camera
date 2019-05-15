package com.google.android.apps.photos.api;

import android.net.Uri;
import com.hmdglobal.app.camera.R;

public final class IconQuery {
    public static final String MATCH_PATH_BADGE = "icon/#/badge";
    public static final String MATCH_PATH_DIALOG = "icon/#/dialog";
    public static final String MATCH_PATH_EDITOR = "icon/#/editor";
    public static final String MATCH_PATH_ICON_BASE = "icon/#";
    public static final String MATCH_PATH_INTERACT = "icon/#/interact";
    public static final String MATCH_PATH_SEARCH = "icon/#/search";
    public static final String PATH_ICON = "icon";

    public enum Type {
        BADGE(Configuration.BADGE, R.dimen.badge_icon_size),
        INTERACT(Configuration.INTERACT, R.dimen.interact_icon_size),
        DIALOG("dialog", R.dimen.interact_icon_size),
        SEARCH("search", R.dimen.search_icon_size),
        EDITOR("editor", R.dimen.external_editor_icon_size);
        
        private final int dimensionResourceId;
        private final String path;

        private Type(String path, int dimensionResourceId) {
            this.path = path;
            this.dimensionResourceId = dimensionResourceId;
        }

        public String getPath() {
            return this.path;
        }

        public int getDimensionResourceId() {
            return this.dimensionResourceId;
        }
    }

    public static Uri getUriForBadgeIcon(Uri iconUri) {
        return getUriForType(iconUri, Type.BADGE);
    }

    public static Uri getUriForInteractIcon(Uri iconUri) {
        return getUriForType(iconUri, Type.INTERACT);
    }

    public static Uri getUriForSearchIcon(Uri iconUri) {
        return getUriForType(iconUri, Type.SEARCH);
    }

    public static Uri getUriForDialogIcon(Uri iconUri) {
        return getUriForType(iconUri, Type.DIALOG);
    }

    public static Uri getUriForEditorIcon(Uri iconUri) {
        return getUriForType(iconUri, Type.EDITOR);
    }

    private static Uri getUriForType(Uri iconUri, Type type) {
        return iconUri.buildUpon().appendPath(type.getPath()).build();
    }
}
